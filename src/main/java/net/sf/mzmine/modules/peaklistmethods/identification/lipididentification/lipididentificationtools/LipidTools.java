package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

import net.sf.mzmine.util.FormulaUtils;

public class LipidTools {

  private int numberOfCAtoms;
  private int numberOfDoubleBonds;

  public int getNumberOfCAtoms(String lipidAnnotation) {

    int counterFirstBracket = 0;
    int indexFirstNumber = 0;
    int indexLastNumber = 0;

    // Loop through every char and check for "C"
    for (int i = 0; i < lipidAnnotation.length(); i++) {
      // get first Bracket
      if (lipidAnnotation.charAt(i) == '(') {
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

  public String getSumFormulaOfFragmentContainingFA(String classSpecificFragment) {
    String sumFormula = null;

    // filter out sum formulas
    String[] sumFormulasToAdd = classSpecificFragment.split("\\+");
    String[] sumFormulasToSubstract = classSpecificFragment.split("\\-"); // to do!!!

    // only keep sum formulas
    String unsortedSumFormula = "";
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

  public String getSumFormulaOfSumFormulaFragment(String classSpecificFragment) {
    String sumFormula = null;
    // filter out sum formulas of fragments
    String[] sumFormulasOfFragments = classSpecificFragment.split("fragment ");

    for (int i = 0; i < sumFormulasOfFragments.length; i++) {
      if (sumFormulasOfFragments[i].contains("C")) {
        sumFormula = sumFormulasOfFragments[i];
      }
    }
    return sumFormula;
  }

}
