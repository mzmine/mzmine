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

package io.github.mzmine.modules.visualization.histo_feature_correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RFullCorrelationData;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.time.Instant;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class FeatureCorrelationHistogramTask extends AbstractTask {

  private static final Logger logger = Logger
      .getLogger(FeatureCorrelationHistogramTask.class.getName());
  private final ModularFeatureList flist;
  private final double startBinWidth;
  private FeatureCorrelationHistogramTab tab;

  public FeatureCorrelationHistogramTask(ModularFeatureList flist, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.flist = flist;
    startBinWidth = parameters.getParameter(FeatureCorrelationHistogramParameters.binWidth)
        .getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Creating m/z delta histogram of correlated feature in " + flist.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  public ModularFeatureList getFeatureList() {
    return flist;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    DoubleArrayList valuesUnidentified = new DoubleArrayList();
    DoubleArrayList valuesIdentified = new DoubleArrayList();

    int counter = 0;
    for (RowsRelationship r2r : flist.getMs1CorrelationMap().values()) {
      if (r2r instanceof R2RFullCorrelationData corr) {
        FeatureListRow a = corr.getRowA();
        FeatureListRow b = corr.getRowB();
        // get m/z difference for each data file that was correlated
        for (var entry : corr.getCorrFeatureShape().entrySet()) {
          RawDataFile raw = entry.getKey();
          CorrelationData corrData = entry.getValue();

          IonIdentity ionA = a.getBestIonIdentity();
          IonIdentity ionB = b.getBestIonIdentity();
          // both identified ions from same network
          if (ionA != null && ionB != null && ionA.getNetID() == ionB.getNetID()) {
            valuesIdentified.add(corrData.getPearsonR());
          } else {
            valuesUnidentified.add(corrData.getPearsonR());
          }
          counter++;
        }
      }
    }

    logger.info("Total of " + counter + " correlated features");

    // create histogram dialog
    tab = new FeatureCorrelationHistogramTab(flist, valuesUnidentified, valuesIdentified,
        "Feature shape correlation (Pearson)", "Pearson r", startBinWidth);
    Platform.runLater(() -> {
      MZmineCore.getDesktop().addTab(tab);
    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished mz delta histogram of correlated features in " + flist.getName());
  }

}
