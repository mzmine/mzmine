/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.*;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Task to search and annotate lipids in feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchTask extends AbstractTask {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private double finishedSteps;
  private double totalSteps;
  private final FeatureList featureList;
  private final LipidClasses[] selectedLipids;
  private CustomLipidClass[] customLipidClasses;
  private final int minChainLength;
  private final int maxChainLength;
  private final int maxDoubleBonds;
  private final int minDoubleBonds;
  private final MZTolerance mzTolerance;
  private MZTolerance mzToleranceMS2;
  private final Boolean searchForMSMSFragments;
  private final Boolean keepUnconfirmedAnnotations;
  private double minMsMsScore;

  private final ParameterSet parameters;

  public LipidSearchTask(ParameterSet parameters, FeatureList featureList,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
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
    Object[] selectedObjects = parameters.getParameter(LipidSearchParameters.lipidClasses)
        .getValue();
    this.searchForMSMSFragments =
        parameters.getParameter(LipidSearchParameters.searchForMSMSFragments).getValue();
    if (searchForMSMSFragments.booleanValue()) {
      this.mzToleranceMS2 = parameters.getParameter(LipidSearchParameters.searchForMSMSFragments)
          .getEmbeddedParameters().getParameter(LipidSearchMSMSParameters.mzToleranceMS2)
          .getValue();
      this.keepUnconfirmedAnnotations = parameters
          .getParameter(LipidSearchParameters.searchForMSMSFragments).getEmbeddedParameters()
          .getParameter(LipidSearchMSMSParameters.keepUnconfirmedAnnotations).getValue();
      this.minMsMsScore = parameters.getParameter(LipidSearchParameters.searchForMSMSFragments)
          .getEmbeddedParameters().getParameter(LipidSearchMSMSParameters.minimumMsMsScore)
          .getValue();
    } else {
      this.keepUnconfirmedAnnotations = true;
    }
    Boolean searchForCustomLipidClasses = parameters.getParameter(
        LipidSearchParameters.customLipidClasses).getValue();
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
    if (totalSteps == 0) {
      return 0;
    }
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

    logger.info("Starting lipid annotation in " + featureList);

    List<FeatureListRow> rows = featureList.getRows();
    if (featureList instanceof ModularFeatureList) {
      ((ModularFeatureList) featureList).addRowType(new LipidMatchListType());
    }
    totalSteps = rows.size();

    // build lipid species database
    Set<ILipidAnnotation> lipidDatabase = buildLipidDatabase();

    // start lipid annotation
    rows.parallelStream().forEach(row -> {
      for (ILipidAnnotation lipidAnnotation : lipidDatabase) {
        findPossibleLipid(lipidAnnotation, row);
      }
      finishedSteps++;
    });

    // Add task description to featureList
    (featureList).addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Lipid annotation",
        LipidSearchModule.class, parameters, getModuleCallDate()));

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
    for (ILipidClass lipidClass : lipidClasses) {
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {

          if (chainLength / 2 < chainDoubleBonds || chainLength == 0) {
            continue;
          }

          // Prepare a lipid instance
          ILipidAnnotation lipid = LIPID_FACTORY.buildSpeciesLevelLipid(lipidClass,
              chainLength, chainDoubleBonds);
          if (lipid != null) {
            lipidDatabase.add(lipid);
          }
        }
      }
    }
  }

  /**
   * Check if candidate peak may be a possible adduct of a given main peak
   */
  private void findPossibleLipid(ILipidAnnotation lipid, FeatureListRow row) {
    if (isCanceled()) {
      return;
    }
    Set<MatchedLipid> possibleRowAnnotations = new HashSet<>();
    Set<IonizationType> ionizationTypeList = new HashSet<>();
    LipidFragmentationRule[] fragmentationRules = lipid.getLipidClass().getFragmentationRules();
    for (LipidFragmentationRule fragmentationRule : fragmentationRules) {
      ionizationTypeList.add(fragmentationRule.getIonizationType());
    }
    for (IonizationType ionization : ionizationTypeList) {
      if (!Objects.requireNonNull(row.getBestFeature().getRepresentativeScan()).getPolarity()
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
  }

  private void addAnnotationsToFeatureList(FeatureListRow row,
      Set<MatchedLipid> possibleRowAnnotations) {

    for (MatchedLipid matchedLipid : possibleRowAnnotations) {
      if (matchedLipid != null) {
        row.addLipidAnnotation(matchedLipid);
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
    if (!row.getAllFragmentScans().isEmpty()) {
      List<Scan> msmsScans = row.getAllFragmentScans();
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
          for (DataPoint dataPoint : massList) {
            Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(dataPoint.getMZ());
            LipidFragment annotatedFragment = msmsLipidTools.checkForClassSpecificFragment(
                mzTolRangeMSMS, lipid, ionization, rules,
                new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan);
            if (annotatedFragment != null) {
              annotatedFragments.add(annotatedFragment);
            }
          }
        }
        if (!annotatedFragments.isEmpty()) {

          // check for class specific fragments like head group fragment
          MatchedLipid matchedLipid =
              msmsLipidTools.confirmSpeciesLevelAnnotation(row.getAverageMZ(), lipid,
                  annotatedFragments, massList, minMsMsScore, mzToleranceMS2, ionization);
          addUniqueMatchedLipid(matchedLipid, matchedLipids);

          // predict molecular species level annotations
          Set<MatchedLipid> molecularSpeciesLevelMatchedLipids =
              msmsLipidTools.predictMolecularSpeciesLevelAnnotation(annotatedFragments, lipid,
                  row.getAverageMZ(), massList, minMsMsScore, mzToleranceMS2, ionization);
          if (matchedLipid != null && molecularSpeciesLevelMatchedLipids != null
              && !molecularSpeciesLevelMatchedLipids.isEmpty()) {
            combineMsMsScores(matchedLipid, molecularSpeciesLevelMatchedLipids);
          }

          if (molecularSpeciesLevelMatchedLipids != null
              && !molecularSpeciesLevelMatchedLipids.isEmpty()) {
            for (MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
              addUniqueMatchedLipid(molecularSpeciesLevelMatchedLipid, matchedLipids);
            }
          }
        }
      }
      if (keepUnconfirmedAnnotations.booleanValue() && matchedLipids.isEmpty()) {
        MatchedLipid unconfirmedMatchedLipid =
            new MatchedLipid(lipid, row.getAverageMZ(), ionization, null, 0.0);
        unconfirmedMatchedLipid
            .setComment("Warning, this annotation is based on MS1 mass accurracy only!");
        matchedLipids.add(unconfirmedMatchedLipid);
      }

    }
    return matchedLipids;
  }

  /*
   * Add MS/MS score from species level annotaiton to molecular species level annotation
   */
  private void combineMsMsScores(MatchedLipid speciesLevelMatchedLipid,
      Set<MatchedLipid> molecularSpeciesLevelMatchedLipids) {
    for (MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
      if (speciesLevelMatchedLipid != null && molecularSpeciesLevelMatchedLipid != null
          && speciesLevelMatchedLipid.getLipidAnnotation().getLipidAnnotationLevel()
          .equals(LipidAnnotationLevel.SPECIES_LEVEL)
          && molecularSpeciesLevelMatchedLipid.getLipidAnnotation().getLipidAnnotationLevel()
          .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)
          && molecularSpeciesLevelMatchedLipid.getLipidAnnotation().getLipidClass()
          .equals(speciesLevelMatchedLipid.getLipidAnnotation().getLipidClass())
          && molecularSpeciesLevelMatchedLipid.getLipidAnnotation().getMolecularFormula()
          .equals(speciesLevelMatchedLipid.getLipidAnnotation().getMolecularFormula())) {
        molecularSpeciesLevelMatchedLipid.getMatchedFragments()
            .addAll(speciesLevelMatchedLipid.getMatchedFragments());
        molecularSpeciesLevelMatchedLipid
            .setMsMsScore(molecularSpeciesLevelMatchedLipid.getMsMsScore()
                + speciesLevelMatchedLipid.getMsMsScore());
      }
    }
  }

  private void addUniqueMatchedLipid(MatchedLipid matchedLipid, Set<MatchedLipid> matchedLipids) {
    if (matchedLipid != null) {
      if (matchedLipids.isEmpty()) {
        matchedLipids.add(matchedLipid);
      } else {
        Set<MatchedLipid> lipidsToAdd = new HashSet<>();
        Set<MatchedLipid> lipidsToRemove = new HashSet<>();
        for (MatchedLipid matchedLipid2 : matchedLipids) {
          if (matchedLipid2 != null && !(matchedLipid.getLipidAnnotation().getAnnotation()
              .equals(matchedLipid2.getLipidAnnotation().getAnnotation()))) {
            lipidsToAdd.add(matchedLipid);
          } else if (matchedLipid2 != null
              && matchedLipid.getLipidAnnotation().getAnnotation()
              .equals(matchedLipid2.getLipidAnnotation().getAnnotation())
              && matchedLipid.getMsMsScore() > matchedLipid2.getMsMsScore()) {
            lipidsToRemove.add(matchedLipid2);
            lipidsToAdd.add(matchedLipid);
          }
        }
        matchedLipids.removeAll(lipidsToRemove);
        matchedLipids.addAll(lipidsToAdd);
      }
    }
  }

  private DataPoint[] deisotopeMassList(DataPoint[] massList) {
    MassListDeisotoperParameters massListDeisotoperParameters = new MassListDeisotoperParameters();
    massListDeisotoperParameters.setParameter(MassListDeisotoperParameters.maximumCharge, 1);
    massListDeisotoperParameters.setParameter(MassListDeisotoperParameters.monotonicShape, true);
    massListDeisotoperParameters.setParameter(MassListDeisotoperParameters.mzTolerance,
        mzToleranceMS2);
    return MassListDeisotoper.filterIsotopes(massList, massListDeisotoperParameters);
  }

}
