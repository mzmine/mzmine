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
