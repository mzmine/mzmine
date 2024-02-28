/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude.NoiseAmplitudeFeatureResolverParameters.MIN_PEAK_HEIGHT;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude.NoiseAmplitudeFeatureResolverParameters.NOISE_AMPLITUDE;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude.NoiseAmplitudeFeatureResolverParameters.PEAK_DURATION;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedPeak;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.maths.CenterFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class NoiseAmplitudeFeatureResolver implements FeatureResolver {

  // The maximum noise level relative to the maximum intensity.
  private static final double MAX_NOISE_LEVEL = 0.3;

  @Override
  public @NotNull String getName() {
    return "Noise amplitude";
  }

  @Override
  public Class<? extends MZmineProcessingModule> getModuleClass() {
    return NoiseAmplitudeResolverModule.class;
  }

  @Override
  public ResolvedPeak[] resolvePeaks(final Feature chromatogram, ParameterSet parameters,
      RSessionWrapper rSession, CenterFunction mzCenterFunction, double msmsRange,
      float rTRangeMSMS) {

    List<Scan> scanNumbers = chromatogram.getScanNumbers();
    final int scanCount = scanNumbers.size();
    double retentionTimes[] = new double[scanCount];
    double intensities[] = new double[scanCount];
    for (int i = 0; i < scanCount; i++) {
      final Scan scanNum = scanNumbers.get(i);
      retentionTimes[i] = scanNum.getRetentionTime();
      DataPoint dp = chromatogram.getDataPointAtIndex(i);
      if (dp != null)
        intensities[i] = dp.getIntensity();
      else
        intensities[i] = 0.0;
    }

    final double amplitudeOfNoise = parameters.getParameter(NOISE_AMPLITUDE).getValue();

    // This treeMap stores the score of frequency of intensity ranges
    final TreeMap<Integer, Integer> binsFrequency = new TreeMap<Integer, Integer>();
    double maxIntensity = 0.0;
    double avgIntensity = 0.0;
    for (final double intensity : intensities) {

      addNewIntensity(intensity, binsFrequency, amplitudeOfNoise);
      maxIntensity = Math.max(maxIntensity, intensity);
      avgIntensity += intensity;
    }

    avgIntensity /= scanCount;

    final List<ResolvedPeak> resolvedPeaks = new ArrayList<ResolvedPeak>(2);

    // If the current chromatogram has characteristics of background or just
    // noise.
    if (avgIntensity <= maxIntensity / 2.0) {

      final double noiseThreshold =
          getNoiseThreshold(binsFrequency, maxIntensity, amplitudeOfNoise);

      boolean activePeak = false;

      final Range<Double> peakDuration = parameters.getParameter(PEAK_DURATION).getValue();
      final double minimumPeakHeight = parameters.getParameter(MIN_PEAK_HEIGHT).getValue();

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

            final ResolvedPeak peak = new ResolvedPeak(chromatogram, currentPeakStart,
                currentPeakEnd, mzCenterFunction, msmsRange, rTRangeMSMS);
            if (peakDuration.contains(RangeUtils.rangeLength(peak.getRawDataPointsRTRange()).doubleValue())
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
   * This method put a new intensity into a treeMap and score the frequency (the number of times
   * that is present this level of intensity).
   * 
   * @param intensity intensity to add to map.
   * @param binsFrequency map of bins to add to.
   * @param amplitudeOfNoise noise amplitude.
   */
  private static void addNewIntensity(final double intensity,
      final TreeMap<Integer, Integer> binsFrequency, final double amplitudeOfNoise) {

    final int bin =
        intensity < amplitudeOfNoise ? 1 : (int) Math.floor(intensity / amplitudeOfNoise);
    binsFrequency.put(bin, binsFrequency.containsKey(bin) ? binsFrequency.get(bin) + 1 : 1);
  }

  /**
   * This method returns the noise threshold level. This level is calculated using the intensity
   * with more data points.
   * 
   * @param binsFrequency bins holding intensity frequencies.
   * @param maxIntensity maximum intensity.
   * @param amplitudeOfNoise noise amplitude.
   * @return the intensity level of the highest frequency bin.
   */
  private static double getNoiseThreshold(final TreeMap<Integer, Integer> binsFrequency,
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

    double noiseThreshold = (numberOfBin + 2) * amplitudeOfNoise;
    if (noiseThreshold / maxIntensity > MAX_NOISE_LEVEL) {

      noiseThreshold = amplitudeOfNoise;
    }

    return noiseThreshold;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return NoiseAmplitudeFeatureResolverParameters.class;
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
  public REngineType getREngineType(ParameterSet parameters) {
    return null;
  }
}
