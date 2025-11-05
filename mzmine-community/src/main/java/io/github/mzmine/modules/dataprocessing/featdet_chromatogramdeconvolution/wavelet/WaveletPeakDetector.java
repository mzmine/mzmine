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
import io.github.mzmine.util.ArrayUtils;
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
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jetbrains.annotations.NotNull;

@NotThreadSafe
public class WaveletPeakDetector extends AbstractResolver {

  private static final Logger logger = Logger.getLogger(WaveletPeakDetector.class.getName());
  private static final double ZERO_THRESHOLD = 1e-9;
  private final double[] scales;
  private final double minSnr;
  private final double minPeakHeight;
  private final double mergeProximityFactor;
  private final double WAVELET_KERNEL_RADIUS_FACTOR;
  private final double LOCAL_NOISE_WINDOW_FACTOR; // Scales how many points to *try* collecting past edges
  private final int MIN_WINDOW_TARGET_POINTS_PER_SIDE = 3; // Minimum target background points per side
  private final NoiseCalculation noiseMethod;
  private final int minDataPoints;
  private final Double topToEdge;
  private final Map<Integer, Map<Double, double[]>> waveletBuffer = new HashMap<>();
  private final int minFittingScales;
  private final boolean robustnessIteration;
  List<Range<Double>> snrRanges = new ArrayList<>();
  private double[] yPadded = new double[0];

  // ... (constants, fields, constructor) ...
  public WaveletPeakDetector(double[] scales, double minSnr, Double topToEdge, double minPeakHeight,
      double mergeProximityFactor, double waveletKernelRadiusFactor, double localNoiseWindowFactor,
      int minFittingScales, boolean robustnessIteration, ModularFeatureList flist,
      ParameterSet parameterSet) {
    super(parameterSet, flist);
    this.minFittingScales = minFittingScales;
    this.robustnessIteration = robustnessIteration;
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
    this.topToEdge = topToEdge;
  }

  private static int findClosestLocalMax(double[] y, int initialIndex, int start, int end) {
    double maxY = y[initialIndex];
    int bestYIndex = initialIndex;
    for (int k = initialIndex + 1; k <= end; k++) {
      if (y[k] > maxY) {
        maxY = y[k];
        bestYIndex = k;
      } else {
        break;
      }
    }
    // find the closest local maximum to the initial index
    for (int k = initialIndex - 1; k >= start; k--) {
      if (y[k] > maxY) {
        maxY = y[k];
        bestYIndex = k;
      } else {
        break;
      }
    }
    return bestYIndex;
  }

  private static void findAndSetLocalMinimaBoundary(double[] y, DetectedPeak peak) {
    final int numPoints = y.length;
    final int peakIdx = peak.peakIndex;

    if (peakIdx < 0 || peakIdx >= numPoints) {
      logger.warning("Invalid peak index " + peakIdx + " encountered during boundary finding.");
      peak.setBoundaryIndices(-1, -1); // Mark as invalid
      return;
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
    while (rightIdx < numPoints - 1) {
      if (y[rightIdx + 1] > y[rightIdx]) { // Valley minimum found
        break;
      }
      if (rightIdx <= numPoints - 3 && isNearZero(y[rightIdx + 1]) && isNearZero(y[rightIdx + 2])) {
        rightIdx++; // Include the first zero
        break;
      }
      rightIdx++;
    }

    // *** Set boundaries on the peak object ***
    peak.setBoundaryIndices(leftIdx, rightIdx);
  }

  private static void findAndSetLocalMinimaBoundaryWithTolerance(double[] y, DetectedPeak peak,
      final int numTol) {
    final int numPoints = y.length;
    final int peakIdx = peak.peakIndex;

    if (peakIdx < 0 || peakIdx >= numPoints) {
      logger.warning("Invalid peak index " + peakIdx + " encountered during boundary finding.");
      peak.setBoundaryIndices(-1, -1); // Mark as invalid
      return;
    }

    // --- Find Left Boundary Index ---
    int leftIdx = peakIdx;
    int numIncreasing = 0;
    while (leftIdx > 0) {
      leftIdx--;
      if (leftIdx > 1 && y[leftIdx - 1] < y[leftIdx]) {
        // still decreasing
        continue;
      }
      numIncreasing++;
      if (numIncreasing > numTol) {
        leftIdx += (numIncreasing - 1);
        break;
      }
    }

    // --- Find Right Boundary Index ---
    numIncreasing = 0;
    int rightIdx = peakIdx;
    while (rightIdx < numPoints - 2) {
      rightIdx++;
      if (y[rightIdx + 1] < y[rightIdx]) {
        // still decreasing
        numIncreasing = 0;
        continue;
      }
      numIncreasing++;
      if (numIncreasing > numTol) {
        rightIdx -= (numIncreasing - 1);
        break;
      }
    }

    // *** Set boundaries on the peak object ***
    peak.setBoundaryIndices(leftIdx, rightIdx);
  }

  /**
   * Helper to check if a value is close to zero
   */
  private static boolean isNearZero(double value) {
    return Math.abs(value) < ZERO_THRESHOLD;
  }

  private static int peakScaleToDipTolerance(double scale) {
//    return 0;
    if (scale < 2) {
      return 0;
    }
    if (scale >= 2 && scale <= 4) {
      return 1;
    }
    return 2;
  }

  @Override
  public List<Range<Double>> resolve(double[] x, double[] y) {
    if (x == null || y == null || x.length != y.length || x.length < 5) {
      logger.warning(
          "Warning: Invalid input data (null, mismatched length, or too short). Returning empty list.");
      return Collections.emptyList();
    }
    final int n = y.length;

    // Compute CWT
    final double[][] cwtCoefficients = calculateCWT(y, scales);

    // Find Potential Peaks
    final List<PotentialPeak> potentialPeaks = findPotentialPeaksFromCWT(cwtCoefficients, scales, x,
        y);

    // Initial Filtering - Height Only
    final List<DetectedPeak> heightFilteredPeaks = filterByHeight(potentialPeaks, y, x,
        minPeakHeight);

    if (heightFilteredPeaks.isEmpty()) {
      return Collections.emptyList();
    }

    // determine & SET Index Ranges for Height-Filtered Peaks
    findAndSetLocalMinimaBoundaries(heightFilteredPeaks, y);

    // Local Noise/Baseline Estimation and SNR Filter
    final List<DetectedPeak> finalDetectedPeaks = estimateLocalNoiseBaselineAndFilterBySNR(
        heightFilteredPeaks, x, y, minSnr);
    // todo: maybe filter peaks that have a lot of zero->non zero transitions in their proximity
    if (finalDetectedPeaks.isEmpty()) {
      return Collections.emptyList();
    }

    // second pass to re-estimate noise without bad peaks.
    final List<DetectedPeak> secondPassPeaks =
        robustnessIteration ? estimateLocalNoiseBaselineAndFilterBySNR(finalDetectedPeaks, x, y,
            minSnr) : finalDetectedPeaks;
    logger.finest("Second noise pass removed %d signals".formatted(
        finalDetectedPeaks.size() - secondPassPeaks.size()));

    final List<PeakRange> finalPeakRanges = convertPeaksToPeakRanges(secondPassPeaks, x);

    // Merge Overlapping / Proximal Peaks
    final List<Range<Double>> mergedRanges = mergePeakRanges(finalPeakRanges, mergeProximityFactor,
        x, y);

    // Sort final ranges by start time
    mergedRanges.sort(Comparator.comparing(Range::lowerEndpoint));

    /*return finalPeakRanges.stream().map(p -> {
      final Range<Integer> range = p.indexRange();
      return Range.closed(x[range.lowerEndpoint()], x[range.upperEndpoint()]);
    }).toList();*/
    return mergedRanges;
  }

  private double[][] calculateCWT(double[] y, double[] scales) {
    final int n = y.length;
    int N_padded = Integer.highestOneBit(n);
    if (N_padded < n) {
      N_padded <<= 1;
    }
    if (N_padded == 0) {
      N_padded = 2; // Handle very small n
    }
    if (yPadded.length != N_padded) {
      yPadded = Arrays.copyOf(y, N_padded); // Pad with zeros
    } else {
      System.arraycopy(y, 0, yPadded, 0, y.length);
      Arrays.fill(yPadded, y.length, N_padded, 0d);
    }

    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
    Complex[] yFFT = fft.transform(yPadded, TransformType.FORWARD);
    double[][] cwt = new double[scales.length][n];

    for (int i = 0; i < scales.length; i++) {
      final double scale = scales[i];
      final double[] waveletKernel = generateMexicanHat(N_padded, scale);
      Complex[] waveletFFT = fft.transform(waveletKernel, TransformType.FORWARD);
      Complex[] waveletFFTConj = Arrays.stream(waveletFFT).map(Complex::conjugate)
          .toArray(Complex[]::new);

      Complex[] productFFT = new Complex[N_padded];
      for (int k = 0; k < N_padded; k++) {
        productFFT[k] = yFFT[k].multiply(waveletFFTConj[k]);
      }

      Complex[] convolutionComplex = fft.transform(productFFT, TransformType.INVERSE);
      double scaleFactor = 1.0 / Math.sqrt(Math.max(scale, 1));
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
      final double[] wavelet = new double[length];
      final double scaleSq = scale * scale;
      final int support = (int) Math.min(length / 2.0, WAVELET_KERNEL_RADIUS_FACTOR * scale);
      final double normFactor = 1.0;

      for (int i = -support; i <= support; i++) {
        double t = (double) i;
        double tSq = t * t;
        double term1 = (1.0 - tSq / scaleSq);
        double term2 = Math.exp(-tSq / (2.0 * scaleSq));
        double value = normFactor * term1 * term2;
        int index = (i + length) % length;
        wavelet[index] = value;
      }

      final double sum = Arrays.stream(wavelet).sum();
      if (Math.abs(sum) > 1e-9) {
        double mean = sum / length;
        for (int i = 0; i < length; i++) {
          wavelet[i] -= mean;
        }
      }
      return wavelet;
    });
  }

  private List<PotentialPeak> findPotentialPeaksFromCWT(double[][] cwt, final double[] scales,
      final double[] x, final double[] y) {
    int nScales = cwt.length;
    int nPoints = cwt[0].length;
    final List<PotentialPeak> allMaxima = new ArrayList<>();

    for (int i = 0; i < nScales; i++) {
      double[] cwtAtScale = cwt[i];
      for (int j = 1; j < nPoints - 1; j++) {
        // do we have a local maximum in cwt?
        if (cwtAtScale[j] > cwtAtScale[j - 1] && cwtAtScale[j] > cwtAtScale[j + 1]
            && cwtAtScale[j] > 0) {
          // check if the index of the cwt maximum corresponds to a maximum in the actual y data.
          // this may sometimes not be the case for non symmetric peaks. -> disabled for now
//          boolean isLocalMaxInY =
//              (j > 0 && j < y.length - 1 && y[j] >= y[j - 1] && y[j] >= y[j + 1]) || (j == 0
//                  && y.length > 1 && y[j] >= y[j + 1]) || (j == y.length - 1 && y.length > 1
//                  && y[j] >= y[j - 1]);
//          if (isLocalMaxInY) {
//            allMaxima.add(new PotentialPeak(j, scales[i], cwtAtScale[j], y[j]));
//          }

          // find the closest local maximum to the initial index
          final int searchRadius = Math.max(1, (int) (scales[i] / 2.0));
          final int start = Math.max(0, j - searchRadius);
          final int end = Math.min(y.length - 1, j + searchRadius);
          final int bestYIndex = findClosestLocalMax(y, j, start, end);
          // set the actual cwtAtScale, but the index and y value at the actual data maximum (not cwt maximum)
          allMaxima.add(new PotentialPeak(bestYIndex, scales[i], cwtAtScale[j], y[bestYIndex]));
        }
      }
    }

    final Map<Integer, List<PotentialPeak>> groupedByIndex = allMaxima.stream()
        .collect(Collectors.groupingBy(p -> p.index()));

    List<PotentialPeak> bestPotentials = new ArrayList<>();
    for (Map.Entry<Integer, List<PotentialPeak>> entry : groupedByIndex.entrySet()) {
      List<PotentialPeak> peaksAtIndex = entry.getValue();
      if (peaksAtIndex.size() >= minFittingScales) {
        peaksAtIndex.stream()
            .max(Comparator.comparingDouble(p -> p.cwtValue() / Math.sqrt(p.scale())))
            .ifPresent(bestPotentials::add);
      }
    }
    bestPotentials.sort(Comparator.comparingInt(PotentialPeak::index));

    List<PotentialPeak> refinedPotentials = new ArrayList<>();
    Set<Integer> addedIndices = new HashSet<>();
    for (PotentialPeak pp : bestPotentials) {

      if (Double.isInfinite(y[pp.index()])) {
        continue;
      }

      if (!addedIndices.contains(pp.index())) {
        refinedPotentials.add(
            new PotentialPeak(pp.index(), pp.scale(), pp.cwtValue(), y[pp.index()]));
        addedIndices.add(pp.index());
      }
    }

    refinedPotentials.sort(Comparator.comparingInt(PotentialPeak::index));
    return refinedPotentials;
  }

  private List<DetectedPeak> filterByHeight(List<PotentialPeak> potentials, double[] y, double[] x,
      double minPeakHeight) {
    final List<DetectedPeak> heightFiltered = new ArrayList<>();

    for (final PotentialPeak pp : potentials) {
      final double peakYValue = pp.originalY();
      if (peakYValue >= minPeakHeight && pp.index() >= 0 && pp.index() < x.length) {
        heightFiltered.add(
            new DetectedPeak(pp.index(), x[pp.index()], peakYValue, Double.NaN, pp.scale()));
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
  private void findAndSetLocalMinimaBoundaries(List<DetectedPeak> peaks, double[] y) {
    if (peaks == null || peaks.isEmpty() || y == null || y.length == 0) {
      return;
    }

    for (DetectedPeak peak : peaks) {
//      findAndSetLocalMinimaBoundary(y, peak);
      findAndSetLocalMinimaBoundaryWithTolerance(y, peak, peakScaleToDipTolerance(peak.scale()));
    }
  }

  /**
   * Calculates local noise/baseline and filters by SNR. Now reads boundary indices directly from
   * the peak objects.
   */
  private List<DetectedPeak> estimateLocalNoiseBaselineAndFilterBySNR(List<DetectedPeak> peaks,
      final double[] x, final double[] y, final double minSnr) {

    final List<DetectedPeak> finalPeaks = new ArrayList<>();
    final int n = y.length;

    // *** Build combined exclusion zones from peak boundaries ***
    final RangeSet<Integer> allPeakExclusionZones = TreeRangeSet.create();
    for (DetectedPeak peak : peaks) {
      final Range<Integer> boundaries = peak.getBoundaryIndexRange(n);
      if (boundaries != null) {
        allPeakExclusionZones.add(boundaries);
      }
    }

    for (final DetectedPeak peak : peaks) {
      // --- Get Peak Boundaries directly from object ---
      if (!peak.hasValidBoundaries(n)) {
        logger.warning(
            "Skipping SNR calculation for peak " + peak.toString() + " due to invalid boundaries.");
        continue; // Skip if boundaries are not valid
      }
      final int leftEdgeIdx = peak.leftBoundaryIndex;
      final int rightEdgeIdx = peak.rightBoundaryIndex;
      final double scale = peak.contributingScale;
      if (rightEdgeIdx - leftEdgeIdx < minDataPoints) {
        continue;
      }

      // --- Define Target Number of Background Points per Side ---
      final int targetPointsPerSide = (int) Math.max(MIN_WINDOW_TARGET_POINTS_PER_SIDE,
          (LOCAL_NOISE_WINDOW_FACTOR * (rightEdgeIdx - leftEdgeIdx)) / 2);

      // --- Collect Local Background Samples Dynamically ---
      DoubleArrayList localBackgroundSamples = new DoubleArrayList(targetPointsPerSide * 2);
      int leftSamplesCount = 0;
      int rightSamplesCount = 0;

      // Search Left
      for (int i = leftEdgeIdx - 1; i >= 0 &&
          // allow expansion of the search for a certain range, but not too far so we don't
          // search in disconnected areas
          i > leftEdgeIdx - 2 * targetPointsPerSide && leftSamplesCount < targetPointsPerSide;
          i--) {
        if (!allPeakExclusionZones.contains(i)) {
          localBackgroundSamples.add(y[i]);
          leftSamplesCount++;
        }
      }
      // Search Right
      for (int i = rightEdgeIdx + 1; i < n &&
          // allow expansion of the search for a certain range, but not too far so we don't
          // search in disconnected areas
          i < rightEdgeIdx + 2 * targetPointsPerSide && rightSamplesCount < targetPointsPerSide;
          i++) {
        if (!allPeakExclusionZones.contains(i)) {
          localBackgroundSamples.add(y[i]);
          rightSamplesCount++;
        }
      }

      // check for number of background samples
      if (localBackgroundSamples.size() < MIN_WINDOW_TARGET_POINTS_PER_SIDE * 2) {
        // Todo: This is a test to fall back to just the top/edge ratio ONLY if not enough background samples were found
        final double localBaseline = (y[leftEdgeIdx] + y[rightEdgeIdx]) / 2;
        // todo: if the intensities jump above and below the noise level, we get a lot of dips to 0
        //  this may lead to noise being detected. Since that happens a lot, in these cases we are also
        //  unable to find enough background samples (because there are more of these dips).
        //  Hence we end up here. So let's try to exclude a localBaseline of 0 here (both edges 0) (ONLY HERE).
        //  And we also hope that for true peaks, which would be
        //  further above the noise level, we would get proper samples and hence not end up in here
        if (localBaseline == 0) {
          continue;
        }
        final double fallbackSnr = peak.peakY / localBaseline;
        if (fallbackSnr > minSnr) {
          peak.snr = fallbackSnr;
          finalPeaks.add(peak);
        }
        continue;
      }

      final double localBaseline = (y[leftEdgeIdx] + y[rightEdgeIdx]) / 2;
      final double localNoiseStdDev = getLocalNoiseEstimate(peak, localBackgroundSamples,
          localBaseline);

      // --- Calculate SNR ---
      final double signalHeight = peak.peakY - localBaseline;
      if (signalHeight <= 0) {
        continue;
      }
      final double localSnr =
          (localNoiseStdDev > 0) ? (signalHeight / localNoiseStdDev) : Double.POSITIVE_INFINITY;

      // --- Filter based on local SNR ---
      if (localSnr >= minSnr || (topToEdge != null && localBaseline > 0.0
          && peak.peakY / localBaseline >= topToEdge)) {
        // Update the SNR on the existing peak object before adding
        peak.snr = localSnr;
        finalPeaks.add(peak); // Add the peak that passed
      }
    }
    return finalPeaks;
  }

  private double getLocalNoiseEstimate(DetectedPeak peak, DoubleArrayList localBackgroundSamples,
      double localBaseline) {
    return switch (noiseMethod) {
      case STANDARD_DEVIATION -> MathUtils.calcStd(localBackgroundSamples.toDoubleArray());
      case MEDIAN_ABSOLUTE_DEVIATION -> {
        final double[] absDevs = Arrays.stream(localBackgroundSamples.toDoubleArray())
            .map(val -> Math.abs(val - localBaseline)).toArray();
        try {
          final Median medianCalc = new Median();
          final double localMad = medianCalc.evaluate(absDevs);
          yield 1.4826 * localMad;
        } catch (IllegalArgumentException madEx) {
          logger.warning(
              "Warning: Could not calculate local MAD for peak at index " + peak.peakIndex + ": "
                  + madEx.getMessage() + ". Assuming zero noise.");
          yield 0.0; // Assume zero if MAD fails
        }
      }
    };
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
      if (peak.hasValidBoundaries(x.length)) {
        final int leftIdx = peak.leftBoundaryIndex;
        final int rightIdx = peak.rightBoundaryIndex;

        // Check indices are valid for x array access
        peakRanges.add(
            new PeakRange(Range.closed(x[leftIdx], x[rightIdx]), Range.closed(leftIdx, rightIdx),
                peak.peakIndex, peak.peakX, peak.peakY));
      }
    }
    return peakRanges;
  }

  private List<Range<Double>> mergePeakRanges(List<PeakRange> peakRanges, double proximityFactor,
      double[] x, double[] y) {
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

        if (previous.range().isConnected(current.range())) {
          // more than 40 % range length overlap || Todo: or check if one peak enlcoses max of the other
          if (RangeUtils.rangeLength(previous.range().intersection(current.range()))
              > (RangeUtils.rangeLength(previous.range()) + RangeUtils.rangeLength(current.range()))
              * 0.4) {
            shouldMerge = true;
          } else {
            final int minIndex = ArrayUtils.indexOfMin(y, current.indexRange().lowerEndpoint(),
                previous.indexRange().upperEndpoint() + 1);
            merged.removeLast();
            // replace previous peak with end in local minimum
            merged.add(previous.withIndexRange(
                Range.closed(previous.indexRange().lowerEndpoint(), minIndex), x));

            // replace current peak with new start point
            current = current.withIndexRange(
                Range.closed(minIndex, current.indexRange().upperEndpoint()), x);
          }
        } else if (proximityFactor > 0) {
          final double prevWidth =
              previous.range().upperEndpoint() - previous.range().lowerEndpoint();
          final double currWidth =
              current.range().upperEndpoint() - current.range().lowerEndpoint();
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

  @Override
  public @NotNull Class<? extends MZmineModule> getModuleClass() {
    return WaveletResolverModule.class; // Placeholder
  }
}
