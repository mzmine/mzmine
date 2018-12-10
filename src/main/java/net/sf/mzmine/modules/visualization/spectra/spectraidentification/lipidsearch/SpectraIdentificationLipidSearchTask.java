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

package net.sf.mzmine.modules.visualization.spectra.spectraidentification.lipidsearch;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.LipidModification;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils.LipidIdentity;
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
   * @param peakListRow peak-list row to identify.
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
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0)
      return 0;
    return ((double) finishedSteps) / totalSteps;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
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
            searchedMass = massList[i].getMZ();
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
              massListAnnotated.add(massList[i]);
            }
            annotation = findPossibleLipidModification(lipidChain, searchedMass);
            if (annotation != "") {
              allCompoundIDs.add(annotation);
              massListAnnotated.add(massList[i]);
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
      lipidAnnoation = lipid.getName() + ionizationType.getAdduct() + ", Δ "
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
    // If search for modifications is selected search for modifications in MS1
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
        lipidAnnoation = lipid + " " + ionizationType.getAdduct() + " " + lipidModification[j]
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
