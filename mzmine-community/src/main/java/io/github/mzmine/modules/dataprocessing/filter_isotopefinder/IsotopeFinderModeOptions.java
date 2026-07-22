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

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModelModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.formula.FormulaEnvelopeModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModule;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import org.jetbrains.annotations.NotNull;

/**
 * Selectable isotope pattern detection modes. Each maps to an {@link EnvelopeModelModule} that
 * builds the predicted isotope envelope used to score charges and bound the pattern.
 */
public enum IsotopeFinderModeOptions implements ModuleOptionsEnum<EnvelopeModelModule> {

  /**
   * Carbon-averagine envelope: estimates carbon count from m/z, no formula prediction. Default.
   */
  SIGNAL_BASED,
  /**
   * Formula-prediction envelope: enumerates candidate formulas and unions their predicted
   * patterns.
   */
  FORMULA_PREDICTION;

  @Override
  public Class<? extends EnvelopeModelModule> getModuleClass() {
    return switch (this) {
      case SIGNAL_BASED -> CarbonAveragineEnvelopeModule.class;
      case FORMULA_PREDICTION -> FormulaEnvelopeModule.class;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case SIGNAL_BASED -> "Signal based (carbon-averagine)";
      case FORMULA_PREDICTION -> "Formula prediction";
    };
  }

  @Override
  public String getStableId() {
    // do not change these values for save/load
    return switch (this) {
      case SIGNAL_BASED -> "signal_based";
      case FORMULA_PREDICTION -> "formula_prediction";
    };
  }

  /**
   * @param value selected mode with its embedded parameters.
   * @param ctx   shared top-level configuration.
   * @return the configured envelope model for the selected mode.
   */
  public static @NotNull EnvelopeModel createModel(
      @NotNull final ValueWithParameters<IsotopeFinderModeOptions> value,
      @NotNull final EnvelopeContext ctx) {
    return value.value().getModuleInstance().createModel(value.parameters(), ctx);
  }
}
