/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.ptproject.qsim.qnetsimengine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.ptproject.qsim.InternalInterface;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.qnetsimengine.VehicularDepartureHandler.VehicleBehavior;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
public class QNetsimEngine extends QSimEngineInternalI implements MobsimEngine {

	private static final class NodeIdComparator implements Comparator<QNode>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final QNode o1, final QNode o2) {
			return o1.getNode().getId().compareTo(o2.getNode().getId());
		}
	}


	private static final Logger log = Logger.getLogger(QNetsimEngine.class);

	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one
	 * car is in one of the many queues).
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	/*package*/ static boolean simulateAllLinks = false;
	/*package*/ static boolean simulateAllNodes = false;

	/*
	 * "Classic" behavior is using an array of nodes. However, in many
	 * cases it is faster, if non-active nodes are de-activated. Note that
	 * enabling this option might (slightly) change the simulation results. 
	 * A node that is de-activated and re-activated later will be at another 
	 * position in the list of nodes which are simulated. As a result, different
	 * random numbers will be used when the node is simulated.  
	 * In the future each node should get its own random number generator - then
	 * there is no difference anymore between the results w/o de-activated nodes.
	 */
	/*package*/ static boolean useNodeArray = false;
	/*package*/   QNetwork network;

	/*package*/  List<AbstractQLink> allLinks = null;
	/*package*/  List<QNode> allNodes = null;
	/** This is the collection of links that have to be moved in the simulation */
	/*package*/  List<AbstractQLink> simLinksList = new ArrayList<AbstractQLink>();
	/** This is the collection of nodes that have to be moved in the simulation */
	/*package*/  QNode[] simNodesArray = null;
	/*package*/  List<QNode> simNodesList = null;
	/** This is the collection of links that have to be activated in the current time step */
	/*package*/  ArrayList<AbstractQLink> simActivateLinks = new ArrayList<AbstractQLink>();

	/** This is the collection of nodes that have to be activated in the current time step */
	/*package*/  ArrayList<QNode> simActivateNodes = new ArrayList<QNode>();

	private final Map<Id, QVehicle> vehicles = new HashMap<Id, QVehicle>();

	private final Random random;
	private final QSim qsim;

	private final AgentSnapshotInfoBuilder positionInfoBuilder;

	private final double stucktimeCache;
	private final DepartureHandler dpHandler ;
	
	/*package*/ InternalInterface internalInterface = null ;
	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
	}

	public QNetsimEngine(final QSim sim, final Random random) {
		this.random = random;
		this.qsim = sim;

		this.stucktimeCache = sim.getScenario().getConfig().getQSimConfigGroup().getStuckTime();

		// configuring the car departure hander (including the vehicle behavior)
		QSimConfigGroup qSimConfigGroup = this.qsim.getScenario().getConfig().getQSimConfigGroup();
		VehicleBehavior vehicleBehavior;
		if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VEHICLE_BEHAVIOR_EXCEPTION)) {
			vehicleBehavior = VehicleBehavior.EXCEPTION;
		} else if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VEHICLE_BEHAVIOR_TELEPORT)) {
			vehicleBehavior = VehicleBehavior.TELEPORT;
		} else if (qSimConfigGroup.getVehicleBehavior().equals(QSimConfigGroup.VEHICLE_BEHAVIOR_WAIT)) {
			vehicleBehavior = VehicleBehavior.WAIT_UNTIL_IT_COMES_ALONG;
		} else {
			throw new RuntimeException("Unknown vehicle behavior option.");
		}
		dpHandler = new VehicularDepartureHandler(this, vehicleBehavior);

		// yyyyyy I am quite sceptic if the following should stay since it does not work.  kai, feb'11
		if ( "queue".equals( sim.getScenario().getConfig().getQSimConfigGroup().getTrafficDynamics() ) ) {
			QLinkImpl.HOLES=false ;
		} else if ( "withHolesExperimental".equals( sim.getScenario().getConfig().getQSimConfigGroup().getTrafficDynamics() ) ) {
			QLinkImpl.HOLES = true ;
		} else {
			throw new RuntimeException("trafficDynamics defined in config that does not exist: "
					+ sim.getScenario().getConfig().getQSimConfigGroup().getTrafficDynamics() ) ;
		}
		if (sim.getScenario().getConfig().scenario().isUseLanes()) {
			if (((ScenarioImpl) sim.getScenario()).getLaneDefinitions() == null) {
				throw new IllegalStateException(
						"Lane definitions have to be set if feature is enabled!");
			}
			log.info("Lanes enabled...");
			network = new QNetwork(sim.getScenario().getNetwork(), new QLanesNetworkFactory(new DefaultQNetworkFactory(), ((ScenarioImpl) sim.getScenario()).getLaneDefinitions()));
		} else {
			network = new QNetwork(sim.getScenario().getNetwork(), new DefaultQNetworkFactory());
		}
		network.getLinkWidthCalculator().setLinkWidth(sim.getScenario().getConfig().otfVis().getLinkWidth());
		network.initialize(this);
		
		this.positionInfoBuilder = this.createAgentSnapshotInfoBuilder( sim.getScenario() );
	}

	public void addParkedVehicle(MobsimVehicle veh, Id startLinkId) {
		vehicles.put(veh.getId(), (QVehicle) veh);
		AbstractQLink qlink = network.getNetsimLinks().get(startLinkId);
		qlink.addParkedVehicle(veh);
	}

	private AgentSnapshotInfoBuilder createAgentSnapshotInfoBuilder(Scenario scenario){
		String  snapshotStyle = scenario.getConfig().getQSimConfigGroup().getSnapshotStyle();
		if ("queue".equalsIgnoreCase(snapshotStyle)){
			return new QueueAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
		else  if ("equiDist".equalsIgnoreCase(snapshotStyle)){
			return new EquiDistAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
		else if ("withHolesExperimental".equalsIgnoreCase(snapshotStyle)){
			log.warn("The snapshot style \"withHolesExperimental\" is no longer supported, using \"queue\" instead. ");
			return new QueueAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
		else {
			log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not supported. Using equiDist");
			return new EquiDistAgentSnapshotInfoBuilder(scenario, this.network.getAgentSnapshotInfoFactory());
		}
	}

	@Override
	public void onPrepareSim() {
		this.allLinks = new ArrayList<AbstractQLink>(network.getNetsimLinks().values());
		this.allNodes = new ArrayList<QNode>(network.getNetsimNodes().values());
		if (useNodeArray) {
			this.simNodesArray = network.getNetsimNodes().values().toArray(new QNode[network.getNetsimNodes().values().size()]);
			//dg[april08] as the order of nodes has an influence on the simulation
			//results they are sorted to avoid indeterministic simulations
			Arrays.sort(this.simNodesArray, new NodeIdComparator());			
		} else {
			simNodesList = new ArrayList<QNode>();
			Collections.sort(simNodesList, new NodeIdComparator());
		}
		if (simulateAllLinks) {
			this.simLinksList.addAll(this.allLinks);
		}
	}

	@Override
	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (AbstractQLink link : this.allLinks) {
			link.clearVehicles();
		}
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	@Override
	public void doSimStep(final double time) {
		moveNodes(time);
		moveLinks(time);
	}

	private void moveNodes(final double time) {
		if (useNodeArray) {
			for (QNode node : this.simNodesArray) {
				if (node.isActive() /*|| node.isSignalized()*/ || simulateAllNodes) {
					/* It is faster to first test if the node is active, and only then call moveNode(),
					 * than calling moveNode() directly and that one returns immediately when it's not
					 * active. Most likely, the getter isActive() can be in-lined by the compiler, while
					 * moveNode() cannot, resulting in fewer method-calls when isActive() is used.
					 * -marcel/20aug2008
					 */
					node.moveNode(time, random);
				}
			}			
		} else {
			reactivateNodes();
			ListIterator<QNode> simNodes = this.simNodesList.listIterator();
			QNode node;

			while (simNodes.hasNext()) {
				node = simNodes.next();
				node.moveNode(time, random);

				if (!node.isActive()) simNodes.remove();
			}
		}
	}

	private void moveLinks(final double time) {
		reactivateLinks();
		ListIterator<AbstractQLink> simLinks = this.simLinksList.listIterator();
		AbstractQLink link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.moveLink(time);
			if (!isActive && !simulateAllLinks) {
				simLinks.remove();
			}
		}
	}

	@Override
	protected void activateLink(final AbstractQLink link) {
		if (!simulateAllLinks) {
			this.simActivateLinks.add(link);
		}
	}

	private void reactivateLinks() {
		if ((!simulateAllLinks) && (!this.simActivateLinks.isEmpty())) {
			this.simLinksList.addAll(this.simActivateLinks);
			this.simActivateLinks.clear();
		}
	}

	@Override
	protected void activateNode(QNode node) {
		if (!useNodeArray && !simulateAllNodes) {
			this.simActivateNodes.add(node);
		}
	}

	private void reactivateNodes() {
		if ((!simulateAllNodes) && (!this.simActivateNodes.isEmpty())) {
			this.simNodesList.addAll(this.simActivateNodes);
			this.simActivateNodes.clear();
		}
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		if (useNodeArray) return this.simNodesArray.length;
		else return this.simNodesList.size();
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return this.simLinksList.size();
	}

	@Override
	public QSim getMobsim() {
		return this.qsim;
	}

	AgentSnapshotInfoBuilder getAgentSnapshotInfoBuilder(){
		return this.positionInfoBuilder;
	}

	public NetsimNetwork getNetsimNetwork() {
		return this.network ;
	}

	/**
	 * convenience method so that stuck time can be cached without caching it in every node separately.  kai, jun'10
	 */
	double getStuckTime() {
		return this.stucktimeCache ;
	}

	public DepartureHandler getDepartureHandler() {
		return dpHandler;
	}

	public final Map<Id, QVehicle> getVehicles() {
		return Collections.unmodifiableMap(this.vehicles);
	}

	public final void registerAdditionalAgentOnLink(final MobsimAgent planAgent) {
		Id linkId = planAgent.getCurrentLinkId(); 
		AbstractQLink qLink = network.getNetsimLink(linkId);
		qLink.registerAdditionalAgentOnLink(planAgent);
	}

	public MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId) {
		AbstractQLink qLink = network.getNetsimLink(linkId);
		return qLink.unregisterAdditionalAgentOnLink(agentId);
	}

	void letAgentArrive(QVehicle veh) {
		double now = qsim.getSimTimer().getTimeOfDay();
		MobsimDriverAgent driver = veh.getDriver();
		qsim.getEventsManager().processEvent(qsim.getEventsManager().getFactory().createPersonLeavesVehicleEvent(now, driver.getId(), veh.getId()));		
		driver.endLegAndAssumeControl(now);
		this.internalInterface.arrangeNextAgentState(driver) ;
	}

}
