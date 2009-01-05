/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.peakrecognition;

import java.util.Hashtable;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

/**
 * ResolvedPeak
 * 
 */
public class ResolvedPeak implements ChromatographicPeak {

    // Data file of this chromatogram
    private RawDataFile dataFile;

    // Chromatogram m/z, RT, height, area
    private double mz, rt, height, area;

    // Scan numbers
    private int scanNumbers[];

    private Hashtable<Integer, MzPeak> mzPeaksMap;

    // Top intensity scan, fragment scan
    private int representativeScan, fragmentScan;

    // Ranges of raw data points
    private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
            rawDataPointsRTRange;

    /**
     * Initializes this peak using data points from a given chromatogram -
     * regionStart marks the index of the first data point (inclusive),
     * regionEnd marks the index of the last data point (inclusive)
     */
    public ResolvedPeak(ChromatographicPeak chromatogram, int regionStart,
            int regionEnd) {

        this.dataFile = chromatogram.getDataFile();

        // Make an array of scan numbers of this peak
        scanNumbers = new int[regionEnd - regionStart + 1];
        System.arraycopy(dataFile.getScanNumbers(1), regionStart,
                scanNumbers, 0, regionEnd - regionStart + 1);

        mzPeaksMap = new Hashtable<Integer, MzPeak>();

        // Set raw data point ranges, height, rt and representative scan
        height = Double.MIN_VALUE;
        double allMzValues[] = new double[scanNumbers.length];
        for (int i = 0; i < scanNumbers.length; i++) {

            MzPeak mzPeak = chromatogram.getMzPeak(scanNumbers[i]);
            if (mzPeak == null) continue;
            
            mzPeaksMap.put(scanNumbers[i], mzPeak);

            allMzValues[i] = mzPeak.getMZ();

            if (rawDataPointsIntensityRange == null) {
                rawDataPointsIntensityRange = new Range(mzPeak.getIntensity());
                rawDataPointsMZRange = new Range(mzPeak.getMZ());
                rawDataPointsRTRange = new Range(dataFile.getScan(
                        scanNumbers[i]).getRetentionTime());
            } else {
                rawDataPointsRTRange.extendRange(dataFile.getScan(
                        scanNumbers[i]).getRetentionTime());
            }
            for (MzDataPoint dp : mzPeak.getRawDataPoints()) {
                rawDataPointsIntensityRange.extendRange(dp.getIntensity());
                rawDataPointsMZRange.extendRange(dp.getMZ());
            }

            if (height < mzPeak.getIntensity()) {
                height = mzPeak.getIntensity();
                rt = dataFile.getScan(scanNumbers[i]).getRetentionTime();
                representativeScan = scanNumbers[i];
            }
        }

        // Calculate median m/z
        mz = MathUtils.calcQuantile(allMzValues, 0.5f);

        // Update area
        area = 0;
        for (int i = 1; i < scanNumbers.length; i++) {
            MzPeak previousPeak = mzPeaksMap.get(scanNumbers[i - 1]);
            MzPeak currentPeak = mzPeaksMap.get(scanNumbers[i]);
            double previousRT = dataFile.getScan(scanNumbers[i - 1]).getRetentionTime();
            double currentRT = dataFile.getScan(scanNumbers[i]).getRetentionTime();
            double previousHeight = previousPeak != null ? previousPeak.getIntensity() : 0;
            double currentHeight = currentPeak != null ? currentPeak.getIntensity() : 0;
            area += (currentRT - previousRT) * (currentHeight + previousHeight)
                    / 2;
        }

        // Update fragment scan
        fragmentScan = -1;
        double topBasePeak = 0;
        int[] fragmentScanNumbers = dataFile.getScanNumbers(2,
                rawDataPointsRTRange);
        for (int number : fragmentScanNumbers) {
            Scan scan = dataFile.getScan(number);
            if (rawDataPointsMZRange.contains(scan.getPrecursorMZ())) {
                if ((fragmentScan == -1)
                        || (scan.getBasePeak().getIntensity() > topBasePeak)) {
                    fragmentScan = number;
                    topBasePeak = scan.getBasePeak().getIntensity();
                }
            }
        }

    }

    public MzPeak getMzPeak(int scanNumber) {
        return mzPeaksMap.get(scanNumber);
    }

    /**
     * This method returns m/z value of the chromatogram
     */
    public double getMZ() {
        return mz;
    }

    /**
     * This method returns a string with the basic information that defines this
     * peak
     * 
     * @return String information
     */
    public String toString() {
        return PeakUtils.peakToString(this);
    }

    public double getArea() {
        return area;
    }

    public double getHeight() {
        return height;
    }

    public int getMostIntenseFragmentScanNumber() {
        return fragmentScan;
    }

    public PeakStatus getPeakStatus() {
        return PeakStatus.DETECTED;
    }

    public double getRT() {
        return rt;
    }

    public Range getRawDataPointsIntensityRange() {
        return rawDataPointsIntensityRange;
    }

    public Range getRawDataPointsMZRange() {
        return rawDataPointsMZRange;
    }

    public Range getRawDataPointsRTRange() {
        return rawDataPointsRTRange;
    }

    public int getRepresentativeScanNumber() {
        return representativeScan;
    }

    public int[] getScanNumbers() {
        return scanNumbers;
    }

    public RawDataFile getDataFile() {
        return dataFile;
    }

}
