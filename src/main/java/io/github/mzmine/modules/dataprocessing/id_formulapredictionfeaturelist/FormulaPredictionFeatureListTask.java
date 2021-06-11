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
package io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FormulaAnnotationType;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaPredictionFeatureListTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private Range<Double> massRange;
  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;
  private IonizationType ionType;
  private int charge;
  private ModularFeatureList featureList;
  private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
  private ParameterSet isotopeParameters, msmsParameters, ratiosParameters, rdbeParameters, parameters;
  private MZTolerance mzTolerance;
  private String message;
  private int totalRows, finishedRows;
  private int maxBestFormulasPerFeature;
  private final double minScore;
  private final double minMSMSScore;
  private Boolean isSorting;
  private Double sortPPMFactor = 20d;
  private Double sortIsotopeFactor = 0d;
  private Double sortMSMSFactor = 0d;
  private List<ResultFormula> resultingFormulas;

  /**
   * @param parameters
   * @param featureList
   */
  FormulaPredictionFeatureListTask(ModularFeatureList featureList, ParameterSet parameters) {
    super(null); // no new data stored -> null

    this.featureList = featureList;
    charge = parameters.getParameter(FormulaPredictionFeatureListParameters.charge).getValue();
    ionType = parameters.getParameter(FormulaPredictionFeatureListParameters.ionization).getValue();
    mzTolerance =
        parameters.getParameter(FormulaPredictionFeatureListParameters.mzTolerance).getValue();
    elementCounts =
        parameters.getParameter(FormulaPredictionFeatureListParameters.elements).getValue();

    checkIsotopes =
        parameters.getParameter(FormulaPredictionFeatureListParameters.isotopeFilter).getValue();
    isotopeParameters = parameters
        .getParameter(FormulaPredictionFeatureListParameters.isotopeFilter).getEmbeddedParameters();
    if (checkIsotopes) {
      // Only get the value if the isotope checking is activated, otherwise we might get a NPE
      minScore = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold).getValue();
    } else {
      minScore = 0d;
    }

    checkMSMS =
        parameters.getParameter(FormulaPredictionFeatureListParameters.msmsFilter).getValue();
    msmsParameters = parameters.getParameter(FormulaPredictionFeatureListParameters.msmsFilter)
        .getEmbeddedParameters();
    if (checkMSMS) {
      // Only get the value if the MSMS checking is activated, otherwise we might get a NPE
      minMSMSScore = msmsParameters.getParameter(MSMSScoreParameters.msmsMinScore).getValue();
    } else {
      minMSMSScore = 0d;
    }

    checkRDBE =
        parameters.getParameter(FormulaPredictionFeatureListParameters.rdbeRestrictions).getValue();
    rdbeParameters =
        parameters.getParameter(FormulaPredictionFeatureListParameters.rdbeRestrictions)
            .getEmbeddedParameters();

    checkRatios =
        parameters.getParameter(FormulaPredictionFeatureListParameters.elementalRatios).getValue();
    ratiosParameters =
        parameters.getParameter(FormulaPredictionFeatureListParameters.elementalRatios)
            .getEmbeddedParameters();

    maxBestFormulasPerFeature = parameters
        .getParameter(FormulaPredictionFeatureListParameters.maxBestFormulasPerFeature).getValue();

    // get sorting parameters
    isSorting = parameters.getParameter(FormulaPredictionFeatureListParameters.sorting).getValue();
    if (isSorting) {
      FormulaSortParameters sortParam = parameters
          .getParameter(FormulaPredictionFeatureListParameters.sorting).getEmbeddedParameters();
      sortPPMFactor = sortParam.getParameter(FormulaSortParameters.MAX_PPM_WEIGHT).getValue();
      sortMSMSFactor = sortParam.getParameter(FormulaSortParameters.MSMS_SCORE_WEIGHT).getValue();
      sortIsotopeFactor =
          sortParam.getParameter(FormulaSortParameters.ISOTOPE_SCORE_WEIGHT).getValue();
    }
    message = "Formula Prediction";
    this.parameters = parameters;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0;
    }
    return (double) finishedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    totalRows = featureList.getNumberOfRows();

    featureList.addRowType(new FormulaAnnotationType());

    for (FeatureListRow row : featureList.getRows()) {

      if (row.getPeakIdentities().size() > 0) {
        continue;
      }
      this.resultingFormulas = new ArrayList<>();

      double searchedMass = (row.getAverageMZ() - ionType.getAddedMass()) * charge;

      message = "Formula prediction for "
                + MZmineCore.getConfiguration().getMZFormat().format(searchedMass);

      massRange = mzTolerance.getToleranceRange(searchedMass);

      IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
          massRange.upperEndpoint(), elementCounts);

      IMolecularFormula cdkFormula;

      // create a map to store ResultFormula and relative mass deviation
      // for sorting
      while ((cdkFormula = generator.getNextFormula()) != null) {
        // Mass is ok, so test other constraints
        ResultFormula molf = checkConstraints(cdkFormula, row, searchedMass);

        if (isCanceled() || getStatus().equals(TaskStatus.ERROR)) {
          return;
        }

        if (molf != null) {
          resultingFormulas.add(molf);
        }
      }

      if (isCanceled()) {
        return;
      }

      // Add the new formula entry top results
      row.setFormulas(resultingFormulas.subList(0, maxBestFormulasPerFeature));

      if (isCanceled()) {
        return;
      }
      finishedRows++;
    }

    if (isCanceled()) {
      return;
    }

    featureList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        FormulaPredictionFeatureListModule.class, parameters));

    logger.finest("Finished formula search for all the features");

    setStatus(TaskStatus.FINISHED);

  }

  /**
   *
   * @param cdkFormula
   * @return null if molecular formula does not match requirements
   */
  private ResultFormula checkConstraints(IMolecularFormula cdkFormula, FeatureListRow peakListRow, double searchedMass) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula, ratiosParameters);
      if (!check) {
        return null;
      }
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeParameters);
      if (!check) {
        return null;
      }
    }

    // Calculate isotope similarity score
    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    IsotopePattern predictedIsotopePattern = null;
    Double isotopeScore = null;
    if ((checkIsotopes) && (detectedPattern != null)) {

      String stringFormula = MolecularFormulaManipulator.getString(cdkFormula);

      String adjustedFormula = FormulaUtils.ionizeFormula(stringFormula, ionType, charge);

      final double isotopeNoiseLevel = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopeNoiseLevel).getValue();

      final double detectedPatternHeight = detectedPattern.getBasePeakIntensity();

      final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

      predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(adjustedFormula,
          minPredictedAbundance, charge, ionType.getPolarity());

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeParameters);

      final double minScore = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold).getValue();

      if (isotopeScore < minScore) {
        return null;
      }

    }

    // MS/MS evaluation is slowest, so let's do it last
    Double msmsScore = null;
    Map<Double, String> msmsAnnotations = null;

    if (checkMSMS) {
      Scan msmsScan = peakListRow.getBestFragmentation();
      if(msmsScan != null) {
        MassList ms2MassList = msmsScan.getMassList();
        if (ms2MassList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(
              "The MS/MS scan #" + msmsScan.getScanNumber() + " in file " + msmsScan.getDataFile()
                  .getName()
              + " does not have a mass list");
          return null;
        }

        MSMSScore score =
            MSMSScoreCalculator.evaluateMSMS(cdkFormula, msmsScan, msmsParameters);

        double minMSMSScore =
            msmsParameters.getParameter(MSMSScoreParameters.msmsMinScore).getValue();

        if (score != null) {
          msmsScore = score.getScore();
          msmsAnnotations = score.getAnnotation();

          // Check the MS/MS condition
          if (msmsScore < minMSMSScore) {
            return null;
          }
        }
      }

    }

    // Create a new formula entry
   return new ResultFormula(cdkFormula, predictedIsotopePattern,
        isotopeScore, msmsScore, msmsAnnotations, searchedMass);
  }

  @Override
  public void cancel() {
    super.cancel();

    // We need to cancel the formula generator, because searching for next
    // candidate formula may take a looong time
    if (generator != null) {
      generator.cancel();
    }

  }
}
