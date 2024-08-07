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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements;

import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class ElementalHeuristicChecker {

  public static boolean checkFormula(IMolecularFormula formula, boolean checkHC, boolean checkNOPS,
      boolean checkMultiple) {

    // if we dont have to do checks, return true.
    if(!checkHC && !checkNOPS && !checkMultiple) {
      return true;
    }

    double eH = 0, eC = 0, eN = 0, eO = 0, eP = 0, eS = 0;
    for (IIsotope isotope : formula.isotopes()) {
      if (isotope.getSymbol().equals("C")) {
        eC += formula.getIsotopeCount(isotope);
      }
      if (isotope.getSymbol().equals("N")) {
        eN += formula.getIsotopeCount(isotope);
      }
      if (isotope.getSymbol().equals("O")) {
        eO += formula.getIsotopeCount(isotope);
      }
      if (isotope.getSymbol().equals("P")) {
        eP += formula.getIsotopeCount(isotope);
      }
      if (isotope.getSymbol().equals("S")) {
        eS += formula.getIsotopeCount(isotope);
      }
      if (isotope.getSymbol().equals("H")) {
        eH += formula.getIsotopeCount(isotope);
      }
    }

    // If there is no carbon, consider the formula OK
    if (eC == 0) {
      return true;
    }

    if (checkHC) {
      double rHC = eH / eC;
      if ((rHC < 0.1) || (rHC > 6)) {
        return false;
      }
    }

    if (checkNOPS) {
      double rPC = eP / eC;
      double rNC = eN / eC;
      double rOC = eO / eC;
      double rSC = eS / eC;
      if ((rNC > 4) || (rOC > 3) || (rPC > 2) || (rSC > 3)) {
        return false;
      }
    }

    if (checkMultiple) {

      // Multiple rule #1
      if ((eN > 1) && (eO > 1) && (eP > 1) && (eS > 1)) {
        if ((eN >= 10) || (eO >= 20) || (eP >= 4) || (eS >= 3)) {
          return false;
        }
      }

      // Multiple rule #2
      if ((eN > 3) && (eO > 3) && (eP > 3)) {
        if ((eN >= 11) || (eO >= 22) || (eP >= 6)) {
          return false;
        }
      }

      // Multiple rule #3
      if ((eO > 1) && (eP > 1) && (eS > 1)) {
        if ((eO >= 14) || (eP >= 3) || (eS >= 3)) {
          return false;
        }
      }

      // Multiple rule #4
      if ((eN > 1) && (eP > 1) && (eS > 1)) {
        if ((eN >= 4) || (eP >= 3) || (eS >= 3)) {
          return false;
        }
      }

      // Multiple rule #5
      if ((eN > 6) && (eO > 6) && (eS > 6)) {
        if ((eN >= 19) || (eO >= 14) || (eS >= 8)) {
          return false;
        }
      }

    }

    return true;

  }

}
