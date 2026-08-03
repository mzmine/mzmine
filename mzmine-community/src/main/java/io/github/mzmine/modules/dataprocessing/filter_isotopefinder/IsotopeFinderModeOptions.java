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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import org.jetbrains.annotations.NotNull;

/**
 * Selectable isotope finder algorithms. Each maps to an {@link IsotopeFinderAlgorithmModule} that
 * carries the full setup of the detection run as its embedded parameters, so the top-level
 * {@link IsotopeFinderParameters} only has the feature lists and the algorithm choice.
 * <p>
 * The formula-prediction algorithm is not exposed yet: the classes in the {@code formula} package are
 * kept for it, but no option maps to them, so it is hidden from the GUI and from saved batches.
 */
public enum IsotopeFinderModeOptions implements ModuleOptionsEnum<IsotopeFinderAlgorithmModule> {

  /**
   * Simplified setup: m/z tolerance, 13C requirement, and maximum charge only. Default.
   */
  AUTOMATIC,
  /**
   * Full carbon-averagine setup: estimates the carbon count from m/z, no formula prediction.
   */
  CARBON_AVERAGINE;

  @Override
  public Class<? extends IsotopeFinderAlgorithmModule> getModuleClass() {
    return switch (this) {
      case AUTOMATIC -> AutomaticIsotopeFinderModule.class;
      case CARBON_AVERAGINE -> CarbonAveragineAlgorithmModule.class;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case AUTOMATIC -> "Automatic";
      case CARBON_AVERAGINE -> "Carbon-averagine";
    };
  }

  @Override
  public String getStableId() {
    // do not change these values for save/load
    return switch (this) {
      case AUTOMATIC -> "automatic";
      // kept from the former "Signal based (carbon-averagine)" mode so a saved selection still resolves
      case CARBON_AVERAGINE -> "signal_based";
    };
  }

  /**
   * @param value selected algorithm with its embedded parameters.
   * @return the full carbon-averagine setup the selected algorithm resolves to.
   */
  public static @NotNull CarbonAveragineAlgorithmParameters resolve(
      @NotNull final ValueWithParameters<IsotopeFinderModeOptions> value) {
    return value.value().getModuleInstance().resolve(value.parameters());
  }
}
