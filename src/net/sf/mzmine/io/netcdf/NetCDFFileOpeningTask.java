/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/**
 *
 */
package net.sf.mzmine.io.netcdf;

import java.io.File;
import java.util.Hashtable;

import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.DistributableTask;
import net.sf.mzmine.util.Logger;


/**
 *
 */
public class NetCDFFileOpeningTask implements DistributableTask {

    private File originalFile;
    private TaskStatus status;
    private String errorMessage;

	private int parsedScans;
	private int totalScans;

	private NetCDFFile buildingFile;
	private NetCDFScan buildingScan;



    /**
     *
     */
    public NetCDFFileOpeningTask(File fileToOpen, PreloadLevel preloadLevel) {
        originalFile = fileToOpen;
        status = TaskStatus.WAITING;

        buildingFile = new NetCDFFile(fileToOpen, preloadLevel);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Opening file " + originalFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return totalScans == 0 ? 0 : (float) parsedScans / totalScans;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        return buildingFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getPriority()
     */
    public TaskPriority getPriority() {
        return TaskPriority.NORMAL;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

		// Open NetCDF-file
		ucar.nc2.NetcdfFile inputFile;
		try {
			inputFile = new ucar.nc2.NetcdfFile(originalFile.getPath());
		} catch (Exception e) {
			status = TaskStatus.ERROR;
            errorMessage = "Failed to open file.";
            return;
		}


		// Read number of scans
		ucar.nc2.Variable scanIndexVariable = inputFile.findVariable("scan_index");
		if (scanIndexVariable == null) {
			status = TaskStatus.ERROR;
            errorMessage = "Could not find variable scan_index.";
            return;
		}
		totalScans = scanIndexVariable.getShape()[0];



		// Read scan start positions
		int[] scanStartPositions = new int[totalScans+1]; // Extra element is required, because element totalScans+1 is used to find the stop position for last scan

		ucar.ma2.Array scanIndexArray = null;
		try {
			scanIndexArray = scanIndexVariable.read();
		} catch (Exception e) {
			status = TaskStatus.ERROR;
            errorMessage = "Could not read from variable scan_index.";
            return;

		}

		ucar.ma2.IndexIterator scanIndexIterator = scanIndexArray.getIndexIterator();
		int ind = 0;
		while (scanIndexIterator.hasNext()) {
			scanStartPositions[ind] = ((Integer)scanIndexIterator.next()).intValue();
			ind++;
		}
		scanIndexIterator = null; scanIndexArray = null; scanIndexVariable = null;

		// Calc stop position for the last scan
		ucar.nc2.Variable massValueVariable = inputFile.findVariable("mass_values");
		if (massValueVariable == null) {
			status = TaskStatus.ERROR;
            errorMessage = "Could not find variable mass_values.";
            return;
		}
		scanStartPositions[totalScans] = (int)massValueVariable.getSize();	// This defines the end index of the last scan
		massValueVariable = null;



		// Read retention times
		double[] retentionTimes = new double[totalScans];

		ucar.nc2.Variable scanTimeVariable = inputFile.findVariable("scan_acquisition_time");
		if (scanTimeVariable == null) {
			status = TaskStatus.ERROR;
            errorMessage = "Could not find variable scan_acquisition_time.";
            return;
		}
		ucar.ma2.Array scanTimeArray = null;
		try {
			scanTimeArray = scanTimeVariable.read();
		} catch (Exception e) {
			status = TaskStatus.ERROR;
            errorMessage = "Could not read from variable scan_acquisition_time.";
            return;
		}

		ucar.ma2.IndexIterator scanTimeIterator = scanTimeArray.getIndexIterator();
		ind = 0;
		while (scanTimeIterator.hasNext()) {
			if (scanTimeVariable.getDataType().getPrimitiveClassType() == float.class) {
				retentionTimes[ind] = ((Float)scanTimeIterator.next()).doubleValue();
			}
			if (scanTimeVariable.getDataType().getPrimitiveClassType() == double.class) {
				retentionTimes[ind] = ((Double)scanTimeIterator.next()).doubleValue();
			}
			ind++;
		}

		scanTimeIterator = null; scanTimeArray = null; scanTimeVariable = null;


		// TODO: Read (optional) variable scan_type



		// Fix problems caused by new QStar data converter

		// assume scan is missing when scan_index[i]<0
		// for these scans, fix variables:
		// -	scan_acquisition_time: interpolate/extrapolate using times of present scans
		// -	scan_index: fill with following good value

		// Calculate number of good scans
		int numberOfGoodScans = 0;
		for (int i=0; i<totalScans; i++) {
			if (scanStartPositions[i]>=0) { numberOfGoodScans++; }
		}

		// Is there need to fix something?
		if (numberOfGoodScans<totalScans) {


			// Fix scan_acquisition_time
			// - calculate average delta time between present scans
			double sumDelta=0; int n=0;
			for (int i=0; i<totalScans; i++) {
				// Is this a present scan?
				if (scanStartPositions[i]>=0) {
					// Yes, find next present scan
					for (int j=i+1; j<totalScans; j++) {
						if (scanStartPositions[j]>=0) {
							sumDelta += (retentionTimes[j]-retentionTimes[i])/((double)(j-i));
							n++;
							break;
						}
					}
				}
			}
			double avgDelta=sumDelta/(double)n;
			// - fill missing scan times using nearest good scan and avgDelta
			for (int i=0; i<totalScans; i++) {
				// Is this a missing scan?
				if (scanStartPositions[i]<0) {
					// Yes, find nearest present scan
					int nearestI = Integer.MAX_VALUE;
					for (int j=1; 1<2; j++) {
						if ((i+j)<totalScans) {
							if (scanStartPositions[i+j]>=0) {
								nearestI = i+j;
								break;
							}
						}
						if ((i-j)>=0) {
							if (scanStartPositions[i-j]>=0) {
								nearestI = i+j;
								break;
							}
						}

						// Out of bounds?
						if ( ((i+j)>=totalScans) && ((i-j)<0) ) {
							break;
						}
					}

					if (nearestI!=Integer.MAX_VALUE) {

						retentionTimes[i] = retentionTimes[nearestI] + (i-nearestI) * avgDelta;

					} else {
						if (i>0) { retentionTimes[i] = retentionTimes[i-1]; } else { retentionTimes[i] = 0; }
						Logger.putFatal("ERROR: Could not fix incorrect QStar scan times.");
					}
				}
			}

			// Fix scanStartPositions by filling gaps with next good value
			for (int i=0; i<totalScans; i++) {
				if (scanStartPositions[i]<0) {
					for (int j=i+1; j<(totalScans+1); j++) {
						if (scanStartPositions[j]>=0) {
							scanStartPositions[i] = scanStartPositions[j];
							break;
						}
					}
				}
			}
		}

		Hashtable<Integer, Double> retentionTimeHash = new Hashtable<Integer, Double>();
		for (int i=0; i<totalScans; i++) {
			retentionTimeHash.put(new Integer(i), new Double(retentionTimes[i]));
		}
		buildingFile.addRetentionTimes(retentionTimeHash);


		// Get datapoint variables
		massValueVariable = inputFile.findVariable("mass_values");
		ucar.nc2.Variable intensityValueVariable = inputFile.findVariable("intensity_values");
		if (intensityValueVariable==null) {
			status = TaskStatus.ERROR;
            errorMessage = "Could not find variable intensity_values.";
            return;
		}

		// Initialize variables for scan starting position and length
		int[] scanStartPosition = new int[1];
		int[] scanLength = new int[1];

		// Parse the scans
		for (int i=0; i<totalScans; i++) {

			// Get starting position and shape for this scan
			scanStartPosition[0] = scanStartPositions[i];
			scanLength[0] = scanStartPositions[i+1] - scanStartPositions[i];

			if (scanLength[0]==0) {
				// An empty scan needs some special attention..
				double[] massValues = new double[0];
				double[] intensityValues = new double[0];
				buildingScan = new NetCDFScan(i, retentionTimes[i], massValues, intensityValues);
			} else {

				// Read mass and intensity values
				ucar.ma2.Array massValueArray;
				ucar.ma2.Array intensityValueArray;
				try {
					massValueArray = massValueVariable.read(scanStartPosition, scanLength);
					intensityValueArray = intensityValueVariable.read(scanStartPosition, scanLength);
				} catch (Exception e) {
					Logger.putFatal("Could not read from variables mass_values and/or intensity_values.");
					Logger.putFatal(e.toString());
					status = TaskStatus.ERROR;
					errorMessage = "Could not read from variables mass_values and/or intensity_values.";
					return;
				}

				// Translate values to plain Java arrays

				double[] massValues = null;

				if (massValueVariable.getDataType().getPrimitiveClassType() == double.class) {
					massValues = (double[])massValueArray.copyTo1DJavaArray();
				}

				if (massValueVariable.getDataType().getPrimitiveClassType() == float.class) {
					float[] floatMassValues = (float[])massValueArray.copyTo1DJavaArray();
					massValues = new double[floatMassValues.length];
					for (int j=0; j<massValues.length; j++) { massValues[j] = (double)(floatMassValues[j]); }
					floatMassValues = null;
				}

				if (massValueVariable.getDataType().getPrimitiveClassType() == short.class) {
					short[] shortMassValues = (short[])massValueArray.copyTo1DJavaArray();
					massValues = new double[shortMassValues.length];
					for (int j=0; j<massValues.length; j++) { massValues[j] = (double)(shortMassValues[j]); }
					shortMassValues = null;
				}

				if (massValueVariable.getDataType().getPrimitiveClassType() == int.class) {
					int[] intMassValues = (int[])massValueArray.copyTo1DJavaArray();
					massValues = new double[intMassValues.length];
					for (int j=0; j<massValues.length; j++) { massValues[j] = (double)(intMassValues[j]); }
					intMassValues = null;
				}


				double[] intensityValues = null;

				if (intensityValueVariable.getDataType().getPrimitiveClassType() == double.class) {
					intensityValues = (double[])intensityValueArray.copyTo1DJavaArray();
				}

				if (intensityValueVariable.getDataType().getPrimitiveClassType() == float.class) {
					float[] floatIntensityValues = (float[])intensityValueArray.copyTo1DJavaArray();
					intensityValues = new double[floatIntensityValues.length];
					for (int j=0; j<intensityValues.length; j++) { intensityValues[j] = (float)(floatIntensityValues[j]); }
					floatIntensityValues = null;
				}

				if (intensityValueVariable.getDataType().getPrimitiveClassType() == short.class) {
					short[] shortIntensityValues = (short[])intensityValueArray.copyTo1DJavaArray();
					intensityValues = new double[shortIntensityValues.length];
					for (int j=0; j<intensityValues.length; j++) { intensityValues[j] = (double)(shortIntensityValues[j]); }
					shortIntensityValues = null;
				}

				if (intensityValueVariable.getDataType().getPrimitiveClassType() == int.class) {
					int[] intIntensityValues = (int[])intensityValueArray.copyTo1DJavaArray();
					intensityValues = new double[intIntensityValues.length];
					for (int j=0; j<intensityValues.length; j++) {	intensityValues[j] = (double)(intIntensityValues[j]); }
					intIntensityValues = null;
				}

				buildingScan = new NetCDFScan(i, retentionTimes[i], massValues, intensityValues);

				massValueArray = null; intensityValueArray = null;
				massValues = null; intensityValues = null;

			}

			buildingFile.addIndexEntry(i, scanStartPosition[0], scanLength[0]);
			buildingFile.addScan(buildingScan);
			parsedScans++;
			buildingScan = null;

		}



		// Close the raw data file
		try {
			inputFile.close();
		} catch (Exception e) {
			status = TaskStatus.ERROR;
            errorMessage = "Failed to close file.";
            return;
		}

		status = TaskStatus.FINISHED;



    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
		status = TaskStatus.CANCELED;
    }

}
