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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.noiseamplitude;

import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.noiseamplitude.NoiseAmplitudePeakDetectorParameters.MIN_PEAK_HEIGHT;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.noiseamplitude.NoiseAmplitudePeakDetectorParameters.NOISE_AMPLITUDE;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.noiseamplitude.NoiseAmplitudePeakDetectorParameters.PEAK_DURATION;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.R.RSessionWrapper;

import com.google.common.collect.Range;

/**
 *
 */
public class NoiseAmplitudePeakDetector implements PeakResolver {

    // The maximum noise level relative to the maximum intensity.
    private static final double MAX_NOISE_LEVEL = 0.3;

    public @Nonnull
    String getName() {
        return "Noise amplitude";
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
        
        final double amplitudeOfNoise = parameters
                .getParameter(NOISE_AMPLITUDE).getValue();

        // This treeMap stores the score of frequency of intensity ranges
        final TreeMap<Integer, Integer> binsFrequency = new TreeMap<Integer, Integer>();
        double maxIntensity = 0.0;
        double avgIntensity = 0.0;
        for (final double intensity : intensities) {

            addNewIntensity(intensity, binsFrequency, amplitudeOfNoise);
            maxIntensity = Math.max(maxIntensity, intensity);
            avgIntensity += intensity;
        }

        avgIntensity /= (double) scanCount;

        final List<ResolvedPeak> resolvedPeaks = new ArrayList<ResolvedPeak>(2);

        // If the current chromatogram has characteristics of background or just
        // noise.
        if (avgIntensity <= maxIntensity / 2.0) {

            final double noiseThreshold = getNoiseThreshold(binsFrequency,
                    maxIntensity, amplitudeOfNoise);

            boolean activePeak = false;

            final Range<Double> peakDuration = parameters.getParameter(
                    PEAK_DURATION).getValue();
            final double minimumPeakHeight = parameters.getParameter(
                    MIN_PEAK_HEIGHT).getValue();

            // Index of starting region of the current peak.
            int currentPeakStart = 0;
            for (int i = 0; i < scanCount; i++) {

                if (intensities[i] > noiseThreshold && !activePeak) {

                    currentPeakStart = i;
                    activePeak = true;
                }

                if (intensities[i] <= noiseThreshold && activePeak) {

                    int currentPeakEnd = i;

                    // If the last data point is zero, ignore it.
                    if (intensities[currentPeakEnd] == 0.0) {

                        currentPeakEnd--;
                    }

                    if (currentPeakEnd - currentPeakStart > 0) {

                        final ResolvedPeak peak = new ResolvedPeak(
                                chromatogram, currentPeakStart, currentPeakEnd);
                        if (peakDuration.contains(RangeUtils.rangeLength(peak
                                .getRawDataPointsRTRange()))
                                && peak.getHeight() >= minimumPeakHeight) {

                            resolvedPeaks.add(peak);
                        }
                    }

                    activePeak = false;
                }
            }
        }

        return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
    }

    /**
     * This method put a new intensity into a treeMap and score the frequency
     * (the number of times that is present this level of intensity).
     * 
     * @param intensity
     *            intensity to add to map.
     * @param binsFrequency
     *            map of bins to add to.
     * @param amplitudeOfNoise
     *            noise amplitude.
     */
    private static void addNewIntensity(final double intensity,
            final TreeMap<Integer, Integer> binsFrequency,
            final double amplitudeOfNoise) {

        final int bin = intensity < amplitudeOfNoise ? 1 : (int) Math
                .floor(intensity / amplitudeOfNoise);
        binsFrequency
                .put(bin,
                        binsFrequency.containsKey(bin) ? binsFrequency.get(bin) + 1
                                : 1);
    }

    /**
     * This method returns the noise threshold level. This level is calculated
     * using the intensity with more data points.
     * 
     * @param binsFrequency
     *            bins holding intensity frequencies.
     * @param maxIntensity
     *            maximum intensity.
     * @param amplitudeOfNoise
     *            noise amplitude.
     * @return the intensity level of the highest frequency bin.
     */
    private static double getNoiseThreshold(
            final TreeMap<Integer, Integer> binsFrequency,
            final double maxIntensity, final double amplitudeOfNoise) {

        int numberOfBin = 0;
        int maxFrequency = 0;

        for (final Integer bin : binsFrequency.keySet()) {

            final int freq = binsFrequency.get(bin);
            if (freq > maxFrequency) {

                maxFrequency = freq;
                numberOfBin = bin;
            }
        }

        double noiseThreshold = (double) (numberOfBin + 2) * amplitudeOfNoise;
        if (noiseThreshold / maxIntensity > MAX_NOISE_LEVEL) {

            noiseThreshold = amplitudeOfNoise;
        }

        return noiseThreshold;
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return NoiseAmplitudePeakDetectorParameters.class;
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
