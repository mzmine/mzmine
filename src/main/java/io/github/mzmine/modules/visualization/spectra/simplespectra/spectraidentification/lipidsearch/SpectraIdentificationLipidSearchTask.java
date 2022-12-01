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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.lipidsearch;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.SpectraDatabaseSearchLabelGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Task to search and annotate lipids in spectra
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationLipidSearchTask extends AbstractTask {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private Object[] selectedObjects;
  private DataPoint[] massList;
  private LipidClasses[] selectedLipids;
  private int minChainLength;
  private int maxChainLength;
  private int maxDoubleBonds;
  private int minDoubleBonds;
  private MZTolerance mzTolerance;
  private IonizationType ionizationType;
  private Boolean searchForCustomLipidClasses;
  private CustomLipidClass[] customLipidClasses;
  private Boolean searchForMSMSFragments;
  private Boolean ionizationAutoSearch;
  private Scan currentScan;
  private SpectraPlot spectraPlot;
  private Map<DataPoint, String> annotatedMassList = new HashMap<>();

  private int finishedSteps = 0;
  private int totalSteps;

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  /**
   * Create the task.
   * 
   * @param parameters task parameters.
   */
  public SpectraIdentificationLipidSearchTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    this.minChainLength =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.chainLength).getValue()
            .lowerEndpoint();
    this.maxChainLength =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.chainLength).getValue()
            .upperEndpoint();
    this.minDoubleBonds =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.doubleBonds).getValue()
            .lowerEndpoint();
    this.maxDoubleBonds =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.doubleBonds).getValue()
            .upperEndpoint();
    this.mzTolerance =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.mzTolerance).getValue();
    this.selectedObjects =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.lipidClasses).getValue();
    this.searchForMSMSFragments = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.searchForMSMSFragments).getValue();
    if (searchForMSMSFragments.booleanValue()) {
      this.ionizationAutoSearch =
          parameters.getParameter(SpectraIdentificationLipidSearchParameters.searchForMSMSFragments)
              .getEmbeddedParameters()
              .getParameter(LipidSpeactraSearchMSMSParameters.ionizationAutoSearch).getValue();
    } else {
      this.ionizationAutoSearch = false;
    }
    this.searchForCustomLipidClasses = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.customLipidClasses).getValue();
    if (searchForCustomLipidClasses.booleanValue()) {
      this.customLipidClasses = SpectraIdentificationLipidSearchParameters.customLipidClasses
          .getEmbeddedParameter().getChoices();
    }
    if (currentScan.getMassList() == null) {
      setErrorMessage("Mass List cannot be found.\nCheck if MS2 Scans have a Mass List");
      setStatus(TaskStatus.ERROR);
      return;
    } else {
      massList = currentScan.getMassList().getDataPoints();
    }
    // Convert Objects to LipidClasses
    selectedLipids = Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
        .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0)
      return 0;
    return ((double) finishedSteps) / totalSteps;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Signal identification " + " using the Lipid Search module";
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    Set<DataPoint> massesSet = new HashSet<>(Arrays.asList(massList));

    totalSteps = massList.length;
    // loop through every peak in mass list
    if (getStatus() != TaskStatus.PROCESSING) {
      return;
    }

    // build lipid species database
    Set<ILipidAnnotation> lipidDatabase = buildLipidDatabase();

    // start lipid search
    massesSet.parallelStream().forEach(dataPoint -> {
      for (ILipidAnnotation lipid : lipidDatabase) {
        findPossibleLipid(lipid, dataPoint);
      }
      finishedSteps++;
    });

    // new mass list
    DataPoint[] massListAnnotated = annotatedMassList.keySet().toArray(new DataPoint[0]);
    String[] annotations = annotatedMassList.values().toArray(new String[0]);
    DataPointsDataSet detectedCompoundsDataset =
        new DataPointsDataSet("Detected compounds", massListAnnotated);

    // Add label generator for the dataset
    SpectraDatabaseSearchLabelGenerator labelGenerator =
        new SpectraDatabaseSearchLabelGenerator(annotations, spectraPlot);
    spectraPlot.addDataSet(detectedCompoundsDataset, Color.orange, true, labelGenerator, true);
    spectraPlot.getXYPlot().getRenderer()
        .setSeriesItemLabelGenerator(spectraPlot.getXYPlot().getSeriesCount(), labelGenerator);
    spectraPlot.getXYPlot().getRenderer().setDefaultPositiveItemLabelPosition(new ItemLabelPosition(
        ItemLabelAnchor.CENTER, TextAnchor.TOP_LEFT, TextAnchor.BOTTOM_CENTER, 0.0), true);
    setStatus(TaskStatus.FINISHED);

  }

  private Set<ILipidAnnotation> buildLipidDatabase() {

    Set<ILipidAnnotation> lipidDatabase = new HashSet<>();

    // add selected lipids
    buildLipidCombinations(lipidDatabase, selectedLipids);

    // add custom lipids
    if (customLipidClasses != null && customLipidClasses.length > 0) {
      buildLipidCombinations(lipidDatabase, selectedLipids);
    }

    return lipidDatabase;
  }

  private void buildLipidCombinations(Set<ILipidAnnotation> lipidDatabase,
      ILipidClass[] lipidClasses) {
    // Try all combinations of fatty acid lengths and double bonds
    for (int i = 0; i < lipidClasses.length; i++) {
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {

          // If we have non-zero fatty acid, which is shorter
          // than minimal length, skip this lipid
          if (((chainLength > 0) && (chainLength < minChainLength))) {
            finishedSteps++;
            continue;
          }

          // If we have more double bonds than carbons, it
          // doesn't make sense, so let's skip such lipids
          if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
            finishedSteps++;
            continue;
          }

          // Prepare a lipid instance
          lipidDatabase.add(
              LIPID_FACTORY.buildSpeciesLevelLipid(lipidClasses[i], chainLength, chainDoubleBonds));
        }
      }
    }
  }

  private void findPossibleLipid(ILipidAnnotation lipid, DataPoint dataPoint) {
    if (isCanceled())
      return;
    Set<IonizationType> ionizationTypeList = new HashSet<>();
    if (ionizationAutoSearch.booleanValue()) {
      LipidFragmentationRule[] fragmentationRules = lipid.getLipidClass().getFragmentationRules();
      for (int i = 0; i < fragmentationRules.length; i++) {
        ionizationTypeList.add(fragmentationRules[i].getIonizationType());
      }
    } else {
      ionizationTypeList.add(ionizationType);
    }
    for (IonizationType ionization : ionizationTypeList) {
      if (!currentScan.getPolarity().equals(ionization.getPolarity())) {
        continue;
      }
      double lipidIonMass = MolecularFormulaManipulator.getMass(lipid.getMolecularFormula(),
          AtomContainerManipulator.MonoIsotopic) + ionization.getAddedMass();
      Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(dataPoint.getMZ());
      if (mzTolRange12C.contains(lipidIonMass)) {

        // Calc rel mass deviation;
        double relMassDev = ((lipidIonMass - dataPoint.getMZ()) / lipidIonMass) * 1000000;
        annotatedMassList.put(dataPoint, lipid.getAnnotation() + " " + ionization.getAdductName()
            + ", Δ " + NumberFormat.getInstance().format(relMassDev) + " ppm");

        // If search for MSMS fragments is selected search for fragments
        if (searchForMSMSFragments.booleanValue()) {
          searchMsmsFragments(ionization, lipid);
        }

        logger.info("Found lipid: " + lipid.getAnnotation() + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm");
      }
    }

  }

  private void searchMsmsFragments(IonizationType ionization, ILipidAnnotation lipid) {
    MSMSLipidTools msmsLipidTools = new MSMSLipidTools();
    LipidFragmentationRule[] rules = lipid.getLipidClass().getFragmentationRules();
    if (rules.length > 0) {
      for (int i = 0; i < massList.length; i++) {
        Range<Double> mzTolRange = mzTolerance.getToleranceRange(massList[i].getMZ());
        LipidFragment annotatedFragment = msmsLipidTools.checkForClassSpecificFragment(mzTolRange,
            lipid, ionization, rules, massList[i], currentScan);
        if (annotatedFragment != null) {
          double relMassDev = ((annotatedFragment.getMzExact() - massList[i].getMZ())
              / annotatedFragment.getMzExact()) * 1000000;
          annotatedMassList.put(massList[i],
              annotatedFragment.getLipidClass().getAbbr() + " " + annotatedFragment.getRuleType()
                  + " " + ionization.getAdductName() + ", Δ "
                  + NumberFormat.getInstance().format(relMassDev) + " ppm");
        }
      }
    }
  }

}
