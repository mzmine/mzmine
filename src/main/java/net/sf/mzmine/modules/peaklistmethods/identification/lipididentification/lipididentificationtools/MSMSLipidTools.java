package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

import java.util.ArrayList;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.util.FormulaUtils;

public class MSMSLipidTools {

  public ArrayList<String> checkForNegativeClassSpecificFragments(Range<Double> mzTolRangeMSMS,
      PeakIdentity peakIdentity, double lipidIonMass) {
    ArrayList<String> listOfFragments = new ArrayList<String>();
    // load a MSMS fragment library
    MSMSLibrary msmsLibrary = loadMSMSLibrary(peakIdentity);
    // Get possible fatty acids
    FattyAcidTools fattyAcidTools = new FattyAcidTools();
    ArrayList<String> fattyAcidFormulas = fattyAcidTools.calculateFattyAcidFormulas(4, 26, 8, 0);
    ArrayList<String> fattyAcidNames = fattyAcidTools.getFattyAcidNames(4, 26, 8, 0);

    // Loop through possible fragments
    for (int i = 0; i < msmsLibrary.getName().length; i++) {
      // Search for fatty acid
      if (msmsLibrary.getName()[i].equals("sn1")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS.contains(fattyAcidTools.getFAMass((fattyAcidFormulas.get(j))))) {
            listOfFragments.add("FA" + fattyAcidNames.get(j));
          }
        }
      }
      // Search for fatty acid - 2H
      if (msmsLibrary.getName()[i].equals("sn1-2H")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS.contains(fattyAcidTools.getFAMass((fattyAcidFormulas.get(j)))
              + FormulaUtils.calculateExactMass("2H"))) {
            listOfFragments.add("FA-2H " + fattyAcidNames.get(j));
          }
        }
      }
      // Search for fatty acid + O (Hydroxy FA)
      if (msmsLibrary.getName()[i].equals("sn1+O")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS.contains(fattyAcidTools.getFAMass((fattyAcidFormulas.get(j)))
              + FormulaUtils.calculateExactMass("O"))) {
            listOfFragments.add("FA+O " + fattyAcidNames.get(j));
          }
        }
      }
      // Search for [M-H] - fatty acid
      if (msmsLibrary.getName()[i].equals("[M-H]-sn1")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS
              .contains(lipidIonMass - fattyAcidTools.getFAMass((fattyAcidFormulas.get(j))))) {
            listOfFragments.add("[M-H]-FA" + fattyAcidNames.get(j));
          }
        }
      }
      // Search for [M-H] - fatty acid
      if (msmsLibrary.getName()[i].equals("[M-H]-sn1-2H")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS
              .contains(lipidIonMass - fattyAcidTools.getFAMass((fattyAcidFormulas.get(j)))
                  + FormulaUtils.calculateExactMass("2H"))) {
            listOfFragments.add("[M-H]-FA-2H " + fattyAcidNames.get(j));
          }
        }
      }
      // Search for [M-H] - fatty acid -H2O
      if (msmsLibrary.getName()[i].equals("[M-H]-sn1-H2O")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS
              .contains(lipidIonMass - fattyAcidTools.getFAMass((fattyAcidFormulas.get(j))))) {
            listOfFragments.add("[M-H]-FA" + fattyAcidNames.get(j));
          }
        }
      }
      // Search for [M-H] - fatty acid - Sum Formula starting with C
      if (msmsLibrary.getName()[i].contains("[M-H]-sn1-C")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS
              .contains(lipidIonMass - fattyAcidTools.getFAMass((fattyAcidFormulas.get(j)))
                  - FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
            listOfFragments.add("[M-H]-FA" + fattyAcidNames.get(j) + "-"
                + msmsLibrary.getFormulaOfStaticFormula()[i]);
          }
        }
      }
      // Search for [M-H] - Sum Formula starting with C
      if (msmsLibrary.getName()[i].contains("[M-H]-C")) {
        if (mzTolRangeMSMS.contains(lipidIonMass
            - FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
          listOfFragments.add(msmsLibrary.getName()[i]);
        }
      }
      // Search for sum formula starting with C only
      if (msmsLibrary.getName()[i].startsWith("C")) {
        if (mzTolRangeMSMS.contains(
            FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
          listOfFragments.add(msmsLibrary.getName()[i]);
        }
      }
    }
    return listOfFragments;
  }

  public ArrayList<String> checkForPositiveClassSpecificFragments(Range<Double> mzTolRangeMSMS,
      PeakIdentity peakIdentity, double lipidIonMass) {
    ArrayList<String> listOfFragments = new ArrayList<String>();
    // load a MSMS fragment library
    MSMSLibrary msmsLibrary = loadMSMSLibrary(peakIdentity);
    // Get possible fatty acids
    FattyAcidTools fattyAcidTools = new FattyAcidTools();
    ArrayList<String> fattyAcidFormulas = fattyAcidTools.calculateFattyAcidFormulas(4, 26, 8, 0);
    ArrayList<String> fattyAcidNames = fattyAcidTools.getFattyAcidNames(4, 26, 8, 0);
    // Loop through possible fragments
    for (int i = 0; i < msmsLibrary.getName().length; i++) {
      if (mzTolRangeMSMS
          .contains(FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
        listOfFragments.add("Fragment: " + msmsLibrary.getFormulaOfStaticFormula()[i]);
      }
      if (msmsLibrary.getName()[i].contains("-139 -(Erythritol + NH3)")) {
        if (mzTolRangeMSMS.contains(lipidIonMass - 139.08448)) {
          listOfFragments
              .add("[M" + msmsLibrary.getFormulaOfStaticFormula()[i] + " (Erythritol + NH3)]");
        }
      }
      if (msmsLibrary.getName()[i].contains("[M-139 - sn1]")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS.contains(lipidIonMass - 139.08448
              - fattyAcidTools.getFAMass((fattyAcidFormulas.get(j) + "H")))) {
            listOfFragments.add(" [M-139" + "-FA" + fattyAcidNames.get(j) + "]");
          }
        }
      }
      if (msmsLibrary.getName()[i].contains("[M-139 - H2O]")) {
        if (mzTolRangeMSMS
            .contains(lipidIonMass - 139.08448 - FormulaUtils.calculateExactMass("H2O"))) {
          listOfFragments.add(" [M" + msmsLibrary.getFormulaOfStaticFormula()[i]);
        }
      }
    }


    return listOfFragments;

  }

  private MSMSLibrary loadMSMSLibrary(PeakIdentity peakIdentity) {
    // Get lipid class name
    int endIndexSubstring = 0;
    for (int i = 0; i < peakIdentity.getName().length(); i++) {
      if (peakIdentity.getName().charAt(i) == '(') {
        endIndexSubstring = i;
      }
    }
    String lipidClass = peakIdentity.getName().substring(0, endIndexSubstring);
    MSMSLibrary msmsLibrary = MSMSLibrary.valueOf(lipidClass);
    return msmsLibrary;
  }

}
