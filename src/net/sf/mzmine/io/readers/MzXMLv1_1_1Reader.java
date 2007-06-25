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

package net.sf.mzmine.io.readers;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.RawDataFileReader;

import org.proteomecommons.io.mzxml.v1_1_1.MzXMLPeakList;
import org.proteomecommons.io.mzxml.v1_1_1.MzXMLPeakListReader;

/**
 * 
 */
public class MzXMLv1_1_1Reader implements RawDataFileReader {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File originalFile;
    private MzXMLPeakListReader parser;

    int totalScans = -1;

    public MzXMLv1_1_1Reader(File originalFile) {
        this.originalFile = originalFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFileReader#startReading()
     */
    public void startReading() throws IOException {

        // Open NetCDF-file
        try {
            parser = new MzXMLPeakListReader(originalFile.getPath());
        } catch (Exception e) {
            logger.severe(e.toString());
            throw (new IOException("Couldn't open input file" + originalFile));
        }

    }

    /**
     * @see net.sf.mzmine.io.RawDataFileReader#finishReading()
     */
    public void finishReading() throws IOException {
        parser.close();
    }

    /**
     * Reads one scan from the file. Requires that general information has
     * already been read.
     */
    public Scan readNextScan() throws IOException {

        MzXMLPeakList peakList = (MzXMLPeakList) parser.getPeakList();

        // End of file
        if (peakList == null) return null;
        
        // Set total scans if we haven's set it yet
        if (totalScans <= 0)
            totalScans = Integer.parseInt(peakList.getMsRun().getScanCount());

        // Prepare variables
        int scanNumber, msLevel, parentScan;
        float retentionTime, mzValues[], intensityValues[], precursorMZ;
        boolean centroided;
        
        // Get scan number and MS level
        scanNumber = Integer.parseInt(peakList.getNum());
        msLevel = Integer.parseInt(peakList.getMsLevel());
        
        // Parse retention time
        String retentionTimeStr = peakList.getRetentionTime();
        Date currentDate = new Date();
        try {
            DatatypeFactory dataTypeFactory = DatatypeFactory.newInstance();
            Duration dur = dataTypeFactory.newDuration(retentionTimeStr);
            retentionTime = dur.getTimeInMillis(currentDate) / 1000f;
        } catch (DatatypeConfigurationException e) {
            throw new IOException("Could not read next scan: " + e);
        }
        
        // Parse precursor scan details
        if (peakList.getPrecursorScanNum() != null) {
            parentScan = Integer.parseInt(peakList.getPrecursorScanNum());
            org.proteomecommons.io.Peak precursorPeak = peakList.getParentPeak();
            precursorMZ = (float) precursorPeak.getMassOverCharge();
        } else {
            parentScan = 0;
            precursorMZ = 0f;
        }
        
        // Find all peaks with intensity > 0
        org.proteomecommons.io.Peak peaks[] = peakList.getPeaks();
        int numOfGoodPeaks = 0;
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i].getIntensity() > 0) numOfGoodPeaks++;
        }
        
        // Create new mzValues and intensityValues arrays
        mzValues = new float[numOfGoodPeaks];
        intensityValues = new float[numOfGoodPeaks];
        
        // Copy m/z and intensity data
        int peakIndex = 0;
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i].getIntensity() > 0) {
                mzValues[peakIndex] = (float) peaks[i].getMassOverCharge();
                intensityValues[peakIndex] = (float) peaks[i].getIntensity();
                peakIndex++;
            }
        }
        
        // Autodetect whether the scan is centroided
        // Check first 10 m/z values and if their difference is same, data is continous
        centroided = true;
        if (peaks.length > 10) {
            centroided = false;
            float refDifference = (float) Math.abs(peaks[0].getMassOverCharge() - peaks[1].getMassOverCharge());
            for (int i = 1; i < 10; i++) {
                float curDifference = (float) Math.abs(peaks[i].getMassOverCharge() - peaks[i + 1].getMassOverCharge());
                if (Math.abs(curDifference - refDifference) > 0.0001f) {
                    centroided = true;
                    break;
                }
            }
        }
        
        // Create new Scan
        SimpleScan newScan = new SimpleScan(scanNumber, msLevel, retentionTime,
                parentScan, precursorMZ, null,
                mzValues, intensityValues, centroided);
        
        return newScan;
        
    }

    /**
     * Returns total number of scans
     */
    public int getNumberOfScans() {
        return totalScans;
    }

}