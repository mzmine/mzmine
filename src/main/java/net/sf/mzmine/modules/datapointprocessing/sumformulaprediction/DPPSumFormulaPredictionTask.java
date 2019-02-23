/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.datapointprocessing.sumformulaprediction;

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
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPSumFormulaResult;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;
import net.sf.mzmine.util.FormulaUtils;

/**
 * Predicts sum formulas just like
 * net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPSumFormulaPredictionTask extends DataPointProcessingTask {

  private Logger logger = Logger.getLogger(DPPSumFormulaPredictionTask.class.getName());

  int currentIndex;

  private MZTolerance mzTolerance;
  private int foundFormulas = 0;
  private IonizationType ionType;
  private int charge;
  private double noiseLevel;
  private boolean checkRatios;
  private boolean checkRDBE;
  private ParameterSet ratiosParameters;
  private ParameterSet rdbeParameters;
  private ParameterSet isotopeParameters;
  private boolean checkIsotopes;

  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;
  private Range<Double> massRange;

  public DPPSumFormulaPredictionTask(DataPoint[] dataPoints, SpectraPlot targetPlot,
      ParameterSet parameterSet, DataPointProcessingController controller,
      TaskStatusListener listener) {
    super(dataPoints, targetPlot, parameterSet, controller, listener);

    charge = parameterSet.getParameter(DPPSumFormulaPredictionParameters.charge).getValue();
    noiseLevel = parameterSet.getParameter(DPPSumFormulaPredictionParameters.noiseLevel).getValue();
    ionType = parameterSet.getParameter(DPPSumFormulaPredictionParameters.ionization).getValue();

    checkRDBE =
        parameterSet.getParameter(DPPSumFormulaPredictionParameters.rdbeRestrictions).getValue();
    rdbeParameters = parameterSet.getParameter(DPPSumFormulaPredictionParameters.rdbeRestrictions)
        .getEmbeddedParameters();

    isotopeParameters = parameterSet.getParameter(DPPSumFormulaPredictionParameters.isotopeFilter)
        .getEmbeddedParameters();

    checkIsotopes =
        parameterSet.getParameter(DPPSumFormulaPredictionParameters.isotopeFilter).getValue();

    checkRatios =
        parameterSet.getParameter(DPPSumFormulaPredictionParameters.elementalRatios).getValue();
    ratiosParameters = parameterSet.getParameter(DPPSumFormulaPredictionParameters.elementalRatios)
        .getEmbeddedParameters();

    elementCounts =
        parameterSet.getParameter(DPPSumFormulaPredictionParameters.elements).getValue();

    mzTolerance =
        parameterSet.getParameter(DPPSumFormulaPredictionParameters.mzTolerance).getValue();

    currentIndex = 0;
  }

  @Override
  public String getTaskDescription() {
    return "Predicts sum formulas for an array of data points.";
  }

  @Override
  public double getFinishedPercentage() {
    if(getDataPoints().length == 0)
      return 0;
    return ((double) currentIndex / getDataPoints().length);
  }

  @Override
  public void run() {

    if (getDataPoints() == null || getDataPoints().length == 0) {
      logger.info("No data points were passed to " + this.getClass().getName() + " Please check the parameters." );
      setStatus(TaskStatus.CANCELED);
      return;
    }

    setStatus(TaskStatus.PROCESSING);

    if (!(getDataPoints() instanceof ProcessedDataPoint[])) {
      setStatus(TaskStatus.ERROR);
      logger.warning("The array of data points passed to " + this.getClass().getName()
          + " is not an instance of ProcessedDataPoint. Make sure to run mass detection first.");
      return;
    }


    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    for (int i = 0; i < dataPoints.length; i++) {

      if (isCanceled())
        return;

      if (dataPoints[i].getIntensity() < noiseLevel)
        continue;

      massRange =
          mzTolerance.getToleranceRange((dataPoints[i].getMZ() - ionType.getAddedMass()) / charge);
      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
          massRange.upperEndpoint(), elementCounts);

      List<PredResult> formulas =
          generateFormulas((ProcessedDataPoint) dataPoints[i], massRange, charge, generator);

      DPPSumFormulaResult[] results = genereateResults(formulas, 3);

      ((ProcessedDataPoint) dataPoints[i]).addAllResults(results);
      currentIndex++;
//      logger.info("Prediction result: " + dataPoints[i].getMZ() + " = " + results[0].generateLabel());
    }

    setResults((ProcessedDataPoint[]) dataPoints);
    setStatus(TaskStatus.FINISHED);
  }


  private class PredResult {
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

  /**
   * Predicts sum formulas for a given m/z and parameters.
   * 
   * @param mz m/z to generate sum formulas from
   * @param massRange Mass range for sum formulas
   * @param charge Charge of the molecule
   * @param generator instance of MolecularFormulaGenerator
   * @return List<PredResult> sorted by relative ppm difference and String of the formula.
   */
  private List<PredResult> generateFormulas(ProcessedDataPoint dp, Range<Double> massRange,
      int charge, MolecularFormulaGenerator generator) {

    List<PredResult> possibleFormulas = new ArrayList<>();

    IMolecularFormula cdkFormula;

    while ((cdkFormula = generator.getNextFormula()) != null) {

      // Mass is ok, so test other constraints
      if (!checkConstraints(cdkFormula))
        continue;

      String formula = MolecularFormulaManipulator.getString(cdkFormula);

      // calc rel mass deviation
      Double relMassDev = ((((dp.getMZ() - //
          ionType.getAddedMass()) / charge)//
          - (FormulaUtils.calculateExactMass(//
              MolecularFormulaManipulator.getString(cdkFormula))) / charge)
          / ((dp.getMZ() //
              - ionType.getAddedMass()) / charge))
          * 1000000;

      // write to map
      if (checkIsotopes && dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
        double score = getIsotopeSimilarityScore(cdkFormula,
            (IsotopePattern) dp.getFirstResultByType(ResultType.ISOTOPEPATTERN).getValue());
        possibleFormulas.add(new PredResult(relMassDev, formula, score));
      } else {
        possibleFormulas.add(new PredResult(relMassDev, formula));
      }
    }

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

    return possibleFormulas;
  }

  private DPPSumFormulaResult[] genereateResults(List<PredResult> formulas, int n) {
    if (formulas.size() < n)
      n = formulas.size();

    DPPSumFormulaResult[] results = new DPPSumFormulaResult[n];

    for (int i = 0; i < results.length; i++) {
      results[i] = new DPPSumFormulaResult(formulas.get(i).formula, formulas.get(i).ppm,
          formulas.get(i).score);
    }

    return results;
  }

  private double getIsotopeSimilarityScore(IMolecularFormula cdkFormula,
      IsotopePattern detectedPattern) {

    IsotopePattern predictedIsotopePattern = null;
    Double isotopeScore = null;
    String stringFormula = MolecularFormulaManipulator.getString(cdkFormula);

    String adjustedFormula = FormulaUtils.ionizeFormula(stringFormula, ionType, charge);

    final double isotopeNoiseLevel =
        isotopeParameters.getParameter(IsotopePatternScoreParameters.isotopeNoiseLevel).getValue();

    final double detectedPatternHeight = detectedPattern.getHighestDataPoint().getIntensity();

    final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

    predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(adjustedFormula,
        minPredictedAbundance, charge, ionType.getPolarity());

    isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
        predictedIsotopePattern, isotopeParameters);

    return isotopeScore;
  }

  private boolean checkConstraints(IMolecularFormula cdkFormula) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula, ratiosParameters);
      if (!check)
        return false;
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeParameters);
      if (!check)
        return false;
    }

    return true;
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
