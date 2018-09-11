/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

import net.sf.mzmine.util.FormulaUtils;

/**
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidTools {

  private int numberOfCAtoms;
  private int numberOfDoubleBonds;

  /**
   * This method reads out the number of C atoms containing in all radyl chains of a lipid
   * annotation
   */
  public int getNumberOfCAtoms(String lipidAnnotation) {

    int counterFirstBracket = 0;
    int indexFirstNumber = 0;
    int indexLastNumber = 0;

    // Loop through every char and check for "C"
    for (int i = 0; i < lipidAnnotation.length(); i++) {
      // get first Bracket
      if (lipidAnnotation.charAt(i) == '(' && counterFirstBracket < 1) {
        indexFirstNumber = i + 1;
        counterFirstBracket++;
      }
      if (lipidAnnotation.charAt(i) == ':' && counterFirstBracket == 1) {
        indexLastNumber = i - 1;
      }
    }
    if (indexFirstNumber == indexLastNumber) {
      numberOfCAtoms = Integer.parseInt(String.valueOf(lipidAnnotation.charAt(indexFirstNumber)));
    } else {
      numberOfCAtoms = Integer.parseInt(String.valueOf(lipidAnnotation.charAt(indexFirstNumber))
          + String.valueOf(lipidAnnotation.charAt(indexLastNumber)));
    }
    return numberOfCAtoms;
  }

  /**
   * This method reads out the number of double bonds containing in all radyl chains of a lipid
   * annotation
   */
  public int getNumberOfDB(String lipidAnnotation) {

    int counterFirstBracket = 0;
    int indexFirstNumber = 0;
    int indexLastNumber = 0;

    // Loop through every char and check for "C"
    for (int i = 0; i < lipidAnnotation.length(); i++) {
      // get first Bracket
      if (lipidAnnotation.charAt(i) == ':') {
        indexFirstNumber = i + 1;
        counterFirstBracket++;
      }
      if (lipidAnnotation.charAt(i) == ')' && counterFirstBracket == 1) {
        indexLastNumber = i - 1;
      }
    }
    if (indexFirstNumber == indexLastNumber) {
      numberOfDoubleBonds =
          Integer.parseInt(String.valueOf(lipidAnnotation.charAt(indexFirstNumber)));
    } else {
      numberOfDoubleBonds =
          Integer.parseInt(String.valueOf(lipidAnnotation.charAt(indexFirstNumber))
              + String.valueOf(lipidAnnotation.charAt(indexLastNumber)));
    }
    return numberOfDoubleBonds;
  }

  /**
   * This method reads out the sum formula of class specific MS/MS fragment that also contains a
   * fatty acid
   */
  public String getSumFormulasToAddOfFragmentContainingFA(String classSpecificFragment) {
    String sumFormula = null;

    // filter out sum formulas
    String[] sumFormulasToAdd = classSpecificFragment.split("\\+");

    // only keep sum formulas
    String unsortedSumFormula = "";

    // add sum formulas
    for (int i = 0; i < sumFormulasToAdd.length; i++) {
      if (sumFormulasToAdd[i].contains("M") || sumFormulasToAdd[i].contains("FA")) {
        continue;
      } else {
        unsortedSumFormula = unsortedSumFormula + sumFormulasToAdd[i];
      }
    }
    sumFormula = FormulaUtils.formatFormula(FormulaUtils.parseFormula(unsortedSumFormula));
    return sumFormula;
  }

  /**
   * This method reads out the sum formula of class specific MS/MS fragment which need be
   * substracted from the exact mass
   */
  public String getSumFormulasToSubstractOfFragment(String classSpecificFragment) {
    String sumFormula = null;

    // filter out sum formulas
    String[] sumFormulasToSubstract = classSpecificFragment.split("\\-");

    // only keep sum formulas
    String unsortedSumFormula = "";

    // add sum formulas
    for (int i = 0; i < sumFormulasToSubstract.length; i++) {
      if (sumFormulasToSubstract[i].contains("M") || sumFormulasToSubstract[i].contains("FA")) {
        continue;
      } else {
        unsortedSumFormula = unsortedSumFormula + sumFormulasToSubstract[i];
      }
    }
    sumFormula = FormulaUtils.formatFormula(FormulaUtils.parseFormula(unsortedSumFormula));
    return sumFormula;
  }

  /**
   * This method reduces a class specific fragment String to its sum formulas
   */
  public String getSumFormulaOfSumFormulaFragment(String classSpecificFragment) {
    String sumFormula = null;
    // filter out sum formulas of fragments
    String[] sumFormulasOfFragments = classSpecificFragment.split("fragment ");

    for (int i = 0; i < sumFormulasOfFragments.length; i++) {
      if (sumFormulasOfFragments[i].contains("C") || sumFormulasOfFragments[i].contains("O")) {
        sumFormula = sumFormulasOfFragments[i];
      }
    }
    return sumFormula;
  }

}
