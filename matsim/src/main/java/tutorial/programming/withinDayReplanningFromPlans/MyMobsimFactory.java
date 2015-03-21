/* *********************************************************************** *
 * project: org.matsim.*
 * MyMobsimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package tutorial.programming.withinDayReplanningFromPlans;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.TripRouter;

/**
 * @author nagel
 *
 */
public class MyMobsimFactory implements MobsimFactory {

	private Provider<TripRouter> tripRouterFactory;

	MyMobsimFactory(Provider<TripRouter> tripRouterFactory) {
		this.tripRouterFactory = tripRouterFactory;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager events) {
		QSim qSim = (QSim) ControlerDefaults.createDefaultQSimFactory().createMobsim(sc, events) ;
		
		qSim.addQueueSimulationListeners(new MyWithinDayMobsimListener(this.tripRouterFactory.get())) ;

		return qSim ;
	}

}
