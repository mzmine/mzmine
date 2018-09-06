package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

import java.util.ArrayList;
import net.sf.mzmine.datamodel.PeakIdentity;

public class FattyAcidTools {

  /**
   * This method calculates all possible fatty acids formulas for a selected annotated lipid
   */
  public ArrayList<String> calculateFattyAcidFormulas(PeakIdentity peakIdentity) {

    ArrayList<String> fattyAcidFormulas = new ArrayList<String>();
    LipidTools lipidTools = new LipidTools();
    int minFattyAcidLength = 1;
    int maxFattyAcidLength = lipidTools.getNumberOfCAtoms(peakIdentity.getName());
    int minNumberOfDoubleBonds = 0;
    int maxNumberOfDoubleBonds = lipidTools.getNumberOfDB(peakIdentity.getName());

    for (int fattyAcidLength =
        minFattyAcidLength; fattyAcidLength <= maxFattyAcidLength; fattyAcidLength++) {
      for (int fattyAcidDoubleBonds =
          minNumberOfDoubleBonds; fattyAcidDoubleBonds <= maxNumberOfDoubleBonds; fattyAcidDoubleBonds++) {
        if (((fattyAcidDoubleBonds >= 0)
            && (fattyAcidDoubleBonds > fattyAcidLength - 1) == false)) {
          fattyAcidFormulas.add(calculateFattyAcidFormula(fattyAcidLength, fattyAcidDoubleBonds));
        }
      }
    }

    return fattyAcidFormulas;
  }

  /**
   * This method creates all possible fatty acid names for a selected annotated lipid
   */
  public ArrayList<String> getFattyAcidNames(PeakIdentity peakIdentity) {

    ArrayList<String> fattyAcidNames = new ArrayList<String>();
    LipidTools lipidTools = new LipidTools();
    int minFattyAcidLength = 1;
    int maxFattyAcidLength = lipidTools.getNumberOfCAtoms(peakIdentity.getName());
    int minNumberOfDoubleBonds = 0;
    int maxNumberOfDoubleBonds = lipidTools.getNumberOfDB(peakIdentity.getName());

    for (int fattyAcidLength =
        minFattyAcidLength; fattyAcidLength <= maxFattyAcidLength; fattyAcidLength++) {
      for (int fattyAcidDoubleBonds =
          minNumberOfDoubleBonds; fattyAcidDoubleBonds <= maxNumberOfDoubleBonds; fattyAcidDoubleBonds++) {
        if (((fattyAcidDoubleBonds >= 0)
            && (fattyAcidDoubleBonds > fattyAcidLength - 1) == false)) {
          fattyAcidNames.add(getFattyAcidName(fattyAcidLength, fattyAcidDoubleBonds));
        }
      }
    }

    return fattyAcidNames;
  }


  public String calculateFattyAcidFormula(int fattyAcidLength, int fattyAcidDoubleBonds) {
    int numberOfHydrogens = fattyAcidLength * 2 - fattyAcidDoubleBonds * 2;
    String fattyAcidFormula = "C" + fattyAcidLength + 'H' + numberOfHydrogens + 'O' + 2;

    return fattyAcidFormula;
  }

  public String getFattyAcidName(int fattyAcidLength, int fattyAcidDoubleBonds) {
    return new String("(" + fattyAcidLength + ":" + fattyAcidDoubleBonds + ")");
  }

}
