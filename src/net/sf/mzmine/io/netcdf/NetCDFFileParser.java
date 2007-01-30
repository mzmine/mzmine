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

package net.sf.mzmine.io.netcdf;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;

/**
 * This is a helper class used for parsing NetCDF files
 */
public class NetCDFFileParser {

	private File originalFile;
	private ucar.nc2.NetcdfFile inputFile;

	private int totalScans;
	private Hashtable<Integer, Integer[]> scansIndex;
	private Hashtable<Integer, Double> scansRetentionTimes;

	private ucar.nc2.Variable massValueVariable;
	private ucar.nc2.Variable intensityValueVariable;

	NetCDFFileParser(File originalFile) {
		this.originalFile = originalFile;
	}

	/**
	 * Opens the netcdf file
	 */
	public void openFile() throws IOException {

		// Open NetCDF-file
		try {
			inputFile = new ucar.nc2.NetcdfFile(originalFile.getPath());
		} catch (Exception e) {
			// Logger.putFatal(e.toString());
			throw (new IOException("Couldn't open input file" + originalFile));
		}

		
		
	}


	/**
	 * Closes the netcdf file
	 */
	public void closeFile() throws IOException {

		// Open NetCDF-file
		try {
			inputFile = new ucar.nc2.NetcdfFile(originalFile.getPath());
		} catch (Exception e) {
			// Logger.putFatal(e.toString());
			throw (new IOException("Couldn't open input file" + originalFile));
		}

	}

	/**
	 * Reads general information from the file
	 */
	public void readGeneralInformation() throws IOException {

		
		// Find mass_values and intensity_values variables
		massValueVariable = inputFile.findVariable("mass_values");
		if (massValueVariable == null) {
			// Logger.putFatal("Could not find variable mass_values");
			throw (new IOException("Could not find variable mass_values"));
		}

		intensityValueVariable = inputFile.findVariable("intensity_values");
		if (intensityValueVariable == null) {
			// Logger.putFatal("Could not find variable intensity_values");
			throw (new IOException("Could not find variable intensity_values"));
		}				
		
		// Read number of scans
		ucar.nc2.Variable scanIndexVariable = inputFile.findVariable("scan_index");
		if (scanIndexVariable == null) {
			// Logger.putFatal("Could not find variable scan_index from file " + originalFile);
			throw (new IOException("Could not find variable scan_index from file " + originalFile));
		}
		totalScans = scanIndexVariable.getShape()[0];



		// Read scan start positions
		int[] scanStartPositions = new int[totalScans+1]; // Extra element is required, because element totalScans+1 is used to find the stop position for last scan

		ucar.ma2.Array scanIndexArray = null;
		try {
			scanIndexArray = scanIndexVariable.read();
		} catch (Exception e) {
			// Logger.putFatal(e.toString());
			throw (new IOException("Could not read from variable scan_index from file " + originalFile));
		}

		ucar.ma2.IndexIterator scanIndexIterator = scanIndexArray.getIndexIterator();
		int ind = 0;
		while (scanIndexIterator.hasNext()) {
			scanStartPositions[ind] = ((Integer)scanIndexIterator.next()).intValue();
			ind++;
		}
		scanIndexIterator = null; scanIndexArray = null; scanIndexVariable = null;

		// Calc stop position for the last scan
		scanStartPositions[totalScans] = (int)massValueVariable.getSize();	// This defines the end index of the last scan

		// Read retention times
		double[] retentionTimes = new double[totalScans];

		ucar.nc2.Variable scanTimeVariable = inputFile.findVariable("scan_acquisition_time");
		if (scanTimeVariable == null) {
			// Logger.putFatal("Could not find variable scan_acquisition_time from file " + originalFile);
			throw (new IOException("Could not find variable scan_acquisition_time from file " + originalFile));
		}
		ucar.ma2.Array scanTimeArray = null;
		try {
			scanTimeArray = scanTimeVariable.read();
		} catch (Exception e) {
			// Logger.putFatal(e.toString());
			throw (new IOException("Could not read from variable scan_acquisition_time from file " + originalFile));
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
						// Logger.putFatal("ERROR: Could not fix incorrect QStar scan times.");
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

		// Collect information about retention times, start positions and lengths for scans
		scansRetentionTimes = new Hashtable<Integer, Double>();
		scansIndex = new Hashtable<Integer, Integer[]>();
		for (int i=0; i<totalScans; i++) {

			Integer scanNum = new Integer(i);

			Integer[] startAndLength = new Integer[2];
			startAndLength[0] = scanStartPositions[i];
			startAndLength[1] = scanStartPositions[i+1] - scanStartPositions[i];

			scansRetentionTimes.put(scanNum, new Double(retentionTimes[i]));
			scansIndex.put(scanNum, startAndLength);

		}

		scanStartPositions = null;
		retentionTimes = null;

	}


	/**
	 * Reads one scan from the file.
	 * Requires that general information has already been read.
	 */
	public Scan parseScan(int scanNum) throws IOException {

		// Get scan starting position and length
		int[] scanStartPosition = new int[1];
		int[] scanLength = new int[1];
		Integer[] startAndLength = scansIndex.get(new Integer(scanNum));
		if (startAndLength==null) {
			// Logger.putFatal("Could not find scan start position and length for scan " + scanNum);
			throw (new IOException("Could not find scan start position and length for scan " + scanNum));
		}
		scanStartPosition[0] = startAndLength[0];
		scanLength[0] = startAndLength[1];


		// Get retention time of the scan
		Double retentionTime = scansRetentionTimes.get(new Integer(scanNum));
		if (retentionTime==null) {
			// Logger.putFatal("Could not find retention time for scan " + scanNum);
			throw (new IOException("Could not find retention time for scan " + scanNum));
		}


		// An empty scan needs some special attention..
		if (scanLength[0]==0) {
            return null;
		}

/*		
		// Find mass_values and intensity_values variables
		ucar.nc2.Variable massValueVariable = inputFile.findVariable("mass_values");
		if (massValueVariable == null) {
			// Logger.putFatal("Could not find variable mass_values");
			throw (new IOException("Could not find variable mass_values"));
		}

		ucar.nc2.Variable intensityValueVariable = inputFile.findVariable("intensity_values");
		if (intensityValueVariable == null) {
			// Logger.putFatal("Could not find variable intensity_values");
			throw (new IOException("Could not find variable intensity_values"));
		}
*/
		// Read mass and intensity values
		ucar.ma2.Array massValueArray;
		ucar.ma2.Array intensityValueArray;
		try {
			massValueArray = massValueVariable.read(scanStartPosition, scanLength);
			intensityValueArray = intensityValueVariable.read(scanStartPosition, scanLength);
		} catch (Exception e) {
			// Logger.putFatal("Could not read from variables mass_values and/or intensity_values.");
			// Logger.putFatal(e.toString());
			throw (new IOException("Could not read from variables mass_values and/or intensity_values."));
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

		return new SimpleScan(scanNum, 1, retentionTime, -1, 0, null, massValues, intensityValues, false);


	}


	/**
	 * Returns total number of scans
	 */
	public int getTotalScans() {
		return totalScans;
	}





}