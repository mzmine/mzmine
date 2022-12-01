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

package io.github.mzmine.modules.dataanalysis.projectionplots;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotParameters;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotVisualizerModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ProjectionPlotWindow extends Stage {

  private static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");
  private static final Image labelsIcon =
      FxIconUtil.loadImageFromResources("icons/annotationsicon.png");

  private final Scene mainScene;
  private final BorderPane mainPane;

  private final ToolBar toolbar;
  private final ProjectionPlotPanel plot;

  public ProjectionPlotWindow(FeatureList featureList, ProjectionPlotDataset dataset,
      ParameterSet parameters) {

    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    toolbar = new ToolBar();
    toolbar.setOrientation(Orientation.VERTICAL);
    Button axesButton = new Button(null, new ImageView(axesIcon));
    axesButton.setTooltip(new Tooltip("Setup ranges for axes"));
    Button labelsButton = new Button(null, new ImageView(labelsIcon));
    labelsButton.setTooltip(new Tooltip("Toggle sample names"));
    toolbar.getItems().addAll(axesButton, labelsButton);
    mainPane.setRight(toolbar);

    plot = new ProjectionPlotPanel(this, dataset, parameters);
    mainPane.setCenter(plot);

    axesButton.setOnAction(e -> {
      AxesSetupDialog dialog = new AxesSetupDialog(this, plot.getChart().getXYPlot());
      dialog.showAndWait();
    });

    labelsButton.setOnAction(e -> plot.cycleItemLabelMode());

    String title = featureList.getName();
    title = title.concat(" : ");
    title = title.concat(dataset.toString());
    if (parameters.getParameter(ProjectionPlotParameters.featureMeasurementType)
        .getValue() == FeatureMeasurementType.HEIGHT)
      title = title.concat(" (using feature heights)");
    else
      title = title.concat(" (using feature areas)");

    this.setTitle(title);

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);

    ParameterSet paramSet =
        MZmineCore.getConfiguration().getModuleParameters(ScatterPlotVisualizerModule.class);
    WindowSettingsParameter settings = paramSet.getParameter(ScatterPlotParameters.windowSettings);

  }



}
