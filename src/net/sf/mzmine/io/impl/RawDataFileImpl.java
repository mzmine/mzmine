/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.util.CollectionUtils;

/**
 * RawDataFile implementation
 * 
 */
class RawDataFileImpl implements RawDataFile, RawDataFileWriter {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private String fileName;
    private int numOfScans = 0;

    private Hashtable<Integer, Float> dataMinMZ, dataMaxMZ, dataMinRT,
            dataMaxRT, dataMaxBasePeakIntensity, dataMaxTIC,
            dataTotalRawSignal;

    /**
     * Preloaded scans
     */
    private Hashtable<Integer, Scan> scans;

    private PreloadLevel preloadLevel;

    /**
     * Maps scan MS level to a list of scan numbers in that level
     */
    private Hashtable<Integer, ArrayList<Integer>> scanNumbers;

    /**
     * 
     */
    RawDataFileImpl(String name, PreloadLevel preloadLevel) {
        this.fileName = name;
        this.preloadLevel = preloadLevel;

        scanNumbers = new Hashtable<Integer, ArrayList<Integer>>();

        dataMinMZ = new Hashtable<Integer, Float>();
        dataMaxMZ = new Hashtable<Integer, Float>();
        dataMinRT = new Hashtable<Integer, Float>();
        dataMaxRT = new Hashtable<Integer, Float>();
        dataMaxBasePeakIntensity = new Hashtable<Integer, Float>();
        dataMaxTIC = new Hashtable<Integer, Float>();
        dataTotalRawSignal = new Hashtable<Integer, Float>();

        scans = new Hashtable<Integer, Scan>();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getNumOfScans()
     */
    public int getNumOfScans() {
        return numOfScans;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScan(int)
     */
    public Scan getScan(int scanNumber) {
        return scans.get(scanNumber);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinMZ()
     */
    public float getDataMinMZ(int msLevel) {
        if (!scanNumbers.containsKey(msLevel))
            return -1;
        return dataMinMZ.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public float getDataMaxMZ(int msLevel) {
        if (!scanNumbers.containsKey(msLevel))
            return -1;
        return dataMaxMZ.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int)
     */
    public int[] getScanNumbers(int msLevel) {
        if (!scanNumbers.containsKey(msLevel))
            return new int[0];
        return getScanNumbers(msLevel, dataMinRT.get(msLevel),
                dataMaxRT.get(msLevel));
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int, float, float)
     */
    public int[] getScanNumbers(int msLevel, float rtMin, float rtMax) {
        ArrayList<Integer> numbersList = scanNumbers.get(msLevel);
        if (numbersList == null)
            return new int[0];

        ArrayList<Integer> eligibleScans = new ArrayList<Integer>();

        Iterator<Integer> iter = numbersList.iterator();
        while (iter.hasNext()) {
            Integer scanNumber = iter.next();
            float rt = scans.get(scanNumber).getRetentionTime();
            if ((rt >= rtMin) && (rt <= rtMax))
                eligibleScans.add(scanNumber);
        }

        int[] numbersArray = CollectionUtils.toIntArray(eligibleScans);
        Arrays.sort(numbersArray);

        return numbersArray;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers()
     */
    public int[] getScanNumbers() {

        Set<Integer> allScanNumbers = new HashSet<Integer>();
        Enumeration<ArrayList<Integer>> scanNumberLists = scanNumbers.elements();

        while (scanNumberLists.hasMoreElements())
            allScanNumbers.addAll(scanNumberLists.nextElement());

        int[] numbersArray = CollectionUtils.toIntArray(allScanNumbers);
        Arrays.sort(numbersArray);

        return numbersArray;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getMSLevels()
     */
    public int[] getMSLevels() {

        Set<Integer> msLevelsSet = scanNumbers.keySet();
        int[] msLevels = CollectionUtils.toIntArray(msLevelsSet);
        Arrays.sort(msLevels);
        return msLevels;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxBasePeakIntensity()
     */
    public float getDataMaxBasePeakIntensity(int msLevel) {
        return dataMaxBasePeakIntensity.get(msLevel).floatValue();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxTotalIonCurrent()
     */
    public float getDataMaxTotalIonCurrent(int msLevel) {
        return dataMaxTIC.get(msLevel).floatValue();
    }

    public float getDataTotalRawSignal(int msLevel) {
        return dataTotalRawSignal.get(msLevel).floatValue();
    }

    /**
     * 
     */
    public void addScan(Scan newScan) {

        int scanNumber = newScan.getScanNumber();
        int msLevel = newScan.getMSLevel();

        // Store the scan data
        switch (preloadLevel) {
        case NO_PRELOAD:
            StorableScan storedScan = new StorableScan(newScan);
            scans.put(scanNumber, storedScan);
            break;
        case PRELOAD_ALL_SCANS:
            scans.put(scanNumber, newScan);
            break;
        case PRELOAD_FULL_SCANS:
            if (msLevel == 1) {
                scans.put(scanNumber, newScan);
            } else {
                StorableScan storedFullScan = new StorableScan(newScan);
                scans.put(scanNumber, storedFullScan);
            }
            break;

        }

        // Update global information

        if ((dataMinMZ.get(msLevel) == null)
                || (dataMinMZ.get(msLevel) > newScan.getMZRangeMin()))
            dataMinMZ.put(msLevel, newScan.getMZRangeMin());
        if ((dataMaxMZ.get(msLevel) == null)
                || (dataMaxMZ.get(msLevel) < newScan.getMZRangeMax()))
            dataMaxMZ.put(msLevel, newScan.getMZRangeMax());
        if ((dataMinRT.get(msLevel) == null)
                || (dataMinRT.get(msLevel) > newScan.getRetentionTime()))
            dataMinRT.put(msLevel, newScan.getRetentionTime());
        if ((dataMaxRT.get(msLevel) == null)
                || (dataMaxRT.get(msLevel) < newScan.getRetentionTime()))
            dataMaxRT.put(msLevel, newScan.getRetentionTime());
        if ((dataMaxBasePeakIntensity.get(msLevel) == null)
                || (dataMaxBasePeakIntensity.get(msLevel) < newScan.getBasePeakIntensity()))
            dataMaxBasePeakIntensity.put(msLevel,
                    newScan.getBasePeakIntensity());

        float scanTIC = 0;

        for (float intensity : newScan.getIntensityValues())
            scanTIC += intensity;

        if ((dataMaxTIC.get(msLevel) == null)
                || (scanTIC > dataMaxTIC.get(msLevel)))
            dataMaxTIC.put(msLevel, scanTIC);

        Float prevSum = dataTotalRawSignal.get(msLevel);
        if (prevSum == null)
            prevSum = 0.0f;
        dataTotalRawSignal.put(msLevel, prevSum + scanTIC);

        ArrayList<Integer> scanList = scanNumbers.get(msLevel);
        if (scanList == null) {
            scanList = new ArrayList<Integer>(64);
            scanNumbers.put(msLevel, scanList);
        }

        scanList.add(newScan.getScanNumber());

        // If this is a gragment scan, update the fragmentScans[] array of its
        // parent
        if (newScan.getParentScanNumber() > 0) {
            Scan parentScan = scans.get(newScan.getParentScanNumber());
            if (parentScan != null) {
                if (parentScan instanceof StorableScan) {
                    int fragmentScans[] = ((StorableScan) parentScan).getFragmentScanNumbers();
                    if (fragmentScans != null) {
                        ArrayList<Integer> fragmentScansList = new ArrayList<Integer>();
                        for (int fragmentScan : fragmentScans)
                            fragmentScansList.add(fragmentScan);
                        fragmentScansList.add(newScan.getScanNumber());
                        fragmentScans = CollectionUtils.toIntArray(fragmentScansList);
                        ((StorableScan) parentScan).setFragmentScanNumbers(fragmentScans);
                    } else {
                        ((StorableScan) parentScan).setFragmentScanNumbers(new int[] { newScan.getScanNumber() });
                    }
                }
                if (parentScan instanceof SimpleScan) {
                    int fragmentScans[] = ((SimpleScan) parentScan).getFragmentScanNumbers();
                    if (fragmentScans != null) {
                        ArrayList<Integer> fragmentScansList = new ArrayList<Integer>();
                        for (int fragmentScan : fragmentScans)
                            fragmentScansList.add(fragmentScan);
                        fragmentScansList.add(newScan.getScanNumber());
                        fragmentScans = CollectionUtils.toIntArray(fragmentScansList);
                        ((SimpleScan) parentScan).setFragmentScanNumbers(fragmentScans);
                    } else {
                        ((SimpleScan) parentScan).setFragmentScanNumbers(new int[] { newScan.getScanNumber() });
                    }
                }
            }
        }

        numOfScans++;

    }

    public String toString() {
        return fileName;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinRT()
     */
    public float getDataMinRT(int msLevel) {
        return dataMinRT.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxRT()
     */
    public float getDataMaxRT(int msLevel) {
        return dataMaxRT.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getPreloadLevel()
     */
    public PreloadLevel getPreloadLevel() {
        return preloadLevel;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFileWriter#finishWriting()
     */
    public RawDataFile finishWriting() throws IOException {
        logger.finest("Writing of file " + fileName + " finished");
        return this;
    }

}
