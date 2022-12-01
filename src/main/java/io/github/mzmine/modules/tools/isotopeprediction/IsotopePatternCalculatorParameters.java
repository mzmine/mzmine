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

package io.github.mzmine.modules.tools.isotopeprediction;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.FormulaParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import javafx.collections.FXCollections;

public class IsotopePatternCalculatorParameters extends SimpleParameterSet {

  public static final FormulaParameter formula = new FormulaParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final IntegerParameter charge =
      new IntegerParameter("Charge", "Charge of the molecule (z for calculating m/z values)", 1);

  public static final ComboParameter<PolarityType> polarity = new ComboParameter<PolarityType>(
      "Polarity",
      "Set positive or negative charge of the molecule. Depending on polarity, electron mass is added or removed.",
      FXCollections.observableArrayList(PolarityType.values()));

  public static final PercentParameter minAbundance = new PercentParameter("Minimum abundance",
      "Minimum abundance of the predicted isotopes", 0.001);

  public IsotopePatternCalculatorParameters() {
    super(new UserParameter[] {formula, charge, polarity, minAbundance});
  }

}
