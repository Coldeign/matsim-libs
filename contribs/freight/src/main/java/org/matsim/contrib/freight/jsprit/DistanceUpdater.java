package org.matsim.contrib.freight.jsprit;

import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.algorithm.state.StateUpdater;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

	/**
	 * Given class for working with the a distance constraint
	 *
	 */
	public class DistanceUpdater implements StateUpdater, ActivityVisitor {

		private final StateManager stateManager;

		private final StateId distanceStateId;

		private VehicleRoute vehicleRoute;

		private double distance = 0.;

		private TourActivity prevAct;

		private final NetworkBasedTransportCosts netBasedCosts;

		public DistanceUpdater(StateId distanceStateId, StateManager stateManager,
				NetworkBasedTransportCosts netBasedCosts) {
			this.stateManager = stateManager;
			this.distanceStateId = distanceStateId;
			this.netBasedCosts = netBasedCosts;
		}

		@Override
		public void begin(VehicleRoute vehicleRoute) {
			distance = 0.;
			prevAct = vehicleRoute.getStart();
			this.vehicleRoute = vehicleRoute;
		}

		@Override
		public void visit(TourActivity tourActivity) {
			distance += getDistance(prevAct, tourActivity);
			prevAct = tourActivity;
		}

		@Override
		public void finish() {
			distance += getDistance(prevAct, vehicleRoute.getEnd());
			stateManager.putRouteState(vehicleRoute, distanceStateId, distance);
		}

		double getDistance(TourActivity from, TourActivity to) {
			return netBasedCosts.getTransportDistance(from.getLocation(), to.getLocation(), 0, null, null);
		}
	}


