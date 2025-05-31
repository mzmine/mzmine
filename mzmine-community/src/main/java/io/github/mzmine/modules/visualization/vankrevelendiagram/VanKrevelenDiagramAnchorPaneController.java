/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class VanKrevelenDiagramAnchorPaneController {

  private static final Logger logger = Logger.getLogger(
      VanKrevelenDiagramAnchorPaneController.class.getName());

  @FXML
  private BorderPane plotPane;
  @FXML
  private BorderPane bubbleLegendPane;

  private ParameterSet parameters;


  private FeatureList featureList;

  @FXML
  public void initialize(ParameterSet parameters) {
    this.parameters = parameters.cloneParameterSet();
    this.featureList = parameters.getParameter(VanKrevelenDiagramParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];

    String title = "Van Krevelen Diagram of" + featureList;
    String zAxisLabel = parameters.getParameter(VanKrevelenDiagramParameters.colorScaleValues)
        .getValue().getName();
    plotPane.setCenter(new Label("Preparing content"));
    VanKrevelenDiagramXYZDataset vanKrevelenDiagramXYZDataset = new VanKrevelenDiagramXYZDataset(
        parameters);
    vanKrevelenDiagramXYZDataset.addTaskStatusListener((task, newStatus, oldStatus) -> {
      if (newStatus == TaskStatus.FINISHED) {
        if (vanKrevelenDiagramXYZDataset.getItemCount(0) > 0) {
          VanKrevelenDiagramChart vanKrevelenDiagramChart = new VanKrevelenDiagramChart(title,
              "O/C", "H/C", zAxisLabel, vanKrevelenDiagramXYZDataset);
          VanKrevelenDiagramBubbleLegend vanKrevelenDiagramBubbleLegend = new VanKrevelenDiagramBubbleLegend(
              vanKrevelenDiagramXYZDataset);
          FxThread.runLater(() -> {
            plotPane.setCenter(vanKrevelenDiagramChart);
            bubbleLegendPane.setCenter(vanKrevelenDiagramBubbleLegend);
          });
        } else {
          FxThread.runLater(() -> {
            plotPane.setCenter(new Label(
                "Nothing to plot. Check if the selected feature list has annotations. A Van Krevelen Diagram requires molecular formulas."));
          });
        }
      }
    });
  }

  public FeatureList getFeatureList() {
    return featureList;
  }

  public BorderPane getPlotPane() {
    return plotPane;
  }

  public void setPlotPane(BorderPane plotPane) {
    this.plotPane = plotPane;
  }

}
