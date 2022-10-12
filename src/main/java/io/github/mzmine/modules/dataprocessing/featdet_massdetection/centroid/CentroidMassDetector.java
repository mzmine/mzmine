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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid;

import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.DetectIsotopesParameter;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IsotopesUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 * Remove peaks below the given noise level.
 */
public class CentroidMassDetector implements MassDetector {

  // Variables for the detection of isotopes below the noise level
  private List<Element> isotopeElements;
  private int isotopeMaxCharge;
  // Possible m/z differences between isotopes
  private List<Double> isotopesMzDiffs;
  // Used to optimize getMassValues
  private double maxIsotopeMzDiff;

  @Override
  public double[][] getMassValues(MassSpectrum spectrum, ParameterSet parameters) {

    final double noiseLevel = parameters.getParameter(CentroidMassDetectorParameters.noiseLevel)
        .getValue();

    boolean detectIsotopes = parameters.getParameter(CentroidMassDetectorParameters.detectIsotopes)
        .getValue();

    // If isotopes are going to be detected get all the required parameters
    MZTolerance isotopesMzTolerance = null;
    if (detectIsotopes) {
      ParameterSet isotopesParameters = parameters.getParameter(
          CentroidMassDetectorParameters.detectIsotopes).getEmbeddedParameters();
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
    // use number of centroid signals as base array list capacity
    final int points = spectrum.getNumberOfDataPoints();
    // lists of primitive doubles
    DoubleArrayList mzs = new DoubleArrayList(points);
    DoubleArrayList intensities = new DoubleArrayList(points);

    // Find possible mzPeaks
    for (int i = 0; i < points; i++) {
      // Is intensity above the noise level or m/z value corresponds to isotope mass?
      double intensity = spectrum.getIntensityValue(i);
      double mz = spectrum.getMzValue(i);
      if (intensity >= noiseLevel || (detectIsotopes
          // If the difference between current m/z and last detected m/z is greater than maximum
          // possible isotope m/z difference do not call isPossibleIsotopeMz
          && (mzs.isEmpty()
          || Doubles.compare(mz - mzs.getDouble(mzs.size() - 1), maxIsotopeMzDiff) <= 0)
          && IsotopesUtils.isPossibleIsotopeMz(mz, mzs, isotopesMzDiffs, isotopesMzTolerance))) {
        // Yes, then mark this index as mzPeak
        mzs.add(mz);
        intensities.add(intensity);
      }
    }
    return new double[][]{mzs.toDoubleArray(), intensities.toDoubleArray()};
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities, ParameterSet parameters) {
    assert mzs.length == intensities.length;

    final double noiseLevel = parameters.getParameter(CentroidMassDetectorParameters.noiseLevel)
        .getValue();
    return getMassValues(mzs, intensities, noiseLevel);
  }

  public double[][] getMassValues(double[] mzs, double[] intensities, double noiseLevel) {
    assert mzs.length == intensities.length;

    // use number of centroid signals as base array list capacity
    final int points = mzs.length;
    // lists of primitive doubles
    DoubleArrayList pickedMZs = new DoubleArrayList(points);
    DoubleArrayList pickedIntensities = new DoubleArrayList(points);

    // Find possible mzPeaks
    for (int i = 0; i < points; i++) {
      // Is intensity above the noise level?
      if (intensities[i] >= noiseLevel) {
        // Yes, then mark this index as mzPeak
        pickedMZs.add(mzs[i]);
        pickedIntensities.add(intensities[i]);
      }
    }
    return new double[][]{pickedMZs.toDoubleArray(), pickedIntensities.toDoubleArray()};
  }

  @Override
  public @NotNull String getName() {
    return "Centroid";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return CentroidMassDetectorParameters.class;
  }

}
