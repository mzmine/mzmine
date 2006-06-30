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
package net.sf.mzmine.io.mzxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Logger;

/**
 * Class representing raw data file in MZXML format.
 * 
 */
class MZXMLFile implements RawDataFile {

    /**
     * This is the file containing current version of the data By default it is
     * same as originalFile, but if raw data is modified then this is the
     * current working copy of the data file.
     */
    private File currentFile;

    private Vector<Operation> history;

    private int numOfScans = 0;

    private Hashtable<Integer, Double> dataMinMZ, dataMaxMZ, dataMinRT, dataMaxRT;
    private Hashtable<Integer, Double> dataMaxBasePeakIntensity, dataMaxTIC;
    private Hashtable<Integer, Double> retentionTimes;

    /**
     * Preloaded scans
     */
    private Hashtable<Integer, Scan> scans;

    private PreloadLevel preloadLevel;

    private StringBuffer dataDescription;

    private Hashtable<Integer, Long> scansIndex;

    /**
     * Maps scan MS level to a list of scan numbers in that level
     */
    private Hashtable<Integer, ArrayList<Integer>> scanNumbers;

    /**
     * 
     */
    MZXMLFile(File currentFile, PreloadLevel preloadLevel) {
        this(currentFile, preloadLevel, null);
    }

    /**
     * 
     */
    MZXMLFile(File currentFile, PreloadLevel preloadLevel,
            Vector<Operation> history) {

        this.currentFile = currentFile;
        this.preloadLevel = preloadLevel;
        if (history == null) {
            this.history = new Vector<Operation>();
        } else {
            this.history = history;
        }

        dataDescription = new StringBuffer();
        scansIndex = new Hashtable<Integer, Long>();
        scanNumbers = new Hashtable<Integer, ArrayList<Integer>>();
        retentionTimes = new Hashtable<Integer, Double>();
        dataMinMZ = new Hashtable<Integer, Double>();
        dataMaxMZ = new Hashtable<Integer, Double>();
        dataMinRT = new Hashtable<Integer, Double>();
        dataMaxRT = new Hashtable<Integer, Double>();
        dataMaxBasePeakIntensity = new Hashtable<Integer, Double>();
        dataMaxTIC = new Hashtable<Integer, Double>();
        if (preloadLevel != PreloadLevel.NO_PRELOAD)
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
    public Scan getScan(int scanNumber) throws IOException {

        /* check if we have desired scan in memory */
        if (scans != null) {
            Scan preloadedScan = scans.get(new Integer(scanNumber));
            if (preloadedScan != null)
                return preloadedScan;
        }

        Long filePos = scansIndex.get(new Integer(scanNumber));
        if (filePos == null)
            throw (new IllegalArgumentException("Scan " + scanNumber
                    + " is not present in file " + currentFile + "("
                    + getOriginalFile() + ")"));

        MZXMLScan buildingScan = new MZXMLScan();

        // Logger.putFatal("Skip mzXML file to position " + filePos);

        FileInputStream fileIN = null;
        fileIN = new FileInputStream(currentFile);
        fileIN.skip(filePos);

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {

            // Parse the file
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(fileIN, buildingScan);
            saxParser.reset();

        } catch (Exception e) {

            if (!e.getMessage().equals("Scan reading finished")) {

                Logger.putFatal(e.toString());
                throw (new IOException("Couldn't parse scan " + scanNumber
                        + " from file " + currentFile + "(" + getOriginalFile()
                        + ")"));

            }
        }

        if (buildingScan.getScanNumber() != scanNumber) {
            throw new IOException(
                    "Error while reading from mzXML file: Retrieving incorrect scan #"
                            + buildingScan.getScanNumber()
                            + " supposed-to-be #" + scanNumber);
        }

        return buildingScan;

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
        if (! scanNumbers.containsKey(msLevel)) return -1;
        return dataMinMZ.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public double getDataMaxMZ(int msLevel) {
        if (! scanNumbers.containsKey(msLevel)) return -1;
        return dataMaxMZ.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int)
     */
    public int[] getScanNumbers(int msLevel) {
        if (! scanNumbers.containsKey(msLevel)) return new int[0];
        return getScanNumbers(msLevel, dataMinRT.get(msLevel), dataMaxRT.get(msLevel));
    }
    
    /**
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers(int, double, double)
     */
    public int[] getScanNumbers(int msLevel, double rtMin, double rtMax) {
        ArrayList<Integer> numbersList = scanNumbers.get(msLevel);
        if (numbersList == null) 
            return new int[0];

        ArrayList<Integer> eligibleScans = new ArrayList<Integer>();
        
        Iterator<Integer> iter = numbersList.iterator();
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
     * @see net.sf.mzmine.io.RawDataFile#getScanNumbers()
     */
    public int[] getScanNumbers() {
        
        Set<Integer> allScanNumbers = new HashSet<Integer>();
        Enumeration<ArrayList<Integer>> scanNumberLists = scanNumbers.elements();
        
        while (scanNumberLists.hasMoreElements()) 
            allScanNumbers.addAll(scanNumberLists.nextElement());
        
        int[] numbersArray = CollectionUtils.toArray(allScanNumbers);
        Arrays.sort(numbersArray);
        
        return numbersArray;
        
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getMSLevels()
     */
    public int[] getMSLevels() {

        Set<Integer> msLevelsSet = scanNumbers.keySet();
        int[] msLevels = CollectionUtils.toArray(msLevelsSet);
        Arrays.sort(msLevels);
        return msLevels;

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
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxBasePeakIntensity()
     */
    public double getDataMaxBasePeakIntensity(int msLevel) {
        return dataMaxBasePeakIntensity.get(msLevel).doubleValue();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxTotalIonCurrent()
     */
    public double getDataMaxTotalIonCurrent(int msLevel) {
        return dataMaxTIC.get(msLevel).doubleValue();
    }

    void addIndexEntry(Integer scanNumber, Long filePosition) {
        scansIndex.put(scanNumber, filePosition);
    }

    void addDataDescription(String description) {
        if (dataDescription.length() > 0)
            dataDescription.append("\n");
        dataDescription.append(description);
    }

    /**
     * SAX parser calls this method when parsing the XML file
     */
    void addScan(Scan newScan) {

        /* if we want to keep data in memory, save a reference */
        if (preloadLevel == PreloadLevel.PRELOAD_ALL_SCANS)
            scans.put(new Integer(newScan.getScanNumber()), newScan);

        int msLevel = newScan.getMSLevel();
        
        if ((dataMinMZ.get(msLevel) == null) || (dataMinMZ.get(msLevel) > newScan.getMZRangeMin()))
            dataMinMZ.put(msLevel, newScan.getMZRangeMin());
        if ((dataMaxMZ.get(msLevel) == null) || (dataMaxMZ.get(msLevel) < newScan.getMZRangeMax()))
            dataMaxMZ.put(msLevel, newScan.getMZRangeMax());
        if ((dataMinRT.get(msLevel) == null) || (dataMinRT.get(msLevel) > newScan.getRetentionTime()))
            dataMinRT.put(msLevel, newScan.getRetentionTime());
        if ((dataMaxRT.get(msLevel) == null) || (dataMaxRT.get(msLevel) < newScan.getRetentionTime()))
            dataMaxRT.put(msLevel, newScan.getRetentionTime());
        if ((dataMaxBasePeakIntensity.get(newScan.getMSLevel()) == null)
                || (dataMaxBasePeakIntensity.get(newScan.getMSLevel()) < newScan
                        .getBasePeakIntensity()))
            dataMaxBasePeakIntensity.put(newScan.getMSLevel(), newScan
                    .getBasePeakIntensity());

        retentionTimes.put(newScan.getScanNumber(), newScan.getRetentionTime());
        
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

    public String toString() {
        return getOriginalFile().getName();
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMinRT()
     */
    public double getDataMinRT(int msLevel) {
        return dataMinRT.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxRT()
     */
    public double getDataMaxRT(int msLevel) {
        return dataMaxRT.get(msLevel);
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getPreloadLevel()
     */
    public PreloadLevel getPreloadLevel() {
        return preloadLevel;
    }

    /**
     * 
     */
    public RawDataFileWriter createNewTemporaryFile() throws IOException {

        // Create new temp file
        File workingCopy;
        try {
            workingCopy = File.createTempFile("MZmine", null);
            workingCopy.deleteOnExit();
        } catch (SecurityException e) {
            Logger.putFatal("Could not prepare newly created temporary copy for deletion on exit.");
            throw new IOException("Could not prepare newly created temporary copy for deletion on exit.");
        }

        return new MZXMLFileWriter(this, workingCopy, preloadLevel);

    }


}
