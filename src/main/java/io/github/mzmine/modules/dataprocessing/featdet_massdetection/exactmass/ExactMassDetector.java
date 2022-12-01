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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass;

import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.DetectIsotopesParameter;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IsotopesUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

public class ExactMassDetector implements MassDetector {

  // Variables for the detection of isotopes below the noise level
  private List<Element> isotopeElements;
  private int isotopeMaxCharge;
  // Possible m/z differences between isotopes
  private List<Double> isotopesMzDiffs;
  // Used to optimize getMassValues
  private double maxIsotopeMzDiff;

  @NotNull
  public static double[][] getMassValues(MassSpectrum spectrum, double noiseLevel) {
    return getMassValues(spectrum, noiseLevel, false, null, null, 0d);
  }

  @NotNull
  public static double[][] getMassValues(MassSpectrum spectrum, double noiseLevel,
      boolean detectIsotopes, MZTolerance isotopesMzTolerance, List<Double> isotopesMzDiffs,
      double maxIsotopeMzDiff) {
    // lists of primitive doubles
    DoubleArrayList mzs = new DoubleArrayList(128);
    DoubleArrayList intensities = new DoubleArrayList(128);

    // First get all candidate peaks (local maximum)
    int localMaximumIndex = 0;
    ArrayList<Integer> rangeDataPoints = new ArrayList<>();

    boolean ascending = true;

    // Iterate through all data points
    for (int i = 0; i < spectrum.getNumberOfDataPoints() - 1; i++) {
      double intensity = spectrum.getIntensityValue(i);
      double nextIntensity = spectrum.getIntensityValue(i + 1);

      boolean nextIsBigger = nextIntensity > intensity;
      boolean nextIsZero = Double.compare(nextIntensity, 0d) == 0;
      boolean currentIsZero = Double.compare(intensity, 0d) == 0;

      // Ignore zero intensity regions
      if (currentIsZero) {
        continue;
      }

      // Add current (non-zero) data point to the current m/z peak
      rangeDataPoints.add(i);

      // Check for local maximum
      if (ascending && (!nextIsBigger)) {
        localMaximumIndex = i;
        ascending = false;
        continue;
      }

      // Check for the end of the peak
      if ((!ascending) && (nextIsBigger || nextIsZero)) {

        // Calculate the exact mass
        double exactMz = calculateExactMass(spectrum, localMaximumIndex, rangeDataPoints);

        // Add the m/z peak if it is above the noise level or m/z value corresponds to isotope mass
        if (spectrum.getIntensityValue(localMaximumIndex) > noiseLevel || //
            (detectIsotopes
                // If the difference between current m/z and last detected m/z is greater than maximum
                // possible isotope m/z difference do not call isPossibleIsotopeMz
                && (mzs.isEmpty()
                || Doubles.compare(exactMz - mzs.getDouble(mzs.size() - 1), maxIsotopeMzDiff) <= 0)
                && IsotopesUtils.isPossibleIsotopeMz(exactMz, mzs, isotopesMzDiffs,
                isotopesMzTolerance))) {

          // Add data point to lists
          mzs.add(exactMz);
          intensities.add(spectrum.getIntensityValue(localMaximumIndex));
        }

        // Reset and start with new peak
        ascending = true;
        rangeDataPoints.clear();
      }
    }

    // Return an array of detected MzPeaks sorted by MZ
    return new double[][]{mzs.toDoubleArray(), intensities.toDoubleArray()};
  }

  /**
   * This method calculates the exact mass of a peak using the FWHM concept and linear equation (y =
   * mx + b).
   *
   * @return double
   */
  private static double calculateExactMass(MassSpectrum spectrum, int topIndex,
      List<Integer> rangeDataPoints) {

    /*
     * According with the FWHM concept, the exact mass of this peak is the half point of FWHM. In
     * order to get the points in the curve that define the FWHM, we use the linear equation.
     *
     * First we look for, in left side of the peak, 2 data points together that have an intensity
     * less (first data point) and bigger (second data point) than half of total intensity. Then we
     * calculate the slope of the line defined by this two data points. At least, we calculate the
     * point in this line that has an intensity equal to the half of total intensity
     *
     * We repeat the same process in the right side.
     */

    double xRight = -1, xLeft = -1;
    double halfIntensity = spectrum.getIntensityValue(topIndex) / 2;

    for (int i = 0; i < rangeDataPoints.size() - 1; i++) {

      // Left side of the curve
      if ((spectrum.getIntensityValue(rangeDataPoints.get(i)) <= halfIntensity) && (
          spectrum.getMzValue(rangeDataPoints.get(i)) < spectrum.getMzValue(topIndex)) && (
          spectrum.getIntensityValue(rangeDataPoints.get(i + 1)) >= halfIntensity)) {

        // First point with intensity just less than half of total
        // intensity
        double leftY1 = spectrum.getIntensityValue(rangeDataPoints.get(i));
        double leftX1 = spectrum.getMzValue(rangeDataPoints.get(i));

        // Second point with intensity just bigger than half of total
        // intensity
        double leftY2 = spectrum.getIntensityValue(rangeDataPoints.get(i + 1));
        double leftX2 = spectrum.getMzValue(rangeDataPoints.get(i + 1));

        // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
        double mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);

        if (mLeft == 0.0) {
          // If slope is zero, we calculate the desired point as the
          // middle point
          xLeft = (leftX1 + leftX2) / 2;
        } else {
          // We calculate the desired point (at half intensity) with
          // the linear equation
          // X = X1 + [(Y - Y1) / m ]
          // where Y = half of total intensity
          xLeft = leftX1 + (((halfIntensity) - leftY1) / mLeft);
        }
        continue;
      }

      // Right side of the curve
      if ((spectrum.getIntensityValue(rangeDataPoints.get(i)) >= halfIntensity) && (
          spectrum.getMzValue(rangeDataPoints.get(i)) > spectrum.getMzValue(topIndex)) && (
          spectrum.getIntensityValue(rangeDataPoints.get(i + 1)) <= halfIntensity)) {

        // First point with intensity just bigger than half of total
        // intensity
        double rightY1 = spectrum.getIntensityValue(rangeDataPoints.get(i));
        double rightX1 = spectrum.getMzValue(rangeDataPoints.get(i));

        // Second point with intensity just less than half of total
        // intensity
        double rightY2 = spectrum.getIntensityValue(rangeDataPoints.get(i + 1));
        double rightX2 = spectrum.getMzValue(rangeDataPoints.get(i + 1));

        // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
        double mRight = (rightY1 - rightY2) / (rightX1 - rightX2);

        if (mRight == 0.0) {
          // If slope is zero, we calculate the desired point as the
          // middle point
          xRight = (rightX1 + rightX2) / 2;
        } else {
          // We calculate the desired point (at half intensity) with
          // the
          // linear equation
          // X = X1 + [(Y - Y1) / m ], where Y = half of total
          // intensity
          xRight = rightX1 + (((halfIntensity) - rightY1) / mRight);
        }
        break;
      }
    }

    // We verify the values to confirm we find the desired points. If not we
    // return the same mass value.
    if ((xRight == -1) || (xLeft == -1)) {
      return spectrum.getMzValue(topIndex);
    }

    // The center of left and right points is the exact mass of our peak.
    double exactMass = (xLeft + xRight) / 2;

    return exactMass;
  }

  @Override
  public double[][] getMassValues(MassSpectrum spectrum, ParameterSet parameters) {
    if (spectrum.getNumberOfDataPoints() == 0) {
      return EMPTY_DATA;
    }

    double noiseLevel = parameters.getParameter(ExactMassDetectorParameters.noiseLevel).getValue();
    boolean detectIsotopes = parameters.getParameter(ExactMassDetectorParameters.detectIsotopes)
        .getValue();

    // If isotopes are going to be detected get all the required parameters
    MZTolerance isotopesMzTolerance = null;
    if (detectIsotopes) {
      ParameterSet isotopesParameters = parameters.getParameter(
          ExactMassDetectorParameters.detectIsotopes).getEmbeddedParameters();
      List<Element> isotopeElements = isotopesParameters.getParameter(
          DetectIsotopesParameter.elements).getValue();
      int isotopeMaxCharge = isotopesParameters.getParameter(DetectIsotopesParameter.maxCharge)
          .getValue();
      isotopesMzTolerance = isotopesParameters.getParameter(
          DetectIsotopesParameter.isotopeMzTolerance).getValue();

      // Update isotopesMzDiffs only if isotopeElements and isotopeMaxCharge differ from the last call
      if (!Objects.equals(this.isotopeElements, isotopeElements) || !Objects.equals(
          this.isotopeMaxCharge, isotopeMaxCharge)) {

        // Update isotopesMzDiffs
        this.isotopesMzDiffs = IsotopesUtils.getIsotopesMzDiffs(isotopeElements, isotopeMaxCharge);
        this.maxIsotopeMzDiff = Collections.max(isotopesMzDiffs);

        // Store last called parameters
        this.isotopeElements = isotopeElements;
        this.isotopeMaxCharge = isotopeMaxCharge;
      }
    }

    return getMassValues(spectrum, noiseLevel, detectIsotopes, isotopesMzTolerance, isotopesMzDiffs,
        maxIsotopeMzDiff);
  }

  @Override
  public @NotNull String getName() {
    return "Exact mass";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ExactMassDetectorParameters.class;
  }

}
