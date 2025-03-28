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
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.StatUtils;


import java.util.*;
import org.jetbrains.annotations.NotNull;

public class WaveletResolver extends AbstractResolver {

    // --- Configuration ---
    private final int[] scales; // Scales (in data points) to analyze. Critical parameter!
    private final double snrThreshold; // Signal-to-Noise Ratio threshold for peak detection in CWT coefficients
    private final double peakHeightMinThreshold; // Minimum absolute intensity for a peak apex in the raw data
    private final double boundaryThresholdFactor; // Factor of peak height to determine start/end (e.g., 0.05 for 5%)
    private final int minPeakWidthPoints; // Minimum peak width in data points
    private final int maxPeakWidthPoints; // Maximum peak width in data points

    // --- Constants ---
    private static final double MEXICAN_HAT_NORMALIZATION_FACTOR = 2.0 / (Math.sqrt(3.0) * Math.pow(Math.PI, 0.25));
    private static final int WAVELET_KERNEL_RADIUS_FACTOR = 5; // Determines how wide the wavelet kernel is relative to scale (e.g., +/- 5*scale/2 points)


    /**
     * Constructor for WaveletResolver.
     *
     * @param scales                Array of scales (widths in data points) to analyze. Choose based on expected peak widths. E.g., {5, 10, 15, 20, 30}
     * @param snrThreshold          Minimum Signal-to-Noise ratio in the CWT domain to consider a feature a peak. Typically >= 3.
     * @param peakHeightMinThreshold Minimum absolute intensity of the raw data point identified as the peak apex.
     * @param boundaryThresholdFactor Factor of the peak apex's height used to find peak boundaries. E.g., 0.05 means boundaries are where intensity drops below 5% of apex height.
     * @param minPeakWidthPoints    Minimum allowed peak width in data points.
     * @param maxPeakWidthPoints    Maximum allowed peak width in data points.
     */
    public WaveletResolver(int[] scales,
                           double snrThreshold,
                           double peakHeightMinThreshold,
                           double boundaryThresholdFactor,
                           int minPeakWidthPoints,
                           int maxPeakWidthPoints) {
      super();

      if (scales == null || scales.length == 0) {
            throw new IllegalArgumentException("Scales array cannot be null or empty.");
        }
        Arrays.sort(scales); // Ensure scales are sorted, important for noise estimation
        this.scales = scales;
        this.snrThreshold = snrThreshold;
        this.peakHeightMinThreshold = peakHeightMinThreshold;
        this.boundaryThresholdFactor = boundaryThresholdFactor;
        this.minPeakWidthPoints = minPeakWidthPoints;
        this.maxPeakWidthPoints = maxPeakWidthPoints;
    }

    /**
     * Detects peaks in the given time series data using CWT.
     *
     * @param x Time points (must be sorted and reasonably equally spaced).
     * @param y Intensity points corresponding to x.
     * @return List of Guava Ranges representing the [start_x, end_x] of detected peaks.
     */
    public List<Range<Double>> resolve(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length) {
            throw new IllegalArgumentException("Input arrays x and y must be non-null and have the same length.");
        }
        if (x.length < 5) { // Need some points for analysis
             return Collections.emptyList();
        }

        int n = y.length;

        // --- 1. Compute CWT Coefficients for each scale ---
        // Stores CWT coefficients: Map<Scale, CoefficientsArray>
        Map<Integer, double[]> cwtCoefficients = new TreeMap<>(); // Use TreeMap to keep scales sorted
        for (int scale : scales) {
            // Ensure scale is reasonable for data length and kernel factor
            int kernelRadius = Math.min(n / 2 -1 , (scale * WAVELET_KERNEL_RADIUS_FACTOR) / 2);
          if (kernelRadius < 1) {
            continue; // Skip scales too large for the data or kernel settings
          }

            double[] waveletKernel = generateMexicanHatKernel(scale, kernelRadius);
            double[] coefficients = convolve(y, waveletKernel);
            cwtCoefficients.put(scale, coefficients);
        }

        if (cwtCoefficients.isEmpty()) {
            System.err.println("Warning: No valid scales for CWT computation given data length and kernel settings.");
            return Collections.emptyList();
        }


        // --- 2. Estimate Noise Level ---
        // Use the coefficients at the smallest scale to estimate noise (MAD is robust)
        double[] smallestScaleCoeffs = cwtCoefficients.values().iterator().next(); // First entry due to TreeMap
        double noiseStdDev = estimateNoiseLevelMAD(smallestScaleCoeffs);
        // Avoid division by zero if signal is flat
        noiseStdDev = Math.max(noiseStdDev, 1e-9);


        // --- 3. Find Candidate Peak Ridges/Maxima ---
        // Find local maxima in CWT coefficients at each scale exceeding SNR threshold
        // Map: Index -> List of scales where a maximum occurs at this index
        Map<Integer, List<Integer>> maximaMap = new HashMap<>();

        for (Map.Entry<Integer, double[]> entry : cwtCoefficients.entrySet()) {
            int scale = entry.getKey();
            double[] coeffs = entry.getValue();
            double threshold = snrThreshold * noiseStdDev;

            findLocalMaxima(coeffs, threshold, scale, maximaMap);
        }

        // --- 4. Identify Peak Centers from Maxima ---
        // Peaks should ideally show maxima across multiple adjacent scales.
        // We simplify here: Find indices that are maxima at scales relevant to peak width.
        // A more advanced method would trace "ridges" in the scale-space plane.
        // This simplified version finds indices that are maxima in *any* scale and uses the scale
        // with the highest CWT coefficient value at that index as the 'characteristic scale'.

        List<PeakCandidate> candidates = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : maximaMap.entrySet()) {
            int index = entry.getKey();
            List<Integer> contributingScales = entry.getValue();

          if (contributingScales.isEmpty()) {
            continue; // Should not happen based on findLocalMaxima logic
          }

            // Find the scale with the maximum absolute CWT coefficient at this index
            int bestScale = -1;
            double maxCoeffValue = -Double.MAX_VALUE;

            for (int scale : contributingScales) {
                double coeff = cwtCoefficients.get(scale)[index];
                if (Math.abs(coeff) > maxCoeffValue) {
                    maxCoeffValue = Math.abs(coeff);
                    bestScale = scale;
                }
            }
             // Basic check: Ensure the peak in raw data meets minimum height
            if (y[index] < peakHeightMinThreshold) {
                 continue;
            }

            candidates.add(new PeakCandidate(index, bestScale, maxCoeffValue / noiseStdDev)); // Store index, scale, and calculated SNR
        }

        // Sort candidates by index for easier processing
        candidates.sort(Comparator.comparingInt(PeakCandidate::getIndex));


        // --- 5. Determine Peak Boundaries ---
        List<Range<Double>> detectedPeaks = new ArrayList<>();
        boolean[] used = new boolean[n]; // Keep track of points assigned to a peak

        for (PeakCandidate candidate : candidates) {
            int centerIndex = candidate.getIndex();

            if (used[centerIndex]) {
                continue; // This point is already part of another (likely wider) peak found earlier
            }

            double peakHeight = y[centerIndex];

            // Define boundary threshold based on peak height
            double boundaryThresholdValue = peakHeight * boundaryThresholdFactor;

            // Find start index: walk left from center
            int startIndex = centerIndex;
            while (startIndex > 0 && y[startIndex - 1] >= boundaryThresholdValue && y[startIndex - 1] < y[startIndex] && !used[startIndex - 1]) {
                // Stop if going uphill away from peak or below threshold or point used
                 startIndex--;
            }
             // Refine start: ensure we are at a local minimum or below threshold
             while (startIndex > 0 && y[startIndex - 1] < y[startIndex] && y[startIndex] >= boundaryThresholdValue && !used[startIndex-1]) {
                startIndex--;
             }
              // One last check to ensure we didn't walk too far into another peak's baseline
             if(startIndex > 0 && y[startIndex-1] > y[startIndex] && y[startIndex] < boundaryThresholdValue) {
                  startIndex++; // Step back if we went past a minimum and below threshold
             }


            // Find end index: walk right from center
            int endIndex = centerIndex;
            while (endIndex < n - 1 && y[endIndex + 1] >= boundaryThresholdValue && y[endIndex + 1] < y[endIndex] && !used[endIndex+1] ) {
                 // Stop if going uphill away from peak or below threshold or point used
                endIndex++;
            }
             // Refine end: ensure we are at a local minimum or below threshold
             while (endIndex < n - 1 && y[endIndex + 1] < y[endIndex] && y[endIndex] >= boundaryThresholdValue && !used[endIndex+1]) {
                endIndex++;
             }
              // One last check
             if(endIndex < n - 1 && y[endIndex+1] > y[endIndex] && y[endIndex] < boundaryThresholdValue) {
                 endIndex--; // Step back
             }

            // --- 6. Filter by Width and Mark Used Points ---
             int peakWidthPoints = endIndex - startIndex + 1;
             if (peakWidthPoints >= minPeakWidthPoints && peakWidthPoints <= maxPeakWidthPoints) {
                 // Mark points as used
                 boolean canAdd = true;
                 for (int i = startIndex; i <= endIndex; i++) {
                    if (used[i]) {
                       // Overlap detected. Decide strategy: merge? discard smaller?
                       // Simplest: discard this candidate if it significantly overlaps an existing one.
                       // A more robust approach might merge or re-evaluate boundaries.
                       // For now, we skip if the center was already used, and this loop prevents partial overlaps from adding.
                       canAdd = false;
                       break;
                    }
                 }

                 if(canAdd) {
                    for (int i = startIndex; i <= endIndex; i++) {
                        used[i] = true;
                    }
                    // Use Range.closed for inclusive start/end
                    detectedPeaks.add(Range.closed(x[startIndex], x[endIndex]));
                 }
             }
        }

        // Optional: Post-processing like merging adjacent peaks if needed

        return detectedPeaks;
    }

  @Override
  public @NotNull Class<? extends MZmineModule> getModuleClass() {
    return null;
  }

  // === Helper Methods ===

    /**
     * Generates a Mexican Hat (Ricker) wavelet kernel.
     * f(t) = (1 - t^2) * exp(-t^2 / 2) * C
     * Scaled: f_s(t) = (1 - (t/s)^2) * exp(-(t/s)^2 / 2) * C'
     * @param scale Scale parameter (related to wavelet width)
     * @param radius Number of points on each side of the center (total size = 2*radius + 1)
     * @return The wavelet kernel array.
     */
    private double[] generateMexicanHatKernel(int scale, int radius) {
        int size = 2 * radius + 1;
        double[] kernel = new double[size];
        double scaleFactor = (double) scale / 2.0; // Relate scale parameter to Gaussian width

        for (int i = 0; i < size; i++) {
            double t = (double) (i - radius) / scaleFactor; // Scaled time relative to center
            double tSq = t * t;
            kernel[i] = MEXICAN_HAT_NORMALIZATION_FACTOR * (1.0 - tSq) * Math.exp(-tSq / 2.0);
        }

        // Normalize the kernel to have zero mean (should be close already) and unit energy (optional, but good practice)
        double mean = StatUtils.mean(kernel);
      for (int i = 0; i < size; ++i) {
        kernel[i] -= mean;
      }

        double norm = Math.sqrt(StatUtils.sumSq(kernel));
        if (norm > 1e-9) {
          for (int i = 0; i < size; ++i) {
            kernel[i] /= norm;
          }
        }

        return kernel;
    }

    /**
     * Performs 1D convolution (signal * kernel). Handles boundaries by zero-padding.
     * @param signal The input signal array.
     * @param kernel The kernel array (should ideally have odd length).
     * @return The convolution result array, same length as signal.
     */
    private double[] convolve(double[] signal, double[] kernel) {
        int n = signal.length;
        int m = kernel.length;
        int mHalf = m / 2;
        double[] output = new double[n];

        // For high throughput, parallelizing this outer loop can give significant speedup
        // if n is large and multiple cores are available.
        // IntStream.range(0, n).parallel().forEach(i -> { ... });
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < m; j++) {
                int signalIndex = i - mHalf + j;
                // Boundary handling: zero-padding conceptually
                if (signalIndex >= 0 && signalIndex < n) {
                    sum += signal[signalIndex] * kernel[j];
                }
            }
            output[i] = sum;
        }
        return output;
    }


   /**
     * Estimates noise standard deviation using the Median Absolute Deviation (MAD).
     * noise_stddev ≈ 1.4826 * median(|X_i - median(X)|)
     * @param data Array of data points (e.g., CWT coefficients at smallest scale).
     * @return Estimated standard deviation.
     */
    private double estimateNoiseLevelMAD(double[] data) {
      if (data == null || data.length == 0) {
        return 0.0;
      }

        Median medianCalculator = new Median();
        double median = medianCalculator.evaluate(data);

        double[] absoluteDeviations = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            absoluteDeviations[i] = Math.abs(data[i] - median);
        }

        double mad = medianCalculator.evaluate(absoluteDeviations);

        return mad * 1.4826;
    }

    /**
     * Finds local maxima in an array that exceed a given threshold.
     * @param data The data array (e.g., CWT coefficients).
     * @param threshold The minimum value for a maximum.
     * @param scale The scale associated with this data (for storing in maximaMap).
     * @param maximaMap Map (Index -> List<Scale>) to store results. Modifies this map directly.
     */
    private void findLocalMaxima(double[] data, double threshold, int scale, Map<Integer, List<Integer>> maximaMap) {
        int n = data.length;
      if (n < 3) {
        return; // Need at least 3 points to find a local maximum
      }

        for (int i = 1; i < n - 1; i++) {
            double current = data[i];
            double prev = data[i - 1];
            double next = data[i + 1];

            // Check for local maximum (plateaus are handled by using >= and <)
            // Using strict > < might miss flat tops, using >= <= might include shoulders.
            // This version finds the start of a plateau or a single point max.
            if (current >= prev && current > next && current > threshold) {
                 maximaMap.computeIfAbsent(i, k -> new ArrayList<>()).add(scale);
            }
            // Handle plateaus explicitly: find the center of the plateau
            else if (current == next && current >= prev && current > threshold) {
                int plateauStart = i;
                while (i < n - 1 && data[i] == data[i+1]) {
                    i++;
                }
                 // Now i is the end of the plateau
                int plateauCenter = plateauStart + (i - plateauStart) / 2;
                // Ensure the plateau end drops off
                if (i == n - 1 || data[i] > data[i+1]) {
                     maximaMap.computeIfAbsent(plateauCenter, k -> new ArrayList<>()).add(scale);
                }
                // Continue loop from end of plateau
            }
        }
         // Note: Doesn't check endpoints (index 0 and n-1) as maxima. Add if needed.
    }


    // --- Inner class for candidate peaks ---
    private static class PeakCandidate {
        private final int index;
        private final int characteristicScale;
        private final double snr;


        PeakCandidate(int index, int characteristicScale, double snr) {
            this.index = index;
            this.characteristicScale = characteristicScale;
            this.snr = snr;
        }

        public int getIndex() { return index; }
        public int getCharacteristicScale() { return characteristicScale; }
        public double getSnr() { return snr; }

        @Override
        public String toString() {
            return "PeakCandidate{" +
                   "index=" + index +
                   ", scale=" + characteristicScale +
                   ", snr=" + String.format("%.2f", snr) +
                   '}';
        }
    }

    // --- Example Usage ---
    public static void main(String[] args) {
        // --- Generate Sample Data (Gaussian peak with noise) ---
        int dataSize = 500;
        double[] x = new double[dataSize];
        double[] y = new double[dataSize];
        Random random = new Random();

        for (int i = 0; i < dataSize; i++) {
            x[i] = (double) i / dataSize * 20.0; // Time from 0 to 20

            // Baseline
            y[i] = 5.0 + 0.1 * x[i]; // Small linear baseline drift

            // Add a Gaussian peak
            double peakCenter1 = 5.0;
            double peakWidth1 = 0.5;
            double peakHeight1 = 50.0;
            y[i] += peakHeight1 * Math.exp(-Math.pow(x[i] - peakCenter1, 2) / (2 * peakWidth1 * peakWidth1));

            // Add another wider, smaller peak
            double peakCenter2 = 12.0;
            double peakWidth2 = 1.5;
            double peakHeight2 = 25.0;
            y[i] += peakHeight2 * Math.exp(-Math.pow(x[i] - peakCenter2, 2) / (2 * peakWidth2 * peakWidth2));

             // Add another sharper, smaller peak close to the first one
            double peakCenter3 = 6.0;
            double peakWidth3 = 0.2;
            double peakHeight3 = 15.0;
            y[i] += peakHeight3 * Math.exp(-Math.pow(x[i] - peakCenter3, 2) / (2 * peakWidth3 * peakWidth3));


            // Add noise
            y[i] += random.nextGaussian() * 1.5; // Noise level
        }

        // --- Configure and Run Resolver ---
        // Estimate typical peak widths in data points:
        // Peak 1 width ~ 0.5 time units. Data spacing = 20.0 / 500 = 0.04. Width ~ 0.5 / 0.04 = 12.5 points
        // Peak 2 width ~ 1.5 time units. Width ~ 1.5 / 0.04 = 37.5 points
        // Peak 3 width ~ 0.2 time units. Width ~ 0.2 / 0.04 = 5 points
        int[] scalesToAnalyze = {3, 5, 8, 12, 20, 30, 40, 50}; // Scales around expected widths

        WaveletResolver resolver = new WaveletResolver(
                scalesToAnalyze,
                3.0,   // snrThreshold
                5.0,   // peakHeightMinThreshold (absolute intensity)
                0.05,  // boundaryThresholdFactor (5% of height)
                3,     // minPeakWidthPoints
                100    // maxPeakWidthPoints (adjust based on expected max width)
        );

        long startTime = System.nanoTime();
        List<Range<Double>> peaks = resolver.resolve(x, y);
        long endTime = System.nanoTime();

        System.out.println("Detected Peaks (" + peaks.size() + "):");
        for (Range<Double> peak : peaks) {
            System.out.printf("  Peak from x=%.3f to x=%.3f%n", peak.lowerEndpoint(), peak.upperEndpoint());
        }
        System.out.printf("Processing time: %.3f ms%n", (endTime - startTime) / 1_000_000.0);

         // Optional: You can plot the data and the detected ranges to visually verify.
    }
}
