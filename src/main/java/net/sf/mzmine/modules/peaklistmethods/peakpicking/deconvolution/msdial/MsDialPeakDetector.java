/*
 * Copyright (C) 2017 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.msdial;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay.SavitzkyGolayPeakDetectorParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.R.REngineType;
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
public class MsDialPeakDetector implements PeakResolver {

    // Savitzky-Golay filter width.
    private static final int SG_FILTER_LEVEL = 12;

    public @Nonnull
    String getName() {
        return "MsDial";
    }

    @Override
    public Feature[] resolvePeaks(final Feature chromatogram,
            ParameterSet parameters,
            RSessionWrapper rSession, double msmsRange, double rTRangeMSMS) {

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

        final int numSmoothPoints = parameters.getParameter(MsDialPeakDetectorParameters.NUM_SMOOTHING_POINTS).getValue();
        final int minDataPoints = parameters.getParameter(MsDialPeakDetectorParameters.MIN_POINTS).getValue();
        final double minAmplitude = parameters.getParameter(MsDialPeakDetectorParameters.MIN_PEAK_HEIGHT).getValue();
//        final double amplitudeNoiseFold = parameters.getParameter(MsDialPeakDetectorParameters.AMPLITUDE_NOISE_FOLD).getValue();
//        final double slopeNoiseFold = parameters.getParameter(MsDialPeakDetectorParameters.SLOPE_NOISE_FOLD).getValue();
//        final double peakTopNoiseFold = parameters.getParameter(MsDialPeakDetectorParameters.PEAKTOP_NOISE_FOLD).getValue();

        List<Range<Integer>> peakRanges = new PeakDetector(minDataPoints, minAmplitude,
                4,2, 2)
                .run(performSmoothing(intensities, numSmoothPoints), retentionTimes);


        final Range<Double> durationRange = parameters.getParameter(MsDialPeakDetectorParameters.PEAK_DURATION).getValue();

        final List<Feature> resolvedPeaks = new ArrayList<Feature>(peakRanges.size());

        for (Range<Integer> range : peakRanges) {
            double duration = retentionTimes[range.upperEndpoint()] - retentionTimes[range.lowerEndpoint()];
            if (durationRange.contains(duration))
                resolvedPeaks.add(new ResolvedPeak(chromatogram, range.lowerEndpoint(), range.upperEndpoint(), msmsRange, rTRangeMSMS));
        }

//        // If the current chromatogram has characteristics of background or just
//        // noise return an empty array.
//        if (avgIntensity <= maxIntensity / 2.0) {
//
//            // Calculate second derivatives of intensity values.
//            final double[] secondDerivative = SGDerivative.calculateDerivative(
//                    intensities, false, SG_FILTER_LEVEL);
//
//            // Calculate noise threshold.
//            final double noiseThreshold = calcDerivativeThreshold(
//                    secondDerivative,
//                    parameters.getParameter(DERIVATIVE_THRESHOLD_LEVEL)
//                            .getValue());
//
//            // Search for peaks.
//            Arrays.sort(scanNumbers);
//            final Feature[] resolvedOriginalPeaks = peaksSearch(chromatogram,
//                    scanNumbers, secondDerivative, noiseThreshold,msmsRange,rTRangeMSMS);
//
//            final Range<Double> peakDuration = parameters.getParameter(
//                    PEAK_DURATION).getValue();
//            final double minimumPeakHeight = parameters.getParameter(
//                    MIN_PEAK_HEIGHT).getValue();
//
//            // Apply final filter of detected peaks, according with setup
//            // parameters.
//            for (final Feature p : resolvedOriginalPeaks) {
//
//                if (peakDuration.contains(RangeUtils.rangeLength(p
//                        .getRawDataPointsRTRange()))
//                        && p.getHeight() >= minimumPeakHeight) {
//
//                    resolvedPeaks.add(p);
//                }
//            }
//        }

        return resolvedPeaks.toArray(new Feature[resolvedPeaks.size()]);
    }

    private double[] performSmoothing(@Nonnull double[] intensities, int numPoints) {
        for (int i = 0; i < intensities.length; ++i) {
            int n = Math.min(numPoints, i + 1);
            double nominator = 0.0;
            for (int j = 1; j <= n; ++j)
                nominator += j * intensities[i - n + j];
            int denominator = n * (n + 1) / 2;
            intensities[i] = nominator / denominator;
        }
        return intensities;
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return MsDialPeakDetectorParameters.class;
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

    @Override
    public REngineType getREngineType(@Nonnull ParameterSet parameters) {
        return REngineType.RSERVE;
    }
}
