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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.LipidAnnotationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 * Task to search and annotate lipids in feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchTask extends AbstractTask {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private double finishedSteps;
  private double totalSteps;
  private ModularFeatureList featureList;
  private Object[] selectedObjects;
  private LipidClasses[] selectedLipids;
  private Boolean searchForCustomLipidClasses;
  private CustomLipidClass[] customLipidClasses;
  private int minChainLength;
  private int maxChainLength;
  private int maxDoubleBonds;
  private int minDoubleBonds;
  private MZTolerance mzTolerance;
  private MZTolerance mzToleranceMS2;
  private IonizationType ionizationType;
  private Boolean searchForMSMSFragments;
  private Boolean ionizationAutoSearch;
  private Boolean keepUnconfirmedAnnotations;
  private Boolean searchForModifications;
  private double[] lipidModificationMasses;
  private LipidModification[] lipidModification;
  private double minMsMsScore;

  private ParameterSet parameters;

  /**
   * @param parameters
   * @param featureList
   */
  public LipidSearchTask(ParameterSet parameters, ModularFeatureList featureList) {
    super(null);
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
    this.mzTolerance = parameters.getParameter(LipidSearchParameters.mzTolerance).getValue();
    this.selectedObjects = parameters.getParameter(LipidSearchParameters.lipidClasses).getValue();
    this.ionizationType =
        parameters.getParameter(LipidSearchParameters.ionizationMethod).getValue();
    this.searchForMSMSFragments =
        parameters.getParameter(LipidSearchParameters.searchForMSMSFragments).getValue();
    this.searchForModifications =
        parameters.getParameter(LipidSearchParameters.searchForModifications).getValue();
    if (searchForModifications.booleanValue()) {
      this.lipidModification =
          LipidSearchParameters.searchForModifications.getEmbeddedParameter().getValue();
    }
    if (searchForMSMSFragments.booleanValue()) {
      this.mzToleranceMS2 = parameters.getParameter(LipidSearchParameters.searchForMSMSFragments)
          .getEmbeddedParameters().getParameter(LipidSearchMSMSParameters.mzToleranceMS2)
          .getValue();
      this.ionizationAutoSearch = parameters
          .getParameter(LipidSearchParameters.searchForMSMSFragments).getEmbeddedParameters()
          .getParameter(LipidSearchMSMSParameters.ionizationAutoSearch).getValue();
      this.keepUnconfirmedAnnotations = parameters
          .getParameter(LipidSearchParameters.searchForMSMSFragments).getEmbeddedParameters()
          .getParameter(LipidSearchMSMSParameters.keepUnconfirmedAnnotations).getValue();
      this.minMsMsScore = parameters.getParameter(LipidSearchParameters.searchForMSMSFragments)
          .getEmbeddedParameters().getParameter(LipidSearchMSMSParameters.minimumMsMsScore)
          .getValue();
    } else {
      this.ionizationAutoSearch = false;
      this.keepUnconfirmedAnnotations = true;
    }
    this.searchForCustomLipidClasses =
        parameters.getParameter(LipidSearchParameters.customLipidClasses).getValue();
    if (searchForCustomLipidClasses.booleanValue()) {
      this.customLipidClasses =
          LipidSearchParameters.customLipidClasses.getEmbeddedParameter().getChoices();
    }
    // Convert Objects to LipidClasses
    this.selectedLipids = Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
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

    List<FeatureListRow> rows = featureList.getRows();
    featureList.addRowType(new LipidAnnotationType());

    // Check if lipids should be modified
    // TODO maybe remove this complety, just use custom lipid classes
    if (searchForModifications.booleanValue()) {
      extractLipidModificationMasses(lipidModification);
    }

    totalSteps = rows.size();

    // build lipid species database
    Set<ILipidAnnotation> lipidDatabase = buildLipidDatabase();

    // start lipid search
    rows.parallelStream().forEach(row -> {
      for (ILipidAnnotation lipidAnnotation : lipidDatabase) {
        findPossibleLipid(lipidAnnotation, row);
      }
      finishedSteps++;
    });

    // Add task description to featureList
    (featureList).addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Lipid annotation",
        LipidSearchModule.class, parameters));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished lipid annotation task in " + featureList);
  }

  private Set<ILipidAnnotation> buildLipidDatabase() {

    Set<ILipidAnnotation> lipidDatabase = new LinkedHashSet<>();

    // add selected lipids
    buildLipidCombinations(lipidDatabase, selectedLipids);

    // add custom lipids
    if (customLipidClasses != null && customLipidClasses.length > 0) {
      buildLipidCombinations(lipidDatabase, customLipidClasses);
    }

    return lipidDatabase;
  }

  private void buildLipidCombinations(Set<ILipidAnnotation> lipidDatabase,
      ILipidClass[] lipidClasses) {
    // Try all combinations of fatty acid lengths and double bonds
    for (int i = 0; i < lipidClasses.length; i++) {
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {

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
          lipidDatabase.add(
              LIPID_FACTORY.buildSpeciesLevelLipid(lipidClasses[i], chainLength, chainDoubleBonds));
        }
      }
    }
  }

  /**
   * Check if candidate peak may be a possible adduct of a given main peak
   *
   * @param mainPeak
   * @param possibleFragment
   */
  private void findPossibleLipid(ILipidAnnotation lipid, FeatureListRow row) {
    if (isCanceled()) {
      return;
    }
    Set<MatchedLipid> possibleRowAnnotations = new HashSet<>();
    Set<IonizationType> ionizationTypeList = new HashSet<>();
    if (ionizationAutoSearch.booleanValue()) {
      LipidFragmentationRule[] fragmentationRules = lipid.getLipidClass().getFragmentationRules();
      for (int i = 0; i < fragmentationRules.length; i++) {
        ionizationTypeList.add(fragmentationRules[i].getIonizationType());
      }
    } else {
      ionizationTypeList.add(ionizationType);
    }
    for (IonizationType ionization : ionizationTypeList) {
      if (!row.getBestFeature().getRepresentativeScan().getPolarity()
          .equals(ionization.getPolarity())) {
        continue;
      }
      double lipidIonMass = MolecularFormulaManipulator.getMass(lipid.getMolecularFormula(),
          AtomContainerManipulator.MonoIsotopic) + ionization.getAddedMass();
      Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(row.getAverageMZ());

      // MS1 check
      if (mzTolRange12C.contains(lipidIonMass)) {

        // If search for MSMS fragments is selected search for fragments
        if (searchForMSMSFragments.booleanValue()) {
          possibleRowAnnotations.addAll(searchMsmsFragments(row, ionization, lipid));
        } else {

          // make MS1 annotation
          possibleRowAnnotations
              .add(new MatchedLipid(lipid, row.getAverageMZ(), ionization, null, 0.0));
        }
      }

    }
    addAnnotationsToFeatureList(row, possibleRowAnnotations);

    // TODO maybe just use custom lipid classes fot this
    // If search for modifications is selected search for modifications
    // in MS1
    // if (searchForModifications.booleanValue()) {
    // searchModifications(row, lipidIonMass, lipid, lipidModificationMasses, mzTolRange12C);
    // }
  }

  private void addAnnotationsToFeatureList(FeatureListRow row,
      Set<MatchedLipid> possibleRowAnnotations) {

    if (!possibleRowAnnotations.isEmpty()) {
      for (MatchedLipid matchedLipid : possibleRowAnnotations) {
        if (matchedLipid != null) {
          row.addLipidAnnotation(matchedLipid);
        }
      }
    }
  }

  /**
   * This method searches for MS/MS fragments. A mass list for MS2 scans will be used if present.
   */
  private Set<MatchedLipid> searchMsmsFragments(FeatureListRow row, IonizationType ionization,
      ILipidAnnotation lipid) {

    Set<MatchedLipid> matchedLipids = new HashSet<>();

    // Check if selected feature has MSMS spectra and LipidIdentity
    if (!row.getAllMS2Fragmentations().isEmpty()) {
      List<Scan> msmsScans = row.getAllMS2Fragmentations();
      for (Scan msmsScan : msmsScans) {
        if (msmsScan.getMassList() == null) {
          setErrorMessage("Mass List cannot be found.\nCheck if MS2 Scans have a Mass List");
          setStatus(TaskStatus.ERROR);
          return new HashSet<>();
        }
        DataPoint[] massList = null;
        massList = msmsScan.getMassList().getDataPoints();
        massList = deisotopeMassList(massList);
        MSMSLipidTools msmsLipidTools = new MSMSLipidTools();
        LipidFragmentationRule[] rules = lipid.getLipidClass().getFragmentationRules();
        Set<LipidFragment> annotatedFragments = new HashSet<>();
        if (rules != null && rules.length > 0) {
          for (int j = 0; j < massList.length; j++) {
            Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(massList[j].getMZ());
            LipidFragment annotatedFragment = msmsLipidTools.checkForClassSpecificFragment(
                mzTolRangeMSMS, lipid, ionization, rules, massList[j], msmsScan);
            if (annotatedFragment != null) {
              annotatedFragments.add(annotatedFragment);
            }
          }
        }
        if (!annotatedFragments.isEmpty()) {

          // check for class specific fragments like head group fragment
          matchedLipids.add(msmsLipidTools.confirmSpeciesLevelAnnotation(row.getAverageMZ(), lipid,
              annotatedFragments, massList, minMsMsScore, mzToleranceMS2, ionization));

          // predict molecular species level annotations
          matchedLipids
              .addAll(msmsLipidTools.predictMolecularSpeciesLevelAnnotation(annotatedFragments,
                  lipid, row.getAverageMZ(), massList, minMsMsScore, mzToleranceMS2, ionization));
        }
      }
      if (keepUnconfirmedAnnotations.booleanValue() && matchedLipids.isEmpty()) {
        matchedLipids.add(new MatchedLipid(lipid, row.getAverageMZ(), ionization, null, 0.0));
      }

    }
    return matchedLipids;
  }

  // private void searchModifications(FeatureListRow rows, double lipidIonMass,
  // LipidFeatureIdentity lipid, double[] lipidModificationMasses,
  // Range<Double> mzTolModification) {
  // for (int j = 0; j < lipidModificationMasses.length; j++) {
  // if (mzTolModification.contains(lipidIonMass + (lipidModificationMasses[j]))) {
  //
  // // Calc relativ mass deviation
  // double relMassDev = ((lipidIonMass + (lipidModificationMasses[j]) - rows.getAverageMZ())
  // / (lipidIonMass + lipidModificationMasses[j])) * 1000000;
  //
  // // Add row identity
  // rows.addLipidAnnotation(
  // new LipidFeatureIdentity(lipid + " " + lipidModification[j], lipid.getName()), true);
  // rows.setComment(rows.getComment() + " Ionization: " + ionizationType.getAdductName()
  // + " Warning: Lipid Annotation was not confirmed by MS/MS and needs to be checked manually!"
  // + lipidModification[j] + ", Δ " + NumberFormat.getInstance().format(relMassDev)
  // + " ppm");
  // logger.info(
  // " Warning: Lipid Annotation was not confirmed by MS/MS and needs to be checked manually! Found
  // modified lipid: "
  // + lipid.getName() + " " + lipidModification[j] + ", Δ "
  // + NumberFormat.getInstance().format(relMassDev) + " ppm");
  // }
  // }
  // }

  private DataPoint[] deisotopeMassList(DataPoint[] massList) {
    MassListDeisotoperParameters massListDeisotoperParameters = new MassListDeisotoperParameters();
    massListDeisotoperParameters.setParameter(MassListDeisotoperParameters.maximumCharge, 1);
    massListDeisotoperParameters.setParameter(MassListDeisotoperParameters.monotonicShape, true);
    massListDeisotoperParameters.setParameter(MassListDeisotoperParameters.mzTolerance,
        mzToleranceMS2);
    return MassListDeisotoper.filterIsotopes(massList, massListDeisotoperParameters);
  }

  private void extractLipidModificationMasses(LipidModification[] lipidModification) {
    lipidModificationMasses = new double[lipidModification.length];
    for (int i = 0; i < lipidModification.length; i++) {
      lipidModificationMasses[i] = lipidModification[i].getModificationMass();
    }
  }

}
