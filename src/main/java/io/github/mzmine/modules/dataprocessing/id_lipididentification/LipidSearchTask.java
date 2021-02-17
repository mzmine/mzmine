/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidIdentity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.collections.ObservableList;

/**
 * Task to search and annotate lipids in feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private double finishedSteps, totalSteps;
  private FeatureList featureList;
  private Object[] selectedObjects;
  private LipidClasses[] selectedLipids;
  private int minChainLength, maxChainLength, maxDoubleBonds, minDoubleBonds;
  private MZTolerance mzTolerance, mzToleranceMS2;
  private IonizationType ionizationType;
  private Boolean searchForMSMSFragments;
  private Boolean searchForModifications;
  private double[] lipidModificationMasses;
  private LipidModification[] lipidModification;



  private ParameterSet parameters;

  /**
   * @param parameters
   * @param featureList
   */
  public LipidSearchTask(ParameterSet parameters, FeatureList featureList) {

    this.featureList = featureList;
    this.parameters = parameters;

    this.minChainLength =
        parameters.getParameter(LipidSearchParameters.chainLength).getValue().lowerEndpoint();
    this.maxChainLength =
        parameters.getParameter(LipidSearchParameters.chainLength).getValue().upperEndpoint();
    this.minDoubleBonds =
        parameters.getParameter(LipidSearchParameters.doubleBonds).getValue().lowerEndpoint();
    this.maxDoubleBonds =
        parameters.getParameter(LipidSearchParameters.doubleBonds).getValue().upperEndpoint();
    mzTolerance = parameters.getParameter(LipidSearchParameters.mzTolerance).getValue();
    selectedObjects = parameters.getParameter(LipidSearchParameters.lipidClasses).getValue();
    ionizationType = parameters.getParameter(LipidSearchParameters.ionizationMethod).getValue();
    searchForMSMSFragments =
        parameters.getParameter(LipidSearchParameters.searchForMSMSFragments).getValue();
    searchForModifications =
        parameters.getParameter(LipidSearchParameters.searchForModifications).getValue();
    if (searchForModifications) {
      this.lipidModification =
          LipidSearchParameters.searchForModifications.getEmbeddedParameter().getValue();
    }
    if (searchForMSMSFragments) {
      mzToleranceMS2 = parameters.getParameter(LipidSearchParameters.searchForMSMSFragments)
          .getEmbeddedParameters().getParameter(LipidSearchMSMSParameters.mzToleranceMS2)
          .getValue();
    }

    // Convert Objects to LipidClasses
    selectedLipids = Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
        .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0)
      return 0;
    return (finishedSteps) / totalSteps;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Prediction of lipids in " + featureList;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info("Starting lipid search in " + featureList);

    FeatureListRow rows[] = featureList.getRows().toArray(FeatureListRow[]::new);

    // Check if lipids should be modified
    if (searchForModifications == true) {
      lipidModificationMasses = getLipidModificationMasses(lipidModification);
    }
    // Calculate how many possible lipids we will try
    totalSteps = ((maxChainLength - minChainLength + 1) * (maxDoubleBonds - minDoubleBonds + 1))
        * selectedLipids.length;

    // Try all combinations of fatty acid lengths and double bonds
    for (int i = 0; i < selectedLipids.length; i++) {
      int numberOfAcylChains = selectedLipids[i].getNumberOfAcylChains();
      int numberOfAlkylChains = selectedLipids[i].getNumberofAlkyChains();
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {
          // Task canceled?
          if (isCanceled())
            return;

          // If we have non-zero fatty acid, which is shorter
          // than minimal length, skip this lipid
          if (((chainLength > 0) && (chainLength < minChainLength))) {
            finishedSteps++;
            continue;
          }

          // If we have more double bonds than carbons, it
          // doesn't make sense, so let's skip such lipids
          if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
            finishedSteps++;
            continue;
          }
          // Prepare a lipid instance
          LipidIdentity lipidChain = new LipidIdentity(selectedLipids[i], chainLength,
              chainDoubleBonds, numberOfAcylChains, numberOfAlkylChains);
          // Find all rows that match this lipid
          findPossibleLipid(lipidChain, rows);
          finishedSteps++;
        }
      }
    }
    // Add task description to peakList
    featureList
        .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Lipid search",
            LipidSearchModule.class, parameters));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished lipid search task in " + featureList);
  }

  /**
   * Check if candidate peak may be a possible adduct of a given main peak
   *
   */
  private void findPossibleLipid(LipidIdentity lipid, FeatureListRow rows[]) {
    double lipidIonMass = 0.0;
    double lipidMass = lipid.getMass();
    lipidIonMass = lipidMass + ionizationType.getAddedMass();
    logger.info("Searching for lipid " + lipid.getDescription() + ", " + lipidIonMass + " m/z");
    for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      if (isCanceled())
        return;
      Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(rows[rowIndex].getAverageMZ());
      if (mzTolRange12C.contains(lipidIonMass)) {

        // Calc rel mass deviation;
        double relMassDev =
            ((lipidIonMass - rows[rowIndex].getAverageMZ()) / lipidIonMass) * 1000000;
        rows[rowIndex].addFeatureIdentity(lipid, false);
        rows[rowIndex].setComment("Ionization: " + ionizationType.getAdductName() + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm"); // Format relativ mass
                                                                       // deviation
        // If search for MSMS fragments is selected search for fragments
        if (searchForMSMSFragments == true) {
          searchMsmsFragments(rows[rowIndex], lipidIonMass, lipid);
        }
        logger.info("Found lipid: " + lipid.getName() + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm");
      }
      // If search for modifications is selected search for modifications
      // in MS1
      if (searchForModifications == true) {
        searchModifications(rows[rowIndex], lipidIonMass, lipid, lipidModificationMasses,
            mzTolRange12C);
      }
    }
  }

  /**
   * This method searches for MS/MS fragments. A mass list for MS2 scans will be used if present. If
   * no mass list is present for MS2 scans it will create one using centroid or exact mass detection
   * algorithm
   */
  private void searchMsmsFragments(FeatureListRow row, double lipidIonMass, LipidIdentity lipid) {

    // Check if selected feature has MSMS spectra
    if (row.getAllMS2Fragmentations() != null) {
      ObservableList<Scan> msmsScans = row.getAllMS2Fragmentations();
      for (Scan msmsScan : msmsScans) {

        DataPoint[] massList = null;
        // check if MS/MS scan already has a mass list
        massList = msmsScan.getMassList().getDataPoints();
        MSMSLipidTools msmsLipidTools = new MSMSLipidTools();

        // check for negative polarity
        if (msmsScan.getPolarity() == PolarityType.NEGATIVE) {

          // check if lipid class has set negative fragments
          String[] fragments = lipid.getLipidClass().getMsmsFragmentsNegativeIonization();
          if (fragments.length > 0) {
            ArrayList<String> listOfAnnotatedNegativeFragments = new ArrayList<String>();
            for (int i = 0; i < massList.length; i++) {
              Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(massList[i].getMZ());
              String annotatedNegativeFragment =
                  msmsLipidTools.checkForNegativeClassSpecificFragment(mzTolRangeMSMS,
                      row.getPreferredFeatureIdentity(), lipidIonMass, fragments);
              if (annotatedNegativeFragment.equals("") == false
                  && row.getComment().contains(annotatedNegativeFragment) == false) {
                listOfAnnotatedNegativeFragments.add(annotatedNegativeFragment);
              }
            }

            if (listOfAnnotatedNegativeFragments.isEmpty() == false) {

              // predict lipid fatty acid composition if possible
              ArrayList<String> listOfPossibleFattyAcidCompositions =
                  msmsLipidTools.predictFattyAcidComposition(listOfAnnotatedNegativeFragments,
                      row.getPreferredFeatureIdentity(),
                      lipid.getLipidClass().getNumberOfAcylChains());
              for (int i = 0; i < listOfPossibleFattyAcidCompositions.size(); i++) {
                // Add possible composition to comment
                if (row.getComment().equals(null)) {
                  row.setComment(" " + listOfPossibleFattyAcidCompositions.get(i) + " MS/MS scan "
                      + msmsScan.getScanNumber() + ", RT " + MZmineCore.getConfiguration()
                          .getRTFormat().format(msmsScan.getRetentionTime()));
                } else {
                  row.setComment(row.getComment() + ";" + " "
                      + listOfPossibleFattyAcidCompositions.get(i) + " MS/MS scan "
                      + msmsScan.getScanNumber() + ", RT " + MZmineCore.getConfiguration()
                          .getRTFormat().format(msmsScan.getRetentionTime()));
                }
              }

              // add class specific fragments
              for (int i = 0; i < listOfAnnotatedNegativeFragments.size(); i++) {
                if (listOfAnnotatedNegativeFragments.get(i).contains("C")
                    || listOfAnnotatedNegativeFragments.get(i).contains("H")
                    || listOfAnnotatedNegativeFragments.get(i).contains("O")) {
                  // Add fragment to comment
                  if (row.getComment().equals(null)) {
                    row.setComment(" " + listOfAnnotatedNegativeFragments.get(i) + " MS/MS scan "
                        + msmsScan.getScanNumber() + ", RT " + MZmineCore.getConfiguration()
                            .getRTFormat().format(msmsScan.getRetentionTime()));
                  } else {
                    row.setComment(row.getComment() + ";" + " "
                        + listOfAnnotatedNegativeFragments.get(i) + " MS/MS scan "
                        + msmsScan.getScanNumber() + ", RT " + MZmineCore.getConfiguration()
                            .getRTFormat().format(msmsScan.getRetentionTime()));
                  }
                }
              }
            }
          }
        }

        // check if lipid class has positive fragments
        if (msmsScan.getPolarity() == PolarityType.POSITIVE) {

          // check if lipid class has set postiev fragments
          String[] fragments = lipid.getLipidClass().getMsmsFragmentsPositiveIonization();
          if (fragments.length > 0) {
            ArrayList<String> listOfAnnotatedPositiveFragments = new ArrayList<String>();
            for (int i = 0; i < massList.length; i++) {
              Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(massList[i].getMZ());
              String annotatedPositiveFragment =
                  msmsLipidTools.checkForPositiveClassSpecificFragment(mzTolRangeMSMS,
                      row.getPreferredFeatureIdentity(), lipidIonMass, fragments);
              if (annotatedPositiveFragment.equals("") == false
                  && row.getComment().contains(annotatedPositiveFragment) == false) {
                listOfAnnotatedPositiveFragments.add(annotatedPositiveFragment);
              }
            }

            // predict lipid fatty acid composition if possible
            ArrayList<String> listOfPossibleFattyAcidCompositions =
                msmsLipidTools.predictFattyAcidComposition(listOfAnnotatedPositiveFragments,
                    row.getPreferredFeatureIdentity(), lipid.getLipidClass().getNumberOfAcylChains());
            for (int i = 0; i < listOfPossibleFattyAcidCompositions.size(); i++) {
              // Add possible composition to comment
              if (row.getComment().equals(null)) {
                row.setComment(" " + listOfPossibleFattyAcidCompositions.get(i) + " MS/MS scan "
                    + msmsScan.getScanNumber() + ", RT " + MZmineCore.getConfiguration()
                        .getRTFormat().format(msmsScan.getRetentionTime()));
              } else {
                row.setComment(
                    row.getComment() + ";" + " " + listOfPossibleFattyAcidCompositions.get(i)
                        + " MS/MS scan " + msmsScan.getScanNumber() + ", RT " + MZmineCore
                            .getConfiguration().getRTFormat().format(msmsScan.getRetentionTime()));
              }
            }

            // add class specific fragments
            for (int i = 0; i < listOfAnnotatedPositiveFragments.size(); i++) {
              if (listOfAnnotatedPositiveFragments.get(i).contains("C")) {
                // Add fragment to comment
                if (row.getComment().equals(null)) {
                  row.setComment(" " + listOfAnnotatedPositiveFragments.get(i) + " MS/MS scan "
                      + msmsScan.getScanNumber() + ", RT " + MZmineCore.getConfiguration()
                          .getRTFormat().format(msmsScan.getRetentionTime()));
                } else {
                  row.setComment(row.getComment() + ";" + " "
                      + listOfAnnotatedPositiveFragments.get(i) + " MS/MS scan "
                      + msmsScan.getScanNumber() + ", RT " + MZmineCore.getConfiguration()
                          .getRTFormat().format(msmsScan.getRetentionTime()));
                }
              }
            }
          }
        }
      }
    }
  }

  private void searchModifications(FeatureListRow rows, double lipidIonMass, LipidIdentity lipid,
      double[] lipidModificationMasses, Range<Double> mzTolModification) {
    for (int j = 0; j < lipidModificationMasses.length; j++) {
      if (mzTolModification.contains(lipidIonMass + (lipidModificationMasses[j]))) {
        // Calc relativ mass deviation
        double relMassDev = ((lipidIonMass + (lipidModificationMasses[j]) - rows.getAverageMZ())
            / (lipidIonMass + lipidModificationMasses[j])) * 1000000;
        // Add row identity
        rows.addFeatureIdentity(new SimpleFeatureIdentity(lipid + " " + lipidModification[j]), false);
        rows.setComment("Ionization: " + ionizationType.getAdductName() + " " + lipidModification[j]
            + ", Δ " + NumberFormat.getInstance().format(relMassDev) + " ppm");
        logger.info("Found modified lipid: " + lipid.getName() + " " + lipidModification[j] + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm");
      }
    }
  }

  private double[] getLipidModificationMasses(LipidModification[] lipidModification) {
    double[] lipidModificationMasses = new double[lipidModification.length];
    for (int i = 0; i < lipidModification.length; i++) {
      lipidModificationMasses[i] = lipidModification[i].getModificationMass();
    }
    return lipidModificationMasses;
  }
}
