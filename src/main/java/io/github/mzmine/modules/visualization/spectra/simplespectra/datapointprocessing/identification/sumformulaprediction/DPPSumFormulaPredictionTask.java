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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.identification.sumformulaprediction;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResultsLabelGenerator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPSumFormulaResult;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.utility.DynamicParameterUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.SpectraPlotUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Predicts sum formulas just like io.github.mzmine.modules.peaklistmethods.identification.formulaprediction
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class DPPSumFormulaPredictionTask extends DataPointProcessingTask {

  private final Double minIsotopeScore;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  private final Logger logger = Logger.getLogger(DPPSumFormulaPredictionTask.class.getName());
  private final MZTolerance mzTolerance;
  private final int foundFormulas = 0;
  private final IonizationType ionType;
  private final int charge;
  private final double noiseLevel;
  private final boolean checkRatios;
  private final boolean checkRDBE;
  private final boolean checkIsotopes;
  private final int numResults;
  private final MolecularFormulaRange elementCounts;
  int currentIndex;
  private Boolean checkMultiple;
  private Boolean checkNOPS;
  private Boolean checkHC;
  private Boolean rdbeIsInteger;
  private Range<Double> rdbeRange;
  private MolecularFormulaGenerator generator;
  private Range<Double> massRange;

  public DPPSumFormulaPredictionTask(MassSpectrum spectrum, SpectraPlot targetPlot,
      ParameterSet parameterSet, DataPointProcessingController controller,
      TaskStatusListener listener) {
    super(spectrum, targetPlot, parameterSet, controller, listener);

    charge = parameterSet.getParameter(DPPSumFormulaPredictionParameters.charge).getValue();
    noiseLevel = parameterSet.getParameter(DPPSumFormulaPredictionParameters.noiseLevel).getValue();
    ionType = parameterSet.getParameter(DPPSumFormulaPredictionParameters.ionization).getValue();

    checkRDBE = parameterSet.getParameter(DPPSumFormulaPredictionParameters.rdbeRestrictions)
        .getValue();
    if (checkRDBE) {
      RDBERestrictionParameters rdbeParameters = parameterSet
          .getParameter(DPPSumFormulaPredictionParameters.rdbeRestrictions).getEmbeddedParameters();
      rdbeIsInteger = rdbeParameters.getValue(RDBERestrictionParameters.rdbeWholeNum);
      rdbeRange = rdbeParameters.getValue(RDBERestrictionParameters.rdbeRange);
    }

    checkIsotopes = parameterSet.getParameter(DPPSumFormulaPredictionParameters.isotopeFilter)
        .getValue();
    final ParameterSet isoParam = parameterSet
        .getParameter(DPPSumFormulaPredictionParameters.isotopeFilter).getEmbeddedParameters();

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

    checkRatios = parameterSet.getParameter(DPPSumFormulaPredictionParameters.elementalRatios)
        .getValue();
    if (checkRatios) {
      ElementalHeuristicParameters ratiosParameters = parameterSet
          .getParameter(DPPSumFormulaPredictionParameters.elementalRatios).getEmbeddedParameters();
      checkHC = ratiosParameters.getValue(ElementalHeuristicParameters.checkHC);
      checkNOPS = ratiosParameters.getValue(ElementalHeuristicParameters.checkNOPS);
      checkMultiple = ratiosParameters.getValue(ElementalHeuristicParameters.checkMultiple);
    }
    elementCounts = parameterSet.getParameter(DPPSumFormulaPredictionParameters.elements)
        .getValue();

    mzTolerance = parameterSet.getParameter(DPPSumFormulaPredictionParameters.mzTolerance)
        .getValue();

    setDisplayResults(
        parameterSet.getParameter(DPPSumFormulaPredictionParameters.displayResults).getValue());
    Color c = FxColorUtil.fxColorToAWT(
        parameterSet.getParameter(DPPSumFormulaPredictionParameters.datasetColor).getValue());
    setColor(c);

    numResults = parameterSet.getParameter(DPPSumFormulaPredictionParameters.displayResults)
        .getEmbeddedParameter().getValue();

    currentIndex = 0;
  }

  @Override
  public double getFinishedPercentage() {
    if (getDataPoints().getNumberOfDataPoints() == 0) {
      return 0;
    }
    return ((double) currentIndex / getDataPoints().getNumberOfDataPoints());
  }

  @Override
  public void run() {
    if (!checkParameterSet() || !checkValues()) {
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (getDataPoints().getNumberOfDataPoints() == 0) {
      logger.info(
          "Data point/Spectra processing: 0 data points were passed to " + getTaskDescription()
          + " Please check the parameters.");
      setStatus(TaskStatus.CANCELED);
      return;
    }
    /*
     * if (!(getDataPoints() instanceof ProcessedDataPoint[])) {
     *
     * logger.info("Data point/Spectra processing: The array of data points passed to " +
     * getTaskDescription() +
     * " is not an instance of ProcessedDataPoint. Make sure to run mass detection first.");
     * setStatus(TaskStatus.CANCELED); return; }
     */

    setStatus(TaskStatus.PROCESSING);

    List<ProcessedDataPoint> resultList = new ArrayList<>();

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    for (int i = 0; i < dataPoints.getNumberOfDataPoints(); i++) {

      if (isCanceled()) {
        return;
      }

      if (dataPoints.getIntensityValue(i) < noiseLevel) {
        continue;
      }

      massRange = mzTolerance
          .getToleranceRange((dataPoints.getMzValue(i) - ionType.getAddedMass()) / charge);

      ProcessedDataPoint tmp = null; // dataPoints[i]
      MolecularFormulaRange elCounts = DynamicParameterUtils
          .buildFormulaRangeOnIsotopePatternResults(tmp, elementCounts);

      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
          massRange.upperEndpoint(), elCounts);

      List<PredResult> formulas = generateFormulas(tmp, massRange, charge, generator);

      DPPSumFormulaResult[] results = genereateResults(formulas, numResults);

      tmp.addAllResults(results);
      resultList.add(tmp);
      currentIndex++;
    }

    // setResults((ProcessedDataPoint[]) dataPoints);
    setResults(resultList.toArray(new ProcessedDataPoint[0]));
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Predicts sum formulas for a given m/z and parameters.
   *
   * @param massRange Mass range for sum formulas
   * @param charge    Charge of the molecule
   * @param generator instance of MolecularFormulaGenerator
   * @return List<PredResult> sorted by relative ppm difference and String of the formula.
   */
  private List<PredResult> generateFormulas(ProcessedDataPoint dp, Range<Double> massRange,
      int charge, MolecularFormulaGenerator generator) {

    List<PredResult> possibleFormulas = new ArrayList<>();

    IMolecularFormula cdkFormula;

    while ((cdkFormula = generator.getNextFormula()) != null) {

      // Mass is ok, so test other constraints
      if (!checkConstraints(cdkFormula)) {
        continue;
      }

      String formula = MolecularFormulaManipulator.getString(cdkFormula);

      // calc rel mass deviation
      double relMassDev = ((((dp.getMZ() - //
                              ionType.getAddedMass()) / charge)//
                            - (FormulaUtils.calculateExactMass(//
          MolecularFormulaManipulator.getString(cdkFormula))) / charge) / ((dp.getMZ() //
                                                                            - ionType
                                                                                .getAddedMass())
                                                                           / charge)) * 1000000;

      // write to map
      if (checkIsotopes && dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
        double score = getIsotopeSimilarityScore(cdkFormula,
            (IsotopePattern) dp.getFirstResultByType(ResultType.ISOTOPEPATTERN).getValue());
        possibleFormulas.add(new PredResult(relMassDev, formula, score));
      } else {
        possibleFormulas.add(new PredResult(relMassDev, formula));
      }
    }

    evaluateAndSortFormulas(dp, possibleFormulas);

    return possibleFormulas;
  }

  /**
   * Put additional evaluations here. E.g. adduct checks or so
   *
   * @param dp
   * @param possibleFormulas
   */
  private void evaluateAndSortFormulas(ProcessedDataPoint dp, List<PredResult> possibleFormulas) {

    // sort by score or ppm
    if (checkIsotopes && dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
      possibleFormulas.sort((Comparator<PredResult>) (PredResult o1, PredResult o2) -> {
        return -1 * Double.compare(Math.abs(o1.score), Math.abs(o2.score)); // *-1 to sort
        // descending
      });
    } else {
      possibleFormulas.sort((Comparator<PredResult>) (PredResult o1, PredResult o2) -> {
        return Double.compare(Math.abs(o1.ppm), Math.abs(o2.ppm));
      });
    }
  }

  private DPPSumFormulaResult[] genereateResults(List<PredResult> formulas, int n) {
    if (formulas.size() < n) {
      n = formulas.size();
    }

    DPPSumFormulaResult[] results = new DPPSumFormulaResult[n];

    for (int i = 0; i < results.length; i++) {
      results[i] = new DPPSumFormulaResult(formulas.get(i).formula, formulas.get(i).ppm,
          formulas.get(i).score);
    }

    return results;
  }

  private float getIsotopeSimilarityScore(IMolecularFormula cdkFormula,
      IsotopePattern detectedPattern) {

    IsotopePattern predictedIsotopePattern = null;
    final IMolecularFormula clonedFormula = FormulaUtils.cloneFormula(cdkFormula);
    ionType.ionizeFormula(clonedFormula);

    Integer isotopeBasePeak = detectedPattern.getBasePeakIndex();
    if (isotopeBasePeak == null) {
      return 0f;
    }
    final double detectedPatternHeight = detectedPattern.getBasePeakIntensity();

    final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

    predictedIsotopePattern = IsotopePatternCalculator
        .calculateIsotopePattern(clonedFormula, minPredictedAbundance, charge,
            ionType.getPolarity());

    return IsotopePatternScoreCalculator
        .getSimilarityScore(detectedPattern, predictedIsotopePattern, isotopeMZTolerance,
            isotopeNoiseLevel);
  }

  private boolean checkConstraints(IMolecularFormula cdkFormula) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker
          .checkFormula(cdkFormula, checkHC, checkNOPS, checkMultiple);
      if (!check) {
        return false;
      }
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeRange, rdbeIsInteger);
      if (!check) {
        return false;
      }
    }

    return true;
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

  @Override
  public void displayResults() {
    if (displayResults || getController().isLastTaskRunning()) {
      SpectraPlotUtils.clearDatasetLabelGenerators(getTargetPlot(), DPPResultsDataSet.class);
      DPPResultsLabelGenerator labelGen = new DPPResultsLabelGenerator(getTargetPlot());
      getTargetPlot().addDataSet(
          new DPPResultsDataSet("Sum formula prediction results (" + getResults().length + ")",
              getResults()), color, false, labelGen, true);
    }
  }

  private static class PredResult {

    public double ppm;
    public String formula;
    public double score;

    PredResult(double ppm, String formula) {
      this.ppm = ppm;
      this.formula = formula;
    }

    PredResult(double ppm, String formula, double score) {
      this.ppm = ppm;
      this.formula = formula;
      this.score = score;
    }
  }
}
