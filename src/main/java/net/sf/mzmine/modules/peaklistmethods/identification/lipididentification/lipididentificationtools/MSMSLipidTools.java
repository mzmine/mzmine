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

import java.util.ArrayList;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.util.FormulaUtils;

/**
 * This class contains methods for MS/MS lipid identifications
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
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
            annotatedFragment = "M-FA" + fattyAcidNames.get(j);
          }
        }
      }

      // check for FA + sum formula fragments
      else if (classSpecificFragments[i].contains("FA") && classSpecificFragments[i].contains("C")
          && classSpecificFragments[i].contains("\\+")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          double massOfFragment = FormulaUtils.calculateExactMass(
              lipidTools.getSumFormulasToAddOfFragmentContainingFA(classSpecificFragments[i]));
          double accurateMass = IonizationType.NEGATIVE_HYDROGEN.getAddedMass()
              + FormulaUtils.calculateExactMass(fattyAcidFormulas.get(j)) + massOfFragment;
          if (mzTolRangeMSMS.contains(accurateMass)) {
            annotatedFragment = "FA" + fattyAcidNames.get(j) + "+"
                + lipidTools.getSumFormulasToAddOfFragmentContainingFA(classSpecificFragments[i]);
          }
        }
      }

      // check for fragments with M-FA and + sum formula
      else if (classSpecificFragments[i].contains("M") && classSpecificFragments[i].contains("-")
          && classSpecificFragments[i].contains("+")) {
        if (classSpecificFragments[i].contains("FA")) {
          for (int j = 0; j < fattyAcidFormulas.size(); j++) {
            double massOfSumFormulasToSubstract = FormulaUtils.calculateExactMass(
                lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]));
            double massOfSumFormulasToAdd = FormulaUtils.calculateExactMass(
                lipidTools.getSumFormulasToAddOfFragmentContainingFA(classSpecificFragments[i]));
            if (mzTolRangeMSMS
                .contains(lipidIonMass - FormulaUtils.calculateExactMass(fattyAcidFormulas.get(j))
                    + massOfSumFormulasToAdd - massOfSumFormulasToSubstract)) {
              annotatedFragment = "M-FA" + fattyAcidNames.get(j)
                  + lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]) + "+"
                  + lipidTools.getSumFormulasToAddOfFragmentContainingFA(classSpecificFragments[i]);
            }
          }
        }
      }

      // check for fragments with M-FA and - sum formula or M - sum formula
      else if (classSpecificFragments[i].contains("M") && classSpecificFragments[i].contains("-")) {
        if (classSpecificFragments[i].contains("FA")) {
          for (int j = 0; j < fattyAcidFormulas.size(); j++) {
            double massOfSumFormulasToSubstract = FormulaUtils.calculateExactMass(
                lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]));
            if (mzTolRangeMSMS
                .contains(lipidIonMass - FormulaUtils.calculateExactMass(fattyAcidFormulas.get(j))
                    - massOfSumFormulasToSubstract)) {
              annotatedFragment = "M-FA-"
                  + lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]);
            }
          }
        }
        // only substract sum formula
        else {
          double massOfSumFormulasToSubstract = FormulaUtils.calculateExactMass(
              lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]));
          if (mzTolRangeMSMS.contains(lipidIonMass - massOfSumFormulasToSubstract)) {
            annotatedFragment =
                "M-" + lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]);
          }
        }
      }

      // check for specific sum formula fragment
      else if (classSpecificFragments[i].contains("C") || classSpecificFragments[i].contains("O")) {
        if (mzTolRangeMSMS.contains(FormulaUtils.calculateExactMass(
            lipidTools.getSumFormulaOfSumFormulaFragment(classSpecificFragments[i])))) {
          annotatedFragment = classSpecificFragments[i];
        }
      }

    }
    return annotatedFragment;
  }

  /**
   * This method checks for positive class specific fragments in the MS/MS spectra of an annotated
   * lipid
   * 
   * returns a list of annotated fragments
   */
  public String checkForPositiveClassSpecificFragment(Range<Double> mzTolRangeMSMS,
      PeakIdentity peakIdentity, double lipidIonMass, String[] classSpecificFragments) {
    String annotatedFragment = "";

    // load lipid tools to get information of annotations
    LipidTools lipidTools = new LipidTools();

    // load fatty acid tools to build fatty acids
    FattyAcidTools fattyAcidTools = new FattyAcidTools();
    ArrayList<String> fattyAcidFormulas = fattyAcidTools.calculateFattyAcidFormulas(peakIdentity);
    ArrayList<String> fattyAcidNames = fattyAcidTools.getFattyAcidNames(peakIdentity);

    for (int i = 0; i < classSpecificFragments.length; i++) {

      // check for FA residues
      if (classSpecificFragments[i].equals("M-FA")) {
        for (int j = 0; j < fattyAcidFormulas.size(); j++) {
          if (mzTolRangeMSMS
              .contains(lipidIonMass - FormulaUtils.calculateExactMass(fattyAcidFormulas.get(j)))) {
            annotatedFragment = "FA" + fattyAcidNames.get(j);
          }
        }
      }


      // check for fragments with M-FA and + sum formula
      else if (classSpecificFragments[i].contains("M") && classSpecificFragments[i].contains("-")
          && classSpecificFragments[i].contains("+")) {
        if (classSpecificFragments[i].contains("FA")) {
          for (int j = 0; j < fattyAcidFormulas.size(); j++) {
            double massOfSumFormulasToSubstract = FormulaUtils.calculateExactMass(
                lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]));
            double massOfSumFormulasToAdd = FormulaUtils.calculateExactMass(
                lipidTools.getSumFormulasToAddOfFragmentContainingFA(classSpecificFragments[i]));
            if (mzTolRangeMSMS
                .contains(lipidIonMass - FormulaUtils.calculateExactMass(fattyAcidFormulas.get(j))
                    + massOfSumFormulasToAdd - massOfSumFormulasToSubstract)) {
              annotatedFragment = "M-FA" + fattyAcidNames.get(j)
                  + lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]) + "+"
                  + lipidTools.getSumFormulasToAddOfFragmentContainingFA(classSpecificFragments[i]);
            }
          }
        }
      }


      // check for fragments with M-FA and - sum formula or M - sum formula
      else if (classSpecificFragments[i].contains("M") && classSpecificFragments[i].contains("-")) {
        if (classSpecificFragments[i].contains("FA")) {
          for (int j = 0; j < fattyAcidFormulas.size(); j++) {
            double massOfSumFormulasToSubstract = FormulaUtils.calculateExactMass(
                lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]));

            if (mzTolRangeMSMS
                .contains(lipidIonMass - FormulaUtils.calculateExactMass(fattyAcidFormulas.get(j))
                    - massOfSumFormulasToSubstract)) {
              annotatedFragment = "M-FA-"
                  + lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]);
            }
          }
        }
        // only substract sum formula
        else {
          double massOfSumFormulasToSubstract = FormulaUtils.calculateExactMass(
              lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]));
          if (mzTolRangeMSMS.contains(lipidIonMass - massOfSumFormulasToSubstract)) {
            annotatedFragment =
                "M-" + lipidTools.getSumFormulasToSubstractOfFragment(classSpecificFragments[i]);
          }
        }
      }

      // check for specific sum formula fragment
      else if (classSpecificFragments[i].contains("C") || classSpecificFragments[i].contains("O")) {
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
      if (listOfDetectedFragments.get(i).contains("FA(")) {
        int numberOfCAtomsInFragment = lipidTools.getNumberOfCAtoms(listOfDetectedFragments.get(i));
        int numberOfDBInFragment = lipidTools.getNumberOfDB(listOfDetectedFragments.get(i));

        for (int j = 0; j < listOfDetectedFragments.size(); j++) {

          // only check for annotated fragments with information on FA composition
          if (listOfDetectedFragments.get(j).contains("FA(")) {
            // check if number of C atoms is equal
            testNumberOfCAtoms = numberOfCAtomsInFragment
                + lipidTools.getNumberOfCAtoms(listOfDetectedFragments.get(j));
            if (testNumberOfCAtoms == totalNumberOfCAtoms) {
              // check number of double bonds
              testNumberOfDoubleBonds =
                  numberOfDBInFragment + lipidTools.getNumberOfDB(listOfDetectedFragments.get(j));
              if (testNumberOfDoubleBonds == totalNumberOfDB) {
                fattyAcidComposition
                    .add("FA(" + lipidTools.getNumberOfCAtoms(listOfDetectedFragments.get(i)) + ":"
                        + lipidTools.getNumberOfDB(listOfDetectedFragments.get(i)) + ")" + "_"
                        + "FA(" + lipidTools.getNumberOfCAtoms(listOfDetectedFragments.get(j)) + ":"
                        + lipidTools.getNumberOfDB(listOfDetectedFragments.get(j)) + ")");
              }
            }
          }
        }
      }
    }
    fattyAcidComposition = removeDoubleEntries(fattyAcidComposition);
    return fattyAcidComposition;
  }

  private ArrayList<String> removeDoubleEntries(ArrayList<String> fattyAcidComposition) {
    for (int i = 0; i < fattyAcidComposition.size(); i++) {
      String[] oneFattyAcidComposition = fattyAcidComposition.get(i).split("_");
      // only remove ones
      int ctr = 0;
      for (int j = 0; j < fattyAcidComposition.size(); j++) {
        String[] compareFattyAcidComposition = fattyAcidComposition.get(j).split("_");
        if (oneFattyAcidComposition[0].equals(compareFattyAcidComposition[1])
            && oneFattyAcidComposition[1].equals(compareFattyAcidComposition[0]) && ctr == 0) {
          fattyAcidComposition.remove(j);
          ctr++;
        }
      }
    }
    return fattyAcidComposition;
  }
}
