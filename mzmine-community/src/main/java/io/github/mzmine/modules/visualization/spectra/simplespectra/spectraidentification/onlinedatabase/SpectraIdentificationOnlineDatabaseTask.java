/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.onlinedatabase;

import static io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.SingleRowIdentificationParameters.DATABASE;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetector;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.DBGateway;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.SpectraDatabaseSearchLabelGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import java.awt.Color;
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
 * Task for identifying peaks by searching on-line databases.
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 * @deprecated because of old API usage. Hard to maintain. This was removed from the interfaces and
 * is only here as reference point
 */
@Deprecated
public class SpectraIdentificationOnlineDatabaseTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private int finishedItems = 0, numItems;

  private final ValueWithParameters<OnlineDatabases> db;
  private double searchedMass;
  private final double noiseLevel;
  private final MZTolerance mzTolerance;
  private final Scan currentScan;
  private final SpectraPlot spectraPlot;
  private final IonizationType ionType;
  private DBGateway gateway;

  /**
   * Create the task.
   *
   * @param parameters task parameters.
   */
  public SpectraIdentificationOnlineDatabaseTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    db = parameters.getParameter(DATABASE).getValueWithParameters();
    try {
      gateway = db.value().getGatewayClass().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }

    mzTolerance = parameters.getParameter(SpectraIdentificationOnlineDatabaseParameters.mzTolerance)
        .getValue();
    ionType = parameters.getParameter(SpectraIdentificationOnlineDatabaseParameters.ionizationType)
        .getValue();
    noiseLevel = parameters.getParameter(SpectraIdentificationOnlineDatabaseParameters.noiseLevel)
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
    return "Peak identification of " + massFormater.format(searchedMass) + " using " + db;
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
    ArrayList<String> allCompoundIDs = new ArrayList<>();

    // Create a new mass list for MS/MS scan. Check if sprectrum is profile
    // or centroid mode
    AutoMassDetector massDetector = new AutoMassDetector(noiseLevel);
    massList = massDetector.getMassValues(currentScan);

    numItems = massList.length;
    for (int i = 0; i < massList.length; i++) {
      // loop through every peak in mass list
      if (getStatus() != TaskStatus.PROCESSING) {
        return;
      }
      searchedMass = massList[0][i] - ionType.getAddedMass();
      try {
        // find candidate compounds
        String[] compoundIDs = gateway.findCompounds(searchedMass, mzTolerance, 1, db.parameters());
        // Combine strings
        String annotation = "";
        // max number of compounds to top three for visualization
        int counter = 0;
        for (int j = 0; !isCanceled() && j < compoundIDs.length; j++) {
          final CompoundDBAnnotation compound = gateway.getCompound(compoundIDs[j],
              db.parameters());

          // In case we failed to retrieve data, skip this compound
          if (compound == null) {
            continue;
          }
          if (counter < 3) {
            int number = counter + 1;
            annotation = annotation + " " + number + ". " + compound.getCompoundName();
            counter++;
          }
        }
        if (annotation != "") {
          allCompoundIDs.add(annotation);
          massListAnnotated.add(new SimpleDataPoint(massList[0][i], massList[1][i]));
        }
      } catch (Exception e) {
        e.printStackTrace();
        logger.log(Level.WARNING, "Could not connect to " + db, e);
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not connect to " + db + ": " + ExceptionUtils.exceptionToString(e));
        return;
      }
      finishedItems++;
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

}
