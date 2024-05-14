/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class NoiseAmplitudeFeatureResolver extends AbstractResolver {

  // The maximum noise level relative to the maximum intensity.
  private static final double MAX_NOISE_LEVEL = 0.3;
  private final Range<Double> peakDuration;
  private final double minimumPeakHeight;
  private double amplitudeOfNoise = generalParameters.getParameter(NOISE_AMPLITUDE).getValue();

  protected NoiseAmplitudeFeatureResolver(@NotNull ParameterSet parameters,
      @NotNull ModularFeatureList flist) {
    super(parameters, flist);
    peakDuration = generalParameters.getParameter(PEAK_DURATION).getValue();
    minimumPeakHeight = generalParameters.getParameter(MIN_PEAK_HEIGHT).getValue();
  }

  /**
   * This method put a new intensity into a treeMap and score the frequency (the number of times
   * that is present this level of intensity).
   *
   * @param intensity        intensity to add to map.
   * @param binsFrequency    map of bins to add to.
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
   * @param binsFrequency    bins holding intensity frequencies.
   * @param maxIntensity     maximum intensity.
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
  public Class<? extends MZmineProcessingModule> getModuleClass() {
    return NoiseAmplitudeResolverModule.class;
  }

  @Override
  public @NotNull List<Range<Double>> resolve(double[] x, double[] intensities) {
    if (x.length != intensities.length) {
      throw new AssertionError("Length of x, y and indices array does not match.");
    }

    final int scanCount = x.length;

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

    final List<Range<Double>> resolvedPeaks = new ArrayList<>();

    // If the current chromatogram has characteristics of background or just
    // noise.
    if (avgIntensity <= maxIntensity / 2.0) {

      final double noiseThreshold =
          getNoiseThreshold(binsFrequency, maxIntensity, amplitudeOfNoise);

      boolean activePeak = false;

      int currentPeakStart = 0;
      double currentPeakMax = 0d;
      for (int i = 0; i < scanCount; i++) {

        if (intensities[i] > noiseThreshold && !activePeak) {
          currentPeakStart = i;
          activePeak = true;
        }

        if(activePeak) {
          currentPeakMax = Math.max(currentPeakMax, intensities[i]);
        }

        if (intensities[i] <= noiseThreshold && activePeak) {
          int currentPeakEnd = i;

          // If the last data point is zero, ignore it.
          if (intensities[currentPeakEnd] == 0.0) {
            currentPeakEnd--;
          }

          if (currentPeakEnd - currentPeakStart > 0) {
            final Range<Double> featureRange = Range.closed(x[currentPeakStart], x[currentPeakEnd]);
            if (peakDuration.contains(x[currentPeakEnd] - x[currentPeakStart])
                && currentPeakMax >= minimumPeakHeight) {
              resolvedPeaks.add(featureRange);
            }
            currentPeakMax = 0;
          }

          activePeak = false;
        }
      }
    }

    return resolvedPeaks;
  }
}
