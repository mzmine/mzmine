/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.ScanUtils;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * 
 */
public class NetCDFReadTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private NetcdfFile inputFile;

    private int parsedScans;
    private int totalScans = 0, numberOfGoodScans, scanNum = 0;

    private Hashtable<Integer, Integer[]> scansIndex;
    private Hashtable<Integer, Double> scansRetentionTimes;

    private File file;
    private MZmineProject project;
    private RawDataFileWriter newMZmineFile;
    private RawDataFile finalRawDataFile;

    private Variable massValueVariable, intensityValueVariable;

    // Some software produces netcdf files with a scale factor such as 0.05
    private double massValueScaleFactor = 1;
    private double intensityValueScaleFactor = 1;

    public NetCDFReadTask(MZmineProject project, File fileToOpen,
	    RawDataFileWriter newMZmineFile) {
	this.project = project;
	this.file = fileToOpen;
	this.newMZmineFile = newMZmineFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

	// Update task status
	setStatus(TaskStatus.PROCESSING);
	logger.info("Started parsing file " + file);

	try {

	    // Open file
	    this.startReading();

	    // Parse scans
	    Scan buildingScan;
	    while ((buildingScan = this.readNextScan()) != null) {

		// Check if cancel is requested
		if (isCanceled()) {
		    return;
		}
		// buildingFile.addScan(scan);
		newMZmineFile.addScan(buildingScan);
		parsedScans++;

	    }

	    // Close file
	    this.finishReading();
	    finalRawDataFile = newMZmineFile.finishWriting();
	    project.addFile(finalRawDataFile);

	} catch (Throwable e) {
	    logger.log(Level.SEVERE, "Could not open file " + file.getPath(), e);
	    setErrorMessage(ExceptionUtils.exceptionToString(e));
	    setStatus(TaskStatus.ERROR);
	    return;
	}

	logger.info("Finished parsing " + file + ", parsed " + parsedScans
		+ " scans");

	// Update task status
	setStatus(TaskStatus.FINISHED);

    }

    public String getTaskDescription() {
	return "Opening file " + file;
    }

    public void startReading() throws IOException {

	// Open NetCDF-file
	try {
	    inputFile = NetcdfFile.open(file.getPath());
	} catch (Exception e) {
	    logger.severe(e.toString());
	    throw (new IOException("Couldn't open input file" + file));
	}

	/*
	 * DEBUG: dump all variables for (Variable v : inputFile.getVariables())
	 * { System.out.println("variable " + v.getShortName()); }
	 */

	// Find mass_values and intensity_values variables
	massValueVariable = inputFile.findVariable("mass_values");
	if (massValueVariable == null) {
	    logger.severe("Could not find variable mass_values");
	    throw (new IOException("Could not find variable mass_values"));
	}
	assert (massValueVariable.getRank() == 1);

	Attribute massScaleFacAttr = massValueVariable
		.findAttribute("scale_factor");
	if (massScaleFacAttr != null) {
	    massValueScaleFactor = massScaleFacAttr.getNumericValue()
		    .doubleValue();
	}

	intensityValueVariable = inputFile.findVariable("intensity_values");
	if (intensityValueVariable == null) {
	    logger.severe("Could not find variable intensity_values");
	    throw (new IOException("Could not find variable intensity_values"));
	}
	assert (intensityValueVariable.getRank() == 1);

	Attribute intScaleFacAttr = intensityValueVariable
		.findAttribute("scale_factor");
	if (intScaleFacAttr != null) {
	    intensityValueScaleFactor = intScaleFacAttr.getNumericValue()
		    .doubleValue();
	}

	// Read number of scans
	Variable scanIndexVariable = inputFile.findVariable("scan_index");
	if (scanIndexVariable == null) {
	    logger.severe("Could not find variable scan_index from file "
		    + file);
	    throw (new IOException(
		    "Could not find variable scan_index from file " + file));
	}
	totalScans = scanIndexVariable.getShape()[0];

	// Read scan start positions
	// Extra element is required, because element totalScans+1 is used to
	// find the stop position for last scan
	int[] scanStartPositions = new int[totalScans + 1];

	Array scanIndexArray = null;
	try {
	    scanIndexArray = scanIndexVariable.read();
	} catch (Exception e) {
	    logger.severe(e.toString());
	    throw (new IOException(
		    "Could not read from variable scan_index from file " + file));
	}

	IndexIterator scanIndexIterator = scanIndexArray.getIndexIterator();
	int ind = 0;
	while (scanIndexIterator.hasNext()) {
	    scanStartPositions[ind] = ((Integer) scanIndexIterator.next())
		    .intValue();
	    ind++;
	}
	scanIndexIterator = null;
	scanIndexArray = null;
	scanIndexVariable = null;

	// Calc stop position for the last scan
	// This defines the end index of the last scan
	scanStartPositions[totalScans] = (int) massValueVariable.getSize();

	// Start scan RT
	double[] retentionTimes = new double[totalScans];
	Variable scanTimeVariable = inputFile
		.findVariable("scan_acquisition_time");
	if (scanTimeVariable == null) {
	    logger.severe("Could not find variable scan_acquisition_time from file "
		    + file);
	    throw (new IOException(
		    "Could not find variable scan_acquisition_time from file "
			    + file));
	}
	Array scanTimeArray = null;
	try {
	    scanTimeArray = scanTimeVariable.read();
	} catch (Exception e) {
	    logger.severe(e.toString());
	    throw (new IOException(
		    "Could not read from variable scan_acquisition_time from file "
			    + file));
	}
	IndexIterator scanTimeIterator = scanTimeArray.getIndexIterator();
	ind = 0;
	while (scanTimeIterator.hasNext()) {
	    if (scanTimeVariable.getDataType().getPrimitiveClassType() == float.class) {
		retentionTimes[ind] = ((Double) scanTimeIterator.next()) / 60d;
	    }
	    if (scanTimeVariable.getDataType().getPrimitiveClassType() == double.class) {
		retentionTimes[ind] = ((Double) scanTimeIterator.next()) / 60d;
	    }
	    ind++;
	}
	// End scan RT

	// Cleanup
	scanTimeIterator = null;
	scanTimeArray = null;
	scanTimeVariable = null;

	// Fix problems caused by new QStar data converter
	// assume scan is missing when scan_index[i]<0
	// for these scans, fix variables:
	// - scan_acquisition_time: interpolate/extrapolate using times of
	// present scans
	// - scan_index: fill with following good value

	// Calculate number of good scans
	numberOfGoodScans = 0;
	for (int i = 0; i < totalScans; i++) {
	    if (scanStartPositions[i] >= 0) {
		numberOfGoodScans++;
	    }
	}

	// Is there need to fix something?
	if (numberOfGoodScans < totalScans) {

	    // Fix scan_acquisition_time
	    // - calculate average delta time between present scans
	    double sumDelta = 0;
	    int n = 0;
	    for (int i = 0; i < totalScans; i++) {
		// Is this a present scan?
		if (scanStartPositions[i] >= 0) {
		    // Yes, find next present scan
		    for (int j = i + 1; j < totalScans; j++) {
			if (scanStartPositions[j] >= 0) {
			    sumDelta += (retentionTimes[j] - retentionTimes[i])
				    / ((double) (j - i));
			    n++;
			    break;
			}
		    }
		}
	    }
	    double avgDelta = sumDelta / (double) n;
	    // - fill missing scan times using nearest good scan and avgDelta
	    for (int i = 0; i < totalScans; i++) {
		// Is this a missing scan?
		if (scanStartPositions[i] < 0) {
		    // Yes, find nearest present scan
		    int nearestI = Integer.MAX_VALUE;
		    for (int j = 1; 1 < 2; j++) {
			if ((i + j) < totalScans) {
			    if (scanStartPositions[i + j] >= 0) {
				nearestI = i + j;
				break;
			    }
			}
			if ((i - j) >= 0) {
			    if (scanStartPositions[i - j] >= 0) {
				nearestI = i + j;
				break;
			    }
			}

			// Out of bounds?
			if (((i + j) >= totalScans) && ((i - j) < 0)) {
			    break;
			}
		    }

		    if (nearestI != Integer.MAX_VALUE) {

			retentionTimes[i] = retentionTimes[nearestI]
				+ (i - nearestI) * avgDelta;

		    } else {
			if (i > 0) {
			    retentionTimes[i] = retentionTimes[i - 1];
			} else {
			    retentionTimes[i] = 0;
			}
			logger.severe("ERROR: Could not fix incorrect QStar scan times.");
		    }
		}
	    }

	    // Fix scanStartPositions by filling gaps with next good value
	    for (int i = 0; i < totalScans; i++) {
		if (scanStartPositions[i] < 0) {
		    for (int j = i + 1; j < (totalScans + 1); j++) {
			if (scanStartPositions[j] >= 0) {
			    scanStartPositions[i] = scanStartPositions[j];
			    break;
			}
		    }
		}
	    }
	}

	// Collect information about retention times, start positions and
	// lengths for scans
	scansRetentionTimes = new Hashtable<Integer, Double>();
	scansIndex = new Hashtable<Integer, Integer[]>();
	for (int i = 0; i < totalScans; i++) {

	    Integer scanNum = new Integer(i);

	    Integer[] startAndLength = new Integer[2];
	    startAndLength[0] = scanStartPositions[i];
	    startAndLength[1] = scanStartPositions[i + 1]
		    - scanStartPositions[i];

	    scansRetentionTimes.put(scanNum, new Double(retentionTimes[i]));
	    scansIndex.put(scanNum, startAndLength);

	}

	scanStartPositions = null;
	retentionTimes = null;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFileReader#finishReading()
     */
    public void finishReading() throws IOException {
	inputFile.close();
    }

    /**
     * Reads one scan from the file. Requires that general information has
     * already been read.
     */
    private Scan readNextScan() throws IOException {

	// Get scan starting position and length
	int[] scanStartPosition = new int[1];
	int[] scanLength = new int[1];
	Integer[] startAndLength = scansIndex.get(scanNum);

	// End of file
	if (startAndLength == null) {
	    return null;
	}
	scanStartPosition[0] = startAndLength[0];
	scanLength[0] = startAndLength[1];

	// Get retention time of the scan
	Double retentionTime = scansRetentionTimes.get(scanNum);
	if (retentionTime == null) {
	    logger.severe("Could not find retention time for scan " + scanNum);
	    throw (new IOException("Could not find retention time for scan "
		    + scanNum));
	}

	// An empty scan needs special attention..
	if (scanLength[0] == 0) {
	    scanNum++;
	    return new SimpleScan(null, scanNum, 1,
		    retentionTime.doubleValue(), 0, 0, null, new DataPoint[0],
		    MassSpectrumType.CENTROIDED, PolarityType.UNKNOWN, "", null);
	}

	// Is there any way how to extract polarity from netcdf?
	PolarityType polarity = PolarityType.UNKNOWN;

	// Is there any way how to extract scan definition from netcdf?
	String scanDefinition = "";

	// Read mass and intensity values
	Array massValueArray;
	Array intensityValueArray;
	try {
	    massValueArray = massValueVariable.read(scanStartPosition,
		    scanLength);
	    intensityValueArray = intensityValueVariable.read(
		    scanStartPosition, scanLength);
	} catch (Exception e) {
	    logger.log(
		    Level.SEVERE,
		    "Could not read from variables mass_values and/or intensity_values.",
		    e);
	    throw (new IOException(
		    "Could not read from variables mass_values and/or intensity_values."));
	}

	Index massValuesIndex = massValueArray.getIndex();
	Index intensityValuesIndex = intensityValueArray.getIndex();

	int arrayLength = massValueArray.getShape()[0];

	DataPoint dataPoints[] = new DataPoint[arrayLength];

	for (int j = 0; j < arrayLength; j++) {
	    Index massIndex0 = massValuesIndex.set0(j);
	    Index intensityIndex0 = intensityValuesIndex.set0(j);

	    double mz = massValueArray.getDouble(massIndex0)
		    * massValueScaleFactor;
	    double intensity = intensityValueArray.getDouble(intensityIndex0)
		    * intensityValueScaleFactor;
	    dataPoints[j] = new SimpleDataPoint(mz, intensity);

	}

	scanNum++;

	// Auto-detect whether this scan is centroided
	MassSpectrumType spectrumType = ScanUtils
		.detectSpectrumType(dataPoints);

	SimpleScan buildingScan = new SimpleScan(null, scanNum, 1,
		retentionTime.doubleValue(), 0, 0, null, dataPoints,
		spectrumType, polarity, scanDefinition, null);

	return buildingScan;

    }

}
