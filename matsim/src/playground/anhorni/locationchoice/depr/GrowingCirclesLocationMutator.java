/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetSecLoc.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.depr;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class GrowingCirclesLocationMutator extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String H = "h";
	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String EDUCATION = "education";

	private static final String SHOP_0 = "shop_retail_gt2500sqm";
	private static final String SHOP_1 = "shop_retail_get1000sqm";
	private static final String SHOP_2 = "shop_retail_get400sqm";
	private static final String SHOP_3 = "shop_retail_get100sqm";
	private static final String SHOP_4 = "shop_other";

	private static final String LEISURE_0 = "leisure_gastro";
	private static final String LEISURE_1 = "leisure_culture";
	private static final String LEISURE_2 = "leisure_sports";
	
	
	private static final CoordImpl ZERO = new CoordImpl(0.0,0.0);

	private NetworkLayer network;
	//private final Persons persons;

	private QuadTree<ActivityFacility> shopFacQuadTree = null;
	private QuadTree<ActivityFacility> leisFacQuadTree = null;
	private QuadTree<ActivityFacility> educFacQuadTree = null;
	
	private ActivityFacilities facilities = null;  
	private final TreeMap<Id,ActivityFacility> shop_facilities=new TreeMap<Id,ActivityFacility>();
	private final TreeMap<Id,ActivityFacility> leisure_facilities=new TreeMap<Id,ActivityFacility>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public GrowingCirclesLocationMutator(final NetworkLayer network, final ActivityFacilities facilities) {
		super();
		this.init(network, facilities);
	}


	private void init(final NetworkLayer network, final ActivityFacilities facilities) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;//(Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
		this.network=network;
		this.buildShopFacQuadTree();
		this.buildLeisFacQuadTree();
		this.buildEducFacQuadTree();
		
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilitiesForActivityType("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilitiesForActivityType("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilitiesForActivityType("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilitiesForActivityType("leisure_sports"));
		
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	private void buildShopFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building shop facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (final ActivityFacility f : this.shop_facilities.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.shopFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : this.shop_facilities.values()) {
			this.shopFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	private void buildLeisFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building leisure facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (final ActivityFacility f : this.leisure_facilities.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.leisFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : this.leisure_facilities.values()) {
			this.leisFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	private void buildEducFacQuadTree() {
		Gbl.startMeasurement();
		System.out.println("      building education facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (final ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOption(EDUCATION) != null) {
				if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
				if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
				if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
				if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.educFacQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : this.facilities.getFacilities().values()) {
			if (f.getActivityOption(EDUCATION) != null) {
				this.educFacQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final QuadTree<ActivityFacility> getFacilities(final String act_type) {
		if (E.equals(act_type)) { return this.educFacQuadTree; }
		else if (S.equals(act_type)) { return this.shopFacQuadTree; }
		else if (L.equals(act_type)) { return this.leisFacQuadTree; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	private final String getFacilityActType(final String act_type) {
		if (E.equals(act_type)) { return EDUCATION; }
		else if (S.equals(act_type)) { return SHOP_0; }
		else if (L.equals(act_type)) { return LEISURE_0; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	private final ActivityFacility getFacility(final Collection<ActivityFacility> fs, String act_type) {
		act_type = this.getFacilityActType(act_type);
		int i = 0;
		final int[] dist_sum = new int[fs.size()];
		Iterator<ActivityFacility> f_it = fs.iterator();
		ActivityFacility f = f_it.next();
		dist_sum[i] = f.getActivityOption(act_type).getCapacity().intValue();
		if ((dist_sum[i] == 0) || (dist_sum[i] == Integer.MAX_VALUE)) {
			dist_sum[i] = 1;
			f.getActivityOption(act_type).setCapacity(1);
		}
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			int val = f.getActivityOption(act_type).getCapacity().intValue();
			if ((val == 0) || (val == Integer.MAX_VALUE)) {
				val = 1;
				f.getActivityOption(act_type).setCapacity(1);
			}
			dist_sum[i] = dist_sum[i-1] + val;
		}

		final int r = MatsimRandom.getRandom().nextInt(dist_sum[fs.size()-1]);

		i=-1;
		f_it = fs.iterator();
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			if (r < dist_sum[i]) {
				return f;
			}
		}
		Gbl.errorMsg("It should never reach this line!");
		return null;
	}

	private final ActivityFacility getFacility(final Coord coord, final double radius, final String act_type) {
		final Collection<ActivityFacility> fs = this.getFacilities(act_type).get(coord.getX(),coord.getY(),radius);
		if (fs.isEmpty()) {
			if (radius > 200000) { Gbl.errorMsg("radius>200'000 meters and still no facility found!"); }
			return this.getFacility(coord,2.0*radius,act_type);
		}
		return this.getFacility(fs,act_type);
	}

	private final ActivityFacility getFacility(final CoordImpl coord1, final CoordImpl coord2, final double radius, final String act_type) {
		final Collection<ActivityFacility> fs = this.getFacilities(act_type).get(coord1.getX(),coord1.getY(),radius);
		fs.addAll(this.getFacilities(act_type).get(coord2.getX(),coord2.getY(),radius));
		if (fs.isEmpty()) {
			if (radius > 200000) { Gbl.errorMsg("radius>200'000 meters and still no facility found!"); }
			return this.getFacility(coord1,coord2,2.0*radius,act_type);
		}
		return this.getFacility(fs,act_type);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final PersonImpl person) {
		Coord home_coord = null;
		Coord prim_coord = null;
		final PlanImpl plan = person.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				final ActivityImpl act = (ActivityImpl) pe;
				if (act.getType().startsWith(H)) {
					if (act.getCoord() == null) { Gbl.errorMsg("Person id=" + person.getId() + " has no home coord!"); }
					if (act.getCoord().equals(ZERO)) { Gbl.errorMsg("Person id=" + person.getId() + " has a ZERO home coord!"); }
					home_coord = act.getCoord();
				}
				else {
					if ((act.getCoord() != null) && (!act.getCoord().equals(ZERO))) { prim_coord = act.getCoord(); }
				}
			}
		}
		if ((prim_coord == null) || (home_coord.equals(prim_coord))) {
			// only one location

			final double radius=10000.0;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					final ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
						final ActivityFacility f = this.getFacility(home_coord,radius,act.getType());
						act.setLink(this.network.getNearestLink(f.getCoord()));
						act.setCoord(f.getCoord());
					}
				}
			}
		}
		else {
			// two locations
			//
			//           c1               c2
			//    home ---|---|---|---|---|--- prim
			//             \             /
			//              \ r       r /
			//               \         /
			//
			double dx = prim_coord.getX() - home_coord.getX();
			double dy = prim_coord.getY() - home_coord.getY();
			final double radius = Math.sqrt(dx*dx+dy*dy)/3.0;
			dx = dx/6.0;
			dy = dy/6.0;
			final CoordImpl coord1 = new CoordImpl(home_coord.getX()+dx,home_coord.getY()+dy);
			final CoordImpl coord2 = new CoordImpl(prim_coord.getX()-dx,prim_coord.getY()+dy);
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					final ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord() == null) || (act.getCoord().equals(ZERO))) {
						final ActivityFacility f = this.getFacility(coord1,coord2,radius,act.getType());
						act.setLink(this.network.getNearestLink(f.getCoord()));
						act.setCoord(f.getCoord());
					}
				}
			}

			// clear the route
			final List<? extends BasicPlanElement> actslegs = plan.getPlanElements();
			for (int j = 1; j < actslegs.size(); j=j+2) {
				final LegImpl leg = (LegImpl)actslegs.get(j);
				leg.setRoute(null);
			}

		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(final PlanImpl plan) {
	}
}

