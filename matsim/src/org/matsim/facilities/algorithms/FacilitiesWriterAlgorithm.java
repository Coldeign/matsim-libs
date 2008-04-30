/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesWriterAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.facilities.algorithms;

import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;

/**
 * Use this facilities writer when streaming facilities.
 * 
 * @author meisterk
 *
 */
public class FacilitiesWriterAlgorithm extends FacilityAlgorithm {

	private FacilitiesWriter facilitiesWriter = null;
	
	public FacilitiesWriterAlgorithm(Facilities facilities) {
		super();
		this.facilitiesWriter = new FacilitiesWriter(facilities);
		this.facilitiesWriter.writeOpenAndinit();
	}
	
	@Override
	public void run(Facility facility) {
		this.facilitiesWriter.writeFacility(facility);
	}

	public void finish() {
		this.facilitiesWriter.writeFinish();
	}
	
}
