/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.obsoletedatastructures;
import java.io.File;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import net.iharder.xmlizable.Base64;
import net.sf.mzmine.methods.filtering.MZXMLHandlerForPreload;
import net.sf.mzmine.methods.filtering.MZXMLHandlerForRetrieve;
import net.sf.mzmine.util.Logger;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.ma2.IndexIterator;
import ucar.nc2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;


/**
 * This class represent raw data file as it is stored on a node
 */
public class RawDataAtNode {
	public static final int FILETYPE_UNDETERMINED=-1;
	public static final int FILETYPE_UNKNOWN=-2;
	public static final int FILETYPE_NETCDF=1;
	public static final int FILETYPE_MZXML=2;

	// Properties of the data file
	private int rawDataID;							// rawDataID (unique for every raw data file in whole cluster)
	private File originalRawDataFile;				// Original raw data file
	private File workingCopy;						// Working copy of the raw data file
	private int fileType = FILETYPE_UNDETERMINED;	// File type of raw data file (Netcdf, ?mzXML?, ?something else?)

	// Properties of the raw data
	private int numberOfScans;						// Number of MS-scans in the raw data file
	private double[] scanTimes;						// Times of scans (in secs)
	private double minMZValue;						// Minimum and maximum m/z value in the raw data file
	private double maxMZValue;
	private double maxIntensityValue;				// Maximum intensity value in the raw data file
	private boolean maxIntensityFound = false;		// True when min and max have been determined (For NetCDF files it is necessary to first do one read through the file)
	private boolean dataModified = false;			// True when working copy of the raw data file has been altered (due to filtering)

	// Scan browsing
	private int browsingStartScan;
	private int browsingStopScan;


	// Properties specific to a raw data file in NetCDF-format.
	private NetcdfFile netcdf_file;				// Object for reading a raw data file

	private DataType netcdf_mzDataType;			// These store datatypes that are used for storing mz and intensity measurements in the netcdf file
	private DataType netcdf_intensityDataType;
	private Variable netcdf_mzVariable;				// These store variable containing mz and intensity measurements
	private Variable netcdf_intensityVariable;

	private int[] netcdf_scanStartingPoints;		// List of scan starting indices in mz and intensity measurement arrays
	private int netcdf_currentScanNumber;			// Current scan number in scan browsing
	private int[] netcdf_origin;					// Cursor position in file
	private int[] netcdf_shape;					// Area from cursor to forward
	private double[] netcdf_scanMZRangeMin;		// MZ range minimum for each scan
	private double[] netcdf_scanMZRangeMax;		// MZ range maximum for each scan


	// Properties specific to a raw data file in mzXML-format
	private Vector<Integer> mzxml_ms1Scans;
	private Hashtable<Integer, Long> mzxml_scanOffsets;
	private int mzxml_currentScanNumber;
	private MZXMLHandlerForRetrieve mzxml_scanHandler;

	private FileWriter mzxml_newWorkingCopyWriter;
	private long mzxml_newWorkingCopyBytesWritten;
	private int mzxml_newWorkingCopyScansWritten;
	private Vector<Integer> mzxml_newMS1Scans;
	private Hashtable<Integer, Long> mzxml_newScanOffsets;

	private DatatypeFactory mzxml_datatypeFactory;

	// These are used for temporary storing of data file and raw data properties when file is being modified
	private File newWorkingCopy;					// New working copy file
	private Vector<Double> newScanTimes;
	private double newMinMZValue;
	private double newMaxMZValue;
	private double newMaxIntensityValue;

	private NetcdfFileWriteable netcdf_fileOut;		// Netcdf writer for new working copy
	private DataType netcdf_newMZDataType;
	private DataType netcdf_newIntensityDataType;
	private int[] netcdf_fileOutOrigin;			// Cursor position in output file
	private Vector<Integer> netcdf_fileOutScanOffsets;
	private Vector<Double> netcdf_fileOutScanTimes;
	private Vector<Double> netcdf_fileOutScanMinMZs;
	private Vector<Double> netcdf_fileOutScanMaxMZs;





	/**
	 * Constructor
	 * @param _dataFilePath	Filepath to the origianl raw data file (relative to common datastructure starting point)
	 * @param _dataFileName Filename of the original raw data file
	 */
	public RawDataAtNode(int _rawDataID, File _originalRawDataFile) {
		rawDataID = _rawDataID;
		originalRawDataFile = _originalRawDataFile;
	}


	/**
	 * Returns the filename of original raw data file without leading path
	 */
	public String getNiceName() {
		return originalRawDataFile.getName();
	}


	/**
	 * Returns the raw data ID of this raw data file
	 */
	public int getRawDataID() {
		return rawDataID;
	}

	/*
	 * Sets the file pointing to the working copy of the raw data file represented by this RawDataAtNode object
	 */
	public void setWorkingCopy(File f) {
		workingCopy = f;
	}

	/**
	 * Returns the working copy
	 */
	public File getWorkingCopy() {
		return workingCopy;
	}

	/**
	 * Returns the number of scans
	 */
	public int getNumberOfScans() {
		return numberOfScans;
	}

	/**
	 * Returns scan times
	 */
	public double[] getScanTimes() {
		return scanTimes;
	}

	/**
	 * Returns time of a single scan
	 */
	public double getScanTime(int scanNumber) {
		return scanTimes[scanNumber];
	}

	/**
	 * Returns number of scan that is closest to given time
	 * (uses inefficient search method...)
	 */
	public int getScanNumberByTime(double rt) {
		for (int scanNumber=0; scanNumber<scanTimes.length; scanNumber++) {
			if (scanTimes[scanNumber]>=rt) { return scanNumber; }
		}
		return (scanTimes.length-1);
	}


	/**
	 * Returns true if max intensity has been reliably found
	 */
	public boolean getDataMaxIntensityFound() {
		return maxIntensityFound;
	}

	/**
	 * Return maximum intensity
	 */
	public double getDataMaxIntensity() {
		return maxIntensityValue;
	}

	/**
	 * Returns overall minimum mz value
	 */
	public double getMinMZValue() {
		return minMZValue;
	}

	/**
	 * Returns overall maximum mz value
	 */
	public double getMaxMZValue() {
		return maxMZValue;
	}


	/**
	 * Returns number of datapoints
	 * Note: This function currently return -1 for mzXML files, but it doesn't matter because return value is only used for NetCDF-files (to speed up file writing)
	 */
	public int getNumberOfDatapoints() {
		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}

		if (fileType == FILETYPE_NETCDF) {
			return netcdf_scanStartingPoints[netcdf_scanStartingPoints.length-1];
		}
		if (fileType == FILETYPE_MZXML) {
			return -1;
		}

		return -1;
	}

	/**
	 * Return modified status
	 */
	public boolean isModified() {
		return dataModified;
	}

	public double calculateTotalRawSignal() {
		double ans = 0;

		initializeScanBrowser(0, getNumberOfScans()-1);
		for (int i=0; i<getNumberOfScans(); i++) {
			Scan s = getNextScan();
			ans += s.getTotalIonCurrent();
		}

		finalizeScanBrowser();

		return ans;
	}



	/**
	 * Preloads some important details about the data
	 * @return	1 = preload ok, -1 = preload failed
	 */
	public int preLoad() {
		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}
		if (fileType == FILETYPE_UNKNOWN) {
			return -1;
		}
		if (fileType == FILETYPE_NETCDF) {
			int retval = preLoadNetCDF();
			return retval;
		}
		if (fileType == FILETYPE_MZXML) {
			int retval = preLoadMZXML();
			return retval;
		}
		return -1;
	}



	/* Methods for reading through the scans */


	/**
	 * Initializes scan browser to retrieve scans in given range
	 * @param	startScan	First scan to retrieve
	 * @param	stopScan	Last scan to retrieve
	 */
	public void initializeScanBrowser(int startScan, int stopScan) {

		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}
		if (fileType == FILETYPE_NETCDF) {
			initializeScanBrowserNetCDF(startScan, stopScan);
		}

		if (fileType == FILETYPE_MZXML) {
			initializeScanBrowserMZXML(startScan, stopScan);
		}

		browsingStartScan = startScan;
		browsingStopScan = stopScan;
	}

	/**
	 * Returns next scan
	 */
	public Scan getNextScan() {

		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}

		Scan s = null;
		if (fileType == FILETYPE_NETCDF) {
			s = getNextScanNetCDF();
		}

		if (fileType == FILETYPE_MZXML) {
			s = getNextScanMZXML();
		}

		if (!maxIntensityFound) {
			double tmpInt = s.getMaxIntensity();
			if (tmpInt>maxIntensityValue) { maxIntensityValue = tmpInt; }
		}

		return s;
	}

	/**
	 * Cleans up after browsing through scans
	 */
	public void finalizeScanBrowser() {

		if ( (browsingStartScan==0) && (browsingStopScan==(numberOfScans-1)) ) {
			maxIntensityFound = true;
		}

		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}
		if (fileType == FILETYPE_NETCDF) {
			finalizeScanBrowserNetCDF();
		}
		if (fileType == FILETYPE_MZXML) {
			finalizeScanBrowserMZXML();
		}

	}



	/* Methods for writing modified scans to new file */


	/**
	 * Prepares a preloaded raw data file for writing modifications to it
	 * @param 	expectedNumberOfDatapoints	How many datapoints there will be written to file (-1 if unknown).
	 * @param 	expectedNumberOfScans		How many scans there will be written to file (-1)
	 *
	 * NOTE! When writing NetCDF-files, both of these can't be -1. For mzXML-files it doesn't matter.
	 *
	 * @return	1 = file ready for writing scans,	-1 = preparing failed
	 */
	public int initializeForWriting(int expectedNumberOfDatapoints, int expectedNumberOfScans) {

		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}
		if (fileType == FILETYPE_UNKNOWN) {
			return -1;
		}

		// For writing NETCDF-file, it is necessary to know either number of scans or datapoints in advance (to avoid error with Multiple UnlimitedDimensions)
		if ( (fileType == FILETYPE_NETCDF) && (expectedNumberOfDatapoints==-1) && (expectedNumberOfScans==-1)) {
			return -1;
		}

		// Create new temp file
		try {
			newWorkingCopy = File.createTempFile("MZmine", null);
			newWorkingCopy.deleteOnExit();
		} catch (Exception e) {
			Logger.putFatal("Could not create new temporary mzxml file!");
			return -1;
		}


		if (fileType == FILETYPE_NETCDF) {
			int retval = openNetCDFFileForWriting(expectedNumberOfDatapoints, expectedNumberOfScans);
			return retval;
		}
		if (fileType == FILETYPE_MZXML) {
			int retval = openMZXMLFileForWriting();
			return retval;
		}

		return -1;

	}



	/**
	 * Prepares a preloaded raw data file for writing modifications to it
	 *
	 * @return	1 = file ready for writing scans,	-1 = preparing failed
	 */
	public int initializeForWriting() {

		return initializeForWriting(-1,-1);
	}



	/**
	 * Sets a scan to raw data file (replaces old intensities with new ones)
	 */
	public int setScan(Scan s) {

		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}

		if (fileType == FILETYPE_NETCDF) {
			return setScanNetCDF(s);
		}

		if (fileType == FILETYPE_MZXML) {
			return setScanMZXML(s);
		}

		return -1;

	}

	/**
	 * Finishes writing to a raw data file
	 */
	public int finalizeAfterWriting() {

		if (fileType == FILETYPE_UNDETERMINED) {
			fileType = checkFileType();
		}
		if (fileType == FILETYPE_UNKNOWN) {
			return -1;
		}

		if (fileType == FILETYPE_NETCDF) {
			int retval = closeNetCDFFileForWriting();
			return retval;
		}
		if (fileType == FILETYPE_MZXML) {
			int retval = closeMZXMLFileForWriting();
			return retval;
		}

		return -1;

	}


	// *********************************** Methods for different file formats ***********************************

	/**
	 * Determine filetype.
	 */
	public int checkFileType() {


		// Extract file extension from the file path
		String name = originalRawDataFile.getName();

		StringTokenizer st = new StringTokenizer(name, new String("."));

		String fileNameExtension = null;
		while (st.hasMoreTokens()) { fileNameExtension = st.nextToken(); }

		if (fileNameExtension!=null) {
			if ( (fileNameExtension.equalsIgnoreCase("mzxml")) || (fileNameExtension.equalsIgnoreCase("xml")) ) {
				return FILETYPE_MZXML;
			}
		}

		// Assume NetCDF if not mzXML
		return FILETYPE_NETCDF;
	}



	// *********************************************** Methods for NetCDF format *************************************


	/**
	 * Returns a array of minimum MZ values (for each scan)
	 */
	public double[] netdf_getScanMZRangeMins() { return netcdf_scanMZRangeMin; }
	public double[] netdf_getScanMZRangeMaxs() { return netcdf_scanMZRangeMax; }


	/**
	 * Preloads netcdf-file
	 */
	private int preLoadNetCDF() {

		/*
		Preload tasks. Determine...
		- number of scans
		- min and max m/z value
		- scan start times
		- initialize min and max intensity value (these will be determined exactly later during the first read through of the file)

		Netcdf specific tasks. Determine
		- scan starting indices is mz and intensity measurement point arrays
		- datatypes used for representing mz and intensity measurements
		*/


		// Open NetCDF-file
		netcdf_file = openNetCDFFile(workingCopy);
		if (netcdf_file==null) {
			return -1;
		}


		// Read number of scans
		Variable scanIndexVariable = netcdf_file.findVariable("scan_index");
		if (scanIndexVariable == null) {
			Logger.put("Scan index variable is missing from NetCDF-file. Unable to load");
			return -1;
		}
		numberOfScans = scanIndexVariable.getShape()[0];


		// Read scan starting points in whole measurement arrays (NetCDF specific)
		netcdf_scanStartingPoints = new int[numberOfScans+1]; // Extra element is required, because element i+1 is used to find the stop index corresponding to start index value at element i

		Array scanIndexArray = null;
		try {
			scanIndexArray = scanIndexVariable.read();
		} catch (Exception e) {
			return -1;
		}

		IndexIterator scanIndexIterator = scanIndexArray.getIndexIterator();
		int ind = 0;
		while (scanIndexIterator.hasNext()) {
			netcdf_scanStartingPoints[ind] = ((Integer)scanIndexIterator.next()).intValue();
			ind++;
		}
		scanIndexIterator = null;
		scanIndexArray = null;
		scanIndexVariable = null;

		netcdf_mzVariable = netcdf_file.findVariable("mass_values");
		if (netcdf_mzVariable == null) {
			Logger.put("Mass values array is missing from NetCDF-file. Unable to load");
			return -1;
		}
		netcdf_scanStartingPoints[numberOfScans] = (int)netcdf_mzVariable.getSize();	// This defines the end index of the last scan
		netcdf_mzVariable = null;


		// Read scan starting times
		scanTimes = new double[numberOfScans];

		Variable scanTimeVariable = netcdf_file.findVariable("scan_acquisition_time");
		if (scanTimeVariable == null) {
			Logger.put("Scan acquisition time arary is missing from NetCDF-file. Unable to load");
			return -1;
		}
		Array scanTimeArray = null;
		try {
			scanTimeArray = scanTimeVariable.read();
		} catch (Exception e) {
			return -1;
		}

		IndexIterator scanTimeIterator = scanTimeArray.getIndexIterator();
		ind = 0;
		while (scanTimeIterator.hasNext()) {
			if (scanTimeVariable.getDataType().getPrimitiveClassType() == float.class) {
				scanTimes[ind] = ((Float)scanTimeIterator.next()).doubleValue();
			}
			if (scanTimeVariable.getDataType().getPrimitiveClassType() == double.class) {
				scanTimes[ind] = ((Double)scanTimeIterator.next()).doubleValue();
			}
			ind++;
		}


		scanTimeIterator = null;
		scanTimeArray = null;
		scanTimeVariable = null;


		// Examine m/z range (min and max)


		Variable mzRangeVariable = netcdf_file.findVariable("mass_range_min");
		if (mzRangeVariable == null) {
			Logger.put("Mass range minimum values are missing from NetCDF-file. Unable to load");
			return -1;
		}
		netcdf_scanMZRangeMin = new double[numberOfScans];

		Array dataVals;
		try {
			dataVals = mzRangeVariable.read();
		} catch (Exception e) {
			return -1;
		}
		IndexIterator dataValsIterator = dataVals.getIndexIterator();
		double f = 0;
		ind = 0;

		while (dataValsIterator.hasNext()) {
			if (mzRangeVariable.getDataType().getPrimitiveClassType() == float.class) {
				f = ((Float)dataValsIterator.next()).floatValue();
			}
			if (mzRangeVariable.getDataType().getPrimitiveClassType() == double.class) {
				f = ((Double)dataValsIterator.next()).doubleValue();
			}

			netcdf_scanMZRangeMin[ind] = f;
			ind++;

		}

		dataValsIterator = null;
		dataVals = null;
		mzRangeVariable = null;

		mzRangeVariable = netcdf_file.findVariable("mass_range_max");
		if (mzRangeVariable == null) {
			Logger.put("Mass range maximum values are missing from NetCDF-file. Unable to load");
			return -1;
		}

		netcdf_scanMZRangeMax = new double[numberOfScans];
		try {
			dataVals = mzRangeVariable.read();
		} catch (Exception e) {
			return -1;
		}
		dataValsIterator = dataVals.getIndexIterator();
		ind = 0;
		while (dataValsIterator.hasNext()) {
			if (mzRangeVariable.getDataType().getPrimitiveClassType() == float.class) {
				f = ((Float)dataValsIterator.next()).doubleValue();
			}
			if (mzRangeVariable.getDataType().getPrimitiveClassType() == double.class) {
				f = ((Double)dataValsIterator.next()).doubleValue();
			}

			netcdf_scanMZRangeMax[ind] = f;
			ind++;
		}
		dataValsIterator = null;
		dataVals = null;
		mzRangeVariable = null;


		// Fix problems caused by new QStar data converter
		// -----------------------------------------------
		// assume scan is missing when scan_index[i]<0
		// for these scans, fix variables:
		// mass_range_min/max:	fill with average in all present scans
		// scan_acquisition_time: interpolate/extrapolate using times of present scans
		// scan_index: fill with following good value

		// Calculate number of good scans
		int numberOfGoodScans = 0;
		for (int i=0; i<numberOfScans; i++) {
			if (netcdf_scanStartingPoints[i]>=0) { numberOfGoodScans++; }
		}

		// Is there need to fix something?
		if (numberOfGoodScans<numberOfScans) {

			// Fix netcdf_scanMZRangeMin/Max by replacing gaps with average of good values
			double sumMin=0; double sumMax=0; int n=0;
			for (int i=0; i<numberOfScans; i++) {
				if ( netcdf_scanStartingPoints[i]>=0 ) {
					sumMin += netcdf_scanMZRangeMin[i];
					sumMin += netcdf_scanMZRangeMax[i];
					n++;
				}
			}
			double avgMin = sumMin/(double)n;
			double avgMax = sumMax/(double)n;
			for (int i=0; i<numberOfScans; i++) {
				if (netcdf_scanStartingPoints[i]<0) {
					netcdf_scanMZRangeMin[i] = avgMin;
					netcdf_scanMZRangeMax[i] = avgMax;
				}
			}

			// Fix scan_acquisition_time
			// - calculate average delta time between present scans
			double sumDelta=0; n=0;
			for (int i=0; i<numberOfScans; i++) {
				// Is this a present scan?
				if (netcdf_scanStartingPoints[i]>=0) {
					// Yes, find next present scan
					for (int j=i+1; j<numberOfScans; j++) {
						if (netcdf_scanStartingPoints[j]>=0) {
							sumDelta += (scanTimes[j]-scanTimes[i])/((double)(j-i));
							n++;
							break;
						}
					}
				}
			}
			double avgDelta=sumDelta/(double)n;
			// - fill missing scan times using nearest good scan and avgDelta
			for (int i=0; i<numberOfScans; i++) {
				// Is this a missing scan?
				if (netcdf_scanStartingPoints[i]<0) {
					// Yes, find nearest present scan
					int nearestI = Integer.MAX_VALUE;
					for (int j=1; 1<2; j++) {
						if ((i+j)<numberOfScans) {
							if (netcdf_scanStartingPoints[i+j]>=0) {
								nearestI = i+j;
								break;
							}
						}
						if ((i-j)>=0) {
							if (netcdf_scanStartingPoints[i-j]>=0) {
								nearestI = i+j;
								break;
							}
						}

						// Out of bounds?
						if ( ((i+j)>=numberOfScans) && ((i-j)<0) ) {
							break;
						}
					}

					if (nearestI!=Integer.MAX_VALUE) {

						scanTimes[i] = scanTimes[nearestI] + (i-nearestI) * avgDelta;

					} else {
						if (i>0) { scanTimes[i] = scanTimes[i-1]; } else { scanTimes[i] = 0; }
						Logger.putFatal("ERROR: Could not fix incorrect scan times.");
					}
				}
			}






			// Fix scan_index by filling gaps with next good value
			for (int i=0; i<numberOfScans; i++) {
				if (netcdf_scanStartingPoints[i]<0) {
					for (int j=i+1; j<(numberOfScans+1); j++) {
						if (netcdf_scanStartingPoints[j]>=0) {
							netcdf_scanStartingPoints[i] = netcdf_scanStartingPoints[j];
							break;
						}
					}
				}
			}



		}







		// Calculate overall min and max m/z value (must be done after fixing QStar problems)
		minMZValue = Double.MAX_VALUE;
		maxMZValue = 0;
		for (int i=0; i<numberOfScans; i++) {
			if (minMZValue>netcdf_scanMZRangeMin[i]) { minMZValue=netcdf_scanMZRangeMin[i]; }
			if (maxMZValue<netcdf_scanMZRangeMax[i]) { maxMZValue=netcdf_scanMZRangeMax[i]; }
		}



		// Examine data types used for presenting mz and intensity data
		netcdf_mzVariable = netcdf_file.findVariable("mass_values");
		netcdf_mzDataType = netcdf_mzVariable.getDataType();
		netcdf_mzVariable = null;
		netcdf_intensityVariable = netcdf_file.findVariable("intensity_values");
		netcdf_intensityDataType = netcdf_intensityVariable.getDataType();
		netcdf_intensityVariable = null;


		// Initialize intensity range variables
		maxIntensityValue = Double.MIN_VALUE;
		maxIntensityFound = false;


		// Close the raw data file
		if (closeNetCDFFile(netcdf_file) == -1) {
			return -1;
		}
		netcdf_file = null;

		return 1;
	}




	/**
	 * Initializes for reading scans from NetCDF file
	 * - picks up mz and intensity variables from NetCDF file
	 */
	private void initializeScanBrowserNetCDF(int startScan, int stopScan) {
		// Open NetCDF-file

		// Logger.put("initializeScanBrowserNetCDF, startScan=" + startScan + ", stopScan=" + stopScan);

		netcdf_file = openNetCDFFile(workingCopy);
		if (netcdf_file==null) {
			return;
		}

		// Get variables
		netcdf_mzVariable = netcdf_file.findVariable("mass_values");
		netcdf_intensityVariable = netcdf_file.findVariable("intensity_values");

		// Initialize scan number counter
		netcdf_currentScanNumber = startScan;
		netcdf_origin = new int[1];
		netcdf_shape = new int[1];

	}

	/**
	 * Open a raw data file in NetCDF format for reading
	 * @param	rawDataFile	File object pointing to the raw data file
	 * @return	NetcdfFile object available for reading, null if failed to open the file
	 */
	private NetcdfFile openNetCDFFile(File rawDataFile) {
		NetcdfFile tmpFile;

		try {
			tmpFile = new NetcdfFile(rawDataFile.getPath()); // NetCDF 2.1
		} catch (Exception e) {
			Logger.putFatal("Failed to open temporary NetCDF-file");
			Logger.putFatal(e.toString());
			return null;
		}

		return tmpFile;
	}

	/**
	 *
	 */
	private Scan getNextScanNetCDF() {

		//Logger.put("getNextScanNetCDF, netcdf_currentScanNumber=" + netcdf_currentScanNumber);

		// Find correct cursor location and read area size
		netcdf_origin[0] = netcdf_scanStartingPoints[netcdf_currentScanNumber];
		netcdf_shape[0] = netcdf_scanStartingPoints[netcdf_currentScanNumber+1]-netcdf_scanStartingPoints[netcdf_currentScanNumber];

		//Logger.put("netcdf origin = " + netcdf_origin[0] + ", shape = " + netcdf_shape[0]);


		// An empty scan needs some special attention...
		if (netcdf_shape[0]==0) {
			double[] mzDoubles = new double[0];
			double[] intensityDoubles = new double[0];
			Scan theScan = new Scan(mzDoubles, intensityDoubles, netcdf_currentScanNumber, netcdf_scanMZRangeMin[netcdf_currentScanNumber], netcdf_scanMZRangeMax[netcdf_currentScanNumber]);
			netcdf_currentScanNumber++;
			return theScan;
		}

		// Read mz and intensity measurements
		Array mzArray;
		Array intensityArray;
		try {
			mzArray = netcdf_mzVariable.read(netcdf_origin, netcdf_shape);
			intensityArray = netcdf_intensityVariable.read(netcdf_origin, netcdf_shape);
		} catch (Exception e) {
			return null;
		}

		// Put measurements into double arrays
		double[] mzDoubles = null;
		double[] intensityDoubles = null;

		//Logger.put("netcdf_mzDataType.getPrimitiveClassType()" + netcdf_mzDataType.getPrimitiveClassType());


		if (netcdf_mzDataType.getPrimitiveClassType() == double.class) {

			mzDoubles = (double[])mzArray.copyTo1DJavaArray();
		}
		if (netcdf_mzDataType.getPrimitiveClassType() == float.class) {

			float[] mzFloats = (float[])mzArray.copyTo1DJavaArray();
			mzDoubles = new double[mzFloats.length];
			for (int i=0; i<mzDoubles.length; i++) { mzDoubles[i] = (double)(mzFloats[i]); }
			mzFloats = null;
		}

		if (netcdf_mzDataType.getPrimitiveClassType() == short.class) {
			short[] mzShorts = (short[])mzArray.copyTo1DJavaArray();
			mzDoubles = new double[mzShorts.length];
			for (int i=0; i<mzDoubles.length; i++) { mzDoubles[i] = (double)(mzShorts[i]); }
			mzShorts = null;
		}


		if (netcdf_mzDataType.getPrimitiveClassType() == int.class) {
			int[] mzInts = (int[])mzArray.copyTo1DJavaArray();
			mzDoubles = new double[mzInts.length];
			for (int i=0; i<mzDoubles.length; i++) { mzDoubles[i] = (double)(mzInts[i]); }
			mzInts = null;
		}

		//Logger.put("netcdf_intensityDataType.getPrimitiveClassType()" + netcdf_intensityDataType.getPrimitiveClassType());


		if (netcdf_intensityDataType.getPrimitiveClassType() == double.class) {
			intensityDoubles = (double[])intensityArray.copyTo1DJavaArray();
		}
		if (netcdf_intensityDataType.getPrimitiveClassType() == float.class) {
			float[] intensityFloats = (float[])intensityArray.copyTo1DJavaArray();
			intensityDoubles = new double[intensityFloats.length];
			for (int i=0; i<intensityDoubles.length; i++) {	intensityDoubles[i] = (float)(intensityFloats[i]); }
			intensityFloats = null;
		}


		if (netcdf_intensityDataType.getPrimitiveClassType() == short.class) {
			short[] intensityShorts = (short[])intensityArray.copyTo1DJavaArray();
			intensityDoubles = new double[intensityShorts.length];
			for (int i=0; i<intensityDoubles.length; i++) {	intensityDoubles[i] = (double)(intensityShorts[i]); }
			intensityShorts = null;
		}

		if (netcdf_intensityDataType.getPrimitiveClassType() == int.class) {
			int[] intensityInts = (int[])intensityArray.copyTo1DJavaArray();
			intensityDoubles = new double[intensityInts.length];
			for (int i=0; i<intensityDoubles.length; i++) {	intensityDoubles[i] = (double)(intensityInts[i]); }
			intensityInts = null;
		}

		Scan theScan = new Scan(mzDoubles, intensityDoubles, netcdf_currentScanNumber, netcdf_scanMZRangeMin[netcdf_currentScanNumber], netcdf_scanMZRangeMax[netcdf_currentScanNumber]);

		// Increase scan number counter
		netcdf_currentScanNumber++;

		// Return the scan object
		return theScan;
	}


	/**
	 * Closes given Netcdf file
	 * @param	rawDataFile	NetCDF file to be closed
	 * @return	1 if ok, -1 if failed
	 */
	private int closeNetCDFFile(NetcdfFile tmpFile) {
		try {
			tmpFile.close();
		} catch (Exception e) {
			Logger.putFatal("FAILED to close temporary NetCDF-file");
			Logger.putFatal(e.toString());
			return -1;
		}
		return 1;
	}

	/**
	 *
	 */
	private void finalizeScanBrowserNetCDF() {

		// Close NetCDF-file
		if (closeNetCDFFile(netcdf_file) == -1) {
			return;
		}
		netcdf_file = null;
	}


	/**
	 * Methods for accessing NetCDF-file
	 */
	private int openNetCDFFileForWriting(int expectedNumberOfDatapoints, int expectedNumberOfScans) {

		// Initialize NetCDF object for output
		netcdf_fileOut = new NetcdfFileWriteable();

		// Add dimensions to NetCDF object
		Dimension scanNumberDimension = netcdf_fileOut.addDimension("scan_number", expectedNumberOfScans);
		Dimension pointNumberDimension = netcdf_fileOut.addDimension("point_number", expectedNumberOfDatapoints);
		Dimension[] scanNumberDimensionArray = new Dimension[1]; scanNumberDimensionArray[0] = scanNumberDimension;
		Dimension[] pointNumberDimensionArray = new Dimension[1]; pointNumberDimensionArray[0] = pointNumberDimension;

		// Add variables to NetCDF object
		netcdf_fileOut.addVariable("scan_index", int.class, scanNumberDimensionArray);
		netcdf_fileOut.addVariable("scan_acquisition_time", float.class, scanNumberDimensionArray);
		netcdf_fileOut.addVariable("mass_range_min", float.class, scanNumberDimensionArray);
		netcdf_fileOut.addVariable("mass_range_max", float.class, scanNumberDimensionArray);
		netcdf_fileOut.addVariable("mass_values", float.class, pointNumberDimensionArray);
		netcdf_fileOut.addVariable("intensity_values", float.class, pointNumberDimensionArray);

		// Create NetCDF file
		try {
			netcdf_fileOut.setName(newWorkingCopy.getPath());
			netcdf_fileOut.create();
		} catch (Exception e) {
			Logger.putFatal("Could not open " + newWorkingCopy + " for writing.");
			return -1;
		}

		// Initialize cursor position
		netcdf_fileOutOrigin = new int[1];
		netcdf_fileOutOrigin[0] = 0;

		// Initialize variables for scan specific stuff that is not written to file immediately at setScan
		netcdf_fileOutScanOffsets = new Vector<Integer>();
		netcdf_fileOutScanTimes = new Vector<Double>();
		netcdf_fileOutScanMinMZs = new Vector<Double>();
		netcdf_fileOutScanMaxMZs = new Vector<Double>();

		newScanTimes = new Vector<Double>();
		newMinMZValue = Double.MAX_VALUE;
		newMaxMZValue = Double.MIN_VALUE;
		newMaxIntensityValue = 0;
		netcdf_newMZDataType = DataType.FLOAT;
		netcdf_newIntensityDataType = DataType.FLOAT;

		return 1;

	}



	/**
	 * Writes one scan to raw data file
	 * Overwrites old data, so the scan must have same length as previously. (number of data points in the scan)
	 */
	private int setScanNetCDF(Scan s) {

		// Get new intensity values for this spectum
		double scanTime = scanTimes[s.getScanNumber()];
		double[] massValues = s.getMZValues();
		double[] intensityValues = s.getIntensityValues();

		// Define shape for writing the mz and int values
		int shape[] = new int[1];
		shape[0] = massValues.length;

		double minMZ=0;
		double maxMZ=0;

		// If there are any mass values, then write data to NetCDF-file
		if (massValues.length>0) {

			// Put m/z and int to NetCDF-arrays and monitor for min & max m/z value
			minMZ = Double.MAX_VALUE;
			maxMZ = Double.MIN_VALUE;
			double tmpMZ;
			double tmpInt;
			ucar.ma2.ArrayFloat.D1 intensityValuesArray = new ucar.ma2.ArrayFloat.D1(intensityValues.length);
			ucar.ma2.ArrayFloat.D1 massValuesArray = new ucar.ma2.ArrayFloat.D1(massValues.length);
			for (int ind=0; ind<intensityValues.length; ind++) {
				tmpMZ = massValues[ind];
				tmpInt = intensityValues[ind];

				if (minMZ>tmpMZ) { minMZ = tmpMZ; }
				if (maxMZ<tmpMZ) { maxMZ = tmpMZ; }
				if (newMaxIntensityValue<tmpInt) { newMaxIntensityValue = tmpInt; }

				massValuesArray.set(ind, (float)tmpMZ);
				intensityValuesArray.set(ind, (float)tmpInt);
			}

			// Write m/z and int arrays to NetCDF file
			try {
				netcdf_fileOut.write("mass_values", netcdf_fileOutOrigin, massValuesArray);
				netcdf_fileOut.write("intensity_values", netcdf_fileOutOrigin, intensityValuesArray);
			} catch (Exception e) {
				Logger.putFatal("Could not write m/z & intesity data to netcdf file");
				Logger.putFatal(e.toString());
				return -1;
			}

			// Check if this scan has overall (among all scans) minimum or maximum M/Z
			if (newMinMZValue>minMZ) { newMinMZValue = minMZ; }
			if (newMaxMZValue<maxMZ) { newMaxMZValue = maxMZ; }

		}

		// Store scan's starting offset, retention time and min&max m/z
		netcdf_fileOutScanOffsets.add(new Integer(netcdf_fileOutOrigin[0]));
		netcdf_fileOutScanTimes.add(new Double(scanTime));
		netcdf_fileOutScanMinMZs.add(new Double(minMZ));
		netcdf_fileOutScanMaxMZs.add(new Double(maxMZ));

		// Update cursor position
		netcdf_fileOutOrigin[0] += shape[0];

		return 1;

	}

	private int closeNetCDFFileForWriting() {

		// Write remaining variables: scans' starting offsets, retention times, min&max m/z values
		ucar.ma2.ArrayInt.D1 scanOffsetsArray = new ArrayInt.D1(netcdf_fileOutScanOffsets.size());
		ucar.ma2.ArrayFloat.D1 scanTimesArray = new ucar.ma2.ArrayFloat.D1(netcdf_fileOutScanTimes.size());
		ucar.ma2.ArrayFloat.D1 scanMinMZArray = new ucar.ma2.ArrayFloat.D1(netcdf_fileOutScanMinMZs.size());
		ucar.ma2.ArrayFloat.D1 scanMaxMZArray = new ucar.ma2.ArrayFloat.D1(netcdf_fileOutScanMaxMZs.size());

		for (int ind=0; ind<netcdf_fileOutScanOffsets.size(); ind++) {
			scanOffsetsArray.set(ind, netcdf_fileOutScanOffsets.get(ind).intValue());
			scanTimesArray.set(ind, netcdf_fileOutScanTimes.get(ind).floatValue());
			scanMinMZArray.set(ind, netcdf_fileOutScanMinMZs.get(ind).floatValue());
			scanMaxMZArray.set(ind, netcdf_fileOutScanMaxMZs.get(ind).floatValue());
		}

		// Close CDF file
		try {
			netcdf_fileOut.flush();
			netcdf_fileOut.close();
		} catch (Exception e) {
			return -1;
		}


		// Move new info over old info
		minMZValue = newMinMZValue;
		maxMZValue = newMaxMZValue;
		numberOfScans = netcdf_fileOutScanOffsets.size();

		maxIntensityValue = newMaxIntensityValue;
		maxIntensityFound = true;

		scanTimes = new double[netcdf_fileOutScanOffsets.size()];
		for (int ind=0; ind<netcdf_fileOutScanOffsets.size(); ind++) { scanTimes[ind] = netcdf_fileOutScanTimes.get(ind).doubleValue(); }

		netcdf_mzDataType = netcdf_newMZDataType;
		netcdf_intensityDataType = netcdf_newIntensityDataType;

		// Add one more element scan offsets array ("end index" of last scan)
		netcdf_fileOutScanOffsets.add(new Integer(netcdf_fileOutOrigin[0]));
		netcdf_scanStartingPoints = new int[netcdf_fileOutScanOffsets.size()];
		// Logger.put("Defining netcdf_scanStartingPoints, length=" + netcdf_fileOutScanOffsets.size());
		for (int ind=0; ind<netcdf_fileOutScanOffsets.size(); ind++) { netcdf_scanStartingPoints[ind] = netcdf_fileOutScanOffsets.get(ind).intValue(); }

		// Replace old workingCopy with new one
		File oldWorkingCopy = workingCopy;
		workingCopy = newWorkingCopy;
		newWorkingCopy = null;
		oldWorkingCopy.delete();

		// Free...
		newWorkingCopy = null;
		netcdf_fileOut = null;
		netcdf_fileOutScanOffsets = null;
		netcdf_fileOutScanTimes = null;
		netcdf_fileOutScanMinMZs = null;
		netcdf_fileOutScanMaxMZs = null;
		netcdf_newMZDataType = null;
		netcdf_newIntensityDataType = null;

		return 1;
	}






	// *********************************************** Methods for mzXML format *************************************


	/**
	 * Preloads mzXML-file
	 */
	private int preLoadMZXML() {

		MZXMLHandlerForPreload mzXMLPreloader = new MZXMLHandlerForPreload();
		mzXMLPreloader.preloadFile(workingCopy);

		numberOfScans = mzXMLPreloader.getNumberOfScans();
		minMZValue = mzXMLPreloader.getLowMZ();
		maxMZValue = mzXMLPreloader.getHighMZ();
		maxIntensityValue = Double.MIN_VALUE;
		maxIntensityFound = false;

		scanTimes = mzXMLPreloader.getScanTimes();

		mzxml_ms1Scans = mzXMLPreloader.getScanNumbers();
		mzxml_scanOffsets = mzXMLPreloader.getScanStartPositions();

		return 1;
	}


	/**
	 * Initializes for reading scans from NetCDF file
	 * - picks up mz and intensity variables from NetCDF file
	 */
	private void initializeScanBrowserMZXML(int startScan, int stopScan) {
 		mzxml_currentScanNumber = startScan;
 		mzxml_scanHandler = new MZXMLHandlerForRetrieve(workingCopy, mzxml_ms1Scans, mzxml_scanOffsets);
	}

	/**
	 *
	 */
	private Scan getNextScanMZXML() {

		Scan tmpScan = mzxml_scanHandler.getScan(mzxml_currentScanNumber);
		mzxml_currentScanNumber++;
		return tmpScan;

	}

	/**
	 *
	 */
	private void finalizeScanBrowserMZXML() {
		// Do nothing
		mzxml_scanHandler = null;
	}



	private int openMZXMLFileForWriting() {

		// Initialize variables for storing new scan numbers and file offsets
		// (scan numbers change because everything else than MS^1 scans will be thrown away while writing the new working copy)
		mzxml_newMS1Scans = new Vector<Integer>();
		mzxml_newScanOffsets = new Hashtable<Integer, Long>();

		// Create a DataTypeFactory for converting XML datetypes to seconds
		try {
			mzxml_datatypeFactory = DatatypeFactory.newInstance();
		} catch (Exception e) {
			Logger.put("Could not instantiate DatatypeFactory");
			Logger.put(e.toString());
		}


		// Open writer for the new working copy
		try {
			mzxml_newWorkingCopyWriter = new FileWriter(newWorkingCopy);
		} catch (Exception e) {
			Logger.putFatal("Could not open temporary mzxml file " + newWorkingCopy + "for writing.");
			return -1;
		}
		mzxml_newWorkingCopyBytesWritten = 0;
		mzxml_newWorkingCopyScansWritten = 0;


		// Write xml-file header to the new working copy
		String s;

		// Write column headers
		try {
			s = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
			mzxml_newWorkingCopyWriter.write(s);
			mzxml_newWorkingCopyBytesWritten+= s.length();
			s = "<mzXML>\n";
			mzxml_newWorkingCopyWriter.write(s);
			mzxml_newWorkingCopyBytesWritten+= s.length();
		} catch (Exception e) {
			Logger.put("Could not write to temporary mzXML-file " + newWorkingCopy);
			return -1;
		}

		// Initialize variables which keep track of various things during writing of the file
		newScanTimes = new Vector<Double>();
		newMinMZValue = Double.MAX_VALUE;
		newMaxMZValue = Double.MIN_VALUE;
		newMaxIntensityValue = 0;

		return 1;
	}


	private int setScanMZXML(Scan s) {
		if (s==null) { return -1;}



		// Add scan number to list of MS^1 scan numbers and store current file position
		Integer currentScanNumber = new Integer(mzxml_newWorkingCopyScansWritten++);
		mzxml_newMS1Scans.add(currentScanNumber);
		mzxml_newScanOffsets.put(currentScanNumber, new Long(mzxml_newWorkingCopyBytesWritten));

		newScanTimes.add(scanTimes[s.getScanNumber()]);

		// Find lowMZ and highMZ of the scan
		double[] mzValues = s.getMZValues();
		double[] intValues = s.getIntensityValues();
		double lowMZ = Double.MAX_VALUE;
		double highMZ = Double.MIN_VALUE;
		for (double mz : mzValues) {
			if (lowMZ>mz) { lowMZ = mz; }
			if (highMZ<mz) { highMZ = mz; }
		}
		if (newMinMZValue>lowMZ) { newMinMZValue = lowMZ; }
		if (newMaxMZValue<highMZ) { newMaxMZValue = highMZ; }

		for (double inte : intValues) {
			if (inte>newMaxIntensityValue) { newMaxIntensityValue = inte; }
		}

		// base64 encode mass and intensity values of datapoints
		byte[] tmpArr = new byte[mzValues.length * 4 * 2];
		int tmpArrInd = 0;
		for (int i=0; i<mzValues.length; i++) {
			double mzVal = mzValues[i];
			int intBits = Float.floatToIntBits((float)(mzVal));
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff000000 ) >> 24 );
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff0000 ) >> 16 );
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff00 ) >> 8 );
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff ) );

			double intVal = intValues[i];
			intBits = Float.floatToIntBits((float)(intVal));
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff000000 ) >> 24 );
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff0000 ) >> 16 );
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff00 ) >> 8 );
			tmpArr[tmpArrInd++] = (byte)( ( intBits & 0xff ) );
		}

		String str1;
		String str2 = Base64.encodeBytes(tmpArr);

		// Write scan info and encoded peak data string to file
		try {
			// Format time
			Duration dur = mzxml_datatypeFactory.newDuration( (long) (java.lang.Math.round( 1000 * scanTimes[s.getScanNumber()] ) ) );

			// General Info
			str1 = "<scan num=\"";
			str1 += "" + currentScanNumber + "\" ";
			str1 += "msLevel=\"1\" ";
			str1 += "retentionTime=\"" + dur.toString() + "\">\n";

			mzxml_newWorkingCopyWriter.write(str1);
			mzxml_newWorkingCopyBytesWritten+= str1.length();

			// Peaks
			str1 = "<peaks precision=\"32\" byteOrder=\"network\" pairOrder=\"m/z-int\">";

			mzxml_newWorkingCopyWriter.write(str1);
			mzxml_newWorkingCopyBytesWritten+= str1.length();

			mzxml_newWorkingCopyWriter.write(str2);
			mzxml_newWorkingCopyBytesWritten+= str2.length();


			// Tag-ends
			str1 = "</peaks>\n";
			mzxml_newWorkingCopyWriter.write(str1);
			mzxml_newWorkingCopyBytesWritten+= str1.length();

			str1 = "</scan>\n";
			mzxml_newWorkingCopyWriter.write(str1);
			mzxml_newWorkingCopyBytesWritten+= str1.length();

		} catch (Exception e) {
			Logger.put("Could not write to temporary mzXML-file " + newWorkingCopy);
			return -1;
		}

		return 1;

	}


	private int closeMZXMLFileForWriting() {

		// Write xml-file footer to the new working copy
		String s;
		try {
			s = "</mzXML>\n";
			mzxml_newWorkingCopyWriter.write(s);
			mzxml_newWorkingCopyBytesWritten+= s.length();
		} catch (Exception e) {
			Logger.put("Could not write to temporary mzXML-file " + newWorkingCopy);
			return -1;
		}

		// Trash datatypeFactory
		mzxml_datatypeFactory = null;

		// Close new working copy
		try {
			mzxml_newWorkingCopyWriter.flush();
			mzxml_newWorkingCopyWriter.close();
		} catch (Exception e) {
			Logger.put("Could not close temporary mzXML-file " + newWorkingCopy + "after writing.");
			return -1;
		}

		// Replace old info with new info
		numberOfScans = mzxml_newWorkingCopyScansWritten;
		minMZValue = newMinMZValue;
		maxMZValue = newMaxMZValue;
		maxIntensityValue = newMaxIntensityValue;
		maxIntensityFound = true;

		scanTimes = new double[newScanTimes.size()];
		for (int ind=0; ind<newScanTimes.size(); ind++) { scanTimes[ind] = newScanTimes.get(ind); }

		mzxml_ms1Scans = mzxml_newMS1Scans;
		mzxml_scanOffsets = mzxml_newScanOffsets;

		// Replace old workingCopy with new one
		File oldWorkingCopy = workingCopy;
		workingCopy = newWorkingCopy;
		newWorkingCopy = null;
		oldWorkingCopy.delete();


		return 1;

	}



}