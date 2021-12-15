/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
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
import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaPredictionTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FormulaPredictionTask.class.getName());
  protected final double searchedMass;
  protected final int charge;
  protected final boolean checkMSMS;
  protected final ParameterSet parameters;
  private final Range<Double> massRange;
  private final MolecularFormulaRange elementCounts;
  private final IonizationType ionType;
  private final boolean checkIsotopes;
  private final boolean checkRatios;
  private final boolean checkRDBE;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  private final Double minIsotopeScore;
  protected ResultWindowFX resultWindowFX;
  protected IsotopePattern detectedPattern;
  protected MassSpectrum msmsScan;
  private Range<Double> rdbeRange;
  private Boolean rdbeIsInteger;
  private Boolean checkHCRatio;
  private Boolean checkMultipleRatios;
  private Boolean checkNOPSRatio;
  private Double msmsMinScore;
  private int topNmsmsSignals;
  private MZTolerance msmsMzTolerance;
  private MolecularFormulaGenerator generator;
  private int foundFormulas = 0;
  private Double precursorMz;


  FormulaPredictionTask(ParameterSet parameters, @NotNull Instant moduleCallDate,
      IsotopePattern pattern, MassSpectrum msmsScan) {
    this(parameters, moduleCallDate, null, pattern, msmsScan);
  }

  FormulaPredictionTask(ParameterSet parameters, @NotNull Instant moduleCallDate,
      ResultWindowFX resultWindowFX, IsotopePattern pattern, MassSpectrum msmsScan) {
    super(null, moduleCallDate); // no new data stored -> null
    this.resultWindowFX = resultWindowFX;
    this.parameters = parameters;
    this.detectedPattern = pattern;
    this.msmsScan = msmsScan;

    searchedMass = parameters.getParameter(FormulaPredictionParameters.neutralMass).getValue();
    precursorMz = parameters.getParameter(FormulaPredictionParameters.neutralMass).getIonMass();
    charge = parameters.getParameter(FormulaPredictionParameters.neutralMass).getCharge();
    ionType = parameters.getParameter(FormulaPredictionParameters.neutralMass).getIonType();
    MZTolerance mzTolerance = parameters.getParameter(FormulaPredictionParameters.mzTolerance)
        .getValue();
    massRange = mzTolerance.getToleranceRange(searchedMass);
    elementCounts = parameters.getParameter(FormulaPredictionParameters.elements).getValue();

    checkIsotopes = parameters.getParameter(FormulaPredictionParameters.isotopeFilter).getValue();
    final ParameterSet isoParam = parameters.getParameter(FormulaPredictionParameters.isotopeFilter)
        .getEmbeddedParameters();

    if (checkIsotopes) {
      minIsotopeScore = isoParam.getValue(
          IsotopePatternScoreParameters.isotopePatternScoreThreshold);
      isotopeNoiseLevel = isoParam.getValue(IsotopePatternScoreParameters.isotopeNoiseLevel);
      isotopeMZTolerance = isoParam.getValue(IsotopePatternScoreParameters.mzTolerance);
    } else {
      minIsotopeScore = null;
      isotopeNoiseLevel = null;
      isotopeMZTolerance = null;
    }

    checkMSMS = parameters.getParameter(FormulaPredictionParameters.msmsFilter).getValue();
    if (checkMSMS) {
      ParameterSet msmsParam = parameters.getParameter(FormulaPredictionParameters.msmsFilter)
          .getEmbeddedParameters();

      msmsMinScore = msmsParam.getValue(MSMSScoreParameters.msmsMinScore);
      topNmsmsSignals =
          msmsParam.getValue(MSMSScoreParameters.useTopNSignals) ? msmsParam.getParameter(
              MSMSScoreParameters.useTopNSignals).getEmbeddedParameter().getValue() : -1;
      msmsMzTolerance = msmsParam.getValue(MSMSScoreParameters.msmsTolerance);
    }

    checkRDBE = parameters.getParameter(FormulaPredictionParameters.rdbeRestrictions).getValue();
    if (checkRDBE) {
      ParameterSet rdbeParameters = parameters.getParameter(
          FormulaPredictionParameters.rdbeRestrictions).getEmbeddedParameters();
      rdbeRange = rdbeParameters.getValue(RDBERestrictionParameters.rdbeRange);
      rdbeIsInteger = rdbeParameters.getValue(RDBERestrictionParameters.rdbeWholeNum);
    }

    checkRatios = parameters.getParameter(FormulaPredictionParameters.elementalRatios).getValue();
    if (checkRatios) {
      final ParameterSet elementRatiosParam = parameters.getParameter(
          FormulaPredictionParameters.elementalRatios).getEmbeddedParameters();
      checkHCRatio = elementRatiosParam.getValue(ElementalHeuristicParameters.checkHC);
      checkMultipleRatios = elementRatiosParam.getValue(ElementalHeuristicParameters.checkMultiple);
      checkNOPSRatio = elementRatiosParam.getValue(ElementalHeuristicParameters.checkNOPS);
    }
  }

  @Override
  public double getFinishedPercentage() {
    if (generator == null) {
      return 0;
    }
    return generator.getFinishedPercentage();
  }

  @Override
  public String getTaskDescription() {
    return "Formula prediction for " + MZmineCore.getConfiguration().getMZFormat()
        .format(searchedMass);
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    if (resultWindowFX == null) {
      Platform.runLater(() -> {
        resultWindowFX = new ResultWindowFX(
            "Searching for " + MZmineCore.getConfiguration().getMZFormat().format(searchedMass),
            null, searchedMass, charge, this, parameters);
        resultWindowFX.show();

      });
    }

    logger.finest("Starting search for formulas for " + massRange + " Da");

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

        if (isCanceled()) {
          return;
        }

        // Mass is ok, so test other constraints
        checkConstraints(cdkFormula);


      }

      if (isCanceled()) {
        return;
      }

      logger.finest("Finished formula search for " + massRange + " m/z, found " + foundFormulas
                    + " formulas");

      MZmineCore.runLater(() -> resultWindowFX.setTitle(
          "Finished searching for " + MZmineCore.getConfiguration().getMZFormat()
              .format(searchedMass) + " amu, " + foundFormulas + " formulas found"));
    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
    }

    setStatus(TaskStatus.FINISHED);
  }


  private void checkConstraints(IMolecularFormula cdkFormula) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula, checkHCRatio,
          checkNOPSRatio, checkMultipleRatios);
      if (!check) {
        return;
      }
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeRange, rdbeIsInteger);
      if (!check) {
        return;
      }
    }

    // Calculate isotope similarity score
    final String stringFormula = MolecularFormulaManipulator.getString(cdkFormula);

    final String adjustedFormula = FormulaUtils.ionizeFormula(stringFormula, ionType, charge);

    // Fixed min abundance
    final double minPredictedAbundance = 0.00001;

    final IsotopePattern predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(
        adjustedFormula, minPredictedAbundance, charge, ionType.getPolarity());

    Float isotopeScore = null;
    if (checkIsotopes && detectedPattern != null && predictedIsotopePattern != null) {

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeMZTolerance, isotopeNoiseLevel);

      if (isotopeScore < minIsotopeScore) {
        return;
      }

    }

    // MS/MS evaluation is slowest, so let's do it last
    Float msmsScore = null;
    Map<Double, String> msmsAnnotations = null;

    if ((checkMSMS) && (msmsScan != null)) {
      MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormula, msmsScan, precursorMz, charge,
          msmsMzTolerance, topNmsmsSignals);

      if (score != null) {
        msmsScore = score.getScore();
        msmsAnnotations = score.getAnnotation();

        // Check the MS/MS condition
        if (msmsScore < msmsMinScore) {
          return;
        }
      }
    }

    // Create a new formula entry
    final ResultFormula resultEntry = new ResultFormula(cdkFormula, predictedIsotopePattern,
        isotopeScore, msmsScore, msmsAnnotations, searchedMass);

    // Add the new formula entry
    // Need to execute in runLater because result window might not have been created due to earlier runLater.
    MZmineCore.runLater(() -> {
      if (!isCanceled()) {
        resultWindowFX.addNewListItem(resultEntry);
      }
    });

    foundFormulas++;

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
