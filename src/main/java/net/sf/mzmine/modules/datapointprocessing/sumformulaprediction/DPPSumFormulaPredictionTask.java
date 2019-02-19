package net.sf.mzmine.modules.datapointprocessing.sumformulaprediction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResult;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPSumFormulaResult;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.sumformula.SpectraIdentificationSumFormulaParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;
import net.sf.mzmine.util.FormulaUtils;

public class DPPSumFormulaPredictionTask extends DataPointProcessingTask {

  private Logger logger = Logger.getLogger(DPPSumFormulaPredictionTask.class.getName());

  int currentIndex;

  private MZTolerance mzTolerance;
  private int foundFormulas = 0;
  private IonizationType ionType;
  private int charge;
  private boolean checkRatios;
  private boolean checkRDBE;
  private ParameterSet ratiosParameters;
  private ParameterSet rdbeParameters;

  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;
  private Range<Double> massRange;

  public DPPSumFormulaPredictionTask(DataPoint[] dataPoints, SpectraPlot targetPlot,
      ParameterSet parameterSet, DataPointProcessingController controller,
      TaskStatusListener listener) {
    super(dataPoints, targetPlot, parameterSet, controller, listener);

    charge = parameterSet.getParameter(SpectraIdentificationSumFormulaParameters.charge).getValue();
    ionType =
        parameterSet.getParameter(SpectraIdentificationSumFormulaParameters.ionization).getValue();

    checkRDBE = parameterSet
        .getParameter(SpectraIdentificationSumFormulaParameters.rdbeRestrictions).getValue();
    rdbeParameters =
        parameterSet.getParameter(SpectraIdentificationSumFormulaParameters.rdbeRestrictions)
            .getEmbeddedParameters();

    checkRatios = parameterSet
        .getParameter(SpectraIdentificationSumFormulaParameters.elementalRatios).getValue();
    ratiosParameters =
        parameterSet.getParameter(SpectraIdentificationSumFormulaParameters.elementalRatios)
            .getEmbeddedParameters();

    elementCounts =
        parameterSet.getParameter(SpectraIdentificationSumFormulaParameters.elements).getValue();

    mzTolerance =
        parameterSet.getParameter(SpectraIdentificationSumFormulaParameters.mzTolerance).getValue();

    currentIndex = 0;
  }

  @Override
  public String getTaskDescription() {
    return "Predicts sum formulas for an array of data points.";
  }

  @Override
  public double getFinishedPercentage() {
    return ((double) currentIndex / getDataPoints().length);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if(!(dataPoints instanceof ProcessedDataPoint[])) {
      setStatus(TaskStatus.ERROR);
      logger.warning("The array of data points passed to " + this.getClass().getName() 
          + " is not an instance of ProcessedDataPoint. Make sure to run mass detection first.");
      return;
    }
    
    
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
    for (int i = 0; i < dataPoints.length; i++) {

      if (isCanceled())
        return;

      massRange =
          mzTolerance.getToleranceRange((dataPoints[i].getMZ() - ionType.getAddedMass()) / charge);
      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
          massRange.upperEndpoint(), elementCounts);

      List<PredResult> formulas = generateFormulas(dataPoints[i].getMZ(), massRange, charge, generator);
     
      DPPSumFormulaResult[] results = genereateResults(formulas, 3);
      
      ((ProcessedDataPoint)dataPoints[i]).addAllResults(results);
      currentIndex++;
      
    }

    setResults((ProcessedDataPoint[])dataPoints);
    setStatus(TaskStatus.FINISHED);
  }


  private class PredResult {
    public Double ppm;
    public String formula;

    PredResult(Double ppm, String formula) {
      this.ppm = ppm;
      this.formula = formula;
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
  private List<PredResult> generateFormulas(double mz, Range<Double> massRange, int charge,
      MolecularFormulaGenerator generator) {

    List<PredResult> possibleFormulas = new ArrayList<>();

    IMolecularFormula cdkFormula;

    while ((cdkFormula = generator.getNextFormula()) != null) {

      // Mass is ok, so test other constraints
      if (checkConstraints(cdkFormula) == true) {
        String formula = MolecularFormulaManipulator.getString(cdkFormula);

        // calc rel mass deviation
        Double relMassDev = ((((mz - //
            ionType.getAddedMass()) / charge)//
            - (FormulaUtils.calculateExactMass(//
                MolecularFormulaManipulator.getString(cdkFormula))) / charge)
            / ((mz //
                - ionType.getAddedMass()) / charge))
            * 1000000;

        // write to map
        possibleFormulas.add(new PredResult(relMassDev, formula));
      }
    }

    possibleFormulas.sort((Comparator<PredResult>) (PredResult o1, PredResult o2) -> {
      return Double.compare(Math.abs(o1.ppm), Math.abs(o2.ppm));
    });

    return possibleFormulas;
  }

  private DPPSumFormulaResult[] genereateResults(List<PredResult> formulas, int n) {
    if (formulas.size() < n)
      n = formulas.size();

    DPPSumFormulaResult[] results = new DPPSumFormulaResult[n];

    for (int i = 0; i < results.length; i++) {
      results[i] = new DPPSumFormulaResult(DPPResult.KEY_SUMFORMULARESULT + " " + i,
          formulas.get(i).formula, formulas.get(i).ppm);
    }

    return results;
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
