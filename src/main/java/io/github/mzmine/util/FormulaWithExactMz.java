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

package io.github.mzmine.util;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Used to cache the formula + mz results
 *
 * @param mz exact mz
 */
public record FormulaWithExactMz(IMolecularFormula formula, double mz) {

  @Override
  public String toString() {
    return MolecularFormulaManipulator.getString(formula) + ", mz=" + mz;
  }

  public int getCharge() {
    return Objects.requireNonNullElse(formula.getCharge(), 0);
  }

  /**
   * Charge string as
   *
   * @return +1
   */
  @NotNull
  public String getChargeString() {
    int charge = getCharge();
    String chargeStr = charge > 1 ? "" + charge : "";
    if (charge > 0) {
      return "+" + chargeStr;
    }
    if (charge < 0) {
      return "-" + chargeStr;
    } else {
      return "";
    }
  }
}
