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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.lipidsearch;

import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidIdentity;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.SpectraDatabaseSearchLabelGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 * Task to search and annotate lipids in spectra
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationLipidSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private Object[] selectedObjects;
  private LipidClasses[] selectedLipids;
  private int minChainLength, maxChainLength, maxDoubleBonds, minDoubleBonds;
  private MZTolerance mzTolerance;
  private IonizationType ionizationType;
  private Boolean searchForModifications;
  private double noiseLevel;
  private double[] lipidModificationMasses;
  private LipidModification[] lipidModification;
  private Scan currentScan;
  private SpectraPlot spectraPlot;

  private int finishedSteps = 0, totalSteps;
  private double searchedMass;

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  /**
   * Create the task.
   *
   * @param parameters task parameters.
   */
  public SpectraIdentificationLipidSearchTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot) {

    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    minChainLength = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.minChainLength).getValue();
    maxChainLength = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.maxChainLength).getValue();
    maxDoubleBonds = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.maxDoubleBonds).getValue();
    minDoubleBonds = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.minDoubleBonds).getValue();
    mzTolerance =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.mzTolerance).getValue();
    selectedObjects =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.lipidClasses).getValue();
    ionizationType = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.ionizationMethod).getValue();
    searchForModifications = parameters
        .getParameter(SpectraIdentificationLipidSearchParameters.useModification).getValue();
    lipidModification =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.modification).getValue();
    noiseLevel =
        parameters.getParameter(SpectraIdentificationLipidSearchParameters.noiseLevel).getValue();

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

    // create mass list for scan
    double[][] massList = null;
    ArrayList<DataPoint> massListAnnotated = new ArrayList<>();
    MassDetector massDetector = null;
    ArrayList<String> allCompoundIDs = new ArrayList<>();

    // Create a new mass list for MS/MS scan. Check if sprectrum is profile
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
    totalSteps = massList.length;
    // loop through every peak in mass list
    if (getStatus() != TaskStatus.PROCESSING) {
      return;
    }

    // Check if lipids should be modified
    if (searchForModifications == true) {
      lipidModificationMasses = getLipidModificationMasses(lipidModification);
    }
    // Calculate how many possible lipids we will try
    totalSteps = (((maxChainLength - minChainLength + 1) * (maxDoubleBonds - minDoubleBonds + 1))
        * selectedLipids.length);
    // Combine Strings
    String annotation = "";
    // Try all combinations of fatty acid lengths and double bonds
    for (int j = 0; j < selectedLipids.length; j++) {
      int numberOfAcylChains = selectedLipids[j].getNumberOfAcylChains();
      int numberOfAlkylChains = selectedLipids[j].getNumberofAlkyChains();
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {
          for (int i = 0; i < massList.length; i++) {
            searchedMass = massList[0][i];
            // Task canceled?
            if (isCanceled())
              return;

            // If we have non-zero fatty acid, which is shorter
            // than minimal length, skip this lipid
            if (((chainLength > 0) && (chainLength < minChainLength))) {
              continue;
            }

            // If we have more double bonds than carbons, it
            // doesn't make sense, so let's skip such lipids
            if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
              continue;
            }
            // Prepare a lipid instance
            LipidIdentity lipidChain = new LipidIdentity(selectedLipids[j], chainLength,
                chainDoubleBonds, numberOfAcylChains, numberOfAlkylChains);
            annotation = findPossibleLipid(lipidChain, searchedMass);
            if (annotation != "") {
              allCompoundIDs.add(annotation);
              massListAnnotated.add(new SimpleDataPoint(massList[0][i], massList[1][i]));
            }
            annotation = findPossibleLipidModification(lipidChain, searchedMass);
            if (annotation != "") {
              allCompoundIDs.add(annotation);
              massListAnnotated.add(new SimpleDataPoint(massList[0][i], massList[1][i]));
            }
          }
          finishedSteps++;
        }
      }
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

  private String findPossibleLipid(LipidIdentity lipid, double searchedMass) {
    String lipidAnnoation = "";
    double lipidIonMass = 0.0;
    double lipidMass = lipid.getMass();
    lipidIonMass = lipidMass + ionizationType.getAddedMass();
    logger.info("Searching for lipid " + lipid.getDescription() + ", " + lipidIonMass + " m/z");
    Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(searchedMass);
    if (mzTolRange12C.contains(lipidIonMass)) {
      // Calc rel mass deviation;
      double relMassDev = ((lipidIonMass - searchedMass) / lipidIonMass) * 1000000;
      lipidAnnoation = lipid.getName() + ionizationType.getAdductName() + ", Δ "
          + NumberFormat.getInstance().format(relMassDev) + " ppm"; // Format relativ mass
    }
    return lipidAnnoation;
  }

  private String findPossibleLipidModification(LipidIdentity lipid, double searchedMass) {
    String lipidAnnoation = "";
    double lipidIonMass = 0.0;
    double lipidMass = lipid.getMass();
    lipidIonMass = lipidMass + ionizationType.getAddedMass();
    logger.info("Searching for lipid " + lipid.getDescription() + ", " + lipidIonMass + " m/z");
    Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(searchedMass);
    // If search for modifications is selected search for modifications in
    // MS1
    if (searchForModifications == true) {
      lipidAnnoation = searchModifications(searchedMass, lipidIonMass, lipid,
          lipidModificationMasses, mzTolRange12C);
    }
    return lipidAnnoation;
  }

  private String searchModifications(double searchedMass, double lipidIonMass, LipidIdentity lipid,
      double[] lipidModificationMasses, Range<Double> mzTolModification) {
    String lipidAnnoation = "";
    for (int j = 0; j < lipidModificationMasses.length; j++) {
      if (mzTolModification.contains(lipidIonMass + (lipidModificationMasses[j]))) {
        // Calc relativ mass deviation
        double relMassDev = ((lipidIonMass + (lipidModificationMasses[j]) - searchedMass)
            / (lipidIonMass + lipidModificationMasses[j])) * 1000000;
        // Add row identity
        lipidAnnoation = lipid + " " + ionizationType.getAdductName() + " " + lipidModification[j]
            + ", Δ " + NumberFormat.getInstance().format(relMassDev) + " ppm";
        logger.info("Found modified lipid: " + lipid.getName() + " " + lipidModification[j] + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm");
      }
    }
    return lipidAnnoation;
  }

  private double[] getLipidModificationMasses(LipidModification[] lipidModification) {
    double[] lipidModificationMasses = new double[lipidModification.length];
    for (int i = 0; i < lipidModification.length; i++) {
      lipidModificationMasses[i] = lipidModification[i].getModificationMass();
    }
    return lipidModificationMasses;
  }

}
