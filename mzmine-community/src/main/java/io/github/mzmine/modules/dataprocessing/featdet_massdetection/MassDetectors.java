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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.localmaxima.LocalMaxMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.recursive.RecursiveMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.wavelet.WaveletMassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public enum MassDetectors implements ModuleOptionsEnum<MassDetector> {
  /**
   * Handles Profile and Centroid mode data. Uses {@link ExactMassDetector} first with 0 noise level
   * on profile data
   */
  FACTOR_OF_LOWEST,
  /**
   * Handles both profile and centroid mode data. Uses {@link ExactMassDetector} or
   * {@link CentroidMassDetector} for profile and centroid mode data
   */
  AUTO,
  /**
   * Only on centroided data
   */
  CENTROID,
  /**
   * Only on Profile mode data
   */
  EXACT,
  /**
   * Only on profile mode data
   */
  LOCAL_MAX,
  /**
   *
   */
  RECURSIVE,
  /**
   *
   */
  WAVELET;

  @Override
  public Class<? extends MassDetector> getModuleClass() {
    return switch (this) {
      case FACTOR_OF_LOWEST -> FactorOfLowestMassDetector.class;
      case AUTO -> AutoMassDetector.class;
      case CENTROID -> CentroidMassDetector.class;
      case EXACT -> ExactMassDetector.class;
      case LOCAL_MAX -> LocalMaxMassDetector.class;
      case RECURSIVE -> RecursiveMassDetector.class;
      case WAVELET -> WaveletMassDetector.class;
    };
  }

  @Override
  public String toString() {
    return getStableId();
  }

  @Override
  public String getStableId() {
    // do not change these values for load save
    return switch (this) {
      case FACTOR_OF_LOWEST -> "Factor of lowest signal";
      case AUTO -> "Auto";
      case CENTROID -> "Centroid";
      case EXACT -> "Exact mass";
      case LOCAL_MAX -> "Local maxima";
      case RECURSIVE -> "Recursive threshold";
      case WAVELET -> "Wavelet transform";
    };
  }

  public boolean usesCentroidData() {
    return switch (this) {
      case CENTROID, FACTOR_OF_LOWEST, AUTO -> true;
      case EXACT, LOCAL_MAX, RECURSIVE, WAVELET -> false;
    };
  }

  public boolean usesProfileData() {
    return switch (this) {
      case CENTROID -> false;
      case EXACT, LOCAL_MAX, RECURSIVE, WAVELET, FACTOR_OF_LOWEST, AUTO -> true;
    };
  }

  /**
   * List of modules that contain the name and parameter class. Used by the parameters. use
   * {@link MassDetector#create(ParameterSet)} to create the instance for processing
   */
  @NotNull
  public static List<MassDetector> listModules() {
    return Arrays.stream(values()).map(MassDetectors::getModuleInstance).toList();
  }

  /**
   * Derive a mass detector with the following parameters
   *
   * @param parameters the parameter set of the sub parameters for this detector
   */
  @NotNull
  public MassDetector createMassDetector(@NotNull ParameterSet parameters) {
    return getModuleInstance().create(parameters);
  }

  /**
   * Derive a mass detector with the following parameters
   *
   * @param params selected detector and the sub parameters of this detector
   */
  @NotNull
  public static MassDetector createMassDetector(ValueWithParameters<MassDetectors> params) {
    return params.value().createMassDetector(params.parameters());
  }
}
