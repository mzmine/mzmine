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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Task to create a Kendrick mass plot of selected features of a selected feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final ParameterSet parameters;
  private final FeatureList featureList;
  private final String title;
  private final String xAxisLabel;
  private final String yAxisLabel;
  private final String zAxisLabel;

  public KendrickMassPlotTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    featureList = parameters.getParameter(KendrickMassPlotParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];
    this.parameters = parameters;
    this.title = "Kendrick mass plot of" + featureList;

    if (parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
        .isKendrickType()) {
      String kmdRkm = null;
      if (parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
          .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        kmdRkm = "RKM";
      } else {
        kmdRkm = "KMD";
      }
      xAxisLabel = kmdRkm + " (" + parameters.getParameter(
              KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
          .getValue() + ")";
    } else {
      xAxisLabel = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
          .getName();
    }

    if (parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
        .isKendrickType()) {
      String kmdRkm = null;
      if (parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
          .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        kmdRkm = "RKM";
      } else {
        kmdRkm = "KMD";
      }
      yAxisLabel =
          kmdRkm + " (" + parameters.getParameter(
                  KendrickMassPlotParameters.yAxisCustomKendrickMassBase)
              .getValue() + ")";
    } else {
      yAxisLabel = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
          .getName();
    }

    if (parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
        .isKendrickType()) {
      String kmdRkm = null;
      if (parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
          .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        kmdRkm = "RKM";
      } else {
        kmdRkm = "KMD";
      }
      zAxisLabel = kmdRkm + " (" + parameters.getParameter(
          KendrickMassPlotParameters.colorScaleCustomKendrickMassBase).getValue() + ")";
    } else {
      zAxisLabel = parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
          .getName();
    }
  }

  @Override
  public String getTaskDescription() {
    return "Create Kendrick mass plot for " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Create Kendrick mass plot of " + featureList);
    // Task canceled?
    if (isCanceled()) {
      return;
    }
    KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset = new KendrickMassPlotXYZDataset(
        parameters, 1, 1);
    KendrickMassPlotChart kendrickMassPlotChart = new KendrickMassPlotChart(title, xAxisLabel,
        yAxisLabel, zAxisLabel, kendrickMassPlotXYZDataset);
    KendrickMassPlotBubbleLegend kendrickMassPlotBubbleLegend = new KendrickMassPlotBubbleLegend(
        kendrickMassPlotXYZDataset);

    // Create Kendrick mass plot Tab
    MZmineCore.runLater(() -> {
      KendrickMassPlotTab newTab = new KendrickMassPlotTab(parameters, kendrickMassPlotChart,
          kendrickMassPlotBubbleLegend);
      MZmineCore.getDesktop().addTab(newTab);
    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished creating Kendrick mass plot of " + featureList);
  }

}
