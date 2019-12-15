/*
 * Copyright 2006-2020 The MZmine Development Team
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
package io.github.mzmine.modules.dataprocessing.id_formulapredictionpeaklist;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
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

public class FormulaPredictionPeakListTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private Range<Double> massRange;
  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;
  private IonizationType ionType;
  private double searchedMass;
  private int charge;
  private PeakList peakList;
  private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
  private ParameterSet isotopeParameters, msmsParameters, ratiosParameters, rdbeParameters;
  private MZTolerance mzTolerance;
  private String message;
  private int totalRows, finishedRows;
  private int maxBestFormulasPerPeak;
  private final double minScore;
  private final double minMSMSScore;
  private Boolean isSorting;
  private Double sortPPMFactor = 20d;
  private Double sortIsotopeFactor = 0d;
  private Double sortMSMSFactor = 0d;

  /**
   *
   * @param parameters
   * @param peakList
   * @param peakListRow
   * @param peak
   */
  FormulaPredictionPeakListTask(PeakList peakList, ParameterSet parameters) {

    /*
     * searchedMass = parameters.getParameter(
     * FormulaPredictionPeakListParameters.neutralMass).getValue();
     */
    this.peakList = peakList;
    charge = parameters.getParameter(FormulaPredictionPeakListParameters.charge).getValue();
    ionType = parameters.getParameter(FormulaPredictionPeakListParameters.ionization).getValue();
    mzTolerance =
        parameters.getParameter(FormulaPredictionPeakListParameters.mzTolerance).getValue();
    elementCounts =
        parameters.getParameter(FormulaPredictionPeakListParameters.elements).getValue();

    checkIsotopes =
        parameters.getParameter(FormulaPredictionPeakListParameters.isotopeFilter).getValue();
    isotopeParameters = parameters.getParameter(FormulaPredictionPeakListParameters.isotopeFilter)
        .getEmbeddedParameters();

    checkMSMS = parameters.getParameter(FormulaPredictionPeakListParameters.msmsFilter).getValue();
    msmsParameters = parameters.getParameter(FormulaPredictionPeakListParameters.msmsFilter)
        .getEmbeddedParameters();

    checkRDBE =
        parameters.getParameter(FormulaPredictionPeakListParameters.rdbeRestrictions).getValue();
    rdbeParameters = parameters.getParameter(FormulaPredictionPeakListParameters.rdbeRestrictions)
        .getEmbeddedParameters();

    checkRatios =
        parameters.getParameter(FormulaPredictionPeakListParameters.elementalRatios).getValue();
    ratiosParameters = parameters.getParameter(FormulaPredictionPeakListParameters.elementalRatios)
        .getEmbeddedParameters();

    maxBestFormulasPerPeak = parameters
        .getParameter(FormulaPredictionPeakListParameters.maxBestFormulasPerPeak).getValue();


    minScore = isotopeParameters
        .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold).getValue();


    minMSMSScore = msmsParameters.getParameter(MSMSScoreParameters.msmsMinScore).getValue();

    // get sorting parameters
    isSorting = parameters.getParameter(FormulaPredictionPeakListParameters.sorting).getValue();
    if (isSorting) {
      FormulaSortParameters sortParam = parameters
          .getParameter(FormulaPredictionPeakListParameters.sorting).getEmbeddedParameters();
      sortPPMFactor = sortParam.getParameter(FormulaSortParameters.MAX_PPM_WEIGHT).getValue();
      sortMSMSFactor = sortParam.getParameter(FormulaSortParameters.MSMS_SCORE_WEIGHT).getValue();
      sortIsotopeFactor =
          sortParam.getParameter(FormulaSortParameters.ISOTOPE_SCORE_WEIGHT).getValue();
    }
    message = "Formula Prediction";
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0.0;
    return (double) finishedRows / (double) totalRows;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return message;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    totalRows = peakList.getNumberOfRows();

    for (PeakListRow row : peakList.getRows()) {

      if (row.getPeakIdentities().length > 0) {
        continue;
      }

      this.searchedMass = (row.getAverageMZ() - ionType.getAddedMass()) * charge;

      message = "Formula prediction for "
          + MZmineCore.getConfiguration().getMZFormat().format(searchedMass);

      massRange = mzTolerance.getToleranceRange(searchedMass);

      IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
          massRange.upperEndpoint(), elementCounts);

      IMolecularFormula cdkFormula;

      // create a map to store ResultFormula and relative mass deviation
      // for sorting
      List<MolecularFormulaIdentity> flist = new ArrayList<>();
      while ((cdkFormula = generator.getNextFormula()) != null) {
        if (isCanceled())
          return;

        // Mass is ok, so test other constraints
        if (checkConstraints(cdkFormula, row)) {
          Double isotopeScore = calcIsotopePatternScore(cdkFormula, row);
          Double msmsScore = calcIsotopePatternScore(cdkFormula, row);
          if (getStatus().equals(TaskStatus.ERROR))
            return;

          if ((isotopeScore == null || isotopeScore >= minScore)
              && (msmsScore == null || msmsScore >= minMSMSScore)) {
            // write to map
            MolecularFormulaIdentity molf =
                new MolecularFormulaIdentity(cdkFormula, searchedMass, isotopeScore, msmsScore);
            flist.add(molf);
          }
        }
      }

      if (isCanceled())
        return;

      // sort formulas by ppm difference
      FormulaUtils.sortFormulaList(flist, sortPPMFactor, sortIsotopeFactor, sortMSMSFactor);

      // Add the new formula entry top results
      int ctr = 0;
      for (MolecularFormulaIdentity f : flist) {
        if (ctr < maxBestFormulasPerPeak) {
          f.setPropertyValue(PeakIdentity.PROPERTY_METHOD, this.getClass().getName());
          row.addPeakIdentity(f, false);
          ctr++;
        }
      }
      if (isCanceled())
        return;
      finishedRows++;

    }

    if (isCanceled())
      return;

    logger.finest("Finished formula search for all the peaks");

    setStatus(TaskStatus.FINISHED);

  }

  private boolean checkConstraints(IMolecularFormula cdkFormula, PeakListRow peakListRow) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula, ratiosParameters);
      if (!check) {
        return false;
      }
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeParameters);
      if (!check) {
        return false;
      }
    }
    return true;
  }

  /**
   * 
   * @param cdkFormula
   * @param peakListRow
   * @return null for no MSMS pattern - or a double as the score
   */
  public Double calcMSMSPatternScore(IMolecularFormula cdkFormula, PeakListRow peakListRow) {
    // MS/MS evaluation is slowest, so let's do it last
    Double msmsScore = null;
    Feature bestPeak = peakListRow.getBestPeak();
    RawDataFile dataFile = bestPeak.getDataFile();
    int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();

    if ((checkMSMS) && (msmsScanNumber > 0)) {
      Scan msmsScan = dataFile.getScan(msmsScanNumber);
      String massListName = msmsParameters.getParameter(MSMSScoreParameters.massList).getValue();
      MassList ms2MassList = msmsScan.getMassList(massListName);
      if (ms2MassList == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("The MS/MS scan #" + msmsScanNumber + " in file " + dataFile.getName()
            + " does not have a mass list called '" + massListName + "'");
        return null;
      }

      MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormula, msmsScan, msmsParameters);

      if (score != null) {
        msmsScore = score.getScore();
      }
    }
    return msmsScore;
  }

  /**
   * 
   * @param cdkFormula
   * @param peakListRow
   * @return null for no isotope pattern - or a double as the score
   */
  public Double calcIsotopePatternScore(IMolecularFormula cdkFormula, PeakListRow peakListRow) {
    // Calculate isotope similarity score
    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    IsotopePattern predictedIsotopePattern = null;
    Double isotopeScore = null;
    if ((checkIsotopes) && (detectedPattern != null)) {

      String stringFormula = MolecularFormulaManipulator.getString(cdkFormula);

      String adjustedFormula = FormulaUtils.ionizeFormula(stringFormula, ionType, charge);

      final double isotopeNoiseLevel = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopeNoiseLevel).getValue();

      final double detectedPatternHeight = detectedPattern.getHighestDataPoint().getIntensity();

      final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

      predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(adjustedFormula,
          minPredictedAbundance, charge, ionType.getPolarity());

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeParameters);
    }
    return isotopeScore;
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
