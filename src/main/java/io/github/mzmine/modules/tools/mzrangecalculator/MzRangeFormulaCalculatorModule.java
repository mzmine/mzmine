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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FormulaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * m/z range calculator module. Calculates m/z range from a given chemical formula and m/z
 * tolerance.
 */
public class MzRangeFormulaCalculatorModule implements MZmineModule {

  private static final String MODULE_NAME = "m/z range calculator from formula";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MzRangeFormulaCalculatorParameters.class;
  }

  /**
   * Shows the calculation dialog and returns the calculated m/z range. May return null in case user
   * clicked Cancel.
   */
  @Nullable
  public static Range<Double> showRangeCalculationDialog() {

    ParameterSet myParameters = MZmineCore.getConfiguration()
        .getModuleParameters(MzRangeFormulaCalculatorModule.class);

    if (myParameters == null) {
      return null;
    }

    ExitCode exitCode = myParameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return null;
    }

    return getMzRangeFromFormula(myParameters);
  }

  @Nullable
  public static Range<Double> getMzRangeFromFormula(ParameterSet myParameters) {
    String formula = myParameters.getParameter(MzRangeFormulaCalculatorParameters.formula)
        .getValue().trim();
    IonizationType ionType = myParameters.getParameter(MzRangeFormulaCalculatorParameters.ionType)
        .getValue();
    MZTolerance mzTolerance = myParameters.getParameter(
        MzRangeFormulaCalculatorParameters.mzTolerance).getValue();

    return getMzRangeFromFormula(formula, ionType, mzTolerance);
  }

  @Nullable
  public static Range<Double> getMzRangeFromFormula(String formula, IonizationType ionType,
      MZTolerance mzTolerance) {
    if ((formula == null) || (ionType == null) || (mzTolerance == null)) {
      return null;
    }

    final IMolecularFormula iMolecularFormula = ionType.ionizeFormula(formula);
    final double ionizedMass = FormulaUtils.calculateMzRatio(iMolecularFormula);

    return mzTolerance.getToleranceRange(ionizedMass);
  }

}
