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

package net.sf.mzmine.io.readers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.RawDataFileReader;

import org.proteomecommons.io.mzxml.v2_0.MzXMLPeakList;
import org.proteomecommons.io.mzxml.v2_0.MzXMLPeakListReader;

/**
 * 
 */
@Deprecated
public class MzXMLv2_0Reader implements RawDataFileReader {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File originalFile;
    private MzXMLPeakListReader parser;

    int totalScans = -1;

    public MzXMLv2_0Reader(File originalFile) {
        this.originalFile = originalFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFileReader#startReading()
     */
    public void startReading() throws IOException {

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
        if (peakList == null)
            return null;

        // Set total scans if we haven's set it yet
        if (totalScans <= 0)
            totalScans = Integer.parseInt(peakList.getMsRun().getScanCount());

        // Prepare variables
        int scanNumber, msLevel, parentScan;
        float retentionTime, precursorMZ;
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
        ArrayList<org.proteomecommons.io.Peak> goodPeaks = new ArrayList<org.proteomecommons.io.Peak>(
                1024);
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i].getIntensity() > 0) {
                goodPeaks.add(peaks[i]);
                continue;
            }
            if ((i > 0) && (peaks[i - 1].getIntensity() > 0)) {
                goodPeaks.add(peaks[i]);
                continue;
            }
            if ((i < peaks.length - 1) && (peaks[i + 1].getIntensity() > 0)) {
                goodPeaks.add(peaks[i]);
                continue;
            }
        }

        // Create new mzValues and intensityValues arrays
        DataPoint dataPoints[] = new DataPoint[goodPeaks.size()];

        // Copy m/z and intensity data
        for (int i = 0; i < goodPeaks.size(); i++) {
            dataPoints[i] = new SimpleDataPoint(
                    (float) goodPeaks.get(i).getMassOverCharge(),
                    (float) goodPeaks.get(i).getIntensity());
        }

        // If we have no peaks with intensity of 0, we assume the scan is
        // centroided
        centroided = (peaks.length == goodPeaks.size());

        // Create new Scan
        SimpleScan newScan = new SimpleScan(scanNumber, msLevel, retentionTime,
                parentScan, precursorMZ, null, dataPoints, centroided);

        return newScan;

    }

    /**
     * Returns total number of scans
     */
    public int getNumberOfScans() {
        return totalScans;
    }

}