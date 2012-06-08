/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGridTableWriter.java
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
package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * @author illenberger
 *
 */
public class SpatialGridTableWriter {
	
	public static final String separator = "\t";

	public void write(SpatialGrid grid, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for(int j = 0; j < grid.getNumCols(0); j++) {
			writer.write(SpatialGridTableWriter.separator);
			writer.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
		}
		writer.newLine();
		
		for(int i = grid.getNumRows() - 1; i >=0 ; i--) {
			writer.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
			for(int j = 0; j < grid.getNumCols(i); j++) {
				writer.write(SpatialGridTableWriter.separator);
				Double val = grid.getValue(i, j);
				if(!Double.isNaN(val))
					writer.write(String.valueOf(val));
				else
					writer.write("NA");
			}
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
}
