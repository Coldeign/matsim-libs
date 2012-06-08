package playground.tnicolai.matsim4opus.matsim4urbansim.archive;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;

public class SpatialGridTableWriterERSA_V2 {

	private static final Logger log = Logger.getLogger(SpatialGridTableWriterERSA_V2.class);

	private static final String SQUARE_X_COORD = "x_coord_square";
	private static final String SQUARE_Y_COORD = "y_coord_square";
	private static final String CENTROID_ACCESSIBILITY = "centroid_accessibility";
	private static final String MEAN_ACCESSIBILITY = "mean_accessibility";
	private static final String DERIVATION_ACCESSIBILITY = "derivation_accessibility";
	
	public static void writeTableAndCSV(final SpatialGridOld<SquareLayer> travelTimeAccessibilityGrid,
										final SpatialGridOld<SquareLayer> travelCostAccessibilityGrid,
										final SpatialGridOld<SquareLayer> travelDistanceAccessibilityGrid,
										final Map<Id, Double>travelTimeAccessibilityMap,
										final Map<Id, Double>travelCostAccessibilityMap,
										final Map<Id, Double>travelDistanceAccessibilityMap,
										final double resolutionMeter){

		assert (travelTimeAccessibilityGrid != null);
		assert (travelDistanceAccessibilityGrid != null);
		assert (travelCostAccessibilityGrid != null);
		assert (travelTimeAccessibilityMap != null);
		assert (travelDistanceAccessibilityMap != null);
		assert (travelCostAccessibilityMap != null);

		initSpatialGridsAndDumpCSV(travelTimeAccessibilityGrid, travelCostAccessibilityGrid, travelDistanceAccessibilityGrid,
						 travelTimeAccessibilityMap, travelCostAccessibilityMap, travelDistanceAccessibilityMap);

		log.info("Writing spatial grid tables ...");

		try {
			// Travel Time Accessibility Table
			log.info("Writing Travel Time Accessibility Measures ...");
			writeTable(travelTimeAccessibilityGrid,
					InternalConstants.MATSIM_4_OPUS_TEMP
							+ InternalConstants.TRAVEL_TIME_ACCESSIBILITY
							+ "_GridSize_" + resolutionMeter);
			// Travel Distance Accessibility Table
			log.info("Writing Travel Distance Accessibility Measures ...");
			writeTable(travelDistanceAccessibilityGrid,
					InternalConstants.MATSIM_4_OPUS_TEMP
							+ InternalConstants.TRAVEL_DISTANCE_ACCESSIBILITY
							+ "_GridSize_" + resolutionMeter);
			// Travel Cost Accessibility Table
			log.info("Writing Travel Cost Accessibility Measures ...");
			writeTable(travelCostAccessibilityGrid,
					InternalConstants.MATSIM_4_OPUS_TEMP
							+ InternalConstants.TRAVEL_COST_ACCESSIBILITY
							+ "_GridSize_" + resolutionMeter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("... done writing spatial grid tables!");
	}

	private static void initSpatialGridsAndDumpCSV(final SpatialGridOld<SquareLayer> travelTimeAccessibilityGrid,
											final SpatialGridOld<SquareLayer> travelCostAccessibilityGrid,
											final SpatialGridOld<SquareLayer> travelDistanceAccessibilityGrid,
											final Map<Id, Double>travelTimeAccessibilityMap,
											final Map<Id, Double>travelCostAccessibilityMap,
											final Map<Id, Double>travelDistanceAccessibilityMap) {

		log.info("Initialize spatial grid tables and write them as csv ...");

		// compute derivation ...
		initAndDump(travelCostAccessibilityGrid,travelCostAccessibilityMap, "travel_cost");
		initAndDump(travelDistanceAccessibilityGrid,travelDistanceAccessibilityMap, "travel_distance");
		initAndDump(travelTimeAccessibilityGrid,travelTimeAccessibilityMap, "travel_time");

		log.info("...done filling spatial grids!");
	}

	private static void initAndDump(final SpatialGridOld<SquareLayer> grid, final Map<Id, Double> map, final String type) {

		try{
			BufferedWriter csvWriter = IOUtils
					.getBufferedWriter(InternalConstants.MATSIM_4_OPUS_TEMP + type
							+ "_square_accessibility_indicators_v2"
							+ InternalConstants.FILE_TYPE_CSV);
			// create header
			csvWriter.write(CENTROID_ACCESSIBILITY + ","
							+ MEAN_ACCESSIBILITY + ","
							+ DERIVATION_ACCESSIBILITY + ","
							+ SQUARE_X_COORD + ","
							+ SQUARE_Y_COORD);
			csvWriter.newLine();

			int rows = grid.getNumRows();
			int cols = grid.getNumCols(0);
			log.info("Grid Rows: " + rows + " Grid Columns: " + cols);

			int total = 0;
			int skipped = 0;

			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					total++;
					SquareLayer layer = grid.getValue(r, c);

					if (layer != null) {
						// init
						layer.computeDerivation(map);

						if(layer.getSquareCentroidCoord() != null){
							csvWriter.write(layer.getCentroidAccessibility() + ","
										+ layer.getMeanAccessibility() + ","
										+ layer.getAccessibilityDerivation() + ","
										+ layer.getSquareCentroidCoord().getX() + ","
										+ layer.getSquareCentroidCoord().getY());
							csvWriter.newLine();
						}
						else
							skipped++;
					}
				}
			}
			log.info("Processed " + total + " squares " + skipped + " of them where skipped!");
			csvWriter.flush();
			csvWriter.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private static void writeTable(final SpatialGridOld<SquareLayer> grid, final String fileName) throws IOException {

		BufferedWriter layer1 = IOUtils.getBufferedWriter(fileName + "_Centroid" + InternalConstants.FILE_TYPE_TXT);
		BufferedWriter layer2 = IOUtils.getBufferedWriter(fileName + "_Interpolated" + InternalConstants.FILE_TYPE_TXT);
		BufferedWriter layer3 = IOUtils.getBufferedWriter(fileName + "_Derivation" + InternalConstants.FILE_TYPE_TXT);

		for(int j = 0; j < grid.getNumCols(0); j++) {
			layer1.write("\t");
			layer1.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
			layer2.write("\t");
			layer2.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
			layer3.write("\t");
			layer3.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
		}
		layer1.newLine();
		layer2.newLine();
		layer3.newLine();

		for(int i = grid.getNumRows() - 1; i >=0 ; i--) {
			layer1.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
			layer2.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
			layer3.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));

			for(int j = 0; j < grid.getNumCols(i); j++) {
				layer1.write("\t");
				Double centroid = grid.getValue(i, j).getCentroidAccessibility();
				if(centroid != null)
					layer1.write(String.valueOf(centroid));
				else
					layer1.write("NA");

				layer2.write("\t");
				Double interpolation = grid.getValue(i, j).getMeanAccessibility();
				if(interpolation != null)
					layer2.write(String.valueOf(interpolation));
				else
					layer2.write("NA");

				layer3.write("\t");
				Double derivation = grid.getValue(i, j).getAccessibilityDerivation();
				if(derivation != null)
					layer3.write(String.valueOf(derivation));
				else
					layer3.write("NA");
			}
			layer1.newLine();
			layer2.newLine();
			layer3.newLine();
		}
		layer1.flush();
		layer1.close();
		layer2.flush();
		layer2.close();
		layer3.flush();
		layer3.close();
	}
}
