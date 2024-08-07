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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe;

import com.google.common.collect.Range;
import java.util.HashMap;
import java.util.Map;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class RDBERestrictionChecker {

  /**
   * Calculates possible RDBE (degree of unsaturation) values according to the formula:
   * <p>
   * RDBE = 1 + Sum(ni x vi - 2) / 2
   * <p>
   * where ni is the number of atoms with valence vi. If multiple valences are allowed (e.g. N may
   * have valence 3 or 5), there may be multiple results for RDBE.
   */
  public static Double calculateRDBE(IMolecularFormula formula) {

    double sum = 0;

    /**
     * This table defines the ground valence states. Typically, in most molecules atoms will have the
     * lowest (ground) valence.
     */
    Map<String, Integer> valences2 = new HashMap<String, Integer>();
    valences2.put("H", 1);
    valences2.put("C", 4);
    valences2.put("N", 3);
    valences2.put("O", 2);
    valences2.put("Si", 4);
    valences2.put("P", 3);
    valences2.put("S", 2);
    valences2.put("F", 1);
    valences2.put("Cl", 1);
    valences2.put("Br", 1);
    valences2.put("I", 1);
    valences2.put("Na", 1);
    valences2.put("K", 1);
    valences2.put("Mg", 2);
    valences2.put("Ca", 2);
    valences2.put("Ba", 2);

    for (IIsotope isotope : formula.isotopes()) {

      Integer valence = valences2.get(isotope.getSymbol());
      if (valence == null) {
        return null;
      }
      sum += (valence - 2) * formula.getIsotopeCount(isotope);
    }

    sum /= 2;
    sum += 1;

    return sum;
  }

  public static boolean checkRDBE(double rdbeValue, Range<Double> rdbeRange,
      boolean mustBeInteger) {
    if ((mustBeInteger) && (Math.floor(rdbeValue) != rdbeValue)) {
      return false;
    }

    return rdbeRange.contains(rdbeValue);
  }

}
