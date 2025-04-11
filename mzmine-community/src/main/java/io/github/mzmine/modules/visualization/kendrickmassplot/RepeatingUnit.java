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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.FormulaWithExactMz;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * @param massError error from exact mass to measured mass
 */
public record RepeatingUnit(FormulaWithExactMz formula, double massError) {

  /**
   * @param measuredMass used to calculate the mass error
   */
  public static RepeatingUnit create(final IMolecularFormula formula, final double measuredMass) {
    var exactFormula = new FormulaWithExactMz(formula);
    return new RepeatingUnit(exactFormula, measuredMass - exactFormula.mz());
  }

  @Override
  public String toString() {
    var format = ConfigService.getGuiFormats();
    return formula.formulaString() + ", " + format.mz(formula.mz()) + " Da (Δ" + format.mz(
        massError) + " Da)";
  }
}
