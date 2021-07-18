package io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
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
    exactMassDetectorParameters = new ExactMassDetectorParameters();
    exactMassDetectorParameters.getParameter(ExactMassDetectorParameters.noiseLevel)
        .setValue(autoParam.getParameter(AutoMassDetectorParameters.noiseLevel).getValue());
    return exactMassDetectorParameters;
  }

  private ParameterSet getCentroidParam(ParameterSet autoParam) {
    centroidMassDetectorParameters = new CentroidMassDetectorParameters();
    centroidMassDetectorParameters.getParameter(CentroidMassDetectorParameters.noiseLevel)
        .setValue(autoParam.getParameter(AutoMassDetectorParameters.noiseLevel).getValue());
    return centroidMassDetectorParameters;
  }
}
