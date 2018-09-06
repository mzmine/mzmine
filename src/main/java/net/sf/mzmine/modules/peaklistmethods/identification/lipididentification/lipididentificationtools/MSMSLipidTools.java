package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

import java.util.ArrayList;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.util.FormulaUtils;

public class MSMSLipidTools {

  /**
   * This method checks for negative class specific fragments in the MS/MS spectra of an annotated
   * lipid
   * 
   * returns a list of annotated fragments
   */
  public String checkForNegativeClassSpecificFragment(Range<Double> mzTolRangeMSMS,
      PeakIdentity peakIdentity, double lipidIonMass, String[] classSpecificFragments) {
    String annotatedFragment = "";

    // load lipid tools to get information of annotations
    LipidTools lipidTools = new LipidTools();

    // load fatty acid tools to build fatty acids
    FattyAcidTools fattyAcidTools = new FattyAcidTools();
    ArrayList<String> fattyAcidFormulas = fattyAcidTools.calculateFattyAcidFormulas(peakIdentity);
    ArrayList<String> fattyAcidNames = fattyAcidTools.getFattyAcidNames(peakIdentity);

    for (int i = 0; i < classSpecificFragments.length; i++) {
      double massOfFragment = FormulaUtils.calculateExactMass(
          lipidTools.getSumFormulaOfFragmentContainingFA(classSpecificFragments[i]));

      // check for FA residues
      if (classSpecificFragments[i].equals("FA")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          double mass = IonizationType.NEGATIVE_HYDROGEN.getAddedMass()
              + FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j)));
          if (mzTolRangeMSMS.contains(mass)) {
            annotatedFragment = "FA" + fattyAcidNames.get(j);
          }
        }
      }

      // check for M-FA
      else if (classSpecificFragments[i].equals("M-FA")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          double massFattyAcid = FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j)));
          if (mzTolRangeMSMS
              .contains(lipidIonMass - massFattyAcid - FormulaUtils.calculateExactMass("H"))) {
            annotatedFragment = "M-FA" + fattyAcidNames.get(j) + massFattyAcid;
          }
        }
      }
      //
      // // check for FA + FA + sum formula fragments
      // else if (classSpecificFragments[i].contains("FA+FA+C")) {
      // int ctr = 0;
      // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
      // for (int k = ctr; k < fattyAcidFormulas.size(); k++) {
      // double accurateMass = FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j)))
      // + massOfFragment + FormulaUtils.calculateExactMass((fattyAcidFormulas.get(k)))
      // + 2 * FormulaUtils.calculateExactMass("H");
      // if (mzTolRangeMSMS.contains(accurateMass)) {
      // listOfDetectedFragments
      // .add("FA+" + fattyAcidNames.get(j) + "+" + "FA+" + fattyAcidNames.get(k) + "+"
      // + lipidTools.getSumFormulaOfFragment(classSpecificFragments[i]) + " "
      // + accurateMass);
      // }
      // }
      // ctr++;
      // }
      // }

      // check for FA + sum formula fragments
      else if (classSpecificFragments[i].contains("FA+C")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          double accurateMass = IonizationType.NEGATIVE_HYDROGEN.getAddedMass()
              + FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j))) + massOfFragment;
          if (mzTolRangeMSMS.contains(accurateMass)) {
            annotatedFragment = "FA" + fattyAcidNames.get(j) + "+"
                + lipidTools.getSumFormulaOfFragmentContainingFA(classSpecificFragments[i]) + " "
                + accurateMass;
          }
        }
      }

      // check for specific sum formula fragment
      else if (classSpecificFragments[i].contains("C")) {
        System.out.println("hi");
        if (mzTolRangeMSMS.contains(FormulaUtils.calculateExactMass(
            lipidTools.getSumFormulaOfSumFormulaFragment(classSpecificFragments[i])))) {
          annotatedFragment = classSpecificFragments[i];
        }
      }

    }
    return annotatedFragment;
  }

  /**
   * This methods tries to reconstruct a possible fatty acid composition of the annotated lipid
   * using the annotated MS/MS fragments
   */
  public ArrayList<String> predictFattyAcidComposition(ArrayList<String> listOfDetectedFragments,
      PeakIdentity peakIdentity) {
    ArrayList<String> fattyAcidComposition = new ArrayList<String>();

    // get number of total C atoms and double bonds
    LipidTools lipidTools = new LipidTools();
    int totalNumberOfCAtoms = lipidTools.getNumberOfCAtoms(peakIdentity.getName());
    int totalNumberOfDB = lipidTools.getNumberOfDB(peakIdentity.getName());

    int testNumberOfCAtoms = 0;
    int testNumberOfDoubleBonds = 0;

    // combine all fragments with each other to check for a matching composition
    for (int i = 0; i < listOfDetectedFragments.size(); i++) {
      if (listOfDetectedFragments.get(i).contains("FA")) {
        int numberOfCAtomsInFragment = lipidTools.getNumberOfCAtoms(listOfDetectedFragments.get(i));
        int numberOfDBInFragment = lipidTools.getNumberOfDB(listOfDetectedFragments.get(i));

        for (int j = 0; j < listOfDetectedFragments.size(); j++) {

          // only check for annotated fragments with information on FA composition
          if (listOfDetectedFragments.get(j).contains("FA")) {
            // check if number of C atoms is equal
            testNumberOfCAtoms = numberOfCAtomsInFragment
                + lipidTools.getNumberOfCAtoms(listOfDetectedFragments.get(j));
            if (testNumberOfCAtoms == totalNumberOfCAtoms) {
              // check number of double bonds
              testNumberOfDoubleBonds =
                  numberOfDBInFragment + lipidTools.getNumberOfDB(listOfDetectedFragments.get(j));
              if (testNumberOfDoubleBonds == totalNumberOfDB) {
                fattyAcidComposition
                    .add(listOfDetectedFragments.get(i) + "/" + listOfDetectedFragments.get(j));
              }
            }
          }
        }
      }
    }
    return fattyAcidComposition;
  }

  // public ArrayList<String> checkForNegativeClassSpecificFragmentsOld(Range<Double>
  // mzTolRangeMSMS,
  // PeakIdentity peakIdentity, double lipidIonMass) {
  // ArrayList<String> listOfFragments = new ArrayList<String>();
  // // load a MSMS fragment library
  // MSMSLibrary msmsLibrary = loadMSMSLibrary(peakIdentity);
  // // Get possible fatty acids
  // FattyAcidTools fattyAcidTools = new FattyAcidTools();
  // ArrayList<String> fattyAcidFormulas = fattyAcidTools.calculateFattyAcidFormulas(peakIdentity);
  // ArrayList<String> fattyAcidNames = fattyAcidTools.getFattyAcidNames(peakIdentity);
  //
  // // Loop through possible fragments
  // for (int i = 0; i < msmsLibrary.getName().length; i++) {
  // // Search for fatty acid
  // if (msmsLibrary.getName()[i].equals("sn1")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS
  // .contains(FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j))))) {
  // listOfFragments.add("FA" + fattyAcidNames.get(j));
  // }
  // }
  // }
  // // Search for fatty acid - 2H
  // if (msmsLibrary.getName()[i].equals("sn1-2H")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS.contains(FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j)))
  // + FormulaUtils.calculateExactMass("2H"))) {
  // listOfFragments.add("FA-2H " + fattyAcidNames.get(j));
  // }
  // }
  // }
  // // Search for fatty acid + O (Hydroxy FA)
  // if (msmsLibrary.getName()[i].equals("sn1+O")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS.contains(FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j)))
  // + FormulaUtils.calculateExactMass("O"))) {
  // listOfFragments.add("FA+O " + fattyAcidNames.get(j));
  // }
  // }
  // }
  // // Search for [M-H] - fatty acid
  // if (msmsLibrary.getName()[i].equals("[M-H]-sn1")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS.contains(
  // lipidIonMass - FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j))))) {
  // listOfFragments.add("[M-H]-FA" + fattyAcidNames.get(j));
  // }
  // }
  // }
  // // Search for [M-H] - fatty acid
  // if (msmsLibrary.getName()[i].equals("[M-H]-sn1-2H")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS
  // .contains(lipidIonMass - FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j)))
  // + FormulaUtils.calculateExactMass("2H"))) {
  // listOfFragments.add("[M-H]-FA-2H " + fattyAcidNames.get(j));
  // }
  // }
  // }
  // // Search for [M-H] - fatty acid -H2O
  // if (msmsLibrary.getName()[i].equals("[M-H]-sn1-H2O")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS.contains(
  // lipidIonMass - FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j))))) {
  // listOfFragments.add("[M-H]-FA" + fattyAcidNames.get(j));
  // }
  // }
  // }
  // // Search for [M-H] - fatty acid - Sum Formula starting with C
  // if (msmsLibrary.getName()[i].contains("[M-H]-sn1-C")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS
  // .contains(lipidIonMass - FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j)))
  // - FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
  // listOfFragments.add("[M-H]-FA" + fattyAcidNames.get(j) + "-"
  // + msmsLibrary.getFormulaOfStaticFormula()[i]);
  // }
  // }
  // }
  // // Search for [M-H] - Sum Formula starting with C
  // if (msmsLibrary.getName()[i].contains("[M-H]-C")) {
  // if (mzTolRangeMSMS.contains(lipidIonMass
  // - FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
  // listOfFragments.add(msmsLibrary.getName()[i]);
  // }
  // }
  // // Search for sum formula starting with C only
  // if (msmsLibrary.getName()[i].startsWith("C")) {
  // if (mzTolRangeMSMS.contains(
  // FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
  // listOfFragments.add(msmsLibrary.getName()[i]);
  // }
  // }
  // }
  // return listOfFragments;
  // }

  // public ArrayList<String> checkForPositiveClassSpecificFragments(Range<Double> mzTolRangeMSMS,
  // PeakIdentity peakIdentity, double lipidIonMass) {
  // ArrayList<String> listOfFragments = new ArrayList<String>();
  // // load a MSMS fragment library
  // MSMSLibrary msmsLibrary = loadMSMSLibrary(peakIdentity);
  // // Get possible fatty acids
  // FattyAcidTools fattyAcidTools = new FattyAcidTools();
  // ArrayList<String> fattyAcidFormulas = fattyAcidTools.calculateFattyAcidFormulas(peakIdentity);
  // ArrayList<String> fattyAcidNames = fattyAcidTools.getFattyAcidNames(peakIdentity);
  // // Loop through possible fragments
  // for (int i = 0; i < msmsLibrary.getName().length; i++) {
  // if (mzTolRangeMSMS
  // .contains(FormulaUtils.calculateExactMass(msmsLibrary.getFormulaOfStaticFormula()[i]))) {
  // listOfFragments.add("Fragment: " + msmsLibrary.getFormulaOfStaticFormula()[i]);
  // }
  // if (msmsLibrary.getName()[i].contains("-139 -(Erythritol + NH3)")) {
  // if (mzTolRangeMSMS.contains(lipidIonMass - 139.08448)) {
  // listOfFragments
  // .add("[M" + msmsLibrary.getFormulaOfStaticFormula()[i] + " (Erythritol + NH3)]");
  // }
  // }
  // if (msmsLibrary.getName()[i].contains("[M-139 - sn1]")) {
  // for (int j = 0; j < fattyAcidFormulas.size(); j++) {
  // if (mzTolRangeMSMS.contains(lipidIonMass - 139.08448
  // - FormulaUtils.calculateExactMass((fattyAcidFormulas.get(j) + "H")))) {
  // listOfFragments.add(" [M-139" + "-FA" + fattyAcidNames.get(j) + "]");
  // }
  // }
  // }
  // if (msmsLibrary.getName()[i].contains("[M-139 - H2O]")) {
  // if (mzTolRangeMSMS
  // .contains(lipidIonMass - 139.08448 - FormulaUtils.calculateExactMass("H2O"))) {
  // listOfFragments.add(" [M" + msmsLibrary.getFormulaOfStaticFormula()[i]);
  // }
  // }
  // }
  //
  //
  // return listOfFragments;
  //
  // }

}
