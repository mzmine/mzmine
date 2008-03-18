/*
 * Copyright 2006-2008 The MZmine Development Team
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;

/**
 * RawDataFile implementation
 * 
 */
class RawDataFileImpl implements RawDataFile, RawDataFileWriter {

    private transient Logger logger = Logger.getLogger(this.getClass().getName());

    private String fileName; // this is just a name of this object

    private Hashtable<Integer, Float> dataMinMZ, dataMaxMZ, dataMinRT,
            dataMaxRT, dataMaxBasePeakIntensity, dataMaxTIC;

    private String scanDataFileName, writingScanDataFileName;
    private transient RandomAccessFile scanDataFile, writingScanDataFile;
    /**
     * Preloaded scans
     */
    private Hashtable<Integer, Scan> scans, writingScans;
    private PreloadLevel preloadLevel;

    RawDataFileImpl(String fileName, String suffix, PreloadLevel preloadLevel)
            throws IOException {

        this.preloadLevel = preloadLevel;
        this.fileName = fileName;
        // create temporary file for scan data
        if (preloadLevel != PreloadLevel.PRELOAD_ALL_SCANS) {
            File dirPath = MZmineCore.getCurrentProject().getLocation();
            writingScanDataFileName = fileName + "." + suffix + ".scan";
            File scanfile = new File(dirPath, writingScanDataFileName);
            scanfile.createNewFile();
            writingScanDataFile = new RandomAccessFile(scanfile, "rw");
        }

        // prepare new Hashtable for scans
        writingScans = new Hashtable<Integer, Scan>();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getFilePath()
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanDataFile()
     */
    public RandomAccessFile getScanDataFile() {
        if (!scanDataFile.equals(null)) {
            return scanDataFile;
        } else {
            return null;
        }
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getWritingScanDataFile()
     */
    public RandomAccessFile getWritingScanDataFile() {
        if (!writingScanDataFile.equals(null)) {
            return writingScanDataFile;
        } else {
            return null;
        }
    }

    public void updateScanDataFile(File filePath) {
        try {
            scanDataFile = new RandomAccessFile(filePath, "r");
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Could not open file " + filePath, e);
            return;
        }
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanDataFileName()
     */
    public String getScanDataFileName() {
        return scanDataFileName;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getNumOfScans()
     */
    public int getNumOfScans() {
        return scans.size();
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

        // if there is no cache table, create one
        if (dataMinMZ == null)
            dataMinMZ = new Hashtable<Integer, Float>();

        // check if we have this value already cached
        Float minMZ = dataMinMZ.get(msLevel);
        if (minMZ != null)
            return minMZ.floatValue();

        // find the value
        for (Scan scan : scans.values()) {

            // ignore scans of other ms levels
            if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
                continue;

            if ((minMZ == null) || (scan.getMZRange().getMin() < minMZ))
                minMZ = scan.getMZRange().getMin();
        }

        // return -1 if no scan at this MS level
        if (minMZ == null)
            minMZ = -1f;

        // cache the value
        dataMinMZ.put(msLevel, minMZ);

        return minMZ;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public float getDataMaxMZ(int msLevel) {

        // if there is no cache table, create one
        if (dataMaxMZ == null)
            dataMaxMZ = new Hashtable<Integer, Float>();

        // check if we have this value already cached
        Float maxMZ = dataMaxMZ.get(msLevel);
        if (maxMZ != null)
            return maxMZ.floatValue();

        // find the value
        for (Scan scan : scans.values()) {

            // ignore scans of other ms levels
            if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
                continue;

            if ((maxMZ == null) || (scan.getMZRange().getMax() > maxMZ))
                maxMZ = scan.getMZRange().getMax();

        }

        // return -1 if no scan at this MS level
        if (maxMZ == null)
            maxMZ = -1f;

        // cache the value
        dataMaxMZ.put(msLevel, maxMZ);

        return maxMZ;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinRT()
     */
    public float getDataMinRT(int msLevel) {

        // if there is no cache table, create one
        if (dataMinRT == null)
            dataMinRT = new Hashtable<Integer, Float>();

        // check if we have this value already cached
        Float minRT = dataMinRT.get(msLevel);
        if (minRT != null)
            return minRT.floatValue();

        // find the value
        Enumeration<Scan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();

            // ignore scans of other ms levels
            if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
                continue;

            if ((minRT == null) || (scan.getRetentionTime() < minRT))
                minRT = scan.getRetentionTime();

        }

        // return -1 if no scan at this MS level
        if (minRT == null)
            minRT = -1f;

        // cache the value
        dataMinRT.put(msLevel, minRT);

        return minRT;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxRT()
     */
    public float getDataMaxRT(int msLevel) {

        // if there is no cache table, create one
        if (dataMaxRT == null)
            dataMaxRT = new Hashtable<Integer, Float>();

        // check if we have this value already cached
        Float maxRT = dataMaxRT.get(msLevel);
        if (maxRT != null)
            return maxRT.floatValue();

        // find the value
        Enumeration<Scan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();

            // ignore scans of other ms levels
            if ((msLevel != 0) && (scan.getMSLevel() != msLevel))
                continue;

            if ((maxRT == null) || (scan.getRetentionTime() > maxRT))
                maxRT = scan.getRetentionTime();

        }

        // return -1 if no scan at this MS level
        if (maxRT == null)
            maxRT = -1f;

        // cache the value
        dataMaxRT.put(msLevel, maxRT);

        return maxRT;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int)
     */
    public int[] getScanNumbers(int msLevel) {
        return getScanNumbers(msLevel, new Range(Float.MIN_VALUE, Float.MAX_VALUE));
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int, float, float)
     */
    public int[] getScanNumbers(int msLevel, Range rtRange) {

        ArrayList<Integer> eligibleScanNumbers = new ArrayList<Integer>();

        Enumeration<Scan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();

            if ((scan.getMSLevel() == msLevel)
                    && (rtRange.contains(scan.getRetentionTime())))
                eligibleScanNumbers.add(scan.getScanNumber());
        }

        int[] numbersArray = CollectionUtils.toIntArray(eligibleScanNumbers);
        Arrays.sort(numbersArray);

        return numbersArray;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers()
     */
    public int[] getScanNumbers() {

        Set<Integer> allScanNumbers = scans.keySet();
        int[] numbersArray = CollectionUtils.toIntArray(allScanNumbers);
        Arrays.sort(numbersArray);

        return numbersArray;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getMSLevels()
     */
    public int[] getMSLevels() {

        Set<Integer> msLevelsSet = new HashSet<Integer>();

        Enumeration<Scan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();
            msLevelsSet.add(scan.getMSLevel());
        }

        int[] msLevels = CollectionUtils.toIntArray(msLevelsSet);
        Arrays.sort(msLevels);
        return msLevels;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxBasePeakIntensity()
     */
    public float getDataMaxBasePeakIntensity(int msLevel) {

        // if there is no cache table, create one
        if (dataMaxBasePeakIntensity == null)
            dataMaxBasePeakIntensity = new Hashtable<Integer, Float>();

        // check if we have this value already cached
        Float maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
        if (maxBasePeak != null)
            return maxBasePeak.floatValue();

        // find the value
        Enumeration<Scan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();

            // ignore scans of other ms levels
            if (scan.getMSLevel() != msLevel)
                continue;

            DataPoint scanBasePeak = scan.getBasePeak();
            if (scanBasePeak == null)
                continue;

            if ((maxBasePeak == null)
                    || (scanBasePeak.getIntensity() > maxBasePeak))
                maxBasePeak = scanBasePeak.getIntensity();

        }

        // return -1 if no scan at this MS level
        if (maxBasePeak == null)
            maxBasePeak = -1f;

        // cache the value
        dataMaxBasePeakIntensity.put(msLevel, maxBasePeak);

        return maxBasePeak;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxTotalIonCurrent()
     */
    public float getDataMaxTotalIonCurrent(int msLevel) {

        // if there is no cache table, create one
        if (dataMaxTIC == null)
            dataMaxTIC = new Hashtable<Integer, Float>();

        // check if we have this value already cached
        Float maxTIC = dataMaxTIC.get(msLevel);
        if (maxTIC != null)
            return maxTIC.floatValue();

        // find the value
        Enumeration<Scan> scansEnum = scans.elements();
        while (scansEnum.hasMoreElements()) {
            Scan scan = scansEnum.nextElement();

            // ignore scans of other ms levels
            if (scan.getMSLevel() != msLevel)
                continue;

            if ((maxTIC == null) || (scan.getTIC() > maxTIC))
                maxTIC = scan.getTIC();

        }

        // return -1 if no scan at this MS level
        if (maxTIC == null)
            maxTIC = -1f;

        // cache the value
        dataMaxTIC.put(msLevel, maxTIC);

        return maxTIC;
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
            StorableScan storedScan = new StorableScan(newScan, this);
            writingScans.put(scanNumber, storedScan);
            break;
        case PRELOAD_ALL_SCANS:
            writingScans.put(scanNumber, newScan);
            break;
        case PRELOAD_FULL_SCANS:
            if (msLevel == 1) {
                writingScans.put(scanNumber, newScan);
            } else {
                StorableScan storedFullScan = new StorableScan(newScan, this);
                writingScans.put(scanNumber, storedFullScan);
            }
            break;

        }

        // If this is a fragment scan, update the fragmentScans[] array of its
        // parent
        if (newScan.getParentScanNumber() > 0) {
            Scan parentScan = writingScans.get(newScan.getParentScanNumber());
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

    }

    public String toString() {
        return this.fileName;
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

        logger.finest("Writing of scans to file " + writingScanDataFileName
                + " finished");

        // close temporary file and current data file
        if (scanDataFile != null) {
            scanDataFile.close();
            File dir = MZmineCore.getCurrentProject().getLocation();
            new File(dir, this.scanDataFileName).delete();
        }

        // switch temporary file to current datafile and reopen it for reading
        writingScanDataFile.close();
        scanDataFileName = writingScanDataFileName;
        scanDataFile = new RandomAccessFile(new File(
                MZmineCore.getCurrentProject().getLocation(),
                scanDataFileName.toString()), "r");
        scans = writingScans;

        // discard temporary file
        writingScanDataFile = null;
        writingScanDataFileName = null;
        writingScans = null;

        // discard cached information
        dataMinMZ = null;
        dataMaxMZ = null;
        dataMinRT = null;
        dataMaxRT = null;
        dataMaxBasePeakIntensity = null;
        dataMaxTIC = null;

        return this;

    }

    public float getDataMaxMZ() {
        return getDataMaxMZ(0);
    }

    public float getDataMaxRT() {
        return getDataMaxRT(0);
    }

    public float getDataMinMZ() {
        return getDataMinMZ(0);
    }

    public float getDataMinRT() {
        return getDataMinRT(0);
    }

    private Object readResolve() {
        logger = Logger.getLogger(this.getClass().getName());
        return this;
    }

    public Range getDataMZRange() {
        // TODO this needs cleanup
        return new Range(getDataMinMZ(), getDataMaxMZ());
    }

    public Range getDataMZRange(int msLevel) {
        // TODO this needs cleanup
        return new Range(getDataMinMZ(msLevel), getDataMaxMZ(msLevel));
    }

    public Range getDataRTRange() {
        // TODO this needs cleanup
        return new Range(getDataMinRT(), getDataMaxRT());
    }

    public Range getDataRTRange(int msLevel) {
        // TODO this needs cleanup
        return new Range(getDataMinRT(msLevel), getDataMaxRT(msLevel));
    }
}
