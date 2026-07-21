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

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.util.Isotope;
import io.github.mzmine.util.IsotopesUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProbeIsotopes {

  @Test
  void printRecords() {
    for (final String s : List.of("Cl", "Br", "S", "Si", "Se", "Fe")) {
      final List<Isotope> rec = IsotopesUtils.getIsotopeRecord(s);
      System.out.println("=== " + s + " ===");
      for (final Isotope i : rec) {
        System.out.printf("  A=%d delta=%.6f rel=%.5f%n", i.atomNumber(), i.deltaMass(),
            i.relativeIntensity());
      }
    }
  }

  @Test
  void printPatterns() {
    for (final String f : List.of("C20H30Cl4", "C20H30Br2", "C30H50N4O6S2", "C8H24O4Si4",
        "C30H50N4O6", "C20H28Cl2S2")) {
      final IsotopePattern p = IsotopePatternCalculator.calculateIsotopePattern(f, 0.001, 0.00005,
          1, PolarityType.POSITIVE, false);
      System.out.println("=== " + f + " n=" + p.getNumberOfDataPoints() + " ===");
      double mono = Double.POSITIVE_INFINITY;
      double base = 0;
      for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
        mono = Math.min(mono, p.getMzValue(i));
        base = Math.max(base, p.getIntensityValue(i));
      }
      for (int i = 0; i < p.getNumberOfDataPoints(); i++) {
        System.out.printf("  mz=%.5f d=%.5f relBase=%.4f%n", p.getMzValue(i),
            p.getMzValue(i) - mono, p.getIntensityValue(i) / base);
      }
    }
  }
}
