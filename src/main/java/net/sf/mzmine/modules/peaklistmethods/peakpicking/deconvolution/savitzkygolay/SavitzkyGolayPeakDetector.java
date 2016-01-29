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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay.SavitzkyGolayPeakDetectorParameters.DERIVATIVE_THRESHOLD_LEVEL;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay.SavitzkyGolayPeakDetectorParameters.MIN_PEAK_HEIGHT;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay.SavitzkyGolayPeakDetectorParameters.PEAK_DURATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.R.RSessionWrapper;

import com.google.common.collect.Range;

/**
 * This class implements a peak builder using a match score to link MzPeaks in
 * the axis of retention time. Also uses Savitzky-Golay coefficients to
 * calculate the first and second derivative (smoothed) of raw data points
 * (intensity) that conforms each peak. The first derivative is used to
 * determine the peak's range, and the second derivative to determine the
 * intensity of the peak.
 */
public class SavitzkyGolayPeakDetector implements PeakResolver {

    // Savitzky-Golay filter width.
    private static final int SG_FILTER_LEVEL = 12;

    public @Nonnull
    String getName() {
        return "Savitzky-Golay";
    }

    @Override
    public Feature[] resolvePeaks(final Feature chromatogram,
            ParameterSet parameters,
            RSessionWrapper rSession) {

        int scanNumbers[] = chromatogram.getScanNumbers();
        final int scanCount = scanNumbers.length;
        double retentionTimes[] = new double[scanCount];
        double intensities[] = new double[scanCount];
        RawDataFile dataFile = chromatogram.getDataFile();
        for (int i = 0; i < scanCount; i++) {
            final int scanNum = scanNumbers[i];
            retentionTimes[i] = dataFile.getScan(scanNum).getRetentionTime();
            DataPoint dp = chromatogram.getDataPoint(scanNum);
            if (dp != null)
                intensities[i] = dp.getIntensity();
            else
                intensities[i] = 0.0;
        }
        
        // Calculate intensity statistics.
        double maxIntensity = 0.0;
        double avgIntensity = 0.0;
        for (final double intensity : intensities) {

            maxIntensity = Math.max(intensity, maxIntensity);
            avgIntensity += intensity;
        }

        avgIntensity /= (double) scanCount;

        final List<Feature> resolvedPeaks = new ArrayList<Feature>(2);

        // If the current chromatogram has characteristics of background or just
        // noise return an empty array.
        if (avgIntensity <= maxIntensity / 2.0) {

            // Calculate second derivatives of intensity values.
            final double[] secondDerivative = SGDerivative.calculateDerivative(
                    intensities, false, SG_FILTER_LEVEL);

            // Calculate noise threshold.
            final double noiseThreshold = calcDerivativeThreshold(
                    secondDerivative,
                    parameters.getParameter(DERIVATIVE_THRESHOLD_LEVEL)
                            .getValue());

            // Search for peaks.
            Arrays.sort(scanNumbers);
            final Feature[] resolvedOriginalPeaks = peaksSearch(chromatogram,
                    scanNumbers, secondDerivative, noiseThreshold);

            final Range<Double> peakDuration = parameters.getParameter(
                    PEAK_DURATION).getValue();
            final double minimumPeakHeight = parameters.getParameter(
                    MIN_PEAK_HEIGHT).getValue();

            // Apply final filter of detected peaks, according with setup
            // parameters.
            for (final Feature p : resolvedOriginalPeaks) {

                if (peakDuration.contains(RangeUtils.rangeLength(p
                        .getRawDataPointsRTRange()))
                        && p.getHeight() >= minimumPeakHeight) {

                    resolvedPeaks.add(p);
                }
            }
        }

        return resolvedPeaks.toArray(new Feature[resolvedPeaks.size()]);
    }

    /**
     * Search for peaks.
     * 
     * @param chromatogram
     *            the chromatogram to search.
     * @param scanNumbers
     *            scan number to focus search on
     * @param derivativeOfIntensities
     *            derivatives of intensity values.
     * @param noiseThreshold
     *            noise threshold.
     * @return array of peaks found.
     */
    private static Feature[] peaksSearch(final Feature chromatogram,
            final int[] scanNumbers, final double[] derivativeOfIntensities,
            final double noiseThreshold) {

        // Flag to identify the current and next overlapped peak.
        boolean activeFirstPeak = false;
        boolean activeSecondPeak = false;

        // Flag to indicate the value of 2nd derivative pass noise threshold
        // level.
        boolean passThreshold = false;

        // Number of times that 2nd derivative cross zero value for the current
        // peak detection.
        int crossZero = 0;

        final int totalNumberPoints = derivativeOfIntensities.length;

        // Indexes of start and ending of the current peak and beginning of the
        // next.
        int currentPeakStart = totalNumberPoints;
        int nextPeakStart = totalNumberPoints;
        int currentPeakEnd = 0;

        final List<Feature> resolvedPeaks = new ArrayList<Feature>(2);

        // Shape analysis of derivative of chromatogram "*" represents the
        // original chromatogram shape. "-" represents
        // the shape of chromatogram's derivative.
        //
        // " *** " * * + " + * * + + " + x x + "--+-*-+-----+-*---+---- " + + "
        // + + " +
        //
        for (int i = 1; i < totalNumberPoints; i++) {

            // Changing sign and crossing zero
            if (derivativeOfIntensities[i - 1] < 0.0
                    && derivativeOfIntensities[i] > 0.0
                    || derivativeOfIntensities[i - 1] > 0.0
                    && derivativeOfIntensities[i] < 0.0) {

                if (derivativeOfIntensities[i - 1] < 0.0
                        && derivativeOfIntensities[i] > 0.0) {

                    if (crossZero == 2) {

                        // After second crossing zero starts the next overlapped
                        // peak, but depending of
                        // passThreshold flag is activated.
                        if (passThreshold) {

                            activeSecondPeak = true;
                            nextPeakStart = i;

                        } else {

                            currentPeakStart = i;
                            crossZero = 0;
                            activeFirstPeak = true;
                        }
                    }
                }

                // Finalize the first overlapped peak.
                if (crossZero == 3) {

                    activeFirstPeak = false;
                    currentPeakEnd = i;
                }

                // Increments when detect a crossing zero event
                passThreshold = false;
                if (activeFirstPeak || activeSecondPeak) {

                    crossZero++;
                }
            }

            // Filter for noise threshold level.
            if (Math.abs(derivativeOfIntensities[i]) > noiseThreshold) {

                passThreshold = true;
            }

            // Start peak region.
            if (crossZero == 0 && derivativeOfIntensities[i] > 0.0
                    && !activeFirstPeak) {

                activeFirstPeak = true;
                currentPeakStart = i;
                crossZero++;
            }

            // Finalize the peak region in case of zero values.
            if (derivativeOfIntensities[i - 1] == 0.0
                    && derivativeOfIntensities[i] == 0.0 && activeFirstPeak) {

                currentPeakEnd = crossZero < 3 ? 0 : i;
                activeFirstPeak = false;
                activeSecondPeak = false;
                crossZero = 0;
            }

            // If the peak starts in a region with no data points, move the
            // start to the first available data point.
            while (currentPeakStart < scanNumbers.length - 1) {

                if (chromatogram.getDataPoint(scanNumbers[currentPeakStart]) == null) {

                    currentPeakStart++;

                } else {

                    break;
                }
            }

            // Scan the peak from the beginning and if we find a missing data
            // point inside, we have to finish the
            // peak there.
            for (int newEnd = currentPeakStart; newEnd <= currentPeakEnd; newEnd++) {

                if (chromatogram.getDataPoint(scanNumbers[newEnd]) == null) {

                    currentPeakEnd = newEnd - 1;
                    break;
                }
            }

            // If exists a detected area (difference between indexes) create a
            // new resolved peak for this region of
            // the chromatogram.
            if (currentPeakEnd - currentPeakStart > 0 && !activeFirstPeak) {

                resolvedPeaks.add(new ResolvedPeak(chromatogram,
                        currentPeakStart, currentPeakEnd));

                // If exists next overlapped peak, swap the indexes between next
                // and current, and clean ending index
                // for this new current peak.
                if (activeSecondPeak) {

                    activeSecondPeak = false;
                    activeFirstPeak = true;
                    crossZero = derivativeOfIntensities[i] > 0.0 ? 1 : 2;
                    currentPeakStart = nextPeakStart;

                } else {

                    crossZero = 0;
                    currentPeakStart = totalNumberPoints;
                }

                passThreshold = false;
                nextPeakStart = totalNumberPoints;
                currentPeakEnd = 0;
            }
        }

        return resolvedPeaks.toArray(new Feature[resolvedPeaks.size()]);
    }

    /**
     * Calculates the value according with the comparative threshold.
     * 
     * @param derivativeIntensities
     *            intensity first derivative.
     * @param comparativeThresholdLevel
     *            threshold.
     * @return double derivative threshold level.
     */
    private static double calcDerivativeThreshold(
            final double[] derivativeIntensities,
            final double comparativeThresholdLevel) {

        final int length = derivativeIntensities.length;
        final double[] intensities = new double[length];
        for (int i = 0; i < length; i++) {

            intensities[i] = Math.abs(derivativeIntensities[i]);
        }

        return MathUtils.calcQuantile(intensities, comparativeThresholdLevel);
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return SavitzkyGolayPeakDetectorParameters.class;
    }

    @Override
    public boolean getRequiresR() {
        return false;
    }

    @Override
    public String[] getRequiredRPackages() {
        return null;
    }

    @Override
    public String[] getRequiredRPackagesVersions() {
        return null;
    }
}
