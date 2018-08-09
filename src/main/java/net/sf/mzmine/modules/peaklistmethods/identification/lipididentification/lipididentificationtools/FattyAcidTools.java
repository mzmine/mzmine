package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

import java.util.ArrayList;
import net.sf.mzmine.util.FormulaUtils;

public class FattyAcidTools {

  public ArrayList<String> calculateFattyAcidFormulas(int minfattyAcidLength,
      int maxfattyAcidLength, int maxNumberOfDoubleBonds, int maxOxidationValue) {

    ArrayList<String> fattyAcidFormulas = new ArrayList<String>();

    for (int fattyAcidLength = 0; fattyAcidLength <= maxfattyAcidLength; fattyAcidLength++) {
      for (int fattyAcidDoubleBonds =
          0; fattyAcidDoubleBonds <= maxNumberOfDoubleBonds; fattyAcidDoubleBonds++) {
        for (int oxidationValue = 0; oxidationValue <= maxOxidationValue; oxidationValue++) {
          if (((fattyAcidDoubleBonds >= 0)
              && (fattyAcidDoubleBonds > fattyAcidLength - 1) == false)) {
            fattyAcidFormulas.add(
                calculateFattyAcidFormula(fattyAcidLength, fattyAcidDoubleBonds, oxidationValue));
          }
        }
      }
    }

    return fattyAcidFormulas;
  }

  public ArrayList<String> getFattyAcidNames(int minfattyAcidLength, int maxfattyAcidLength,
      int maxNumberOfDoubleBonds, int maxOxidationValue) {

    ArrayList<String> fattyAcidFormulas = new ArrayList<String>();

    for (int fattyAcidLength = 0; fattyAcidLength <= maxfattyAcidLength; fattyAcidLength++) {
      for (int fattyAcidDoubleBonds =
          0; fattyAcidDoubleBonds <= maxNumberOfDoubleBonds; fattyAcidDoubleBonds++) {
        for (int oxidationValue = 0; oxidationValue <= maxOxidationValue; oxidationValue++) {
          if (((fattyAcidDoubleBonds >= 0)
              && (fattyAcidDoubleBonds > fattyAcidLength - 1) == false)) {
            fattyAcidFormulas
                .add(getFattyAcidName(fattyAcidLength, fattyAcidDoubleBonds, oxidationValue));
          }
        }
      }
    }

    return fattyAcidFormulas;
  }

  public double getFAMass(String fattyAcidFormula) {
    double fattyAcidMass = FormulaUtils.calculateExactMass(fattyAcidFormula);
    return fattyAcidMass;
  }

  public String calculateFattyAcidFormula(int fattyAcidLength, int fattyAcidDoubleBonds,
      int oxidationValue) {

    final int numberOfHydrogens = fattyAcidLength * 2 - fattyAcidDoubleBonds * 2 - 1;
    String fattyAcidFormula = "C" + fattyAcidLength + 'H' + numberOfHydrogens + 'O' + 2;

    return fattyAcidFormula;
  }

  public String getFattyAcidName(int fattyAcidLength, int fattyAcidDoubleBonds,
      int oxidationValue) {
    return new String("(" + fattyAcidLength + ":" + fattyAcidDoubleBonds + ")");
  }

}
