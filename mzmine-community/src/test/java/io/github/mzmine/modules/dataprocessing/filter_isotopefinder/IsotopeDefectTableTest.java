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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeDefectTable;
import io.github.mzmine.util.Isotope;
import io.github.mzmine.util.IsotopesUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The defect table decides which off-13C-grid signals the isotope finder may emit, so its
 * multiplicity behaviour is locked here: a polyhalogen comb deviates by a MULTIPLE of the element's
 * defect, and treating only single substitutions as explainable truncated real Cl/Br combs
 * (measured: polyhalogen patternRecall 0.9934 -> 0.9767).
 */
class IsotopeDefectTableTest {

  private static final double C13 = 1.0033548;
  private static final double WINDOW = 0.006;

  /**
   * @return the 37Cl deviation from the exact 13C grid (~-9.7 mDa).
   */
  private static double chlorineDeviation() {
    double best = 0d;
    double bestDistance = Double.MAX_VALUE;
    for (final Isotope iso : IsotopesUtils.getIsotopeRecord("Cl")) {
      final double distance = Math.abs(iso.deltaMass() - 2d);
      if (distance < bestDistance) {
        bestDistance = distance;
        best = iso.deltaMass();
      }
    }
    return best - 2d * C13;
  }

  @Test
  void explainsSingleAndMultipleSubstitutions() {
    final IsotopeDefectTable table = IsotopeDefectTable.build(List.of("Cl"), 8);
    final double dev = chlorineDeviation();

    assertTrue(table.explains(dev, WINDOW, 1), "one 37Cl must be explainable at offset 1");
    assertTrue(table.explains(3 * dev, WINDOW, 3), "a Cl3 comb deviates by 3x the defect");
    assertTrue(table.explains(6 * dev, WINDOW, 8), "a Cl6 comb is still within the cap");
  }

  @Test
  void rejectsMoreSubstitutionsThanTheOffsetCanHold() {
    final IsotopeDefectTable table = IsotopeDefectTable.build(List.of("Cl"), 8);
    final double dev = chlorineDeviation();

    // a signal one offset above the base cannot carry three heavy atoms
    assertFalse(table.explains(3 * dev, WINDOW, 1),
        "a 3x deviation must not be explained when only one substitution fits the offset");
  }

  @Test
  void rejectsDeviationsNoCandidateProduces() {
    final IsotopeDefectTable table = IsotopeDefectTable.build(List.of("Cl"), 8);

    // +25 mDa is on no side of any Cl multiple (they are all negative, ~-9.7 mDa apart)
    assertFalse(table.explains(0.025d, WINDOW, 8), "an unrelated deviation must not be explained");
  }

  @Test
  void includesM1OnlyElements() {
    // 15N shifts the peak ~-6.3 mDa off the grid and has no M+2 isotope, so an M+2-only table would
    // treat every resolved 15N fine-structure signal as unexplained
    final IsotopeDefectTable table = IsotopeDefectTable.build(List.of("N"), 4);
    double n15 = 0d;
    for (final Isotope iso : IsotopesUtils.getIsotopeRecord("N")) {
      if (Math.abs(iso.deltaMass() - 1d) < 0.5d) {
        n15 = iso.deltaMass();
      }
    }
    assertTrue(table.explains(n15 - C13, 0.001d, 1), "15N must be an explainable deviation");
  }
}
