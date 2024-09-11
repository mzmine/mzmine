/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;

class FormulaUtilsTest {

  @Test
  void getMonoisotopicMass() {
    assertEquals(11.99945142009073, FormulaUtils.getMonoisotopicMass(formula("C+")), 0.000001);
    assertEquals(12, FormulaUtils.getMonoisotopicMass(formula("C")), 0.000001);
    assertEquals(93.99077174390926, FormulaUtils.getMonoisotopicMass(formula("CH2O5-")), 0.000001);
  }

  private static @Nullable IMolecularFormula formula(final String formula) {
    return FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula);
  }

  @Test
  void getFormulaString() {
    // without charge
    assertEquals("C", FormulaUtils.getFormulaString(formula("C"), false));
    assertEquals("CHO2", FormulaUtils.getFormulaString(formula("HCO2+"), false));
    assertEquals("C", FormulaUtils.getFormulaString(formula("C-2"), false));
    // with charge
    assertEquals("C", FormulaUtils.getFormulaString(formula("C"), true));
    assertEquals("[CHO2]+", FormulaUtils.getFormulaString(formula("HCO2+"), true));
    assertEquals("[C]2-", FormulaUtils.getFormulaString(formula("C-2"), true));
  }

  @Test
  void parseFormulaString() {
    parseFormula("D2O", true);
    parseFormula("TEST", true); //
    parseFormula("GGG", true); //
    parseFormula("H2o", true);
    parseFormula("H2O", false);
  }

  private static IMolecularFormula parseFormula(final String formula, boolean resultNull) {
    var f = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula);
    if (resultNull) {
      assertNull(f, () -> "Parsed formula for %s is %s".formatted(formula,
          FormulaUtils.getFormulaString(f, false)));
    } else {
      assertNotNull(f, "Formula for %s is null".formatted(formula));
    }
    return f;
  }
}