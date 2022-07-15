/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
    if (spectrum.getSpectrumType() != MassSpectrumType.CENTROIDED) {
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
