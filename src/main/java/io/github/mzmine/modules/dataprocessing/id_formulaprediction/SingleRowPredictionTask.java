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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
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

public class SingleRowPredictionTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final Range<Double> massRange;
  private final MolecularFormulaRange elementCounts;
  private final IonizationType ionType;
  private final double searchedMass;
  private final int charge;
  private final FeatureListRow peakListRow;
  private final boolean checkIsotopes;
  private final boolean checkMSMS;
  private final boolean checkRatios;
  private final boolean checkRDBE;
  private final ParameterSet parameters;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  private final Double minIsotopeScore;
  protected ResultWindowFX resultWindowFX;
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


  SingleRowPredictionTask(ParameterSet parameters, FeatureListRow peakListRow,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.peakListRow = peakListRow;
    this.parameters = parameters;

    searchedMass = parameters.getParameter(FormulaPredictionParameters.neutralMass).getValue();
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
      minIsotopeScore = isoParam
          .getValue(IsotopePatternScoreParameters.isotopePatternScoreThreshold);
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
      topNmsmsSignals = msmsParam.getValue(MSMSScoreParameters.useTopNSignals) ? msmsParam
          .getParameter(MSMSScoreParameters.useTopNSignals).getEmbeddedParameter().getValue() : -1;
      msmsMzTolerance = msmsParam.getValue(MSMSScoreParameters.msmsTolerance);
    }

    checkRDBE = parameters.getParameter(FormulaPredictionParameters.rdbeRestrictions).getValue();
    if (checkRDBE) {
      ParameterSet rdbeParameters = parameters
          .getParameter(FormulaPredictionParameters.rdbeRestrictions).getEmbeddedParameters();
      rdbeRange = rdbeParameters.getValue(RDBERestrictionParameters.rdbeRange);
      rdbeIsInteger = rdbeParameters.getValue(RDBERestrictionParameters.rdbeWholeNum);
    }

    checkRatios = parameters.getParameter(FormulaPredictionParameters.elementalRatios).getValue();
    if (checkRatios) {
      final ParameterSet elementRatiosParam = parameters
          .getParameter(FormulaPredictionParameters.elementalRatios).getEmbeddedParameters();
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

    Platform.runLater(() -> {
      resultWindowFX = new ResultWindowFX(
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

    peakListRow.getFeatureList().getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(FormulaPredictionModule.class, parameters,
            getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);


  }


  private void checkConstraints(IMolecularFormula cdkFormula) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker
          .checkFormula(cdkFormula, checkHCRatio, checkNOPSRatio, checkMultipleRatios);
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
    final IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();

    final IMolecularFormula clonedFormula = FormulaUtils.cloneFormula(cdkFormula);
    ionType.ionizeFormula(clonedFormula);

    // Fixed min abundance
    final double minPredictedAbundance = 0.00001;

    final IsotopePattern predictedIsotopePattern = IsotopePatternCalculator
        .calculateIsotopePattern(clonedFormula, minPredictedAbundance, charge,
            ionType.getPolarity());

    Float isotopeScore = null;
    if (checkIsotopes && detectedPattern != null && predictedIsotopePattern != null) {

      isotopeScore = IsotopePatternScoreCalculator
          .getSimilarityScore(detectedPattern, predictedIsotopePattern, isotopeMZTolerance,
              isotopeNoiseLevel);

      if (isotopeScore < minIsotopeScore) {
        return;
      }

    }

    // MS/MS evaluation is slowest, so let's do it last
    Float msmsScore = null;
    Feature bestPeak = peakListRow.getBestFeature();
    RawDataFile dataFile = bestPeak.getRawDataFile();
    Map<DataPoint, String> msmsAnnotations = null;
    Scan msmsScan = bestPeak.getMostIntenseFragmentScan();

    if ((checkMSMS) && (msmsScan != null)) {
      MassList ms2MassList = msmsScan.getMassList();
      if (ms2MassList == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "The MS/MS scan #" + msmsScan.getScanNumber() + " in file " + dataFile.getName()
            + " does not have a mass list");
        return;
      }

      MSMSScore score = MSMSScoreCalculator
          .evaluateMSMS(cdkFormula, msmsScan, msmsMzTolerance, topNmsmsSignals);

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
    MZmineCore.runLater(() -> resultWindowFX.addNewListItem(resultEntry));

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
