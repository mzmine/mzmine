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

package net.sf.mzmine.modules.visualization.spectra.spectraidentification.sumformula;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.datasets.DataPointsDataSet;
import net.sf.mzmine.modules.visualization.spectra.spectraidentification.SpectraDatabaseSearchLabelGenerator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.FormulaUtils;

/**
 * Task for sum formula prediction in spectra.
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationSumFormulaTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

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
  private ParameterSet ratiosParameters;
  private ParameterSet rdbeParameters;

  private Range<Double> massRange;
  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;


  /**
   * Create the task.
   * 
   * @param parameters task parameters.
   */
  public SpectraIdentificationSumFormulaTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot) {

    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    charge = parameters.getParameter(SpectraIdentificationSumFormulaParameters.charge).getValue();
    ionType =
        parameters.getParameter(SpectraIdentificationSumFormulaParameters.ionization).getValue();

    checkRDBE = parameters.getParameter(SpectraIdentificationSumFormulaParameters.rdbeRestrictions)
        .getValue();
    rdbeParameters =
        parameters.getParameter(SpectraIdentificationSumFormulaParameters.rdbeRestrictions)
            .getEmbeddedParameters();

    checkRatios = parameters.getParameter(SpectraIdentificationSumFormulaParameters.elementalRatios)
        .getValue();
    ratiosParameters =
        parameters.getParameter(SpectraIdentificationSumFormulaParameters.elementalRatios)
            .getEmbeddedParameters();

    elementCounts =
        parameters.getParameter(SpectraIdentificationSumFormulaParameters.elements).getValue();

    mzTolerance =
        parameters.getParameter(SpectraIdentificationSumFormulaParameters.mzTolerance).getValue();

    noiseLevel =
        parameters.getParameter(SpectraIdentificationSumFormulaParameters.noiseLevel).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (numItems == 0)
      return 0;
    return ((double) finishedItems) / numItems;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
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
    DataPoint[] massList = null;
    ArrayList<DataPoint> massListAnnotated = new ArrayList<>();
    MassDetector massDetector = null;
    ArrayList<String> allCompoundIDs = new ArrayList<>();

    // Create a new mass list for MS/MS scan. Check if sprectrum is profile or centroid mode
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
      massRange =
          mzTolerance.getToleranceRange((massList[i].getMZ() - ionType.getAddedMass()) / charge);
      generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
          massRange.upperEndpoint(), elementCounts);

      IMolecularFormula cdkFormula;
      String annotation = "";
      Map<Double, String> possibleFormulas = new HashMap<Double, String>();
      while ((cdkFormula = generator.getNextFormula()) != null) {
        if (isCanceled())
          return;
        // calc rel mass deviation
        Double relMassDev = ((((massList[i].getMZ() - //
            ionType.getAddedMass()) / charge)//
            - (FormulaUtils.calculateExactMass(//
                MolecularFormulaManipulator.getString(cdkFormula))) / charge)
            / ((massList[i].getMZ() //
                - ionType.getAddedMass()) / charge))
            * 1000000;
        possibleFormulas.put(relMassDev, checkConstraints(cdkFormula));
      }

      Map<Double, String> treeMap = new TreeMap<>(
          (Comparator<Double>) (o1, o2) -> Double.compare(Math.abs(o1), Math.abs(o2)));
      treeMap.putAll(possibleFormulas);

      // get top 5
      int ctr = 0;
      for (Map.Entry<Double, String> entry : treeMap.entrySet()) {
        int number = ctr + 1;
        if (ctr > 4)
          break;
        annotation = annotation + number + ". " + entry.getValue() + " Δ "
            + NumberFormat.getInstance().format(entry.getKey()) + " ppm; ";
        ctr++;
        if (isCanceled())
          return;
      }
      if (annotation != "") {
        allCompoundIDs.add(annotation);
        massListAnnotated.add(massList[i]);
      }
      logger.finest("Finished formula search for " + massRange + " m/z, found " + foundFormulas
          + " formulas");
    }

    // new mass list
    DataPoint[] annotatedMassList = new DataPoint[massListAnnotated.size()];
    massListAnnotated.toArray(annotatedMassList);
    String[] annotations = new String[annotatedMassList.length];
    allCompoundIDs.toArray(annotations);
    DataPointsDataSet detectedCompoundsDataset =
        new DataPointsDataSet("Detected compounds", annotatedMassList);
    // Add label generator for the dataset
    SpectraDatabaseSearchLabelGenerator labelGenerator =
        new SpectraDatabaseSearchLabelGenerator(annotations, spectraPlot);
    spectraPlot.addDataSet(detectedCompoundsDataset, Color.orange, true, labelGenerator);
    spectraPlot.getXYPlot().getRenderer()
        .setSeriesItemLabelGenerator(spectraPlot.getXYPlot().getSeriesCount(), labelGenerator);
    spectraPlot.getXYPlot().getRenderer().setDefaultPositiveItemLabelPosition(new ItemLabelPosition(
        ItemLabelAnchor.CENTER, TextAnchor.TOP_LEFT, TextAnchor.BOTTOM_CENTER, 0.0), true);
    setStatus(TaskStatus.FINISHED);
  }

  private String checkConstraints(IMolecularFormula cdkFormula) {

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormula, ratiosParameters);
      if (!check)
        return null;
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormula);

    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeParameters);
      if (!check)
        return null;
    }

    // Create a new formula
    final String resultFormula = MolecularFormulaManipulator.getString(cdkFormula);

    foundFormulas++;
    return resultFormula;
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
