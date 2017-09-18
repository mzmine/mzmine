/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.masslistmethods.chromatogrambuilder;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Vector;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.ScanUtils;

/**
 * Chromatogram implementing ChromatographicPeak.
 */
public class Chromatogram implements Feature {
    
    private SimplePeakInformation peakInfo;
    
    // Data file of this chromatogram
    private RawDataFile dataFile;

    // Data points of the chromatogram (map of scan number -> m/z peak)
    private Hashtable<Integer, DataPoint> dataPointsMap;

    // Chromatogram m/z, RT, height, area
    private double mz, rt, height, area;
    private Double fwhm = null, tf = null, af = null;

    // Top intensity scan, fragment scan
    private int representativeScan = -1, fragmentScan = -1;

    // Ranges of raw data points
    private Range<Double> rawDataPointsIntensityRange, rawDataPointsMZRange,
            rawDataPointsRTRange;

    // A set of scan numbers of a segment which is currently being connected
    private Vector<Integer> buildingSegment;

    // Keep track of last added data point
    private DataPoint lastMzPeak;

    // Number of connected segments, which have been committed by
    // commitBuildingSegment()
    private int numOfCommittedSegments = 0;

    // Isotope pattern. Null by default but can be set later by deisotoping
    // method.
    private IsotopePattern isotopePattern;
    private int charge = 0;

    // Victor Trevino
    private double mzSum = 0;
    private int mzN = 0;

    private final int scanNumbers[];

    public void outputChromToFile(){
        System.out.println("does nothing");
    }

    /**
     * Initializes this Chromatogram
     */
    public Chromatogram(RawDataFile dataFile, int scanNumbers[]) {
        this.dataFile = dataFile;
        this.scanNumbers = scanNumbers;

        rawDataPointsRTRange = dataFile.getDataRTRange(1);

        dataPointsMap = new Hashtable<Integer, DataPoint>();
        buildingSegment = new Vector<Integer>(128);
    }

    /**
     * This method adds a MzPeak to this Chromatogram. All values of this
     * Chromatogram (rt, m/z, intensity and ranges) are updated on request
     * 
     * @param mzValue
     */
    public void addMzPeak(int scanNumber, DataPoint mzValue) {
        dataPointsMap.put(scanNumber, mzValue);
        lastMzPeak = mzValue;
        mzSum += mzValue.getMZ();
        mzN++;
        mz = mzSum / mzN;
        buildingSegment.add(scanNumber);

    }

    public DataPoint getDataPoint(int scanNumber) {
        return dataPointsMap.get(scanNumber);
    }

    /**
     * Returns m/z value of last added data point
     */
    public DataPoint getLastMzPeak() {
        return lastMzPeak;
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
        return "Chromatogram "
                + MZmineCore.getConfiguration().getMZFormat().format(mz)
                + " m/z";
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

    public @Nonnull FeatureStatus getFeatureStatus() {
        return FeatureStatus.DETECTED;
    }

    public double getRT() {
        return rt;
    }

    public @Nonnull Range<Double> getRawDataPointsIntensityRange() {
        return rawDataPointsIntensityRange;
    }

    public @Nonnull Range<Double> getRawDataPointsMZRange() {
        return rawDataPointsMZRange;
    }

    public @Nonnull Range<Double> getRawDataPointsRTRange() {
        return rawDataPointsRTRange;
    }

    public int getRepresentativeScanNumber() {
        return representativeScan;
    }

    public @Nonnull int[] getScanNumbers() {
        return scanNumbers;
    }

    public @Nonnull RawDataFile getDataFile() {
        return dataFile;
    }

    public IsotopePattern getIsotopePattern() {
        return isotopePattern;
    }

    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
        this.isotopePattern = isotopePattern;
    }

    public void finishChromatogram() {

        int allScanNumbers[] = Ints.toArray(dataPointsMap.keySet());
        Arrays.sort(allScanNumbers);

        // Calculate median m/z
        double allMzValues[] = new double[allScanNumbers.length];
        for (int i = 0; i < allScanNumbers.length; i++) {
            allMzValues[i] = dataPointsMap.get(allScanNumbers[i]).getMZ();
        }
        mz = MathUtils.calcQuantile(allMzValues, 0.5f);

        // Update raw data point ranges, height, rt and representative scan
        height = Double.MIN_VALUE;
        for (int i = 0; i < allScanNumbers.length; i++) {

            DataPoint mzPeak = dataPointsMap.get(allScanNumbers[i]);

            // Replace the MzPeak instance with an instance of SimpleDataPoint,
            // to reduce the memory usage. After we finish this Chromatogram, we
            // don't need the additional data provided by the MzPeak

            dataPointsMap.put(allScanNumbers[i], mzPeak);

            if (i == 0) {
                rawDataPointsIntensityRange = Range
                        .singleton(mzPeak.getIntensity());
                rawDataPointsMZRange = Range.singleton(mzPeak.getMZ());
            } else {
                rawDataPointsIntensityRange = rawDataPointsIntensityRange
                        .span(Range.singleton(mzPeak.getIntensity()));
                rawDataPointsMZRange = rawDataPointsMZRange
                        .span(Range.singleton(mzPeak.getMZ()));
            }

            if (height < mzPeak.getIntensity()) {
                height = mzPeak.getIntensity();
                rt = dataFile.getScan(allScanNumbers[i]).getRetentionTime();
                representativeScan = allScanNumbers[i];
            }
        }

        // Update area
        area = 0;
        for (int i = 1; i < allScanNumbers.length; i++) {
            // For area calculation, we use retention time in seconds
            double previousRT = dataFile.getScan(allScanNumbers[i - 1])
                    .getRetentionTime() * 60d;
            double currentRT = dataFile.getScan(allScanNumbers[i])
                    .getRetentionTime() * 60d;
            double previousHeight = dataPointsMap.get(allScanNumbers[i - 1])
                    .getIntensity();
            double currentHeight = dataPointsMap.get(allScanNumbers[i])
                    .getIntensity();
            area += (currentRT - previousRT) * (currentHeight + previousHeight)
                    / 2;
        }

        // Update fragment scan
        fragmentScan = ScanUtils.findBestFragmentScan(dataFile,
                dataFile.getDataRTRange(1), rawDataPointsMZRange);

        if (fragmentScan > 0) {
            Scan fragmentScanObject = dataFile.getScan(fragmentScan);
            int precursorCharge = fragmentScanObject.getPrecursorCharge();
            if (precursorCharge > 0)
                this.charge = precursorCharge;
        }

        rawDataPointsRTRange = null;

        for (int scanNum : allScanNumbers) {
            double scanRt = dataFile.getScan(scanNum).getRetentionTime();
            DataPoint dp = getDataPoint(scanNum);

            if ((dp == null) || (dp.getIntensity() == 0.0))
                continue;

            if (rawDataPointsRTRange == null)
                rawDataPointsRTRange = Range.singleton(scanRt);
            else
                rawDataPointsRTRange = rawDataPointsRTRange
                        .span(Range.singleton(scanRt));
        }

        // Discard the fields we don't need anymore
        buildingSegment = null;
        lastMzPeak = null;

    }

    public double getBuildingSegmentLength() {
        if (buildingSegment.size() < 2)
            return 0;
        int firstScan = buildingSegment.firstElement();
        int lastScan = buildingSegment.lastElement();
        double firstRT = dataFile.getScan(firstScan).getRetentionTime();
        double lastRT = dataFile.getScan(lastScan).getRetentionTime();
        return (lastRT - firstRT);
    }

    public int getNumberOfCommittedSegments() {
        return numOfCommittedSegments;
    }

    public void removeBuildingSegment() {
        for (int scanNumber : buildingSegment)
            dataPointsMap.remove(scanNumber);
        buildingSegment.clear();
    }

    public void commitBuildingSegment() {
        buildingSegment.clear();
        numOfCommittedSegments++;
    }

    public void addDataPointsFromChromatogram(Chromatogram ch) {
        for (Entry<Integer, DataPoint> dp : ch.dataPointsMap.entrySet()) {
            addMzPeak(dp.getKey(), dp.getValue());
        }
    }

    public int getCharge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }

    public Double getFWHM() {
        return fwhm;
    }

    public void setFWHM(Double fwhm) {
        this.fwhm = fwhm;
    }

    public Double getTailingFactor() {
        return tf;
    }

    public void setTailingFactor(Double tf) {
        this.tf = tf;
    }

    public Double getAsymmetryFactor() {
        return af;
    }

    public void setAsymmetryFactor(Double af) {
        this.af = af;
    }
    public void setPeakInformation(SimplePeakInformation peakInfoIn){
        this.peakInfo = peakInfoIn;
    }
    public SimplePeakInformation getPeakInformation(){
        return peakInfo;
    }

}
