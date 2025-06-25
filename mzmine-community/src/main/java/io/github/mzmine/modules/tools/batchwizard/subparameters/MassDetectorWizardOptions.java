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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetectorParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;

public enum MassDetectorWizardOptions {
  ABSOLUTE_NOISE_LEVEL, FACTOR_OF_LOWEST_SIGNAL;

  @Override
  public String toString() {
    return switch (this) {
      case ABSOLUTE_NOISE_LEVEL -> "Absolute intensity";
      case FACTOR_OF_LOWEST_SIGNAL -> "Factor of lowest signal";
    };
  }

  public ValueWithParameters<MassDetectors> createMassDetectorParameters(double noise) {
    return switch (this) {
      case ABSOLUTE_NOISE_LEVEL -> createAutoMassDetector(noise);
      case FACTOR_OF_LOWEST_SIGNAL -> createFactorOfLowestMassDetector(noise);
    };
  }

  private ValueWithParameters<MassDetectors> createAutoMassDetector(double noise) {
    ParameterSet param = MassDetectors.AUTO.getModuleParameters().cloneParameterSet();
    param.setParameter(AutoMassDetectorParameters.noiseLevel, noise);
    return new ValueWithParameters<>(MassDetectors.AUTO, param);
  }

  private ValueWithParameters<MassDetectors> createFactorOfLowestMassDetector(double noise) {
    ParameterSet param = MassDetectors.FACTOR_OF_LOWEST.getModuleParameters();
    param.setParameter(FactorOfLowestMassDetectorParameters.noiseFactor, noise);
    return new ValueWithParameters<>(MassDetectors.FACTOR_OF_LOWEST, param);
  }
}
