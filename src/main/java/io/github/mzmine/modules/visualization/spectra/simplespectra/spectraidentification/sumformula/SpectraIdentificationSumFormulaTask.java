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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.sumformula;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.SpectraDatabaseSearchLabelGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Task for sum formula prediction in spectra.
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationSumFormulaTask extends AbstractTask {

  private static final Logger logger = Logger
      .getLogger(SpectraIdentificationSumFormulaTask.class.getName());
  private Boolean checkHC;
  private Boolean checkNOPS;
  private Boolean checkMultiple;
  private Range<Double> rdbeRange;
  private Boolean rdbeIsInteger;
  private int finishedItems = 0, numItems;

  private MZTolerance mzTolerance;
  private Scan currentScan;
  private SpectraPlot spectraPlot;
  private double noiseLevel;
  private int foundFormulas = 0;
  private IonizationType ionType;
  private int charge;
  private boolean checkRatios;
  private boolean checkRDBE;

  private Range<Double> massRange;
  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;

  /**
   * Create the task.
   *
   * @param parameters task parameters.
   */
  public SpectraIdentificationSumFormulaTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    charge = parameters.getParameter(SpectraIdentificationSumFormulaParameters.charge).getValue();
    ionType = parameters.getParameter(SpectraIdentificationSumFormulaParameters.ionization)
        .getValue();

    checkRDBE = parameters.getParameter(SpectraIdentificationSumFormulaParameters.rdbeRestrictions)
        .getValue();
    if (checkRDBE) {
      RDBERestrictionParameters rdbeParameters = parameters
          .getParameter(SpectraIdentificationSumFormulaParameters.rdbeRestrictions)
          .getEmbeddedParameters();
      rdbeRange = rdbeParameters.getValue(RDBERestrictionParameters.rdbeRange);
      rdbeIsInteger = rdbeParameters.getValue(RDBERestrictionParameters.rdbeWholeNum);
    }

    checkRatios = parameters.getParameter(SpectraIdentificationSumFormulaParameters.elementalRatios)
        .getValue();
    if (checkRatios) {
      ElementalHeuristicParameters ratiosParameters = parameters
          .getParameter(SpectraIdentificationSumFormulaParameters.elementalRatios)
          .getEmbeddedParameters();
      checkHC = ratiosParameters.getValue(ElementalHeuristicParameters.checkHC);
      checkNOPS = ratiosParameters.getValue(ElementalHeuristicParameters.checkNOPS);
      checkMultiple = ratiosParameters.getValue(ElementalHeuristicParameters.checkMultiple);
    }

    elementCounts = parameters.getParameter(SpectraIdentificationSumFormulaParameters.elements)
        .getValue();

    mzTolerance = parameters.getParameter(SpectraIdentificationSumFormulaParameters.mzTolerance)
        .getValue();

    noiseLevel = parameters.getParameter(SpectraIdentificationSumFormulaParameters.noiseLevel)
        .getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (numItems == 0) {
      return 0;
    }
    return ((double) finishedItems) / numItems;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Sum formula prediction for scan " + currentScan.getScanNumber();
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.finest("Starting search for formulas for " + massRange + " Da");

    // create mass list for scan
    double[][] massList = null;
    ArrayList<DataPoint> massListAnnotated = new ArrayList<>();
    MassDetector massDetector = null;
    ArrayList<String> allCompoundIDs = new ArrayList<>();

    // Create a new mass list for MS/MS scan. Check if spectrum is profile
    // or centroid mode
    if (currentScan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
      massDetector = new CentroidMassDetector();
      CentroidMassDetectorParameters parameters = new CentroidMassDetectorParameters();
      CentroidMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      massList = massDetector.getMassValues(currentScan, parameters);
    } else {
      massDetector = new ExactMassDetector();
      ExactMassDetectorParameters parameters = new ExactMassDetectorParameters();
      ExactMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      massList = massDetector.getMassValues(currentScan, parameters);
    }
    numItems = massList.length;
    // loop through every peak in mass list
    if (getStatus() != TaskStatus.PROCESSING) {
      return;
    }

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    for (int i = 0; i < massList.length; i++) {
      massRange = mzTolerance.getToleranceRange((massList[0][i] - ionType.getAddedMass()) / charge);
      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
          massRange.upperEndpoint(), elementCounts);

      IMolecularFormula cdkFormula;
      String annotation = "";
      // create a map to store ResultFormula and relative mass deviation
      // for sorting
      Map<Double, String> possibleFormulas = new TreeMap<>();
      while ((cdkFormula = generator.getNextFormula()) != null) {
        if (isCanceled()) {
          return;
        }

        // Mass is ok, so test other constraints
        if (checkConstraints(cdkFormula) == true) {
          String formula = MolecularFormulaManipulator.getString(cdkFormula);

          // calc rel mass deviation
          Double relMassDev = ((((massList[0][i] - //
                                  ionType.getAddedMass()) / charge)//
                                - (FormulaUtils.calculateExactMass(//
              MolecularFormulaManipulator.getString(cdkFormula))) / charge) / ((massList[0][i] //
                                                                                - ionType
                                                                                    .getAddedMass())
                                                                               / charge)) * 1000000;

          // write to map
          possibleFormulas.put(relMassDev, formula);
        }
      }

      Map<Double, String> treeMap = new TreeMap<>(
          (Comparator<Double>) (o1, o2) -> Double.compare(Math.abs(o1), Math.abs(o2)));
      treeMap.putAll(possibleFormulas);

      // get top 3
      int ctr = 0;
      for (Map.Entry<Double, String> entry : treeMap.entrySet()) {
        int number = ctr + 1;
        if (ctr > 2) {
          break;
        }
        annotation =
            annotation + number + ". " + entry.getValue() + " Δ " + NumberFormat.getInstance()
                .format(entry.getKey()) + " ppm; ";
        ctr++;
        if (isCanceled()) {
          return;
        }
      }
      if (annotation != "") {
        allCompoundIDs.add(annotation);
        massListAnnotated.add(new SimpleDataPoint(massList[0][i], massList[1][i]));
      }
      logger.finest("Finished formula search for " + massRange + " m/z, found " + foundFormulas
                    + " formulas");
    }

    // new mass list
    DataPoint[] annotatedMassList = new DataPoint[massListAnnotated.size()];
    massListAnnotated.toArray(annotatedMassList);
    String[] annotations = new String[annotatedMassList.length];
    allCompoundIDs.toArray(annotations);
    DataPointsDataSet detectedCompoundsDataset = new DataPointsDataSet("Detected compounds",
        annotatedMassList);
    // Add label generator for the dataset
    SpectraDatabaseSearchLabelGenerator labelGenerator = new SpectraDatabaseSearchLabelGenerator(
        annotations, spectraPlot);
    spectraPlot.addDataSet(detectedCompoundsDataset, Color.orange, true, labelGenerator, true);
    spectraPlot.getXYPlot().getRenderer()
        .setSeriesItemLabelGenerator(spectraPlot.getXYPlot().getSeriesCount(), labelGenerator);
    spectraPlot.getXYPlot().getRenderer().setDefaultPositiveItemLabelPosition(
        new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.TOP_LEFT, TextAnchor.BOTTOM_CENTER,
            0.0), true);
    setStatus(TaskStatus.FINISHED);
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

}
