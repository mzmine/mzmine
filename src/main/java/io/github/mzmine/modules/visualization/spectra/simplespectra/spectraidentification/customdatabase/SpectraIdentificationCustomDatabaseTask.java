/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.customdatabase;

import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;
import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.FieldItem;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.SpectraDatabaseSearchLabelGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;

/**
 * Task to search spectra with a custom database
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class SpectraIdentificationCustomDatabaseTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private int numItems;

  private double noiseLevel;
  private Scan currentScan;
  private SpectraPlot spectraPlot;

  private String[][] databaseValues;
  private int finishedLines = 0;

  private File dataBaseFile;
  private String fieldSeparator;
  private FieldItem[] fieldOrder;
  private boolean ignoreFirstLine;
  private MZTolerance mzTolerance;

  /**
   * Create the task.
   *
   * @param parameters task parameters.
   */
  public SpectraIdentificationCustomDatabaseTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    dataBaseFile = parameters
        .getParameter(SpectraIdentificationCustomDatabaseParameters.dataBaseFile).getValue();

    fieldSeparator = parameters
        .getParameter(SpectraIdentificationCustomDatabaseParameters.fieldSeparator).getValue();

    fieldOrder = (FieldItem[]) parameters.getParameter(SpectraIdentificationCustomDatabaseParameters.fieldOrder)
        .getValue();

    ignoreFirstLine = parameters
        .getParameter(SpectraIdentificationCustomDatabaseParameters.ignoreFirstLine).getValue();

    mzTolerance = parameters.getParameter(SpectraIdentificationCustomDatabaseParameters.mzTolerance)
        .getValue();

    noiseLevel = parameters.getParameter(SpectraIdentificationCustomDatabaseParameters.noiseLevel)
        .getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (numItems == 0)
      return 0;
    return ((double) finishedLines) / numItems;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Peak identification of " + " using database" + dataBaseFile;
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
    numItems = massList.length;

    // load custom database
    try {
      // read database contents in memory
      FileReader dbFileReader = new FileReader(dataBaseFile);
      databaseValues = CSVParser.parse(dbFileReader, fieldSeparator.charAt(0));
      if (ignoreFirstLine)
        finishedLines++;
      for (; finishedLines < databaseValues.length; finishedLines++) {
        if (isCanceled()) {
          dbFileReader.close();
          return;
        }

        int numOfColumns = Math.min(fieldOrder.length, databaseValues[finishedLines].length);
        String lineName = null;
        double lineMZ = 0;

        for (int i = 0; i < numOfColumns; i++) {
          if (fieldOrder[i] == FieldItem.FIELD_NAME)
            lineName = databaseValues[finishedLines][i].toString();
          if (fieldOrder[i] == FieldItem.FIELD_MZ)
            lineMZ = Double.parseDouble(databaseValues[finishedLines][i].toString());
        }

        for (int i = 0; i < massList.length; i++) {
          // loop through every peak in mass list
          if (getStatus() != TaskStatus.PROCESSING) {
            return;
          }
          double searchedMass = massList[0][i];

          Range<Double> mzRange = mzTolerance.getToleranceRange(searchedMass);

          boolean mzMatches = (lineMZ == 0d) || mzRange.contains(lineMZ);
          String annotation = "";
          if (mzMatches) {
            // calc rel mass deviation
            double relMassDev = ((searchedMass - lineMZ) / searchedMass) * 1000000;
            logger.finest("Found compound " + lineName + " m/z "
                + NumberFormat.getInstance().format(searchedMass) + " Δ "
                + NumberFormat.getInstance().format(relMassDev) + " ppm");

            annotation = lineName + " Δ " + NumberFormat.getInstance().format(relMassDev) + " ppm";
          }
          if (annotation != "") {
            allCompoundIDs.add(annotation);
            massListAnnotated.add(new SimpleDataPoint(massList[0][i], massList[1][i]));
          }
        }
        finishedLines++;
      }
      // close the file reader
      dbFileReader.close();
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
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
}
