/*
 * Copyright 2006 Okinawa Institute of Science and Technology
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;
import net.sf.mzmine.util.Logger;

/**
 * Class representing raw data file in MZXML format.
 * 
 */
class MZXMLFile implements RawDataFile {

    private File originalFile;

    private int numOfScans = 0;

    private double dataMinMZ, dataMaxMZ, dataMaxIntensity;

    /**
     * Preloaded scans
     */
    private Hashtable<Integer, MZXMLScan> scans;

    private PreloadLevel preloadLevel;

    private StringBuffer dataDescription;

    private Hashtable<Integer, Long> scansIndex;

    /**
     * Maps scan level -> list of scan numbers in that level
     */
    private Hashtable<Integer, ArrayList<Integer>> scanNumbers;

    /**
     */
    MZXMLFile(File originalFile, PreloadLevel preloadLevel) {
        this.originalFile = originalFile;
        this.preloadLevel = preloadLevel;
        dataDescription = new StringBuffer();
        scansIndex = new Hashtable<Integer, Long>();
        scanNumbers = new Hashtable<Integer, ArrayList<Integer>>();
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
        MZXMLScan preloadedScan = scans.get(new Integer(scanNumber));
        if (preloadedScan != null)
            return preloadedScan;

        Long filePos = scansIndex.get(new Integer(scanNumber));
        if (filePos == null)
            throw (new IllegalArgumentException("Scan " + scanNumber
                    + " is not present in file " + originalFile));

        MZXMLScan buildingScan = new MZXMLScan();

        // Logger.put("Skip mzXML file to position " + filePos);

        FileInputStream fileIN = null;
        fileIN = new FileInputStream(originalFile);
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
                        + " from file " + originalFile));

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
    public double getDataMinMZ() {
        return dataMinMZ;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxMZ()
     */
    public double getDataMaxMZ() {
        return dataMaxMZ;
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
     * @see net.sf.mzmine.io.RawDataFile#getFileName()
     */
    public File getFileName() {
        return originalFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFile#getDataMaxIntensity()
     */
    public double getDataMaxIntensity() {
        return dataMaxIntensity;
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
    void addScan(MZXMLScan newScan) {

        /* if we want to keep data in memory, save a reference */
        if (preloadLevel == PreloadLevel.PRELOAD_ALL_SCANS)
            scans.put(new Integer(newScan.getScanNumber()), newScan);

        if ((numOfScans == 0) || (dataMinMZ > newScan.getMZRangeMin()))
            dataMinMZ = newScan.getMZRangeMin();
        if ((numOfScans == 0) || (dataMaxMZ < newScan.getMZRangeMax()))
            dataMaxMZ = newScan.getMZRangeMax();
        if ((numOfScans == 0)
                || (dataMaxIntensity < newScan.getBasePeakIntensity()))
            dataMaxIntensity = newScan.getBasePeakIntensity();

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
        return originalFile.getName();
    }

}
