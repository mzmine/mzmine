/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import java.util.List;
import java.util.Objects;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaStringConverter {

  /**
   *
   */
//  public static String getString(IMolecularFormula formula) {
//    return getString(formula, orderElements, setOne, true);
//  }

  /**
   *
   */
//  public static String getString(IMolecularFormula formula) {
//    return getString(formula, false);
//  }
  public static String getString(IMolecularFormula formula, boolean setMassNumber) {
    String[] orderElements = getElementOrder(formula);
    boolean setOne = false;

    StringBuilder stringMF = new StringBuilder();
    List<IIsotope> isotopesList = MolecularFormulaManipulator.putInOrder(orderElements, formula);
    Integer q = formula.getCharge();
    if (q != null && q != 0) {
      stringMF.append('[');
    }

    if (!setMassNumber) {
      int count = 0;
      int prev = -1;

      for (IIsotope isotope : isotopesList) {
        if (!Objects.equals(isotope.getAtomicNumber(), prev)) {
          if (count != 0) {
            appendElement(stringMF, (Integer) null, prev, !setOne && count == 1 ? 0 : count);
          }

          prev = isotope.getAtomicNumber();
          count = formula.getIsotopeCount(isotope);
        } else {
          count += formula.getIsotopeCount(isotope);
        }
      }

      if (count != 0) {
        appendElement(stringMF, (Integer) null, prev, !setOne && count == 1 ? 0 : count);
      }
    } else {
      for (IIsotope isotope : isotopesList) {
        int count = formula.getIsotopeCount(isotope);
        appendElement(stringMF, isotope.getMassNumber(), isotope.getAtomicNumber(),
            !setOne && count == 1 ? 0 : count);
      }
    }

    if (q != null && q != 0) {
      stringMF.append(']');
      if (q > 0) {
        if (q > 1) {
          stringMF.append(q);
        }

        stringMF.append('+');
      } else {
        if (q < -1) {
          stringMF.append(-q);
        }

        stringMF.append('-');
      }
    }

    return stringMF.toString();
  }

  private static String[] getElementOrder(final IMolecularFormula formula) {
//    MolecularFormulaManipulator.containsElement(formula,
//        (IElement) formula.getBuilder().newInstance(IElement.class, new Object[]{"C"})) ? getString(
//        formula, MolecularFormulaManipulator.generateOrderEle_Hill_WithCarbons()) :
    return null;
  }

  private static void appendElement(StringBuilder sb, Integer mass, int elem, int count) {
    String symbol = Elements.ofNumber(elem).symbol();
    if (symbol.isEmpty()) {
      symbol = "R";
    }
    if (mass != null) {
      sb.append('[').append(mass).append(']').append(symbol);
    } else {
      sb.append(symbol);
    }
    if (count != 0) {
      sb.append(count);
    }
  }

}
