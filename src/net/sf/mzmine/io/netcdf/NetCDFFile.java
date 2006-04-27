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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.util.Logger;

/**
 *
 */
public class NetCDFFile implements RawDataFile {

    private File originalFile;
    private PreloadLevel preloadLevel;
    private StringBuffer dataDescription;

    private int numOfScans = 0;

    private double dataMinMZ, dataMaxMZ, dataMinRT, dataMaxRT;

    /**
     * Maps scan number -> start position and length for each scan in the datapoint arrays
     */
    private Hashtable<Integer, Integer[]> scansIndex;

    private Hashtable<Integer, Double> dataMaxBasePeakIntensity, dataMaxTIC;

	private Hashtable<Integer, Double> retentionTimes;

    /**
     * Preloaded scans
     */
    private Hashtable<Integer, NetCDFScan> scans;

    /**
     * Maps scan level -> list of scan numbers in that level
     */
    private Hashtable<Integer, ArrayList<Integer>> scanNumbers;


    /**
     */
    NetCDFFile(File originalFile, PreloadLevel preloadLevel) {
        this.originalFile = originalFile;
        this.preloadLevel = preloadLevel;
        dataDescription = new StringBuffer();
        scansIndex = new Hashtable<Integer, Integer[]>();
        scanNumbers = new Hashtable<Integer, ArrayList<Integer>>();
        dataMaxBasePeakIntensity = new Hashtable<Integer, Double>();
        dataMaxTIC = new Hashtable<Integer, Double>();
        if (preloadLevel != PreloadLevel.NO_PRELOAD) scans = new Hashtable<Integer, NetCDFScan>();
    }


    /**
     * @see net.sf.mzmine.io.RawDataFile#getFileName()
     */
    public File getFileName() {
        return originalFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getNumOfScans()
     */
    public int getNumOfScans() {
        return numOfScans;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getMSLevels()
     */
    public int[] getMSLevels() {

        Set<Integer> msLevelsSet = scanNumbers.keySet();
        int[] msLevels = new int[msLevelsSet.size()];
        int index = 0;
        Iterator<Integer> iter = msLevelsSet.iterator();
        while (iter.hasNext())
            msLevels[index++] = iter.next().intValue();
        Arrays.sort(msLevels);
        return msLevels;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int)
     */
    public int[] getScanNumbers(int msLevel) {

        ArrayList<Integer> numbersList = scanNumbers.get(new Integer(msLevel));
        if (numbersList == null)
            return null;

        int[] numbersArray = new int[numbersList.size()];
        int index = 0;
        Iterator<Integer> iter = numbersList.iterator();
        while (iter.hasNext())
            numbersArray[index++] = iter.next().intValue();
        Arrays.sort(numbersArray);
        return numbersArray;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScan(int)
     */
    public Scan getScan(int scanNumber) throws IOException {


        /* check if we have desired scan in memory */
        if (scans != null) {
            NetCDFScan preloadedScan = scans.get(new Integer(scanNumber));
            if (preloadedScan != null)
                return preloadedScan;
        }


		// Fetch scan from file
		NetCDFScan fetchedScan;

		// Get start position and length for this scan
		Integer[] startAndLength = scansIndex.get(new Integer(scanNumber));
		int[] scanStartPosition = new int[1]; scanStartPosition[0] = startAndLength[0];
		int[] scanLength = new int[1]; scanLength[0] = startAndLength[1];

		if (scanLength[0]==0) {
			// An empty scan needs some special attention...
			double[] massValues = new double[0];
			double[] intensityValues = new double[0];
			fetchedScan = new NetCDFScan(scanNumber, retentionTimes.get(new Integer(scanNumber)), massValues, intensityValues);

			massValues = null; intensityValues =  null;

		} else {

			// Open NetCDF-file
			ucar.nc2.NetcdfFile inputFile;
			try {
				inputFile = new ucar.nc2.NetcdfFile(originalFile.getPath());
			} catch (Exception e) {
                Logger.putFatal(e.toString());
                throw (new IOException("Couldn't open input file" + originalFile));
			}


			// Get datapoint variables
			ucar.nc2.Variable massValueVariable = inputFile.findVariable("mass_values");
			ucar.nc2.Variable intensityValueVariable = inputFile.findVariable("intensity_values");
			if (intensityValueVariable==null) {
                Logger.putFatal("Couldn't find variable mass_values and/or intensity_values from file " + originalFile);
                throw (new IOException("Couldn't find variable mass_values and/or intensity_values from file " + originalFile));
			}


			// Read mass and intensity values
			ucar.ma2.Array massValueArray;
			ucar.ma2.Array intensityValueArray;
			try {
				massValueArray = massValueVariable.read(scanStartPosition, scanLength);
				intensityValueArray = intensityValueVariable.read(scanStartPosition, scanLength);
			} catch (Exception e) {
                Logger.putFatal(e.toString());
                throw (new IOException("Couldn't read variable mass_values and/or intensity_values from file " + originalFile));
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

			fetchedScan = new NetCDFScan(scanNumber, retentionTimes.get(new Integer(scanNumber)), massValues, intensityValues);


			// Close the raw data file
			try {
				inputFile.close();
			} catch (Exception e) {
                Logger.putFatal(e.toString());
                throw (new IOException("Couldn't close file " + originalFile));
			}

			massValueArray = null; intensityValueArray = null;
			massValueVariable = null; intensityValueVariable = null;
			massValues = null; intensityValues = null;
			inputFile = null;

		}

        return fetchedScan;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataDescription()
     */
    public String getDataDescription() {
        return dataDescription.toString();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinMZ()
     */
    public double getDataMinMZ() {
        return dataMinMZ;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public double getDataMaxMZ() {
        return dataMaxMZ;
    }

    public String toString() {
        return originalFile.getName();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinRT()
     */
    public double getDataMinRT() {
        return dataMinRT;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxRT()
     */
    public double getDataMaxRT() {
        return dataMaxRT;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxBasePeakIntensity(int)
     */
    public double getDataMaxBasePeakIntensity(int msLevel) {
        return dataMaxBasePeakIntensity.get(msLevel).doubleValue();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxTotalIonCurrent(int)
     */
    public double getDataMaxTotalIonCurrent(int msLevel) {
        return dataMaxTIC.get(msLevel).doubleValue();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getPreloadLevel()
     */
    public PreloadLevel getPreloadLevel() {
        return preloadLevel;
    }


    void addIndexEntry(Integer scanNumber, Integer arrayPositionStart, Integer lengthInArray) {
		Integer[] arrayPosition = new Integer[2];
		arrayPosition[0] = arrayPositionStart; arrayPosition[1] = lengthInArray;
        scansIndex.put(scanNumber, arrayPosition);
    }

    void addDataDescription(String description) {
        if (dataDescription.length() > 0)
            dataDescription.append("\n");
        dataDescription.append(description);
    }

    void addRetentionTimes(Hashtable<Integer, Double> retentionTimes) {
		this.retentionTimes = retentionTimes;
	}

    /**
     *
     */
    void addScan(NetCDFScan newScan) {

        /* if we want to keep data in memory, save a reference */
        if (preloadLevel == PreloadLevel.PRELOAD_ALL_SCANS)
            scans.put(new Integer(newScan.getScanNumber()), newScan);

        if ((numOfScans == 0) || (dataMinMZ > newScan.getMZRangeMin()))
            dataMinMZ = newScan.getMZRangeMin();
        if ((numOfScans == 0) || (dataMaxMZ < newScan.getMZRangeMax()))
            dataMaxMZ = newScan.getMZRangeMax();
        if ((numOfScans == 0) || (dataMinRT > newScan.getRetentionTime()))
            dataMinRT = newScan.getRetentionTime();
        if ((numOfScans == 0) || (dataMaxRT < newScan.getRetentionTime()))
            dataMaxRT = newScan.getRetentionTime();
        if ((dataMaxBasePeakIntensity.get(newScan.getMSLevel()) == null)
                || (dataMaxBasePeakIntensity.get(newScan.getMSLevel()) < newScan
                        .getBasePeakIntensity()))
            dataMaxBasePeakIntensity.put(newScan.getMSLevel(), newScan
                    .getBasePeakIntensity());

        double scanTIC = 0;

        for (double intensity : newScan.getIntensityValues())
            scanTIC += intensity;

        if ((dataMaxTIC.get(newScan.getMSLevel()) == null)
                || (scanTIC > dataMaxTIC.get(newScan.getMSLevel())))
            dataMaxTIC.put(newScan.getMSLevel(), scanTIC);

        ArrayList<Integer> scanList = scanNumbers.get(new Integer(newScan
                .getMSLevel()));
        if (scanList == null) {
            scanList = new ArrayList<Integer>(64);
            scanNumbers.put(new Integer(newScan.getMSLevel()), scanList);
        }
        scanList.add(new Integer(newScan.getScanNumber()));

        numOfScans++;

    }



}
