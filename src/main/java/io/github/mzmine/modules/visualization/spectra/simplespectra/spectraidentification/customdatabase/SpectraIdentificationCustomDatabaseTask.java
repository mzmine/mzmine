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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.customdatabase;

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
import java.time.Instant;
import java.util.ArrayList;
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

  private static final Logger logger = Logger.getLogger(
      SpectraIdentificationCustomDatabaseTask.class.getName());

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
    spectraPlot.addDataSet(detectedCompoundsDataset, Color.orange, true, labelGenerator, true);
    spectraPlot.getXYPlot().getRenderer()
        .setSeriesItemLabelGenerator(spectraPlot.getXYPlot().getSeriesCount(), labelGenerator);
    spectraPlot.getXYPlot().getRenderer().setDefaultPositiveItemLabelPosition(new ItemLabelPosition(
        ItemLabelAnchor.CENTER, TextAnchor.TOP_LEFT, TextAnchor.BOTTOM_CENTER, 0.0), true);
    setStatus(TaskStatus.FINISHED);

  }
}
