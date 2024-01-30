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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest.FactorOfLowestMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.localmaxima.LocalMaxMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.recursive.RecursiveMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.wavelet.WaveletMassDetector;
import io.github.mzmine.parameters.ParameterSet;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public enum MassDetectors {
  AUTO(AutoMassDetector.class), CENTROID(CentroidMassDetector.class), EXACT(
      ExactMassDetector.class), FACTOR_OF_LOWEST(FactorOfLowestMassDetector.class), LOCAL_MAX(
      LocalMaxMassDetector.class), RECURSIVE(RecursiveMassDetector.class), WAVELET(
      WaveletMassDetector.class);

  /**
   * This instance is never used for mass detection but contains the name and factory logic
   */
  private final MassDetector massDetector;

  MassDetectors(final Class<? extends MassDetector> massDetectorClass) {
    this.massDetector = MZmineCore.getModuleInstance(massDetectorClass);
  }

  /**
   * List of modules that contain the name and parameter class. Used by the parameters. use
   * {@link MassDetector#create(ParameterSet)} to create the instance for processing
   */
  @NotNull
  public static List<MassDetector> listModules() {
    return Arrays.stream(values()).map(MassDetectors::getDefaultModule).toList();
  }

  /**
   * Without AUTO mass detector as this only works when spectrum type is known. List of modules that
   * contain the name and parameter class. Used by the parameters. use
   * {@link MassDetector#create(ParameterSet)} to create the instance for processing
   */
  @NotNull
  public static List<MassDetector> listModulesNoAuto() {
    return Arrays.stream(values()).filter(md -> md != AUTO).map(MassDetectors::getDefaultModule)
        .toList();
  }

  @NotNull
  public ParameterSet getParametersCopy() {
    return MZmineCore.getConfiguration().getModuleParameters(massDetector.getClass())
        .cloneParameterSet();
  }

  /**
   * Derive a mass detector with the following parameters
   */
  @NotNull
  public MassDetector createMassDetector(@NotNull ParameterSet parameters) {
    return massDetector.create(parameters);
  }

  @NotNull
  public MassDetector getDefaultModule() {
    return massDetector;
  }

  @Override
  public String toString() {
    return massDetector.getName();
  }
}
