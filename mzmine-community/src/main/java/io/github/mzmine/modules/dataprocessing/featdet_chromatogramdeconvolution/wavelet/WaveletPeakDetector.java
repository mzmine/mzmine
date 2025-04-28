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
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.WaveletResolverParameters.NoiseCalculation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.RangeUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jetbrains.annotations.NotNull;

// ... (rest of imports)

public class WaveletPeakDetector extends AbstractResolver {

  private static final Logger logger = Logger.getLogger(WaveletPeakDetector.class.getName());

  private final double[] scales;
  private final double minSnr;
  private final double minPeakHeight;
  private final double mergeProximityFactor;
  private final double WAVELET_KERNEL_RADIUS_FACTOR;
  private final double LOCAL_NOISE_WINDOW_FACTOR; // Scales how many points to *try* collecting past edges
  private final int MIN_LOCAL_SAMPLES = 3; // Minimum total samples for local estimate
  private final int MIN_WINDOW_TARGET_POINTS = 3; // Minimum target background points per side
  private final double ZERO_THRESHOLD = 1e-9;
  private final NoiseCalculation noiseMethod;
  private final int minDataPoints;

  private final Map<Integer, Map<Double, double[]>> waveletBuffer = new HashMap<>();

  // ... (constants, fields, constructor) ...
  public WaveletPeakDetector(double[] scales, double minSnr, double minPeakHeight,
      double mergeProximityFactor, double waveletKernelRadiusFactor, double localNoiseWindowFactor,
      ModularFeatureList flist, ParameterSet parameterSet) {
    super(parameterSet, flist);
    if (scales == null || scales.length == 0) {
      throw new IllegalArgumentException("Scales array cannot be null or empty.");
    }
    Arrays.sort(scales);
    this.scales = scales;
    this.minSnr = minSnr;
    this.minPeakHeight = minPeakHeight;
    this.mergeProximityFactor = mergeProximityFactor;
    this.WAVELET_KERNEL_RADIUS_FACTOR = waveletKernelRadiusFactor;
    this.LOCAL_NOISE_WINDOW_FACTOR = Math.max(1, localNoiseWindowFactor);
    this.noiseMethod = parameterSet.getValue(WaveletResolverParameters.noiseCalculation);
    this.minDataPoints = parameterSet.getValue(GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS);
  }

  @Override
  public List<Range<Double>> resolve(double[] x, double[] y) {
    if (x == null || y == null || x.length != y.length || x.length < 5) {
      System.err.println(
          "Warning: Invalid input data (null, mismatched length, or too short). Returning empty list.");
      return Collections.emptyList();
    }
    final int n = y.length;

    // 1. Compute CWT
    double[][] cwtCoefficients = calculateCWT(y, scales);

    // 2. Find Potential Peaks
    List<PotentialPeak> potentialPeaks = findPotentialPeaksFromCWT(cwtCoefficients, scales, x, y);

    // 3. Initial Filtering - Height Only
    List<DetectedPeak> heightFilteredPeaks = filterByHeight(potentialPeaks, y, x, minPeakHeight);

    if (heightFilteredPeaks.isEmpty()) {
      return Collections.emptyList();
    }

    // 4. Determine & SET Index Ranges for Height-Filtered Peaks
    //    *** Modified call ***
    findAndSetLocalMinimaBoundaries(heightFilteredPeaks, y);

    // 5. Local Noise/Baseline Estimation and SNR Filter
    //    *** Modified call (no map passed) ***
    List<DetectedPeak> finalDetectedPeaks = estimateLocalNoiseBaselineAndFilterBySNR(
        heightFilteredPeaks, x, y, minSnr); // Pass x here for baseline calculation

    if (finalDetectedPeaks.isEmpty()) {
      return Collections.emptyList();
    }

    // 6. Determine & SET Index Ranges for the FINAL Peaks
    //    *** Modified call ***
    //    (Optional: Recalculate boundaries if desired, or just use existing ones)
    //    Let's assume we want the boundaries recalculated based on the final list context
    //    If not, skip this and use the boundaries already set in finalDetectedPeaks
    findAndSetLocalMinimaBoundaries(finalDetectedPeaks, y);

    // 7. Convert Final Peaks (with boundaries) to PeakRange objects
    //    *** Modified call (no map passed) ***
    List<PeakRange> finalPeakRanges = convertPeaksToPeakRanges(finalDetectedPeaks, x);

    // 8. Merge Overlapping / Proximal Peaks
    List<Range<Double>> mergedRanges = mergePeakRanges(finalPeakRanges, mergeProximityFactor, x);

    // 9. Sort final ranges by start time
    mergedRanges.sort(Comparator.comparing(Range::lowerEndpoint));

    return mergedRanges;
  }

  // ... (calculateCWT, generateMexicanHat, findPotentialPeaksFromCWT, filterByHeight as before) ...
  private double[][] calculateCWT(double[] y, double[] scales) {
    final int n = y.length;
    int N_padded = Integer.highestOneBit(n);
    if (N_padded < n) {
      N_padded <<= 1;
    }
    if (N_padded == 0) {
      N_padded = 2; // Handle very small n
    }

    double[] yPadded = Arrays.copyOf(y, N_padded); // Pad with zeros

    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
    Complex[] yFFT = fft.transform(yPadded, TransformType.FORWARD);
    double[][] cwt = new double[scales.length][n];

    for (int i = 0; i < scales.length; i++) {
      double scale = scales[i];
      double[] waveletKernel = generateMexicanHat(N_padded, scale);
      Complex[] waveletFFT = fft.transform(waveletKernel, TransformType.FORWARD);
      Complex[] waveletFFTConj = Arrays.stream(waveletFFT).map(Complex::conjugate)
          .toArray(Complex[]::new);

      Complex[] productFFT = new Complex[N_padded];
      for (int k = 0; k < N_padded; k++) {
        productFFT[k] = yFFT[k].multiply(waveletFFTConj[k]);
      }

      Complex[] convolutionComplex = fft.transform(productFFT, TransformType.INVERSE);
      double scaleFactor = 1.0 / Math.sqrt(scale);
      for (int j = 0; j < n; j++) {
        cwt[i][j] = convolutionComplex[j].getReal() * scaleFactor;
      }
    }
    return cwt;
  }

  private double[] generateMexicanHat(int length, double scale) {

    final Map<Double, double[]> scaleToWaveletMap = waveletBuffer.computeIfAbsent(length,
        i -> new HashMap<>());

    return scaleToWaveletMap.computeIfAbsent(scale, _ -> {
      double[] wavelet = new double[length];
      double scaleSq = scale * scale;
      int support = (int) Math.min(length / 2.0, WAVELET_KERNEL_RADIUS_FACTOR * scale);
      double normFactor = 1.0;

      for (int i = -support; i <= support; i++) {
        double t = (double) i;
        double tSq = t * t;
        double term1 = (1.0 - tSq / scaleSq);
        double term2 = Math.exp(-tSq / (2.0 * scaleSq));
        double value = normFactor * term1 * term2;
        int index = (i + length) % length;
        wavelet[index] = value;
      }

      double sum = Arrays.stream(wavelet).sum();
      if (Math.abs(sum) > 1e-9) {
        double mean = sum / length;
        for (int i = 0; i < length; i++) {
          wavelet[i] -= mean;
        }
      }
      return wavelet;
    });
  }

  private List<PotentialPeak> findPotentialPeaksFromCWT(double[][] cwt, double[] scales, double[] x,
      double[] y) {
    List<PotentialPeak> potentials = new ArrayList<>();
    int nScales = cwt.length;
    int nPoints = cwt[0].length;
    Map<Integer, List<PotentialPeak>> maximaByScale = new HashMap<>();

    for (int i = 0; i < nScales; i++) {
      maximaByScale.put(i, new ArrayList<>());
      double[] cwtAtScale = cwt[i];
      for (int j = 1; j < nPoints - 1; j++) {
        if (cwtAtScale[j] > cwtAtScale[j - 1] && cwtAtScale[j] > cwtAtScale[j + 1]
            && cwtAtScale[j] > 0) {
          boolean isLocalMaxInY =
              (j > 0 && j < y.length - 1 && y[j] >= y[j - 1] && y[j] >= y[j + 1]) || (j == 0
                  && y.length > 1 && y[j] >= y[j + 1]) || (j == y.length - 1 && y.length > 1
                  && y[j] >= y[j - 1]);
          if (isLocalMaxInY) {
            maximaByScale.get(i).add(new PotentialPeak(j, scales[i], cwtAtScale[j], y[j]));
          }
        }
      }
    }

    List<PotentialPeak> allMaxima = maximaByScale.values().stream().flatMap(List::stream)
        .collect(Collectors.toList());
    Map<Integer, List<PotentialPeak>> groupedByIndex = allMaxima.stream()
        .collect(Collectors.groupingBy(p -> p.index()));

    for (Map.Entry<Integer, List<PotentialPeak>> entry : groupedByIndex.entrySet()) {
      List<PotentialPeak> peaksAtIndex = entry.getValue();
      if (!peaksAtIndex.isEmpty()) {
        peaksAtIndex.stream()
            .max(Comparator.comparingDouble(p -> p.cwtValue() / Math.sqrt(p.scale())))
            .ifPresent(potentials::add);
      }
    }
    potentials.sort(Comparator.comparingInt(p -> p.index()));

    List<PotentialPeak> refinedPotentials = new ArrayList<>();
    Set<Integer> addedIndices = new HashSet<>();
    for (PotentialPeak pp : potentials) {
      int initialIndex = pp.index();
      int searchRadius = Math.max(1, (int) (pp.scale() / 2.0));
      int bestYIndex = initialIndex;
      double maxY = (initialIndex >= 0 && initialIndex < y.length) ? y[initialIndex]
          : Double.NEGATIVE_INFINITY;
      if (Double.isInfinite(maxY)) {
        continue;
      }

      int start = Math.max(0, initialIndex - searchRadius);
      int end = Math.min(y.length - 1, initialIndex + searchRadius);
      for (int k = start; k <= end; k++) {
        if (y[k] > maxY) {
          maxY = y[k];
          bestYIndex = k;
        }
      }
      if (!addedIndices.contains(bestYIndex)) {
        refinedPotentials.add(new PotentialPeak(bestYIndex, pp.scale(), pp.cwtValue(), maxY));
        addedIndices.add(bestYIndex);
      }
    }
    refinedPotentials.sort(Comparator.comparingInt(p -> p.index()));
    return refinedPotentials;
  }

  private List<DetectedPeak> filterByHeight(List<PotentialPeak> potentials, double[] y, double[] x,
      double minPeakHeight) {
    List<DetectedPeak> heightFiltered = new ArrayList<>();

    for (PotentialPeak pp : potentials) {
      double peakYValue = pp.originalY();

      if (peakYValue >= minPeakHeight) {
        double peakXValue = (pp.index() >= 0 && pp.index() < x.length) ? x[pp.index()] : Double.NaN;
        if (Double.isNaN(peakXValue)) {
          System.err.println(
              "Warning: Could not get valid X coordinate for potential peak at index "
                  + pp.index());
        }
        heightFiltered.add(
            new DetectedPeak(pp.index(), peakXValue, peakYValue, Double.NaN, pp.scale()));
      }
    }
    return heightFiltered;
  }

  /**
   * Finds boundary indices based on local minima and *sets them directly* on the DetectedPeak
   * objects. Stops if two consecutive near-zero values are found, including the first zero in the
   * boundary.
   *
   * @param peaks The list of peaks to find and set boundaries for.
   * @param y     The signal intensity array.
   */
  private void findAndSetLocalMinimaBoundaries(List<DetectedPeak> peaks,
      double[] y) { // Changed return type to void
    if (peaks == null || peaks.isEmpty() || y == null || y.length == 0) {
      return;
    }
    int n = y.length;

    for (DetectedPeak peak : peaks) {
      int peakIdx = peak.peakIndex;
      if (peakIdx < 0 || peakIdx >= n) {
        logger.warning("Invalid peak index " + peakIdx + " encountered during boundary finding.");
        peak.setBoundaryIndices(-1, -1); // Mark as invalid
        continue;
      }

      // --- Find Left Boundary Index ---
      int leftIdx = peakIdx;
      while (leftIdx > 0) {
        if (y[leftIdx - 1] > y[leftIdx]) { // Valley minimum found
          break;
        }
        if (leftIdx >= 2 && isNearZero(y[leftIdx - 1]) && isNearZero(y[leftIdx - 2])) {
          leftIdx--; // Include the first zero
          break;
        }
        leftIdx--;
      }

      // --- Find Right Boundary Index ---
      int rightIdx = peakIdx;
      while (rightIdx < n - 1) {
        if (y[rightIdx + 1] > y[rightIdx]) { // Valley minimum found
          break;
        }
        if (rightIdx <= n - 3 && isNearZero(y[rightIdx + 1]) && isNearZero(y[rightIdx + 2])) {
          rightIdx++; // Include the first zero
          break;
        }
        rightIdx++;
      }

      // *** Set boundaries on the peak object ***
      peak.setBoundaryIndices(leftIdx, rightIdx);
    }
    // No return value needed
  }

  /**
   * Helper to check if a value is close to zero
   */
  private boolean isNearZero(double value) {
    return Math.abs(value) < ZERO_THRESHOLD;
  }


  /**
   * Calculates local noise/baseline and filters by SNR. Now reads boundary indices directly from
   * the peak objects.
   */
  private List<DetectedPeak> estimateLocalNoiseBaselineAndFilterBySNR(
      List<DetectedPeak> heightFilteredPeaks, double[] x, double[] y,
      double minSnr) { // Removed map parameter, added x

    List<DetectedPeak> finalPeaks = new ArrayList<>();
    Median medianCalc = new Median();
    final int n = y.length;

    // *** Build combined exclusion zones from peak boundaries ***
    RangeSet<Integer> allPeakExclusionZones = TreeRangeSet.create();
    for (DetectedPeak peak : heightFilteredPeaks) {
      final Range<Integer> boundaries = peak.getBoundaryIndexRange();
      if (boundaries != null) {
        allPeakExclusionZones.add(boundaries);
      }
    }

    for (DetectedPeak peak : heightFilteredPeaks) {
      // --- Get Peak Boundaries directly from object ---
      if (!peak.hasValidBoundaries()) {
        logger.warning(
            "Skipping SNR calculation for peak " + peak.peakIndex + " due to invalid boundaries.");
        continue; // Skip if boundaries are not valid
      }
      int leftEdgeIdx = peak.leftBoundaryIndex;
      int rightEdgeIdx = peak.rightBoundaryIndex;
      double scale = peak.contributingScale;

      // --- Define Target Number of Background Points per Side ---
      // Reverted calculation to use scale, as width (right-left) is often small
      int targetPointsPerSide = (int) Math.max(MIN_WINDOW_TARGET_POINTS,
          LOCAL_NOISE_WINDOW_FACTOR * (leftEdgeIdx - rightEdgeIdx));

      // --- Collect Local Background Samples Dynamically ---
      DoubleArrayList localBackgroundSamples = new DoubleArrayList();
      int leftSamplesCount = 0;
      int rightSamplesCount = 0;

      // Search Left
      for (int i = leftEdgeIdx - 1; i >= 0 && leftSamplesCount < targetPointsPerSide; i--) {
        if (!allPeakExclusionZones.contains(i)) {
          localBackgroundSamples.add(y[i]);
          leftSamplesCount++;
        }
      }
      // Search Right
      for (int i = rightEdgeIdx + 1; i < n && rightSamplesCount < targetPointsPerSide; i++) {
        if (!allPeakExclusionZones.contains(i)) {
          localBackgroundSamples.add(y[i]);
          rightSamplesCount++;
        }
      }

      // --- Check for sufficient TOTAL samples ---
      if (localBackgroundSamples.size() < MIN_LOCAL_SAMPLES) {
        // System.err.println("Warning: Not enough total local background samples ("+localBackgroundSamples.size()+") near peak " + peak.peakIndex + ". Skipping.");
        continue;
      }

      // --- Calculate Local Baseline & Noise ---
      // Keep your baseline calculation using X values
      final double localBaseline;
      // Check edge indices before accessing x array
      if (leftEdgeIdx >= 0 && leftEdgeIdx < x.length && rightEdgeIdx >= 0
          && rightEdgeIdx < x.length) {
        localBaseline = (y[leftEdgeIdx] + y[rightEdgeIdx]) / 2;
      } else {
        System.err.println(
            "Warning: Invalid edge indices [" + leftEdgeIdx + ", " + rightEdgeIdx + "] for peak "
                + peak.peakIndex + " baseline calculation. Skipping peak.");
        continue;
      }

      double localNoiseStdDev = 0.0;
      final double[] localBackgroundSampleArray = localBackgroundSamples.toDoubleArray();
      try {
        // Calculate Noise (MAD from local baseline)
        switch (noiseMethod) {
          case STANDARD_DEVIATION ->
              localNoiseStdDev = MathUtils.calcStd(localBackgroundSampleArray);
          case MEDIAN_ABSOLUTE_DEVIATION -> {
            final double[] absDevs = Arrays.stream(localBackgroundSampleArray)
                .map(val -> Math.abs(val - localBaseline)).toArray();
            double localMad = medianCalc.evaluate(absDevs);
            localNoiseStdDev = 1.4826 * localMad;
          }
        }
      } catch (IllegalArgumentException madEx) {
        logger.warning(
            "Warning: Could not calculate local MAD for peak at index " + peak.peakIndex + ": "
                + madEx.getMessage() + ". Assuming zero noise.");
        localNoiseStdDev = 0.0; // Assume zero if MAD fails
      }

      // --- Calculate SNR ---
      final double signalHeight = peak.peakY - localBaseline;
      double localSnr =
          (localNoiseStdDev > 0) ? (signalHeight / localNoiseStdDev) : Double.POSITIVE_INFINITY;
      if (signalHeight <= 0) {
        localSnr = 0;
      }

      // --- Filter based on local SNR ---
      if (localSnr >= minSnr) {
        // Update the SNR on the existing peak object before adding
        peak.snr = localSnr;
        finalPeaks.add(peak); // Add the peak that passed
      }
    }
    return finalPeaks;
  }

  /**
   * Converts final peaks (with boundaries set) to PeakRange objects. Reads boundary indices
   * directly from the peak objects.
   *
   * @param peaks The final list of detected peaks with boundaries set.
   * @param x     The X-coordinate array.
   * @return A list of PeakRange objects ready for merging.
   */
  private List<PeakRange> convertPeaksToPeakRanges(List<DetectedPeak> peaks,
      double[] x) { // Removed map parameter
    List<PeakRange> peakRanges = new ArrayList<>();
    if (peaks == null || x == null) {
      return peakRanges;
    }

    for (DetectedPeak peak : peaks) {
      // *** Get boundaries directly from the peak object ***
      if (peak.hasValidBoundaries()) {
        int leftIdx = peak.leftBoundaryIndex;
        int rightIdx = peak.rightBoundaryIndex;

        // Check indices are valid for x array access
        if (leftIdx >= 0 && rightIdx < x.length) { // left <= right checked in hasValidBoundaries
          try {
            peakRanges.add(new PeakRange(Range.closed(x[leftIdx], x[rightIdx]),
                Range.closed(leftIdx, rightIdx), peak.peakIndex, peak.peakX, peak.peakY));
          } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(
                "Error creating range for peak " + peak.peakIndex + ": index out of bounds ["
                    + leftIdx + ", " + rightIdx + "] for x-array length " + x.length);
          }
        } else {
          // Should not happen if hasValidBoundaries is true and indices were checked, but good to have fallback
          System.err.println(
              "Warning: Invalid boundary indices [" + leftIdx + ", " + rightIdx + "] found on peak "
                  + peak.peakIndex + " during conversion (x-array length: " + x.length + ").");
        }
      } else {
        System.err.println("Warning: Skipping PeakRange creation for peak " + peak.peakIndex
            + " due to invalid/missing boundaries.");
      }
    }
    return peakRanges;
  }

  // ... (mergePeakRanges remains the same) ...
  private List<Range<Double>> mergePeakRanges(List<PeakRange> peakRanges, double proximityFactor,
      double[] x) {
    if (peakRanges == null || peakRanges.size() <= 1) {
      return peakRanges.stream().map(PeakRange::range).collect(Collectors.toList());
    }

    peakRanges.sort(Comparator.comparingDouble(pr -> pr.range().lowerEndpoint()));
    LinkedList<PeakRange> merged = new LinkedList<>();

    for (PeakRange current : peakRanges) {
      if (merged.isEmpty()) {
        merged.add(current);
      } else {
        PeakRange previous = merged.getLast();
        boolean shouldMerge = false;

        if (previous.range().isConnected(current.range())
            && RangeUtils.rangeLength(previous.range().intersection(current.range()))
            > ZERO_THRESHOLD) {
          shouldMerge = true;
        } else if (proximityFactor > 0) {
          double prevWidth = previous.range().upperEndpoint() - previous.range().lowerEndpoint();
          double currWidth = current.range().upperEndpoint() - current.range().lowerEndpoint();
          double avgWidth = (prevWidth + currWidth) / 2.0;
          if (avgWidth <= 0) {
            avgWidth = Math.max(Math.abs(previous.peakX() - current.peakX()) / 4.0, 1e-6);
          }
          double peakDistance = Math.abs(previous.peakX() - current.peakX());
          if (peakDistance < proximityFactor * avgWidth) {
            shouldMerge = true;
          }
        }

        if (shouldMerge) {
          Range<Double> mergedRange = Range.closed(previous.range().lowerEndpoint(),
              Math.max(previous.range().upperEndpoint(), current.range().upperEndpoint()));
          Range<Integer> mergedIndexRange = Range.closed(previous.indexRange().lowerEndpoint(),
              Math.max(previous.indexRange().upperEndpoint(),
                  current.indexRange().upperEndpoint()));
          PeakRange mergedPeakRange;
          if (current.peakY() >= previous.peakY()) {
            mergedPeakRange = new PeakRange(mergedRange, mergedIndexRange, current.peakIndex(),
                current.peakX(), current.peakY());
          } else {
            mergedPeakRange = new PeakRange(mergedRange, mergedIndexRange, previous.peakIndex(),
                previous.peakX(), previous.peakY());
          }
          merged.removeLast();
          merged.add(mergedPeakRange);
        } else {
          merged.add(current);
        }
      }
    }

    return merged.stream().filter(r -> RangeUtils.rangeLength(r.indexRange()) >= minDataPoints)
        .map(PeakRange::range).collect(Collectors.toList());
  }

  // Required override for AbstractResolver
  @Override
  public @NotNull Class<? extends MZmineModule> getModuleClass() {
    return WaveletResolverModule.class; // Placeholder
  }
}
