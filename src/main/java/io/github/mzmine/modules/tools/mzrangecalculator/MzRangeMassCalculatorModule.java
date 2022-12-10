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

package io.github.mzmine.modules.tools.mzrangecalculator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Range;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.ExitCode;

/**
 * m/z range calculator module. Calculates m/z range from a given mass and m/z tolerance.
 */
public class MzRangeMassCalculatorModule implements MZmineModule {

  private static final String MODULE_NAME = "m/z range calculator from formula";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MzRangeMassCalculatorParameters.class;
  }

  /**
   * Shows the calculation dialog and returns the calculated m/z range. May return null in case user
   * clicked Cancel.
   */
  @Nullable
  public static Range<Double> showRangeCalculationDialog() {

    ParameterSet myParameters =
        MZmineCore.getConfiguration().getModuleParameters(MzRangeMassCalculatorModule.class);

    if (myParameters == null)
      return null;

    ExitCode exitCode = myParameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK)
      return null;

    Double mz = myParameters.getParameter(MzRangeMassCalculatorParameters.mz).getValue();
    MZTolerance mzTolerance =
        myParameters.getParameter(MzRangeMassCalculatorParameters.mzTolerance).getValue();

    if ((mz == null) || (mzTolerance == null))
      return null;

    Range<Double> mzRange = mzTolerance.getToleranceRange(mz);

    return mzRange;
  }

}
