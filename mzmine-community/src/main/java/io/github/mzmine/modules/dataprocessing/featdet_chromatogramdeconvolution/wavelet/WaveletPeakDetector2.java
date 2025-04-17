package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.RangeUtils;
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
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jetbrains.annotations.NotNull;

// Import statements assumed to exist
// import path.to.your.WaveletResolverModule;
// import path.to.your.WaveletResolverParameters;


public class WaveletPeakDetector2 extends AbstractResolver {

  private final double[] scales;
  private final double minSnr;
  private final double minPeakHeight;
  private final double mergeProximityFactor;
  private final double WAVELET_KERNEL_RADIUS_FACTOR;
  private final int LOCAL_NOISE_WINDOW_FACTOR; // Scales how many points to *try* collecting past edges
  private final int MIN_LOCAL_SAMPLES = 3; // Minimum total samples for local estimate
  private final int MIN_WINDOW_TARGET_POINTS = 3; // Minimum target background points per side
  private final double ZERO_THRESHOLD = 1e-9;

  /**
   * Constructor for the WaveletPeakDetector.
   *
   * @param scales                    Array of scales (wavelet widths).
   * @param minSnr                    Minimum Signal-to-Noise ratio.
   * @param minPeakHeight             Minimum absolute y-value of peak maximum.
   * @param mergeProximityFactor      Merging proximity factor.
   * @param waveletKernelRadiusFactor Wavelet kernel support radius factor.
   * @param localNoiseWindowFactor    Factor * scale defining target number of background points per
   *                                  side.
   * @param flist                     Target feature list.
   * @param parameterSet              Parameters for the module.
   */
  public WaveletPeakDetector2(double[] scales, double minSnr, double minPeakHeight,
      double mergeProximityFactor, double waveletKernelRadiusFactor, int localNoiseWindowFactor,
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
  }

  @Override
  public List<Range<Double>> resolve(double[] x, double[] y) {
    // ... (resolve method remains the same) ...
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

    // 4. Determine Index Ranges for Height-Filtered Peaks
    Map<Integer, Range<Integer>> heightFilteredPeakIndexRanges = findLocalMinimaBoundaryIndices(
        heightFilteredPeaks, y);

    // 5. Local Noise/Baseline Estimation and SNR Filter
    List<DetectedPeak> finalDetectedPeaks = estimateLocalNoiseBaselineAndFilterBySNR(
        heightFilteredPeaks, x, y, heightFilteredPeakIndexRanges, minSnr);

    if (finalDetectedPeaks.isEmpty()) {
      return Collections.emptyList();
    }

    // 6. Determine Index Ranges for the Final Peaks
    Map<Integer, Range<Integer>> finalPeakIndexRanges = findLocalMinimaBoundaryIndices(
        finalDetectedPeaks, y);

    // 7. Convert Final Index Ranges to PeakRange objects
    List<PeakRange> finalPeakRanges = convertIndexRangesToPeakRanges(finalDetectedPeaks,
        finalPeakIndexRanges, x);

    // 8. Merge Overlapping / Proximal Peaks
    List<Range<Double>> mergedRanges = mergePeakRanges(finalPeakRanges, this.mergeProximityFactor);

    // 9. Sort final ranges by start time
    mergedRanges.sort(Comparator.comparing(Range::lowerEndpoint));

    return mergedRanges;
  }

  // --- Helper Classes (PotentialPeak, DetectedPeak, PeakRange) ---
  // ... (Keep these classes as they were) ...
  private static class PotentialPeak { /* ... as before ... */

    int index; // Index in the original signal
    double scale; // Scale at which it was prominent
    double cwtValue; // CWT coefficient value at this point/scale
    double originalY; // y-value in the original signal

    PotentialPeak(int index, double scale, double cwtValue, double originalY) {
      this.index = index;
      this.scale = scale;
      this.cwtValue = cwtValue;
      this.originalY = originalY;
    }

    @Override
    public String toString() {
      return "PotentialPeak{index=" + index + ", scale=" + scale + ", cwtValue=" + String.format(
          "%.2f", cwtValue) + ", y=" + String.format("%.2f", originalY) + "}";
    }
  }

  private static class DetectedPeak { /* ... as before ... */

    int peakIndex;     // Index of the maximum in the original signal
    double peakX;       // X value of the maximum
    double peakY;       // Y value of the maximum
    double snr;         // Calculated SNR (Now always local)
    double contributingScale; // A representative scale

    DetectedPeak(int peakIndex, double peakX, double peakY, double snr, double contributingScale) {
      this.peakIndex = peakIndex;
      this.peakX = peakX;
      this.peakY = peakY;
      this.snr = snr;
      this.contributingScale = contributingScale;
    }

    @Override
    public String toString() {
      return "DetectedPeak{idx=" + peakIndex + ", x=" + String.format("%.2f", peakX) + ", y="
          + String.format("%.2f", peakY) + ", snr=" + String.format("%.2f", snr) + ", scale="
          + String.format("%.2f", contributingScale) + "}";
    }
  }

  private static class PeakRange { /* ... as before ... */

    Range<Double> range;
    int peakIndex; // Index of the peak max within this range
    double peakY;   // Height of the peak max
    double peakX;   // X value of the peak max

    PeakRange(Range<Double> range, int peakIndex, double peakX, double peakY) {
      this.range = range;
      this.peakIndex = peakIndex;
      this.peakX = peakX;
      this.peakY = peakY;
    }

    @Override
    public String toString() {
      return "PeakRange{range=" + range + ", peakIdx=" + peakIndex + ", peakY=" + String.format(
          "%.2f", peakY) + "}";
    }
  }

  // --- Core Logic Methods ---
  // ... (calculateCWT, generateMexicanHat, findPotentialPeaksFromCWT) ...
  // ... (filterByHeight) ...
  // ... (convertIndexRangesToPeakRanges, mergePeakRanges) ...
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
        .collect(Collectors.groupingBy(p -> p.index));

    for (Map.Entry<Integer, List<PotentialPeak>> entry : groupedByIndex.entrySet()) {
      List<PotentialPeak> peaksAtIndex = entry.getValue();
      if (!peaksAtIndex.isEmpty()) {
        PotentialPeak bestPeak = peaksAtIndex.stream()
            .max(Comparator.comparingDouble(p -> p.cwtValue / Math.sqrt(p.scale))).orElse(null);
        if (bestPeak != null) {
          potentials.add(bestPeak);
        }
      }
    }
    potentials.sort(Comparator.comparingInt(p -> p.index));

    List<PotentialPeak> refinedPotentials = new ArrayList<>();
    Set<Integer> addedIndices = new HashSet<>();
    for (PotentialPeak pp : potentials) {
      int initialIndex = pp.index;
      int searchRadius = Math.max(1, (int) (pp.scale / 2.0));
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
        refinedPotentials.add(new PotentialPeak(bestYIndex, pp.scale, pp.cwtValue, maxY));
        addedIndices.add(bestYIndex);
      }
    }
    refinedPotentials.sort(Comparator.comparingInt(p -> p.index));
    return refinedPotentials;
  }

  private List<DetectedPeak> filterByHeight(List<PotentialPeak> potentials, double[] y, double[] x,
      double minPeakHeight) {
    List<DetectedPeak> heightFiltered = new ArrayList<>();

    for (PotentialPeak pp : potentials) {
      double peakYValue = pp.originalY;

      if (peakYValue >= minPeakHeight) {
        double peakXValue = (pp.index >= 0 && pp.index < x.length) ? x[pp.index] : Double.NaN;
        if (Double.isNaN(peakXValue)) {
          System.err.println(
              "Warning: Could not get valid X coordinate for potential peak at index " + pp.index);
        }
        heightFiltered.add(
            new DetectedPeak(pp.index, peakXValue, peakYValue, Double.NaN, pp.scale));
      }
    }
    return heightFiltered;
  }

  /**
   * Finds boundary indices based on local minima, stopping if two consecutive near-zero values are
   * found and *including* the first zero in the boundary.
   */
  private Map<Integer, Range<Integer>> findLocalMinimaBoundaryIndices(List<DetectedPeak> peaks,
      double[] y) {
    Map<Integer, Range<Integer>> indexRanges = new HashMap<>();
    if (peaks == null || peaks.isEmpty() || y == null || y.length == 0) {
      return indexRanges;
    }
    int n = y.length;

    for (DetectedPeak peak : peaks) {
      int peakIdx = peak.peakIndex;
      if (peakIdx < 0 || peakIdx >= n) {
        System.err.println(
            "Warning: Invalid peak index " + peakIdx + " encountered during boundary finding.");
        continue;
      }

      // --- Find Left Boundary Index ---
      int leftIdx = peakIdx;
      while (leftIdx > 0) {
        if (y[leftIdx - 1] > y[leftIdx]) { // Valley minimum found
          break;
        }
        // Check for two consecutive zeros
        if (leftIdx >= 2 && isNearZero(y[leftIdx - 1]) && isNearZero(y[leftIdx - 2])) {
          leftIdx--; // *** Move left to include the first zero ***
          break;
        }
        leftIdx--; // Continue search
      }

      // --- Find Right Boundary Index ---
      int rightIdx = peakIdx;
      while (rightIdx < n - 1) {
        if (y[rightIdx + 1] > y[rightIdx]) { // Valley minimum found
          break;
        }
        // Check for two consecutive zeros
        if (rightIdx <= n - 3 && isNearZero(y[rightIdx + 1]) && isNearZero(y[rightIdx + 2])) {
          rightIdx++; // *** Move right to include the first zero ***
          break;
        }
        rightIdx++; // Continue search
      }
      indexRanges.put(peakIdx, Range.closed(leftIdx, rightIdx));
    }
    return indexRanges;
  }

  /**
   * Helper to check if a value is close to zero
   */
  private boolean isNearZero(double value) {
    return Math.abs(value) < ZERO_THRESHOLD;
  }


  /**
   * Calculates local noise/baseline by searching outwards from peak edges until a target number of
   * background points are found, then filters by SNR.
   */
  private List<DetectedPeak> estimateLocalNoiseBaselineAndFilterBySNR(
      List<DetectedPeak> heightFilteredPeaks, double[] x, double[] y,
      Map<Integer, Range<Integer>> heightFilteredPeakIndexRanges, double minSnr) {
    List<DetectedPeak> finalPeaks = new ArrayList<>();
    Median medianCalc = new Median();
    final int n = y.length;

    // Build combined exclusion zones
    RangeSet<Integer> allPeakExclusionZones = TreeRangeSet.create();
    for (Range<Integer> range : heightFilteredPeakIndexRanges.values()) {
      if (range != null) {
        allPeakExclusionZones.add(range);
      }
    }

    for (DetectedPeak peak : heightFilteredPeaks) {
      int peakIdx = peak.peakIndex;
      double scale = peak.contributingScale;

      // Get Peak Boundaries
      Range<Integer> currentPeakRange = heightFilteredPeakIndexRanges.get(peakIdx);
      if (currentPeakRange == null) {
        continue;
      }
      int leftEdgeIdx = currentPeakRange.lowerEndpoint();
      int rightEdgeIdx = currentPeakRange.upperEndpoint();

      // --- Define Target Number of Background Points per Side ---
      int targetPointsPerSide = (int) Math.max(MIN_WINDOW_TARGET_POINTS,
          LOCAL_NOISE_WINDOW_FACTOR * (leftEdgeIdx - rightEdgeIdx));

      // --- Collect Local Background Samples Dynamically ---
      List<Double> localBackgroundSamples = new ArrayList<>();
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
        // System.err.println("Warning: Not enough total local background samples ("+localBackgroundSamples.size()+") near peak " + peakIdx + ". Skipping.");
        continue;
      }

      // --- Calculate Local Baseline & Noise (using collected samples) ---
      final double localBaseline;
      double localNoiseStdDev = 0.0;
      double[] localBackgroundSampleArray = localBackgroundSamples.stream().mapToDouble(d -> d)
          .toArray();

      try {
//        localBaseline = medianCalc.evaluate(localBackgroundSampleArray);
        localBaseline = (x[leftEdgeIdx] + x[rightEdgeIdx]) / 2;
        // Calculate Noise (MAD from local baseline)
        try {
          double[] absDevs = Arrays.stream(localBackgroundSampleArray)
              .map(val -> Math.abs(val - localBaseline)).toArray();
          double localMad = medianCalc.evaluate(absDevs);
          localNoiseStdDev = 1.4826 * localMad;
        } catch (IllegalArgumentException madEx) {
          // System.err.println("Warning: Could not calculate local MAD for peak at index " + peakIdx + ": " + madEx.getMessage() + ". Assuming zero noise.");
          localNoiseStdDev = 0.0; // Assume zero if MAD fails (e.g., all identical)
        }
      } catch (IllegalArgumentException baselineEx) {
        System.err.println(
            "Warning: Could not calculate local baseline median for peak " + peakIdx + ": "
                + baselineEx.getMessage() + ". Skipping peak.");
        continue; // Skip if baseline calculation fails
      }

      // --- Calculate SNR ---
      double peakYValue = peak.peakY;
      double signalHeight = peakYValue - localBaseline;
      double localSnr =
          (localNoiseStdDev > 0) ? (signalHeight / localNoiseStdDev) : Double.POSITIVE_INFINITY;
      if (signalHeight <= 0) {
        localSnr = 0;
      }

      // --- Filter based on local SNR ---
      if (localSnr >= minSnr) {
        finalPeaks.add(new DetectedPeak(peakIdx, peak.peakX, peakYValue, localSnr, scale));
      }
    }
    return finalPeaks;
  }

  private List<PeakRange> convertIndexRangesToPeakRanges(List<DetectedPeak> peaks,
      Map<Integer, Range<Integer>> indexRanges, double[] x) {
    List<PeakRange> peakRanges = new ArrayList<>();
    if (peaks == null || indexRanges == null || x == null) {
      return peakRanges;
    }

    for (DetectedPeak peak : peaks) {
      Range<Integer> indexRange = indexRanges.get(peak.peakIndex);
      if (indexRange != null) {
        int leftIdx = indexRange.lowerEndpoint();
        int rightIdx = indexRange.upperEndpoint();
        if (leftIdx >= 0 && rightIdx < x.length && leftIdx <= rightIdx) {
          try {
            Range<Double> range = Range.closed(x[leftIdx], x[rightIdx]);
            peakRanges.add(new PeakRange(range, peak.peakIndex, peak.peakX, peak.peakY));
          } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(
                "Error creating range for peak " + peak.peakIndex + ": index out of bounds ["
                    + leftIdx + ", " + rightIdx + "] for x-array length " + x.length);
          }
        } else {
          System.err.println(
              "Warning: Invalid index range [" + leftIdx + ", " + rightIdx + "] for peak "
                  + peak.peakIndex + " during final boundary conversion (x-array length: "
                  + x.length + ").");
        }
      } else {
        System.err.println("Warning: Missing index range for final peak " + peak.peakIndex
            + " during conversion.");
      }
    }
    return peakRanges;
  }

  private List<Range<Double>> mergePeakRanges(List<PeakRange> peakRanges, double proximityFactor) {
    if (peakRanges == null || peakRanges.size() <= 1) {
      return peakRanges.stream().map(pr -> pr.range).collect(Collectors.toList());
    }

    peakRanges.sort(Comparator.comparingDouble(pr -> pr.range.lowerEndpoint()));
    LinkedList<PeakRange> merged = new LinkedList<>();

    for (PeakRange current : peakRanges) {
      if (merged.isEmpty()) {
        merged.add(current);
      } else {
        PeakRange previous = merged.getLast();
        boolean shouldMerge = false;

        if (previous.range.isConnected(current.range)
            && RangeUtils.rangeLength(previous.range.intersection(current.range))
            > ZERO_THRESHOLD) {
          shouldMerge = true;
        } else if (proximityFactor > 0) {
          double prevWidth = previous.range.upperEndpoint() - previous.range.lowerEndpoint();
          double currWidth = current.range.upperEndpoint() - current.range.lowerEndpoint();
          double avgWidth = (prevWidth + currWidth) / 2.0;
          if (avgWidth <= 0) {
            avgWidth = Math.max(Math.abs(previous.peakX - current.peakX) / 4.0, 1e-6);
          }
          double peakDistance = Math.abs(previous.peakX - current.peakX);
          if (peakDistance < proximityFactor * avgWidth) {
            shouldMerge = true;
          }
        }

        if (shouldMerge) {
          Range<Double> mergedRange = Range.closed(previous.range.lowerEndpoint(),
              Math.max(previous.range.upperEndpoint(), current.range.upperEndpoint()));
          PeakRange mergedPeakRange;
          if (current.peakY >= previous.peakY) {
            mergedPeakRange = new PeakRange(mergedRange, current.peakIndex, current.peakX,
                current.peakY);
          } else {
            mergedPeakRange = new PeakRange(mergedRange, previous.peakIndex, previous.peakX,
                previous.peakY);
          }
          merged.removeLast();
          merged.add(mergedPeakRange);
        } else {
          merged.add(current);
        }
      }
    }
    return merged.stream().map(pr -> pr.range).collect(Collectors.toList());
  }


  @Override
  public @NotNull Class<? extends MZmineModule> getModuleClass() {
    return WaveletResolverModule.class; // Placeholder
  }
}
