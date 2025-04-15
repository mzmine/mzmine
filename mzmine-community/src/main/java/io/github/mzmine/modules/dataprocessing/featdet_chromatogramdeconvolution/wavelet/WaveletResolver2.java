/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.parameters.ParameterSet;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class WaveletResolver2 extends AbstractResolver {

  // --- Configuration Parameters ---

  // Scales for CWT: Determine the widths of peaks to look for.
  // These are often determined empirically based on expected peak widths.
  // Example: Start with a scale roughly matching the smallest expected peak width (in samples),
  // end with a scale matching the largest, using logarithmic spacing.
  private final double MIN_SCALE; // Min wavelet width in samples (adjust based on data)
  private final double MAX_SCALE; // Max wavelet width in samples (adjust based on data)
  private final int NUM_SCALES = 20;    // Number of scales to analyze

  // SNR Filtering
  private final double MIN_SNR_THRESHOLD; // Minimum Signal-to-Noise Ratio to accept a peak

  // Peak Finding Configuration
  // How close (in samples) peaks can be before being considered the same (for suppression)
  // This should be related to the wavelet scales used. A value around MIN_SCALE is often reasonable.
  private final double PEAK_PROXIMITY_FACTOR = 0.75; // Factor of scale for suppression distance

  // Wavelet Generation Configuration
  // Determines the length of the discrete wavelet kernel based on scale. 5.0 is common for Mexican Hat.
  private final double WAVELET_SUPPORT_FACTOR = 5.0;

  protected WaveletResolver2(@NotNull ParameterSet parameters, @NotNull ModularFeatureList flist) {
    super(parameters, flist);
    MIN_SCALE = parameters.getValue(WaveletResolverParameters.minScale);
    MAX_SCALE = parameters.getValue(WaveletResolverParameters.maxScale);
    MIN_SNR_THRESHOLD = parameters.getValue(WaveletResolverParameters.snr);
  }


  // --- Inner Class for Candidate Peaks ---
  private static class PeakCandidate implements Comparable<PeakCandidate> {

    final double cwtValue; // Max CWT coefficient value for this peak candidate
    final int index;       // Index (position) in the signal of the CWT maximum
    final double scale;    // Scale at which the maximum CWT value occurred
    final int scaleIndex;  // Index of the scale in the scales array

    PeakCandidate(double cwtValue, int index, double scale, int scaleIndex) {
      this.cwtValue = cwtValue;
      this.index = index;
      this.scale = scale;
      this.scaleIndex = scaleIndex;
    }

    // Sort by CWT value descending to process strongest peaks first
    @Override
    public int compareTo(PeakCandidate other) {
      return Double.compare(other.cwtValue, this.cwtValue);
    }

    @Override
    public String toString() {
      return String.format("Candidate[index=%d, scale=%.2f, value=%.3f]", index, scale, cwtValue);
    }
  }

  // --- Main Detection Method ---

  /**
   * Detects peaks in the given time series data using Mexican Hat CWT and SNR filtering.
   *
   * @param x Time/position values (must be sorted ascending).
   * @param y Signal amplitude values.
   * @return A List of Range objects, where each Range contains the start and end x-value of a
   * detected peak.
   */
  public List<Range<Double>> resolve(double[] x, double[] y) {
    if (x == null || y == null || x.length != y.length || x.length < 3) {
      throw new IllegalArgumentException(
          "Input arrays are invalid (null, mismatched lengths, or too short).");
    }
    // Ensure x is sorted (or check if needed based on assumptions)
    // for (int i = 1; i < x.length; i++) {
    //     if (x[i] < x[i-1]) {
    //         throw new IllegalArgumentException("x array must be sorted ascending.");
    //     }
    // }

    int n = y.length;

    // 1. Generate Scales for CWT
    double[] scales = generateLogarithmicScales(MIN_SCALE, MAX_SCALE, NUM_SCALES);

    // 2. Compute Continuous Wavelet Transform (CWT)
    double[][] cwtMatrix = computeCWT(y, scales);

    // 3. Estimate Noise Level from CWT coefficients at the smallest scale
    double noiseLevel = estimateNoiseFromCWT(cwtMatrix[0]); // Smallest scale CWT coefficients

    // 4. Find Local Maxima in CWT Matrix (potential peak centers)
    List<PeakCandidate> candidates = findCwtLocalMaxima(cwtMatrix, scales);

    // 5. Filter Candidates by SNR and Perform Non-Maximum Suppression
    List<PeakCandidate> significantPeaks = filterPeaksBySNRAndProximity(candidates, noiseLevel,
        MIN_SNR_THRESHOLD, scales);

    // 6. Determine Peak Boundaries in the Original Signal
    List<Range<Double>> peakRanges = findPeakBoundaries(significantPeaks, x, y, noiseLevel);

    return peakRanges;
  }

  @Override
  public @NotNull Class<? extends MZmineModule> getModuleClass() {
    return WaveletResolverModule.class;
  }

  // --- Helper Methods ---

  /**
   * Generates logarithmically spaced scales.
   */
  private double[] generateLogarithmicScales(double minScale, double maxScale, int numScales) {
    if (numScales <= 0) {
      return new double[0];
    }
    if (numScales == 1) {
      return new double[]{minScale};
    }
    double[] scales = new double[numScales];
    double factor = Math.pow(maxScale / minScale, 1.0 / (numScales - 1));
    scales[0] = minScale;
    for (int i = 1; i < numScales; i++) {
      scales[i] = scales[i - 1] * factor;
    }
    // Ensure maxScale is included precisely if needed, although log spacing usually preferred
    // scales[numScales - 1] = maxScale;
    return scales;
  }

  /**
   * Computes the CWT of the signal using the Mexican Hat wavelet for different scales.
   */
  private double[][] computeCWT(double[] signal, double[] scales) {
    int n = signal.length;
    int numScales = scales.length;
    double[][] cwtMatrix = new double[numScales][n];

    for (int i = 0; i < numScales; i++) {
      double scale = scales[i];
      // Determine wavelet length based on scale
      int waveletLength = (int) Math.max(3, 2 * Math.ceil(WAVELET_SUPPORT_FACTOR * scale) + 1);
      if (waveletLength % 2 == 0) {
        waveletLength++; // Ensure odd length for symmetry
      }

      double[] wavelet = generateMexicanHatWavelet(waveletLength, scale);
      wavelet = normalizeWaveletL2(wavelet); // Normalize for scale comparison

      cwtMatrix[i] = convolve(signal, wavelet);
    }
    return cwtMatrix;
  }

  /**
   * Generates a discrete Mexican Hat (Ricker) wavelet.
   */
  private double[] generateMexicanHatWavelet(int length, double scale) {
    if (length < 3 || length % 2 == 0) {
      throw new IllegalArgumentException("Wavelet length must be odd and >= 3.");
    }
    double[] wavelet = new double[length];
    int center = length / 2;
    double variance = scale * scale;
    double normalizFactor =
        2.0 / (Math.sqrt(3 * scale) * Math.pow(Math.PI, 0.25)); // Common normalization

    for (int i = 0; i < length; i++) {
      double t = (double) (i - center); // Time relative to center
      double tScaledSq = (t * t) / variance;
      wavelet[i] = (1.0 - tScaledSq) * Math.exp(-tScaledSq / 2.0);
    }

    // Optional: Adjust to have zero mean (important for wavelets)
    // Due to discretization, the mean might not be exactly zero.
    double mean = Arrays.stream(wavelet).average().orElse(0.0);
    for (int i = 0; i < length; ++i) {
      wavelet[i] -= mean;
    }

    // Apply normalization factor (or use L2 norm below) - choose one approach
    // for(int i=0; i<length; ++i) {
    //     wavelet[i] *= normalizFactor;
    // }

    return wavelet;
  }

  /**
   * Normalizes the wavelet to have an L2 norm (energy) of 1.
   */
  private double[] normalizeWaveletL2(double[] wavelet) {
    double sumSq = 0;
    for (double v : wavelet) {
      sumSq += v * v;
    }
    double norm = Math.sqrt(sumSq);
    if (norm > 1e-9) { // Avoid division by zero
      double[] normalized = new double[wavelet.length];
      for (int i = 0; i < wavelet.length; i++) {
        normalized[i] = wavelet[i] / norm;
      }
      return normalized;
    }
    return wavelet; // Return original if norm is near zero
  }


  /**
   * Performs 1D convolution with 'same' boundary handling (output size = signal size).
   */
  private double[] convolve(double[] signal, double[] kernel) {
    int n = signal.length;
    int m = kernel.length;
    double[] output = new double[n];
    int kernelCenter = m / 2;

    for (int i = 0; i < n; i++) {
      double sum = 0;
      for (int j = 0; j < m; j++) {
        int signalIndex = i - kernelCenter + j;

        // Simple boundary handling: treat out-of-bounds as zero (or use reflection/padding)
        if (signalIndex >= 0 && signalIndex < n) {
          sum += signal[signalIndex] * kernel[j];
        }
        // More robust: Reflection padding
        // else if (signalIndex < 0) {
        //     sum += signal[-signalIndex] * kernel[j]; // Reflect start
        // } else { // signalIndex >= n
        //     sum += signal[2 * n - signalIndex - 2] * kernel[j]; // Reflect end
        // }
      }
      output[i] = sum;
    }
    return output;
  }

  /**
   * Estimates noise level using Median Absolute Deviation (MAD) on CWT coefficients.
   */
  private double estimateNoiseFromCWT(double[] cwtCoeffs) {
    if (cwtCoeffs == null || cwtCoeffs.length == 0) {
      return 1e-6; // Avoid division by zero
    }

    // Calculate median of the absolute coefficients
    double[] absCoeffs = Arrays.stream(cwtCoeffs).map(Math::abs).sorted().toArray();
    double medianAbs;
    int n = absCoeffs.length;
    if (n == 0) {
      return 1e-6;
    }
    if (n % 2 == 1) {
      medianAbs = absCoeffs[n / 2];
    } else {
      medianAbs = (absCoeffs[n / 2 - 1] + absCoeffs[n / 2]) / 2.0;
    }

    // MAD constant for Gaussian noise (0.6745)
    // Noise_stddev ≈ MAD / 0.6745
    // We use the median absolute value directly as a robust noise measure here.
    // For strict MAD: calculate median(abs(cwtCoeffs - median(cwtCoeffs))).
    // Using median(abs(coeffs)) is simpler and often effective when noise mean is near zero.
    double noiseLevel = medianAbs / 0.6745;

    return Math.max(noiseLevel, 1e-9); // Prevent zero or negative noise level
  }


  /**
   * Finds local maxima in each scale row of the CWT matrix.
   */
  private List<PeakCandidate> findCwtLocalMaxima(double[][] cwtMatrix, double[] scales) {
    List<PeakCandidate> candidates = new ArrayList<>();
    int numScales = cwtMatrix.length;
    if (numScales == 0) {
      return candidates;
    }
    int n = cwtMatrix[0].length;

    for (int i = 0; i < numScales; i++) { // Iterate through scales
      double[] cwtRow = cwtMatrix[i];
      double scale = scales[i];

      for (int j = 1; j < n - 1; j++) { // Iterate through positions (skip edges)
        // Simple local maximum check: greater than immediate neighbors
        if (cwtRow[j] > cwtRow[j - 1] && cwtRow[j] > cwtRow[j + 1]
            && cwtRow[j] > 0) { // Only consider positive CWT coeffs for peaks
          candidates.add(new PeakCandidate(cwtRow[j], j, scale, i));
        }
        // Optional: Plateau handling (>=) - can lead to broader peaks being detected multiple times initially
        // else if (cwtRow[j] > cwtRow[j - 1] && cwtRow[j] == cwtRow[j + 1] && cwtRow[j] > 0) {
        //    // Start of a plateau, find its end
        //    int k = j + 1;
        //    while (k < n - 1 && cwtRow[k] == cwtRow[j]) {
        //        k++;
        //    }
        //    // Check if it drops after the plateau
        //    if (k < n && cwtRow[k] < cwtRow[j]) {
        //        int plateauCenter = (j + k -1) / 2; // Center of the plateau
        //        candidates.add(new PeakCandidate(cwtRow[plateauCenter], plateauCenter, scale, i));
        //        j = k; // Skip processed plateau points
        //    }
        //}
      }
    }
    return candidates;
  }

  /**
   * Filters candidates by SNR and suppresses nearby weaker peaks.
   */
  private List<PeakCandidate> filterPeaksBySNRAndProximity(List<PeakCandidate> candidates,
      double noiseLevel, double minSNR, double[] scales) {
    // 1. Filter by SNR
    List<PeakCandidate> snrFiltered = candidates.stream()
        .filter(p -> (p.cwtValue / noiseLevel) >= minSNR).collect(Collectors.toList());

    // 2. Sort by CWT value descending (strongest first)
    Collections.sort(snrFiltered); // Uses compareTo in PeakCandidate

    // 3. Non-Maximum Suppression
    List<PeakCandidate> finalPeaks = new ArrayList<>();
    boolean[] isSuppressed = new boolean[snrFiltered.size()];

    for (int i = 0; i < snrFiltered.size(); i++) {
      if (isSuppressed[i]) {
        continue;
      }

      PeakCandidate currentPeak = snrFiltered.get(i);
      finalPeaks.add(currentPeak); // Keep the strongest peak in a neighborhood

      // Define suppression window based on the current peak's scale
      // Peaks are considered 'close' if within roughly +/- scale samples
      double proximityThreshold = currentPeak.scale * PEAK_PROXIMITY_FACTOR;

      // Suppress weaker peaks nearby
      for (int j = i + 1; j < snrFiltered.size(); j++) {
        if (isSuppressed[j]) {
          continue;
        }
        PeakCandidate otherPeak = snrFiltered.get(j);

        if (Math.abs(currentPeak.index - otherPeak.index) <= proximityThreshold) {
          isSuppressed[j] = true; // Suppress the weaker peak
        }
      }
    }

    // Optional: Sort final peaks by index (position) before returning
    finalPeaks.sort(Comparator.comparingInt(p -> p.index));

    return finalPeaks;
  }

  /**
   * Finds the start and end boundaries of peaks in the original signal `y` based on the identified
   * peak centers.
   *
   * @param peaks      List of confirmed PeakCandidate objects (sorted by index is helpful).
   * @param x          The original x-values.
   * @param y          The original y-values.
   * @param noiseLevel Estimated noise level (can be used for thresholding).
   * @return List of Range<Double> corresponding to peak boundaries in x.
   */
  private List<Range<Double>> findPeakBoundaries(List<PeakCandidate> peaks, double[] x, double[] y,
      double noiseLevel) {
    List<Range<Double>> ranges = new ArrayList<>();
    int n = y.length;
    if (n == 0 || peaks.isEmpty()) {
      return ranges;
    }

    // Baseline estimation (simple approach: use noise level, or could be more sophisticated)
    //double baselineThreshold = noiseLevel * 1.5; // Example threshold slightly above noise
    // Alternative: Use a fraction of the peak height
    double boundaryHeightFraction = 0.05; // Find where signal drops to 5% of peak height relative to baseline (can adjust)

    for (PeakCandidate peak : peaks) {
      int peakIndex = peak.index;

      // Ensure peakIndex is valid
      if (peakIndex < 0 || peakIndex >= n) {
        continue;
      }

      double peakHeight = y[peakIndex];
      // Simple baseline: Assume 0 or use a more robust estimate if needed
      double baseline = 0; // Adjust if your signal has a significant offset
      double threshold = baseline + boundaryHeightFraction * (peakHeight - baseline);
      // Ensure threshold is at least slightly above baseline/noise floor
      threshold = Math.max(threshold, baseline + noiseLevel * 0.5);

      // Find start index: move left from peak index
      int startIndex = peakIndex;
      while (startIndex > 0
          && y[startIndex - 1] < y[startIndex]) { // While signal is rising towards the peak
        startIndex--;
        // Optional break condition: stop if signal drops below threshold (prevents going too far on noisy baselines)
        // if (y[startIndex] < threshold) break;
      }
      // Alternative start: search left until signal drops below threshold
      // startIndex = peakIndex;
      // while (startIndex > 0 && y[startIndex-1] > threshold) {
      //     startIndex--;
      // }

      // Find end index: move right from peak index
      int endIndex = peakIndex;
      while (endIndex < n - 1
          && y[endIndex + 1] < y[endIndex]) { // While signal is falling from the peak
        endIndex++;
        // Optional break condition:
        // if (y[endIndex] < threshold) break;
      }
      // Alternative end: search right until signal drops below threshold
      // endIndex = peakIndex;
      // while (endIndex < n - 1 && y[endIndex+1] > threshold) {
      //     endIndex++;
      // }

      // Ensure indices are valid before accessing x
      if (startIndex >= 0 && endIndex < n && startIndex <= endIndex) {
        // Refine boundaries slightly: Sometimes the above stops one step too early/late depending on definition.
        // Find the local minima *around* the found start/end if needed for cleaner ranges.
        // For simplicity, we use the points where the trend changes.

        ranges.add(Range.closed(x[startIndex], x[endIndex]));
      } else {
        System.err.println(
            "Warning: Could not determine valid boundaries for peak candidate: " + peak);
      }
    }

    // Optional: Merge overlapping ranges if peaks are very close and boundaries overlap significantly
    // (Requires sorting ranges by start time and iterating)

    return ranges;
  }
}
