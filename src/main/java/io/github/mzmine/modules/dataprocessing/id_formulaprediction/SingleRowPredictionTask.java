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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import javafx.application.Platform;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
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
import java.util.concurrent.CountDownLatch;

public class SingleRowPredictionTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private Range<Double> massRange;
  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;

  private int foundFormulas = 0;
  private IonizationType ionType;
  private double searchedMass;
  private int charge;
  private PeakListRow peakListRow;
  private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
  private ParameterSet isotopeParameters, msmsParameters, ratiosParameters, rdbeParameters;
  ResultWindowFX resultWindowFX;


  /**
   *
   * @param parameters
   * @param peakListRow
=   */
  SingleRowPredictionTask(ParameterSet parameters, PeakListRow peakListRow) {

    searchedMass = parameters.getParameter(FormulaPredictionParameters.neutralMass).getValue();
    charge = parameters.getParameter(FormulaPredictionParameters.neutralMass).getCharge();
    ionType = parameters.getParameter(FormulaPredictionParameters.neutralMass).getIonType();
    MZTolerance mzTolerance =
        parameters.getParameter(FormulaPredictionParameters.mzTolerance).getValue();

    checkIsotopes = parameters.getParameter(FormulaPredictionParameters.isotopeFilter).getValue();
    isotopeParameters =
        parameters.getParameter(FormulaPredictionParameters.isotopeFilter).getEmbeddedParameters();

    checkMSMS = parameters.getParameter(FormulaPredictionParameters.msmsFilter).getValue();
    msmsParameters =
        parameters.getParameter(FormulaPredictionParameters.msmsFilter).getEmbeddedParameters();

    checkRDBE = parameters.getParameter(FormulaPredictionParameters.rdbeRestrictions).getValue();
    rdbeParameters = parameters.getParameter(FormulaPredictionParameters.rdbeRestrictions)
        .getEmbeddedParameters();

    checkRatios = parameters.getParameter(FormulaPredictionParameters.elementalRatios).getValue();
    ratiosParameters = parameters.getParameter(FormulaPredictionParameters.elementalRatios)
        .getEmbeddedParameters();

    massRange = mzTolerance.getToleranceRange(searchedMass);

    elementCounts = parameters.getParameter(FormulaPredictionParameters.elements).getValue();

    this.peakListRow = peakListRow;

  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (generator == null)
      return 0;
    return generator.getFinishedPercentage();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Formula prediction for "
        + MZmineCore.getConfiguration().getMZFormat().format(searchedMass);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    Platform.runLater(()->{
              resultWindowFX  = new ResultWindowFX(
              "Searching for " + MZmineCore.getConfiguration().getMZFormat().format(searchedMass),
              peakListRow, searchedMass, charge, this);
      resultWindowFX.show();

    });

    logger.finest("Starting search for formulas for " + massRange + " Da");

    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    if ((checkIsotopes) && (detectedPattern == null)) {
      final String msg = "Cannot calculate isotope pattern scores, because selected"
          + " peak does not have any isotopes. Have you run the isotope peak grouper?";
      MZmineCore.getDesktop().displayMessage(null, msg);
    }


    try {

      IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
              massRange.upperEndpoint(), elementCounts);

      IMolecularFormula cdkFormula;
      while ((cdkFormula = generator.getNextFormula()) != null) {

        if (isCanceled())
          return;

        // Mass is ok, so test other constraints
        checkConstraints(cdkFormula);


      }

      if (isCanceled())
        return;

      logger.finest("Finished formula search for " + massRange + " m/z, found " + foundFormulas + " formulas");

      Platform.runLater(() -> { resultWindowFX.setTitle("Finished searching for "
                + MZmineCore.getConfiguration().getMZFormat().format(searchedMass) + " amu, "
                + foundFormulas + " formulas found");
      });
    }
    catch (Exception e){
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
    }

    setStatus(TaskStatus.FINISHED);


  }



  private void checkConstraints(IMolecularFormula cdkFormula) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula, ratiosParameters);
      if (!check)
        return;
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeParameters);
      if (!check)
        return;
    }

    // Calculate isotope similarity score
    final IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();

    final String stringFormula = MolecularFormulaManipulator.getString(cdkFormula);

    final String adjustedFormula = FormulaUtils.ionizeFormula(stringFormula, ionType, charge);

    // Fixed min abundance
    final double minPredictedAbundance = 0.00001;

    final IsotopePattern predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(
        adjustedFormula, minPredictedAbundance, charge, ionType.getPolarity());

    Double isotopeScore = null;
    if ((checkIsotopes) && (detectedPattern != null)) {

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeParameters);

      final double minScore = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold).getValue();

      if (isotopeScore < minScore)
        return;

    }

    // MS/MS evaluation is slowest, so let's do it last
    Double msmsScore = null;
    Feature bestPeak = peakListRow.getBestPeak();
    RawDataFile dataFile = bestPeak.getDataFile();
    Map<DataPoint, String> msmsAnnotations = null;
    int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();

    if ((checkMSMS) && (msmsScanNumber > 0)) {
      Scan msmsScan = dataFile.getScan(msmsScanNumber);
      String massListName = msmsParameters.getParameter(MSMSScoreParameters.massList).getValue();
      MassList ms2MassList = msmsScan.getMassList(massListName);
      if (ms2MassList == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("The MS/MS scan #" + msmsScanNumber + " in file " + dataFile.getName()
            + " does not have a mass list called '" + massListName + "'");
        return;
      }

      MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormula, msmsScan, msmsParameters);

      double minMSMSScore =
          msmsParameters.getParameter(MSMSScoreParameters.msmsMinScore).getValue();

      if (score != null) {
        msmsScore = score.getScore();
        msmsAnnotations = score.getAnnotation();

        // Check the MS/MS condition
        if (msmsScore < minMSMSScore)
          return;
      }

    }

    // Create a new formula entry
    final ResultFormula resultEntry = new ResultFormula(cdkFormula, predictedIsotopePattern,
        rdbeValue, isotopeScore, msmsScore, msmsAnnotations);

    // Add the new formula entry
    resultWindowFX.addNewListItem(resultEntry);

    foundFormulas++;

  }

  @Override
  public void cancel() {
    super.cancel();

    // We need to cancel the formula generator, because searching for next
    // candidate formula may take a looong time
    if (generator != null)
      generator.cancel();

  }

}
