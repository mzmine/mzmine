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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay.SavitzkyGolayFeatureResolverParameters.DERIVATIVE_THRESHOLD_LEVEL;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay.SavitzkyGolayFeatureResolverParameters.MIN_PEAK_HEIGHT;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay.SavitzkyGolayFeatureResolverParameters.PEAK_DURATION;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedPeak;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.maths.CenterFunction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * This class implements a peak builder using a match score to link MzPeaks in the axis of retention
 * time. Also uses Savitzky-Golay coefficients to calculate the first and second derivative
 * (smoothed) of raw data points (intensity) that conforms each peak. The first derivative is used
 * to determine the peak's range, and the second derivative to determine the intensity of the peak.
 */
public class SavitzkyGolayFeatureResolver implements FeatureResolver {

  // Savitzky-Golay filter width.
  private static final int SG_FILTER_LEVEL = 12;

  @Override
  public @NotNull String getName() {
    return "Savitzky-Golay";
  }

  @Override
  public Class<? extends MZmineProcessingModule> getModuleClass() {
    return SavitzkyGolayResolverModule.class;
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

    // Calculate intensity statistics.
    double maxIntensity = 0.0;
    double avgIntensity = 0.0;
    for (final double intensity : intensities) {

      maxIntensity = Math.max(intensity, maxIntensity);
      avgIntensity += intensity;
    }

    avgIntensity /= scanCount;

    final List<ResolvedPeak> resolvedPeaks = new ArrayList<ResolvedPeak>(2);

    // If the current chromatogram has characteristics of background or just
    // noise return an empty array.
    if (avgIntensity <= maxIntensity / 2.0) {

      // Calculate second derivatives of intensity values.
      final double[] secondDerivative =
          SGDerivative.calculateDerivative(intensities, false, SG_FILTER_LEVEL);

      // Calculate noise threshold.
      final double noiseThreshold = calcDerivativeThreshold(secondDerivative,
          parameters.getParameter(DERIVATIVE_THRESHOLD_LEVEL).getValue());

      // Search for peaks.
      scanNumbers.sort(Comparator.comparingInt(Scan::getScanNumber));
      final ResolvedPeak[] resolvedOriginalPeaks = peaksSearch(chromatogram, scanNumbers,
          secondDerivative, noiseThreshold, mzCenterFunction, msmsRange, rTRangeMSMS);

      final Range<Double> peakDuration = parameters.getParameter(PEAK_DURATION).getValue();
      final double minimumPeakHeight = parameters.getParameter(MIN_PEAK_HEIGHT).getValue();

      // Apply final filter of detected peaks, according with setup
      // parameters.
      for (final ResolvedPeak p : resolvedOriginalPeaks) {

        if (peakDuration.contains(RangeUtils.rangeLength(p.getRawDataPointsRTRange()).doubleValue())
            && p.getHeight() >= minimumPeakHeight) {

          resolvedPeaks.add(p);
        }
      }
    }

    return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
  }

  /**
   * Search for peaks.
   * 
   * @param chromatogram the chromatogram to search.
   * @param scanNumbers scan number to focus search on
   * @param derivativeOfIntensities derivatives of intensity values.
   * @param noiseThreshold noise threshold.
   * @return array of peaks found.
   */
  private static ResolvedPeak[] peaksSearch(final Feature chromatogram, final List<Scan> scanNumbers,
      final double[] derivativeOfIntensities, final double noiseThreshold,
      CenterFunction mzCenterFunction, final double msmsRange, final double rTRangeMSMS) {

    // Flag to identify the current and next overlapped peak.
    boolean activeFirstPeak = false;
    boolean activeSecondPeak = false;

    // Flag to indicate the value of 2nd derivative pass noise threshold
    // level.
    boolean passThreshold = false;

    // Number of times that 2nd derivative cross zero value for the current
    // feature detection.
    int crossZero = 0;

    final int totalNumberPoints = derivativeOfIntensities.length;

    // Indexes of start and ending of the current peak and beginning of the
    // next.
    int currentPeakStart = totalNumberPoints;
    int nextPeakStart = totalNumberPoints;
    int currentPeakEnd = 0;

    final List<ResolvedPeak> resolvedPeaks = new ArrayList<ResolvedPeak>(2);

    // Shape analysis of derivative of chromatogram "*" represents the
    // original chromatogram shape. "-" represents
    // the shape of chromatogram's derivative.
    //
    // " *** " * * + " + * * + + " + x x + "--+-*-+-----+-*---+---- " + + "
    // + + " +
    //
    for (int i = 1; i < totalNumberPoints; i++) {

      // Changing sign and crossing zero
      if (derivativeOfIntensities[i - 1] < 0.0 && derivativeOfIntensities[i] > 0.0
          || derivativeOfIntensities[i - 1] > 0.0 && derivativeOfIntensities[i] < 0.0) {

        if (derivativeOfIntensities[i - 1] < 0.0 && derivativeOfIntensities[i] > 0.0) {

          if (crossZero == 2) {

            // After second crossing zero starts the next overlapped
            // peak, but depending of
            // passThreshold flag is activated.
            if (passThreshold) {

              activeSecondPeak = true;
              nextPeakStart = i;

            } else {

              currentPeakStart = i;
              crossZero = 0;
              activeFirstPeak = true;
            }
          }
        }

        // Finalize the first overlapped peak.
        if (crossZero == 3) {

          activeFirstPeak = false;
          currentPeakEnd = i;
        }

        // Increments when detect a crossing zero event
        passThreshold = false;
        if (activeFirstPeak || activeSecondPeak) {

          crossZero++;
        }
      }

      // Filter for noise threshold level.
      if (Math.abs(derivativeOfIntensities[i]) > noiseThreshold) {

        passThreshold = true;
      }

      // Start peak region.
      if (crossZero == 0 && derivativeOfIntensities[i] > 0.0 && !activeFirstPeak) {

        activeFirstPeak = true;
        currentPeakStart = i;
        crossZero++;
      }

      // Finalize the peak region in case of zero values.
      if (derivativeOfIntensities[i - 1] == 0.0 && derivativeOfIntensities[i] == 0.0
          && activeFirstPeak) {

        currentPeakEnd = crossZero < 3 ? 0 : i;
        activeFirstPeak = false;
        activeSecondPeak = false;
        crossZero = 0;
      }

      // If the peak starts in a region with no data points, move the
      // start to the first available data point.
      while (currentPeakStart < scanNumbers.size() - 1) {

        if (chromatogram.getDataPointAtIndex(currentPeakStart) == null) {
          currentPeakStart++;
        } else {
          break;
        }
      }

      // Scan the peak from the beginning and if we find a missing data
      // point inside, we have to finish the
      // peak there.
      for (int newEnd = currentPeakStart; newEnd <= currentPeakEnd; newEnd++) {
        if (chromatogram.getDataPointAtIndex(newEnd) == null) {
          currentPeakEnd = newEnd - 1;
          break;
        }
      }

      // If exists a detected area (difference between indexes) create a
      // new resolved peak for this region of
      // the chromatogram.
      if (currentPeakEnd - currentPeakStart > 0 && !activeFirstPeak) {

        resolvedPeaks.add(new ResolvedPeak(chromatogram, currentPeakStart, currentPeakEnd, mzCenterFunction,
            msmsRange, (float) rTRangeMSMS));

        // If exists next overlapped peak, swap the indexes between next
        // and current, and clean ending index
        // for this new current peak.
        if (activeSecondPeak) {

          activeSecondPeak = false;
          activeFirstPeak = true;
          crossZero = derivativeOfIntensities[i] > 0.0 ? 1 : 2;
          currentPeakStart = nextPeakStart;

        } else {

          crossZero = 0;
          currentPeakStart = totalNumberPoints;
        }

        passThreshold = false;
        nextPeakStart = totalNumberPoints;
        currentPeakEnd = 0;
      }
    }

    return resolvedPeaks.toArray(new ResolvedPeak[resolvedPeaks.size()]);
  }

  /**
   * Calculates the value according with the comparative threshold.
   * 
   * @param derivativeIntensities intensity first derivative.
   * @param comparativeThresholdLevel threshold.
   * @return double derivative threshold level.
   */
  private static double calcDerivativeThreshold(final double[] derivativeIntensities,
      final double comparativeThresholdLevel) {

    final int length = derivativeIntensities.length;
    final double[] intensities = new double[length];
    for (int i = 0; i < length; i++) {

      intensities[i] = Math.abs(derivativeIntensities[i]);
    }

    return MathUtils.calcQuantile(intensities, comparativeThresholdLevel);
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SavitzkyGolayFeatureResolverParameters.class;
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
