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
import com.google.common.primitives.Booleans;
import io.github.mzmine.datamodel.SimpleRange.SimpleIntegerRange;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.WaveletResolverParameters.NoiseCalculation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  private final EdgeDetectors edgeDetector;
  private final boolean dipFilter;
  private final Integer signChangesEveryNPoints;
  private final double maxSimilarHeightRatio;
  private final boolean saturationFilter;
  private final double dataMaxBpc;
  private double[] yPadded = new double[0];

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
    edgeDetector = parameterSet.getParameter(WaveletResolverParameters.advancedParameters)
        .getValueOrDefault(AdvancedWaveletParameters.edgeDetector, EdgeDetectors.ABS_MIN);
    dipFilter = parameterSet.getParameter(WaveletResolverParameters.advancedParameters)
        .getValueOrDefault(AdvancedWaveletParameters.dipFilter, true);
    signChangesEveryNPoints = parameterSet.getParameter(
            WaveletResolverParameters.advancedParameters)
        .getValueOrDefault(AdvancedWaveletParameters.signChanges, null);
    maxSimilarHeightRatio = parameterSet.getParameter(WaveletResolverParameters.advancedParameters)
        .getValueOrDefault(AdvancedWaveletParameters.maxSimilarHeightRatio,
            AdvancedWaveletParameters.DEFAULT_SIM_HEIGHT_RATIO);

    final OptionalDouble dataMaxIntensity = getRawDataFile().getScans().stream()
        .mapToDouble(s -> Objects.requireNonNullElse(s.getBasePeakIntensity(), 0d)).max();
    dataMaxBpc = dataMaxIntensity.orElse(Double.MAX_VALUE);

    saturationFilter = dataMaxIntensity.isPresent() ? parameterSet.getParameter(
            WaveletResolverParameters.advancedParameters)
        .getValueOrDefault(AdvancedWaveletParameters.saturationFilter, true) : false;

  }

  private static int findClosestLocalMax(double[] y, int initialIndex, int start, int end) {
    int bestYIndex = initialIndex;

    // determine which direction to search for the maximum
    final int direction;
    if (end > initialIndex) {
      // this point > next point? -> currently decreasing, go left
      direction = y[initialIndex] > y[initialIndex + 1] ? -1 : 1;
    } else if (start < initialIndex) {
      // this point < previous point? -> currently decreasing, go left
      direction = y[initialIndex] < y[initialIndex - 1] ? -1 : 1;
    } else {
      throw new IllegalArgumentException("Invalid search range for initial index");
    }

    double maxY = y[initialIndex];
    int index = initialIndex;
    while (index > 1 && index < y.length - 1 && y[index + direction] > maxY) {
      index += direction;
      maxY = y[index];
      bestYIndex = index;
    }
    return bestYIndex;
    /*for (int k = initialIndex + direction; k <= end && k >= start; k += direction) {
      if (y[k] > maxY) {
        maxY = y[k];
        bestYIndex = k;
      } else {
        break;
      }
    }
    return bestYIndex;*/
  }

  private static int peakScaleToDipTolerance(double scale) {
    if (scale < 2) {
      return 0;
    }

    if (scale < 5) {
      return 1;
    }

    return (int) Math.round(scale * 0.38);
  }

  private static @NotNull DetectedPeak mergeTwoPeaks(@NotNull final DetectedPeak current,
      @NotNull final DetectedPeak previous) {
    Range<Integer> mergedRange = Range.closed(previous.leftBoundaryIndex(),
        Math.max(previous.rightBoundaryIndex(), current.rightBoundaryIndex()));
    final DetectedPeak mergedPeakRange;
    if (current.peakY() >= previous.peakY()) {
      mergedPeakRange = new DetectedPeak(current.peakIndex(), current.peakX(), current.peakY(),
          current.contributingScale(), current.snr(), mergedRange.lowerEndpoint(),
          mergedRange.upperEndpoint());
    } else {
      mergedPeakRange = new DetectedPeak(previous.peakIndex(), previous.peakX(), previous.peakY(),
          previous.contributingScale(), previous.snr(), mergedRange.lowerEndpoint(),
          mergedRange.upperEndpoint());
    }
    return mergedPeakRange;
  }

  @Nullable
  private DetectedPeak findAndSetBoundaryWithTolerance(double[] y, DetectedPeak peak,
      final int numTol) {
    final int numPoints = y.length;
    final int peakIdx = peak.peakIndex();

    if (peakIdx < 0 || peakIdx >= numPoints) {
      logger.warning("Invalid peak index " + peakIdx + " encountered during boundary finding.");
      return null;
    }

    final EdgeDetector edgeDetector = this.edgeDetector.create(numTol);
    final int leftMin = edgeDetector.detectLeftMinimum(y, peakIdx);
    final int rightMin = edgeDetector.detectRightMinimum(y, peakIdx);

    if (leftMin >= rightMin || rightMin - leftMin < minDataPoints) {
      return null;
    }

    final DetectedPeak peakWithBounds = peak.withBoundaries(leftMin, rightMin);
    if (signChangesEveryNPoints != null) {
      double jaggedness = Jaggedness.signChangesPerNPoints(peakWithBounds, y,
          signChangesEveryNPoints);
      if (jaggedness > 1) {
        return null;
      }
    }

    // *** Set boundaries on the peak object ***
    return peakWithBounds;
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
    List<DetectedPeak> peaksWithBounds = findAndSetBoundaries(heightFilteredPeaks, y);
    if (peaksWithBounds.isEmpty()) {
      return Collections.emptyList();
    }

    peaksWithBounds = mergePeakRanges(peaksWithBounds, mergeProximityFactor, x, y);
    peaksWithBounds = dipFilter ? dipFilter(peaksWithBounds, y) : peaksWithBounds;
    peaksWithBounds = saturationFilter ? saturationFilter(peaksWithBounds, y) : peaksWithBounds;

    // Local Noise/Baseline Estimation and SNR Filter
    final List<DetectedPeak> finalDetectedPeaks = estimateLocalNoiseBaselineAndFilterBySNR(
        peaksWithBounds, x, y, minSnr);
    // todo: maybe filter peaks that have a lot of zero->non zero transitions in their proximity
    if (finalDetectedPeaks.isEmpty()) {
      return Collections.emptyList();
    }

    // second pass to re-estimate noise without bad peaks.
    final List<DetectedPeak> secondPassPeaks =
        robustnessIteration ? estimateLocalNoiseBaselineAndFilterBySNR(finalDetectedPeaks, x, y,
            minSnr) : finalDetectedPeaks;
//    logger.finest("Second noise pass removed %d signals".formatted(
//        finalDetectedPeaks.size() - secondPassPeaks.size()));

    // Merge Overlapping / Proximal Peaks
    List<DetectedPeak> finalPeaks = secondPassPeaks;
//    List<DetectedPeak> finalPeaks = mergePeakRanges(secondPassPeaks, mergeProximityFactor, x,
//        y);
//    finalPeaks = dipFilter ? dipFilter(finalPeaks, y) : finalPeaks;

    return finalPeaks.stream().map(p -> p.asRtRange(x))
        .sorted(Comparator.comparing(Range::lowerEndpoint)).toList();
  }

  /**
   * Ion suppression or spray instability may lead to dips in the baseline, which may be detected as
   * two peaks (one on the left, one on the right)
   *
   * @param peaks
   * @param y
   * @return
   */
  private List<DetectedPeak> dipFilter(List<DetectedPeak> peaks, double[] y) {
    final List<DetectedPeak> dipFiltered = new ArrayList<>(peaks.size());

    boolean lastPeakRemoved = false;
    for (int i = 0; i < peaks.size() - 1; i++) {
      final DetectedPeak current = peaks.get(i);
      final DetectedPeak next = peaks.get(i + 1);

      if (current.rightBoundaryIndex() != next.leftBoundaryIndex()) {
        // not connect, keep
        dipFiltered.add(current);
        continue;
      }

      final double[] edgeAndTop = new double[]{y[current.leftBoundaryIndex()],
          y[current.peakIndex()], y[next.peakIndex()], y[next.rightBoundaryIndex()]};
      final double avg = MathUtils.calcAvg(edgeAndTop);
      final double std = MathUtils.calcStd(edgeAndTop);
      final double rsd = std / avg;

      final double currentTopToRightEdge =
          current.peakY() / Math.max(y[current.rightBoundaryIndex()], 1);
      final double currentTopToLeftEdge =
          current.peakY() / Math.max(y[current.leftBoundaryIndex()], 1);
      final double nextTopToLeftEdge = next.peakY() / Math.max(y[next.leftBoundaryIndex()], 1d);
      final double nextTopToRightEdge = next.peakY() / Math.max(y[next.rightBoundaryIndex()], 1);

      final boolean edgeCriterion = (currentTopToLeftEdge < 2 * currentTopToRightEdge
          && nextTopToLeftEdge > 2 * nextTopToRightEdge && (currentTopToLeftEdge < 1.5
          && nextTopToRightEdge < 1.5));

      final boolean averageValuesCheck = Arrays.stream(edgeAndTop)
          .allMatch(value -> avg * 0.7 < value && avg * 1.3 > value);

      if (averageValuesCheck || rsd < 0.3 || edgeCriterion) {
        /*logger.finest(
            "Dip detected (edge=%s, rsd: %s, avg: %s) at %s and %s".formatted(edgeCriterion,
                String.format("%.2f", rsd), averageValuesCheck, current.toString(),
                next.toString()));*/
      }

      if (Booleans.countTrue(averageValuesCheck, rsd < 0.3, edgeCriterion) >= 2) {
        logger.finest(
            "Dip detected (edge=%s, rsd: %s, avg: %s) at %s and %s".formatted(edgeCriterion,
                String.format("%.2f", rsd), averageValuesCheck, current.toString(),
                next.toString()));

        i++; // avoid both peaks.
        if (i == peaks.size() - 1) {
          lastPeakRemoved = true;
        }
        continue;
      }

      dipFiltered.add(current);
    }

    if (!lastPeakRemoved && !peaks.isEmpty()) {
      dipFiltered.add(peaks.getLast());
    }

    return dipFiltered;
  }

  private List<DetectedPeak> saturationFilter(List<DetectedPeak> peaks, double[] y) {
    if (peaks.isEmpty()) {
      return peaks;
    }

    final double saturationThreshold = dataMaxBpc * 0.9;
    final List<DetectedPeak> saturationFiltered = new ArrayList<>(peaks.size());

    saturationFiltered.add(peaks.getFirst());

    for (int i = 1; i < peaks.size(); i++) {
      final DetectedPeak previous = saturationFiltered.getLast();
      final DetectedPeak current = peaks.get(i);

      // check if the two peaks are connected by a plateau
      // first check the ratios of previous.top / previous.right_edge and current.top / current.left_edge

      if (previous.peakY() < saturationThreshold && current.peakY() < saturationThreshold) {
        saturationFiltered.add(current);
        continue;
      }

      final double previousTopEdge = previous.peakY() / y[previous.rightBoundaryIndex()];
      final double currentTopEdge = current.peakY() / y[current.leftBoundaryIndex()];
      final int saturationTolerance = Math.max(peakScaleToDipTolerance(current.contributingScale()),
          peakScaleToDipTolerance(previous.contributingScale())) * 2;
      final int edgeDistance = Math.abs(
          previous.rightBoundaryIndex() - current.leftBoundaryIndex());

      if (previousTopEdge < 1.2 && currentTopEdge < 1.2 && (edgeDistance < saturationTolerance)) {
        saturationFiltered.removeLast();
        saturationFiltered.add(mergeTwoPeaks(current, previous));
      } else if (previousTopEdge < 1.2 && currentTopEdge < 1.2) {
        boolean allPointsAboveSaturation = true;
        for (int j = previous.rightBoundaryIndex(); j < current.leftBoundaryIndex(); j++) {
          if (y[j] < saturationThreshold) {
            allPointsAboveSaturation = false;
            break;
          }
        }
        if (allPointsAboveSaturation) {
          saturationFiltered.removeLast();
          saturationFiltered.add(mergeTwoPeaks(current, previous));
        }
      } else {
        saturationFiltered.add(current);
      }
    }

    return saturationFiltered;
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

  double[] generateMexicanHat(int length, double scale) {

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
          if (y[bestYIndex] < minPeakHeight) {
            continue;
          }
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
            .max(Comparator.comparingDouble(p -> p.cwtValue() /*/ Math.sqrt(p.scale())*/))
            .ifPresent(bestPotentials::add);
      }
    }
    bestPotentials.sort(Comparator.comparingInt(PotentialPeak::index));

    List<PotentialPeak> refinedPotentials = new ArrayList<>();
    for (PotentialPeak pp : bestPotentials) {
      if (Double.isInfinite(y[pp.index()])) {
        continue;
      }

      refinedPotentials.add(
          new PotentialPeak(pp.index(), pp.scale(), pp.cwtValue(), y[pp.index()]));
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
            new DetectedPeak(pp.index(), x[pp.index()], peakYValue, pp.scale(), Double.NaN));
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
  private List<DetectedPeak> findAndSetBoundaries(List<DetectedPeak> peaks, double[] y) {
    if (peaks == null || peaks.isEmpty() || y == null || y.length == 0) {
      return Collections.emptyList();
    }

    List<DetectedPeak> peaksWithBounds = new ArrayList<>(peaks.size() / 2);
    for (DetectedPeak peak : peaks) {
      final var peakWithBounds = findAndSetBoundaryWithTolerance(y, peak,
          peakScaleToDipTolerance(peak.contributingScale()));
      if (peakWithBounds != null) {
        peaksWithBounds.add(peakWithBounds);
      }
    }
    peaksWithBounds.sort(Comparator.comparingInt(DetectedPeak::leftBoundaryIndex));
    return peaksWithBounds;
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
      final SimpleIntegerRange boundaries = peak.indexRange();
      allPeakExclusionZones.add(boundaries.guava());
    }

    for (final DetectedPeak peak : peaks) {
      // --- Get Peak Boundaries directly from object ---
      if (!peak.hasValidBoundaries(n)) {
        logger.warning(
            "Skipping SNR calculation for peak " + peak.toString() + " due to invalid boundaries.");
        continue; // Skip if boundaries are not valid
      }
      final int leftEdgeIdx = peak.leftBoundaryIndex();
      final int rightEdgeIdx = peak.rightBoundaryIndex();
      final double scale = peak.contributingScale();
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
        final double fallbackSnr = peak.peakY() / localBaseline;
        if (fallbackSnr > minSnr) {
          finalPeaks.add(peak.withSNR(fallbackSnr));
        }
        continue;
      }

      final double localBaseline = (y[leftEdgeIdx] + y[rightEdgeIdx]) / 2;
      final double localNoiseStdDev = getLocalNoiseEstimate(peak, localBackgroundSamples,
          localBaseline);

      // --- Calculate SNR ---
      final double signalHeight = peak.peakY() - localBaseline;
      if (signalHeight <= 0) {
        continue;
      }
      final double localSnr =
          (localNoiseStdDev > 0) ? (signalHeight / localNoiseStdDev) : Double.POSITIVE_INFINITY;

      if (localSnr < 2 * minSnr) {
        // for peaks that are only slightly above the SNR, check if there are a lot of signals that
        // are similar to the height of the actual peak. If that is the case, it might just be a
        // hump on the baseline
        final long signalsOfSimilarHeight = localBackgroundSamples.doubleStream()
            .filter(signal -> peak.peakY() * 0.8 < signal).count();
        final double similarHeightProportion =
            (double) signalsOfSimilarHeight / localBackgroundSamples.size();
        if (similarHeightProportion > maxSimilarHeightRatio) {
          continue;
        }
      }

      // --- Filter based on local SNR ---
      if (localSnr >= minSnr || (topToEdge != null && localBaseline > 0.0
          && peak.peakY() / localBaseline >= topToEdge)) {
        // Update the SNR on the existing peak object before adding
        finalPeaks.add(peak.withSNR(localSnr)); // Add the peak that passed
      }
    }
    return finalPeaks;
  }

//  private int getJaggedness(double[] y, int startInclusive, int endExclusive) {
//
//    int
//    for (int i = startInclusive; i < endExclusive; i++) {
//
//    }
//  }

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
              "Warning: Could not calculate local MAD for peak at index " + peak.peakIndex() + ": "
                  + madEx.getMessage() + ". Assuming zero noise.");
          yield 0.0; // Assume zero if MAD fails
        }
      }
    };
  }

  private List<DetectedPeak> mergePeakRanges(@NotNull List<DetectedPeak> peakRanges,
      double proximityFactor, double[] x, double[] y) {
    if (peakRanges.size() <= 1) {
      return peakRanges;
    }

    peakRanges.sort(Comparator.comparingDouble(pr -> pr.leftBoundaryIndex()));
    LinkedList<DetectedPeak> merged = new LinkedList<>();

    for (DetectedPeak current : peakRanges) {
      if (merged.isEmpty()) {
        merged.add(current);
        continue;
      }
      final DetectedPeak previous = merged.getLast();
      boolean shouldMerge = false;

      if (previous.indexRange().isConnected(current.indexRange())) {
        // check if one peak enlcoses max of the other
        if (previous.indexRange().contains(current.peakIndex()) || current.indexRange()
            .contains(previous.peakIndex())) {
          shouldMerge = true;
        } else {
          final int minIndex = ArrayUtils.indexOfMin(y, current.indexRange().lower(),
              previous.indexRange().upper() + 1);
          merged.removeLast();
          // replace previous peak with end in local minimum
          merged.add(previous.withBoundaries(previous.indexRange().lower(), minIndex));

          if (minIndex < current.peakIndex() && minIndex < current.indexRange().upper()) {
            // replace current peak with new start point
            current = current.withBoundaries(minIndex, current.indexRange().upper());
          }
        }
      }

      if (shouldMerge) {
        final DetectedPeak mergedPeakRange = mergeTwoPeaks(current, previous);
        merged.removeLast();
        merged.add(mergedPeakRange);
      } else {
        merged.add(current);
      }
    }

    return merged;
  }

  @Override
  public @NotNull Class<? extends MZmineModule> getModuleClass() {
    return WaveletResolverModule.class; // Placeholder
  }
}
