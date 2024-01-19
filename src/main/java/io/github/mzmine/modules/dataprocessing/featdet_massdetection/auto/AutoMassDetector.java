/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorUtils;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassesIsotopeDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoMassDetector implements MassDetector {

  private final CentroidMassDetector centroidDetector;
  private final ExactMassDetector exactMassDetector;
  private final boolean filtersActive;

  public AutoMassDetector(final double noiseLevel, final MassesIsotopeDetector isotopeDetector) {
    centroidDetector = new CentroidMassDetector(noiseLevel, isotopeDetector);
    exactMassDetector = new ExactMassDetector(noiseLevel, isotopeDetector);
    filtersActive = noiseLevel > 0;
  }

  @Override
  public MassDetector create(final ParameterSet params) {
    double noiseLevel = params.getValue(AutoMassDetectorParameters.noiseLevel);
    var isotopeDetector = MassDetectorUtils.createIsotopeDetector(
        params.getParameter(AutoMassDetectorParameters.detectIsotopes));
    return new AutoMassDetector(noiseLevel, isotopeDetector);
  }

  @Override
  public boolean filtersActive() {
    return filtersActive;
  }

  @Override
  public @NotNull String getName() {
    return "Auto";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AutoMassDetectorParameters.class;
  }


  @Override
  public double[][] getMassValues(MassSpectrum spectrum) {
    if (spectrum.getSpectrumType() == MassSpectrumType.PROFILE) {
      return exactMassDetector.getMassValues(spectrum);
    } else {
      return centroidDetector.getMassValues(spectrum);
    }
  }

  @Override
  public double[][] getMassValues(double[] mzs, double[] intensities) {
    return MassDetector.super.getMassValues(mzs, intensities);
  }

}
