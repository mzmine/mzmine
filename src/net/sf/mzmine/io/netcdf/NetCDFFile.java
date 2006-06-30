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
import java.util.Vector;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.io.mzxml.MZXMLFileWriter;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Logger;

/**
 * 
 */
public class NetCDFFile implements RawDataFile {

    private File currentFile;

    private PreloadLevel preloadLevel;
    private StringBuffer dataDescription;

    private Vector<Operation> history;

    private int numOfScans = 0;

    private double dataMinMZ, dataMaxMZ, dataMinRT, dataMaxRT;

    private double dataMaxBasePeakIntensity, dataMaxTIC;

    private Hashtable<Integer, Double> retentionTimes;

    private NetCDFFileParser cdfParser;

    /**
     * Preloaded scans
     */
    private Hashtable<Integer, Scan> scans;

    /**
     * Scan numbers (only MS level 1)
     */
    private ArrayList<Integer> scanNumbers;

    /**
     * 
     */
    NetCDFFile(File currentFile, PreloadLevel preloadLevel) {
        this(currentFile, preloadLevel, null);
    }

    /**
     * 
     */
    NetCDFFile(File currentFile, PreloadLevel preloadLevel,
            Vector<Operation> history) {

        this.currentFile = currentFile;
        this.preloadLevel = preloadLevel;
        if (history == null) {
            this.history = new Vector<Operation>();
        } else {
            this.history = history;
        }

        dataDescription = new StringBuffer();
        scanNumbers = new ArrayList<Integer>();
        retentionTimes = new Hashtable<Integer, Double>();
        if (preloadLevel != PreloadLevel.NO_PRELOAD)
            scans = new Hashtable<Integer, Scan>();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getOriginalFile()
     */
    public File getOriginalFile() {
        if (history.size() == 0)
            return currentFile;
        return history.get(0).previousFileName;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public Vector<Operation> getHistory() {
        return history;
    }

    public void addHistory(File previousFile, Method processingMethod,
            MethodParameters parameters) {
        Operation o = new Operation();
        o.previousFileName = previousFile;
        o.processingMethod = processingMethod;
        o.parameters = parameters;
        history.add(o);
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
        return new int[] { 1 };
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getRetentionTime(int)
     */
    public double getRetentionTime(int scanNumber) {
        Double rt = retentionTimes.get(scanNumber);
        if (rt == null)
            return 0;
        else
            return rt.doubleValue();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers()
     */
    public int[] getScanNumbers() {
        return getScanNumbers(1);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int)
     */
    public int[] getScanNumbers(int msLevel) {
        return getScanNumbers(msLevel, dataMinRT, dataMaxRT);
    }
    
    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int, double, double)
     */
    public int[] getScanNumbers(int msLevel, double rtMin, double rtMax) {
        ArrayList<Integer> eligibleScans = new ArrayList<Integer>();
        
        Iterator<Integer> iter = scanNumbers.iterator();
        while (iter.hasNext()) {
            Integer scanNumber = iter.next();
            double rt = retentionTimes.get(scanNumber);
            if ((rt >= rtMin) && (rt <= rtMax)) eligibleScans.add(scanNumber);
        }
        
        int[] numbersArray = CollectionUtils.toArray(eligibleScans);
        Arrays.sort(numbersArray);
        
        return numbersArray;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScan(int)
     */
    public Scan getScan(int scanNumber) throws IOException {

        /* check if we have desired scan in memory */
        if (scans != null) {
            Scan preloadedScan = scans.get(new Integer(scanNumber));
            if (preloadedScan != null)
                return preloadedScan;
        }

        // Fetch scan from file
        cdfParser.openFile();
        Scan fetchedScan = cdfParser.parseScan(scanNumber);
        cdfParser.closeFile();

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
    public double getDataMinMZ(int msLevel) {
        return dataMinMZ;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public double getDataMaxMZ(int msLevel) {
        return dataMaxMZ;
    }

    public String toString() {
        return getOriginalFile().getName();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinRT()
     */
    public double getDataMinRT(int msLevel) {
        return dataMinRT;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxRT()
     */
    public double getDataMaxRT(int msLevel) {
        return dataMaxRT;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxBasePeakIntensity(int)
     */
    public double getDataMaxBasePeakIntensity(int msLevel) {
        if (msLevel == 1)
            return dataMaxBasePeakIntensity;
        else
            return 0;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxTotalIonCurrent(int)
     */
    public double getDataMaxTotalIonCurrent(int msLevel) {
        if (msLevel == 1)
            return dataMaxTIC;
        else
            return 0;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getPreloadLevel()
     */
    public PreloadLevel getPreloadLevel() {
        return preloadLevel;
    }

    void addDataDescription(String description) {
        if (dataDescription.length() > 0)
            dataDescription.append("\n");
        dataDescription.append(description);
    }

    void addParser(NetCDFFileParser cdfParser) {
        this.cdfParser = cdfParser;
    }

    /**
     * 
     */
    void addScan(Scan newScan) {

        /* if we want to keep data in memory, save a reference */
        if (preloadLevel == PreloadLevel.PRELOAD_ALL_SCANS)
            scans.put(newScan.getScanNumber(), newScan);

        if ((numOfScans == 0) || (dataMinMZ > newScan.getMZRangeMin()))
            dataMinMZ = newScan.getMZRangeMin();
        if ((numOfScans == 0) || (dataMaxMZ < newScan.getMZRangeMax()))
            dataMaxMZ = newScan.getMZRangeMax();
        if ((numOfScans == 0) || (dataMinRT > newScan.getRetentionTime()))
            dataMinRT = newScan.getRetentionTime();
        if ((numOfScans == 0) || (dataMaxRT < newScan.getRetentionTime()))
            dataMaxRT = newScan.getRetentionTime();
        if ((numOfScans == 0)
                || (dataMaxBasePeakIntensity < newScan.getBasePeakIntensity()))
            dataMaxBasePeakIntensity = newScan.getBasePeakIntensity();

        retentionTimes.put(newScan.getScanNumber(), newScan.getRetentionTime());

        double scanTIC = 0;

        for (double intensity : newScan.getIntensityValues())
            scanTIC += intensity;

        if ((numOfScans == 0) || (scanTIC > dataMaxTIC))
            dataMaxTIC = scanTIC;

        scanNumbers.add(newScan.getScanNumber());

        numOfScans++;

    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#createNewTemporaryFile()
     */
    public RawDataFileWriter createNewTemporaryFile() throws IOException {

        // Create new temp file
        File workingCopy;
        try {
            workingCopy = File.createTempFile("MZmine", null);
            workingCopy.deleteOnExit();
        } catch (SecurityException e) {
            Logger.putFatal("Could not prepare newly created temporary copy for deletion on exit.");
            throw new IOException(
                    "Could not prepare newly created temporary copy for deletion on exit.");
        }

        // TODO: implement NetCDFFileWriter
        return new MZXMLFileWriter(this, workingCopy, preloadLevel);
    }

}
