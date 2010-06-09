/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import java.util.Arrays;
import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.util.MathUtils;

/**
 * This class implements a peak builder using a match score to link MzPeaks in
 * the axis of retention time. Also uses Savitzky-Golay coefficients to
 * calculate the first and second derivative (smoothed) of raw data points
 * (intensity) that conforms each peak. The first derivative is used to
 * determine the peak's range, and the second derivative to determine the
 * intensity of the peak.
 * 
 */
public class SavitzkyGolayPeakDetector implements PeakResolver {

    private double minimumPeakHeight, minimumPeakDuration,
            derivativeThresholdLevel;

    /**
     * Constructor of Savitzky-Golay Peak Builder
     * 
     * @param parameters
     */
    public SavitzkyGolayPeakDetector(
            SavitzkyGolayPeakDetectorParameters parameters) {

        minimumPeakDuration = (Double) parameters.getParameterValue(SavitzkyGolayPeakDetectorParameters.minimumPeakDuration);
        minimumPeakHeight = (Double) parameters.getParameterValue(SavitzkyGolayPeakDetectorParameters.minimumPeakHeight);
        derivativeThresholdLevel = (Double) parameters.getParameterValue(SavitzkyGolayPeakDetectorParameters.derivativeThresholdLevel);

    }

    /**
     */
    public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
            int scanNumbers[], double retentionTimes[], double intensities[]) {

        Vector<ChromatographicPeak> resolvedPeaks = new Vector<ChromatographicPeak>();

        double maxIntensity = 0;

        double avgChromatoIntensities = 0;
        Arrays.sort(scanNumbers);

        for (int i = 0; i < scanNumbers.length; i++) {
            if (intensities[i] > maxIntensity)
                maxIntensity = intensities[i];
            avgChromatoIntensities += intensities[i];
        }

        avgChromatoIntensities /= scanNumbers.length;

        // If the current chromatogram has characteristics of background or just
        // noise
        // return an empty array.
        if ((avgChromatoIntensities) > (maxIntensity * 0.5f))
            return resolvedPeaks.toArray(new ResolvedPeak[0]);

        double[] chromato2ndDerivative = SGDerivative.calculateDerivative(
                intensities, false, 12);
        double noiseThreshold = calcDerivativeThreshold(chromato2ndDerivative,
                derivativeThresholdLevel);

        ChromatographicPeak[] resolvedOriginalPeaks = SGPeaksSearch(
                chromatogram, chromato2ndDerivative, noiseThreshold);

        // Apply final filter of detected peaks, according with setup parameters
        if (resolvedOriginalPeaks.length != 0) {
            for (ChromatographicPeak p : resolvedOriginalPeaks) {
                double pLength = p.getRawDataPointsRTRange().getSize();
                double pHeight = p.getHeight();
                if ((pLength >= minimumPeakDuration)
                        && (pHeight >= minimumPeakHeight)) {
                    resolvedPeaks.add(p);
                }
            }
        }

        return resolvedPeaks.toArray(new ChromatographicPeak[0]);

    }

    /**
     * 
     * 
     * @param dataFile
     * @param chromatogram
     * @param scanNumbers
     * @param derivativeOfIntensities
     * @param noiseThreshold
     * 
     * @return ChromatographicPeak[]
     */
    private ChromatographicPeak[] SGPeaksSearch(
            ChromatographicPeak chromatogram, double[] derivativeOfIntensities,
            double noiseThreshold) {

        // flag to identify the current and next
        // overlapped peak
        boolean activeFirstPeak = false;
        boolean activeSecondPeak = false;
        // flag to indicate the value of 2nd
        // derivative pass noise threshold level
        boolean passThreshold = false;
        // number of times that 2nd derivative cross zero
        // value for the current peak detection
        int crossZero = 0;

        Vector<ChromatographicPeak> resolvedPeaks = new Vector<ChromatographicPeak>();
        int totalNumberPoints = derivativeOfIntensities.length;

        // Indexes of start and ending of the current peak and beginning of the
        // next
        int currentPeakStart = totalNumberPoints;
        int nextPeakStart = totalNumberPoints;
        int currentPeakEnd = 0;

        /* DEBUGGING
        Chromatogram derivativeChromatogram = new
        Chromatogram(chromatogram.getDataFile());
        */

        /* Shape analysis of derivative of chromatogram
        * "*" represents the original chromatogram shape.
        * "-" represents the shape of chromatogram's derivative.
        *
        * "        ***
        * "       *   *   +
        * "    + *     * + +
        * "   + x       x   +
        * "--+-*-+-----+-*---+----
        * "       +   +
        * "        + +
        * "         +
        * 
        * */
         
        for (int i = 1; i < totalNumberPoints; i++) {

            /* DEBUGGING
            derivativeChromatogram.addMzPeak(scanNumbers[i],
            new SimpleMzPeak(new SimpleDataPoint(chromatogram.getMZ(),
            derivativeOfIntensities[i]*100)));
            */

            // Changing sign and crossing zero
            if (((derivativeOfIntensities[i - 1] < 0.0f) && (derivativeOfIntensities[i] > 0.0f))
                    || ((derivativeOfIntensities[i - 1] > 0.0f) && (derivativeOfIntensities[i] < 0.0f))) {

                if ((derivativeOfIntensities[i - 1] < 0.0f)
                        && (derivativeOfIntensities[i] > 0.0f)) {
                    if (crossZero == 2) {
                        // After second crossing zero starts the next overlapped
                        // peak, but depending of passThreshold flag is
                        // activated
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

                // Finalize the first overlapped peak
                if (crossZero == 3) {
                    activeFirstPeak = false;
                    currentPeakEnd = i;
                }

                // Increments when detect a crossing zero event
                passThreshold = false;
                if ((activeFirstPeak) || (activeSecondPeak)) {
                    crossZero++;
                }

            }

            // Filter for noise threshold level
            if (Math.abs(derivativeOfIntensities[i]) > noiseThreshold) {
                passThreshold = true;
            }

            // Start peak region
            if ((crossZero == 0) && (derivativeOfIntensities[i] > 0)
                    && (!activeFirstPeak)) {
                activeFirstPeak = true;
                currentPeakStart = i;
                crossZero++;
            }

            // Finalize the peak region in case of zero values.
            if ((derivativeOfIntensities[i - 1] == 0)
                    && (derivativeOfIntensities[i] == 0) && (activeFirstPeak)) {
                if (crossZero < 3) {
                    currentPeakEnd = 0;
                } else {
                    currentPeakEnd = i;
                }
                activeFirstPeak = false;
                activeSecondPeak = false;
                crossZero = 0;
            }

            // If exists a detected area (difference between indexes) create a
            // new resolved peak for this region of the chromatogram
            if ((currentPeakEnd - currentPeakStart > 0) && (!activeFirstPeak)) {

                ResolvedPeak newPeak = new ResolvedPeak(chromatogram,
                        currentPeakStart, currentPeakEnd);
                resolvedPeaks.add((ChromatographicPeak) newPeak);

                // If exists next overlapped peak, swap the indexes between next
                // and current, and clean ending index for this new current peak
                if (activeSecondPeak) {
                    activeSecondPeak = false;
                    activeFirstPeak = true;
                    if (derivativeOfIntensities[i] > 0)
                        crossZero = 1;
                    else
                        crossZero = 2;
                    passThreshold = false;
                    currentPeakStart = nextPeakStart;
                    nextPeakStart = totalNumberPoints;
                } else {
                    currentPeakStart = totalNumberPoints;
                    nextPeakStart = totalNumberPoints;
                    crossZero = 0;
                    passThreshold = false;
                    currentPeakEnd = 0;
                }
                // Reset the ending variable
                currentPeakEnd = 0;
            }

        }

        // DEBUGGING
        // derivativeChromatogram.finishChromatogram();
        // ChromatographicPeak peak = (ChromatographicPeak)
        // derivativeChromatogram;
        // resolvedPeaks.add(peak);
        // logger.finest("Size of resolved peak array" + resolvedPeaks.size());

        return resolvedPeaks.toArray(new ChromatographicPeak[0]);
    }

    /**
     * Calculates the value (double) according with the comparative threshold.
     * 
     * @param doubel[] chromato derivative intensities
     * @param double comparative threshold level
     * @return double derivative threshold Level
     */
    private static double calcDerivativeThreshold(
            double[] derivativeIntensities, double comparativeThresholdLevel) {

        double[] intensities = new double[derivativeIntensities.length];
        for (int i = 0; i < derivativeIntensities.length; i++) {
            intensities[i] = Math.abs(derivativeIntensities[i]);
        }

        double threshold = MathUtils.calcQuantile(intensities,
                comparativeThresholdLevel);

        return threshold;
    }

}
