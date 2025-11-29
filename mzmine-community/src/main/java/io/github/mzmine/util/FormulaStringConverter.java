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

import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaStringConverter {

  private static String[] generateOrderEle_Hill_NoCarbons() {
    return new String[]{"Ac", "Ag", "Al", "Am", "Ar", "As", "At", "Au", "B", "Ba", "Be", "Bh", "Bi",
        "Bk", "Br", "C", "Ca", "Cd", "Ce", "Cf", "Cl", "Cm", "Cn", "Co", "Cr", "Cs", "Cu", "Db",
        "Ds", "Dy", "Er", "Es", "Eu", "F", "Fe", "Fm", "Fr", "Ga", "Gd", "Ge", "H", "He", "Hf",
        "Hg", "Ho", "Hs", "I", "In", "Ir", "K", "Kr", "La", "Li", "Lr", "Lu", "Md", "Mg", "Mn",
        "Mo", "Mt", "N", "Na", "Nb", "Nd", "Ne", "Ni", "No", "Np", "O", "Os", "P", "Pa", "Pb", "Pd",
        "Pm", "Po", "Pr", "Pt", "Pu", "Ra", "Rb", "Re", "Rf", "Rg", "Rh", "Rn", "Ru", "S", "Sb",
        "Sc", "Se", "Sg", "Si", "Sm", "Sn", "Sr", "Ta", "Tb", "Tc", "Te", "Th", "Ti", "Tl", "Tm",
        "U", "V", "W", "Xe", "Y", "Yb", "Zn", "Zr",
        // unspecified
        "R"};
  }

  private static String[] generateOrderEle_Hill_WithCarbons() {
    return new String[]{"C", "H", "Ac", "Ag", "Al", "Am", "Ar", "As", "At", "Au", "B", "Ba", "Be",
        "Bh", "Bi", "Bk", "Br", "Ca", "Cd", "Ce", "Cf", "Cl", "Cm", "Cn", "Co", "Cr", "Cs", "Cu",
        "Db", "Ds", "Dy", "Er", "Es", "Eu", "F", "Fe", "Fm", "Fr", "Ga", "Gd", "Ge", "He", "Hf",
        "Hg", "Ho", "Hs", "I", "In", "Ir", "K", "Kr", "La", "Li", "Lr", "Lu", "Md", "Mg", "Mn",
        "Mo", "Mt", "N", "Na", "Nb", "Nd", "Ne", "Ni", "No", "Np", "O", "Os", "P", "Pa", "Pb", "Pd",
        "Pm", "Po", "Pr", "Pt", "Pu", "Ra", "Rb", "Re", "Rf", "Rg", "Rh", "Rn", "Ru", "S", "Sb",
        "Sc", "Se", "Sg", "Si", "Sm", "Sn", "Sr", "Ta", "Tb", "Tc", "Te", "Th", "Ti", "Tl", "Tm",
        "U", "V", "W", "Xe", "Y", "Yb", "Zn", "Zr",
        // unspecified
        "R"};
  }

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
  public static String getString(@Nullable IMolecularFormula formula,
      @NotNull FormulaStringFlavor flavor) {
    return getString(formula, flavor.skipMajorMassNumber(), flavor.showCharge());
  }

  public static String getString(IMolecularFormula formula) {
    return getString(formula, FormulaStringFlavor.DEFAULT_CHARGED);
  }

  public static String getString(IMolecularFormula formula, boolean skipMajorMassNumber) {
    return getString(formula, skipMajorMassNumber, true);
  }

  public static String getString(IMolecularFormula formula, boolean skipMajorMassNumber,
      boolean showCharge) {
    final double skipMassNumberForMajorPercent = 0.6;

    if (skipMassNumberForMajorPercent < 0.5) {
      throw new IllegalArgumentException(
          "Cannot skip mass number for major percent less than 0.5 because this may include more than one isotope");
    }

    String[] orderElements = getElementOrder(formula);

    StringBuilder stringMF = new StringBuilder();
    List<IIsotope> isotopesList = MolecularFormulaManipulator.putInOrder(orderElements, formula);
    final Integer charge = showCharge ? formula.getCharge() : null;
    if (charge != null && charge != 0) {
      stringMF.append('[');
    }

    for (IIsotope isotope : isotopesList) {
      final IIsotope major;
      try {
        major = Isotopes.getInstance().getMajorIsotope(isotope.getAtomicNumber());
      } catch (IOException e) {
        // should not happen
        throw new RuntimeException(e);
      }

      int count = formula.getIsotopeCount(isotope);
      // replace with major number if missing
      Integer massNumber = requireNonNullElse(isotope.getMassNumber(), major.getMassNumber());

      // always showing mass number for elements different from major
      // may skip major if percent >
      if (skipMajorMassNumber && Objects.equals(major.getMassNumber(), massNumber)) {
        // abundance is in 100% not 1.0
        final Double abundance = major.getNaturalAbundance();
        if (abundance != null && skipMassNumberForMajorPercent <= abundance / 100d) {
          massNumber = null;
        }
      }

      appendElement(stringMF, massNumber, isotope.getAtomicNumber(), count == 1 ? 0 : count);
    }

    if (charge != null && charge != 0) {
      stringMF.append(']');
      if (charge > 0) {
        if (charge > 1) {
          stringMF.append(charge);
        }

        stringMF.append('+');
      } else {
        if (charge < -1) {
          stringMF.append(-charge);
        }

        stringMF.append('-');
      }
    }

    return stringMF.toString();
  }

  private static String[] getElementOrder(final IMolecularFormula formula) {
    if (MolecularFormulaManipulator.containsElement(formula,
        formula.getBuilder().newInstance(IElement.class, "C"))) {
      return generateOrderEle_Hill_WithCarbons();
    }
    return generateOrderEle_Hill_NoCarbons();
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
