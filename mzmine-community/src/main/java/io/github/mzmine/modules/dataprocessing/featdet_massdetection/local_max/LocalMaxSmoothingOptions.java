/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorPreprocessor;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorPreprocessorModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optional preprocessing (e.g. smoothing) applied to the consecutive ranges detected by the
 * {@link LocalMaxMassDetector}. Used through a
 * {@link io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter}.
 * Add a new option here together with a {@link MassDetectorPreprocessorModule} to extend the
 * available preprocessing methods.
 */
public enum LocalMaxSmoothingOptions implements ModuleOptionsEnum<MassDetectorPreprocessorModule> {

  NONE, SAVITZKY_GOLAY;

  /**
   * Create the configured preprocessor for this option.
   *
   * @param params the embedded parameters of the selected option (may be null for options without
   *               parameters).
   */
  public @NotNull MassDetectorPreprocessor createPreprocessor(@Nullable final ParameterSet params) {
    return getModuleInstance().createPreprocessor(params);
  }

  @Override
  public Class<? extends MassDetectorPreprocessorModule> getModuleClass() {
    return switch (this) {
      case NONE -> LocalMaxNoSmoothingModule.class;
      case SAVITZKY_GOLAY -> LocalMaxSavitzkyGolayModule.class;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case NONE -> "None";
      case SAVITZKY_GOLAY -> "Savitzky-Golay";
    };
  }

  @Override
  public String getStableId() {
    // do not change these values for load/save
    return switch (this) {
      case NONE -> "none";
      case SAVITZKY_GOLAY -> "savitzky_golay";
    };
  }
}
