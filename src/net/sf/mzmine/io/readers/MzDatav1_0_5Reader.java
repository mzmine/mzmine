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
import java.util.Map;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.RawDataFileReader;

import org.proteomecommons.io.mzdata.v1_05.MzDataPeakList;
import org.proteomecommons.io.mzdata.v1_05.MzDataPeakListReader;
import org.proteomecommons.io.mzdata.v1_05.MzDataPeakListSpectrumSettings;

/**
 * 
 */
@Deprecated
public class MzDatav1_0_5Reader implements RawDataFileReader {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File originalFile;
    private MzDataPeakListReader parser;

    int totalScans = -1;

    public MzDatav1_0_5Reader(File originalFile) {
        this.originalFile = originalFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataFileReader#startReading()
     */
    public void startReading() throws IOException {

        try {
            parser = new MzDataPeakListReader(originalFile.getPath());
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

        MzDataPeakList peakList = (MzDataPeakList) parser.getPeakList();

        // End of file
        if (peakList == null)
            return null;

        MzDataPeakListSpectrumSettings spectrumSettings = peakList.getSpectrumSettings();
        Map spectrumInstrumentParamaters = spectrumSettings.getSpectrumInstrumentParamaters();

        // Set total scans if we haven's set it yet
        if (totalScans <= 0) {
            totalScans = -1; // TODO

        }

        // Prepare variables
        int scanNumber, msLevel, parentScan;
        float retentionTime, precursorMZ;
        boolean centroided;

        // Get scan number and MS level
        scanNumber = spectrumSettings.acqNumber();
        msLevel = spectrumSettings.msLevel();

        // Parse retention time
        if (spectrumInstrumentParamaters != null) {
            String retentionTimeStr = (String) spectrumInstrumentParamaters.get("TimeInMinutes");

        }
        retentionTime = 0;

        // Parse precursor scan details
        if (peakList.getPrecursorList().getPrecursors() != null) {
            parentScan = 0; // Integer.parseInt(peakList.getPrecursorScanNum());
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