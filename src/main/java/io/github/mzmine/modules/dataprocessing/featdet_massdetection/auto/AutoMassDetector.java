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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.DetectIsotopesParameter;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetectorParameters;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoMassDetector implements MassDetector {

  private final CentroidMassDetector centroidDetector = new CentroidMassDetector();
  private final ExactMassDetector exactMassDetector = new ExactMassDetector();

  private ExactMassDetectorParameters exactMassDetectorParameters;
  private CentroidMassDetectorParameters centroidMassDetectorParameters;

  @Override
  public @NotNull String getName() {
    return "Auto";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AutoMassDetectorParameters.class;
  }

  @Override
  public double[][] getMassValues(MassSpectrum spectrum, ParameterSet parameters) {
    if (spectrum.getSpectrumType() == MassSpectrumType.PROFILE) {
      return exactMassDetector.getMassValues(spectrum, getExactParam(parameters));
    } else {
      return centroidDetector.getMassValues(spectrum, getCentroidParam(parameters));
    }
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities, ParameterSet parameters) {
    return MassDetector.super.getMassValues(mzs, intensities, parameters);
  }

  private ParameterSet getExactParam(ParameterSet autoParam) {
    exactMassDetectorParameters = (ExactMassDetectorParameters) (new ExactMassDetectorParameters())
        .cloneParameterSet();
    exactMassDetectorParameters.getParameter(ExactMassDetectorParameters.noiseLevel)
        .setValue(autoParam.getParameter(AutoMassDetectorParameters.noiseLevel).getValue());
    exactMassDetectorParameters.setParameter(ExactMassDetectorParameters.detectIsotopes,
        autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getValue());

    DetectIsotopesParameter detectIsotopesParameter = exactMassDetectorParameters
        .getParameter(ExactMassDetectorParameters.detectIsotopes)
        .getEmbeddedParameters();

    detectIsotopesParameter.getParameter(DetectIsotopesParameter.elements).setValue(
        autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters()
            .getParameter(DetectIsotopesParameter.elements).getValue());
    detectIsotopesParameter.getParameter(DetectIsotopesParameter.isotopeMzTolerance).setValue(
        autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters()
            .getParameter(DetectIsotopesParameter.isotopeMzTolerance).getValue());
    detectIsotopesParameter.getParameter(DetectIsotopesParameter.maxCharge).setValue(
        autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters()
            .getParameter(DetectIsotopesParameter.maxCharge).getValue());

    return exactMassDetectorParameters;
  }

  private ParameterSet getCentroidParam(ParameterSet autoParam) {
    centroidMassDetectorParameters = new CentroidMassDetectorParameters();
    centroidMassDetectorParameters.getParameter(CentroidMassDetectorParameters.noiseLevel)
        .setValue(autoParam.getParameter(AutoMassDetectorParameters.noiseLevel).getValue());
    centroidMassDetectorParameters
        .setParameter(CentroidMassDetectorParameters.detectIsotopes,
            autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getValue());

    DetectIsotopesParameter detectIsotopesParameter = centroidMassDetectorParameters
        .getParameter(CentroidMassDetectorParameters.detectIsotopes)
        .getEmbeddedParameters();
    detectIsotopesParameter.getParameter(DetectIsotopesParameter.elements).setValue(
        autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters()
            .getParameter(DetectIsotopesParameter.elements).getValue());
    detectIsotopesParameter.getParameter(DetectIsotopesParameter.isotopeMzTolerance).setValue(
        autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters()
            .getParameter(DetectIsotopesParameter.isotopeMzTolerance).getValue());
    detectIsotopesParameter.getParameter(DetectIsotopesParameter.maxCharge).setValue(
        autoParam.getParameter(AutoMassDetectorParameters.detectIsotopes).getEmbeddedParameters()
            .getParameter(DetectIsotopesParameter.maxCharge).getValue());

    return centroidMassDetectorParameters;
  }
}
