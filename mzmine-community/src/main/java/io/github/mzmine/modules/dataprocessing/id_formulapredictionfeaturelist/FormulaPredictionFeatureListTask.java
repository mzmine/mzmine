/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
package io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

public class FormulaPredictionFeatureListTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final MolecularFormulaRange elementCounts;
  private final Double minIsotopeScore;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  private final IonizationType ionType;
  private final ModularFeatureList featureList;
  private final boolean checkIsotopes;
  private final boolean checkMSMS;
  private final boolean checkRatios;
  private final boolean checkRDBE;
  private final ParameterSet parameters;
  private final MZTolerance mzTolerance;
  private final int maxBestFormulasPerFeature;
  private final Boolean isSorting;
  private float sortPPMFactor;
  private float sortMSMSFactor;
  private float sortIsotopeFactor;
  private MolecularFormulaGenerator generator;
  private String message;
  private int totalRows, finishedRows;
  private List<ResultFormula> resultingFormulas;
  private Range<Double> rdbeRange;
  private Boolean rdbeIsInteger;
  private Boolean checkHCRatio;
  private Boolean checkMultipleRatios;
  private Boolean checkNOPSRatio;
  private Double msmsMinScore;
  private int topNmsmsSignals;
  private MZTolerance msmsMzTolerance;

  /**
   * @param parameters
   * @param featureList
   */
  FormulaPredictionFeatureListTask(ModularFeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.featureList = featureList;
    ionType = parameters.getParameter(FormulaPredictionFeatureListParameters.ionization).getValue();
    mzTolerance = parameters.getParameter(FormulaPredictionFeatureListParameters.mzTolerance)
        .getValue();
    elementCounts = parameters.getParameter(FormulaPredictionFeatureListParameters.elements)
        .getValue();

    checkIsotopes = parameters.getParameter(FormulaPredictionFeatureListParameters.isotopeFilter)
        .getValue();
    final ParameterSet isoParam = parameters.getParameter(
        FormulaPredictionFeatureListParameters.isotopeFilter).getEmbeddedParameters();

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

    checkMSMS = parameters.getParameter(FormulaPredictionFeatureListParameters.msmsFilter)
        .getValue();
    if (checkMSMS) {
      ParameterSet msmsParam = parameters.getParameter(
          FormulaPredictionFeatureListParameters.msmsFilter).getEmbeddedParameters();

      msmsMinScore = msmsParam.getValue(MSMSScoreParameters.msmsMinScore);
      topNmsmsSignals =
          msmsParam.getValue(MSMSScoreParameters.useTopNSignals) ? msmsParam.getParameter(
              MSMSScoreParameters.useTopNSignals).getEmbeddedParameter().getValue() : -1;
      msmsMzTolerance = msmsParam.getValue(MSMSScoreParameters.msmsTolerance);
    }

    checkRDBE = parameters.getParameter(FormulaPredictionFeatureListParameters.rdbeRestrictions)
        .getValue();
    if (checkRDBE) {
      ParameterSet rdbeParameters = parameters.getParameter(
          FormulaPredictionFeatureListParameters.rdbeRestrictions).getEmbeddedParameters();
      rdbeRange = rdbeParameters.getValue(RDBERestrictionParameters.rdbeRange);
      rdbeIsInteger = rdbeParameters.getValue(RDBERestrictionParameters.rdbeWholeNum);
    }

    checkRatios = parameters.getParameter(FormulaPredictionFeatureListParameters.elementalRatios)
        .getValue();
    if (checkRatios) {
      final ParameterSet elementRatiosParam = parameters.getParameter(
          FormulaPredictionFeatureListParameters.elementalRatios).getEmbeddedParameters();
      checkHCRatio = elementRatiosParam.getValue(ElementalHeuristicParameters.checkHC);
      checkMultipleRatios = elementRatiosParam.getValue(ElementalHeuristicParameters.checkMultiple);
      checkNOPSRatio = elementRatiosParam.getValue(ElementalHeuristicParameters.checkNOPS);
    }

    maxBestFormulasPerFeature = parameters.getParameter(
        FormulaPredictionFeatureListParameters.maxBestFormulasPerFeature).getValue();

    // get sorting parameters
    isSorting = parameters.getParameter(FormulaPredictionFeatureListParameters.sorting).getValue();
    if (isSorting) {
      FormulaSortParameters sortParam = parameters.getParameter(
          FormulaPredictionFeatureListParameters.sorting).getEmbeddedParameters();
      sortPPMFactor = sortParam.getValue(FormulaSortParameters.MAX_PPM_WEIGHT).floatValue();
      sortMSMSFactor = sortParam.getValue(FormulaSortParameters.MSMS_SCORE_WEIGHT).floatValue();
      sortIsotopeFactor = sortParam.getValue(FormulaSortParameters.ISOTOPE_SCORE_WEIGHT)
          .floatValue();
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

    featureList.addRowType(DataTypes.get(
        io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType.class));

    for (FeatureListRow row : featureList.getRows()) {

      if (row.getPeakIdentities().size() > 0) {
        continue;
      }
      this.resultingFormulas = new ArrayList<>();

      double searchedMass =
          (row.getAverageMZ() - ionType.getAddedMass()) * Math.abs(ionType.getCharge());

      message = "Formula prediction for " + MZmineCore.getConfiguration().getMZFormat()
          .format(searchedMass);

      Range<Double> massRange = mzTolerance.getToleranceRange(searchedMass);

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
      if (!resultingFormulas.isEmpty()) {
        FormulaUtils.sortFormulaList(resultingFormulas, sortPPMFactor, sortIsotopeFactor,
            sortMSMSFactor);
        row.setFormulas(resultingFormulas.subList(0,
            Math.min(resultingFormulas.size(), maxBestFormulasPerFeature)));
      }

      if (isCanceled()) {
        return;
      }
      finishedRows++;
    }

    if (isCanceled()) {
      return;
    }

    featureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(FormulaPredictionFeatureListModule.class, parameters,
            getModuleCallDate()));

    logger.finest("Finished formula search for all the features");

    setStatus(TaskStatus.FINISHED);

  }

  /**
   * @param cdkFormula
   * @return null if molecular formula does not match requirements
   */
  private ResultFormula checkConstraints(IMolecularFormula cdkFormula, FeatureListRow peakListRow,
      double searchedMass) {

    // Check elemental ratios
    if (checkRatios && !ElementalHeuristicChecker.checkFormula(cdkFormula, checkHCRatio,
        checkNOPSRatio, checkMultipleRatios)) {
      return null;
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null) && !RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeRange,
        rdbeIsInteger)) {
      return null;
    }

    // Calculate isotope similarity score
    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    IsotopePattern predictedIsotopePattern = null;
    Float isotopeScore = null;
    if ((checkIsotopes) && (detectedPattern != null)) {

      final IMolecularFormula clonedFormula = FormulaUtils.cloneFormula(cdkFormula);
      ionType.ionizeFormula(clonedFormula);

      final double detectedPatternHeight = detectedPattern.getBasePeakIntensity();
      final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

      predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(clonedFormula,
          minPredictedAbundance, ionType.getCharge(), ionType.getPolarity());

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeMZTolerance, isotopeNoiseLevel);

      if (isotopeScore < minIsotopeScore) {
        return null;
      }

    }

    // MS/MS evaluation is slowest, so let's do it last
    Float msmsScore = null;
    Map<DataPoint, String> msmsAnnotations = null;

    if (checkMSMS) {
      Scan msmsScan = peakListRow.getMostIntenseFragmentScan();
      if (msmsScan != null) {
        MassList ms2MassList = msmsScan.getMassList();
        if (ms2MassList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(
              "The MS/MS scan #" + msmsScan.getScanNumber() + " in file " + msmsScan.getDataFile()
                  .getName() + " does not have a mass list");
          return null;
        }

        MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormula, msmsScan, msmsMzTolerance,
            topNmsmsSignals);

        if (score != null) {
          msmsScore = score.explainedIntensity();
          msmsAnnotations = score.annotation();

          // Check the MS/MS condition
          if (msmsScore < msmsMinScore) {
            return null;
          }
        }
      }

    }

    // Create a new formula entry
    return new ResultFormula(cdkFormula, predictedIsotopePattern, isotopeScore, msmsScore,
        msmsAnnotations, searchedMass);
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
