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

package io.github.mzmine.modules.visualization.neutralloss;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskPriority;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


/**
 * Neutral loss visualizer using JFreeChart library
 */
public class NeutralLossVisualizerWindow extends Stage {


  private NeutralLossPlot neutralLossPlot;
  private BorderPane pane;
  private NeutralLossDataSet dataset;
  static final Image dataPointsIcon = new Image("icons/datapointsicon.png");

  private RawDataFile dataFile;

  public NeutralLossVisualizerWindow(RawDataFile dataFile, ParameterSet parameters) {
    pane = new BorderPane();
    pane.setBackground(
        new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), new Insets(0))));

    this.dataFile = dataFile;

    // Retrieve parameter's values
    Range<Double> rtRange =
        parameters.getParameter(NeutralLossParameters.retentionTimeRange).getValue();
    Range<Double> mzRange = parameters.getParameter(NeutralLossParameters.mzRange).getValue();
    int numOfFragments = parameters.getParameter(NeutralLossParameters.numOfFragments).getValue();

    Object xAxisType = parameters.getParameter(NeutralLossParameters.xAxisType).getValue();

    // Set window components
    dataset = new NeutralLossDataSet(dataFile, xAxisType, rtRange, mzRange, numOfFragments, this);

    neutralLossPlot = new NeutralLossPlot(this, dataset, xAxisType);
    pane.setCenter(neutralLossPlot);

    ButtonBar buttonPanel = new ButtonBar();
    Button button = new Button();
    button.setBackground(new Background(
        new BackgroundImage(dataPointsIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
    button.setOnAction(event -> this.handleHighlight("HIGHLIGHT_PRECURSOR"));
    button.setTooltip(new Tooltip("Highlight selected precursor mass range"));
    buttonPanel.getButtons().add(button);
    pane.setRight(buttonPanel);

    MZmineCore.getTaskController().addTask(dataset, TaskPriority.HIGH);

    updateTitle();

    // Add the Windows menu
    MenuBar menuBar = new MenuBar();

    pane.setTop(menuBar);
    this.setScene(new Scene(pane));
    this.setMinHeight(600);
    this.setMinWidth(800);
  }

  void updateTitle() {

    StringBuffer title = new StringBuffer();
    title.append("[");
    title.append(dataFile.getName());
    title.append("]: neutral loss");

    this.setTitle(title.toString());

    NeutralLossDataPoint pos = getCursorPosition();

    if (pos != null) {
      title.append(", ");
      title.append(pos.getName());
    }
    neutralLossPlot.setTitle(title.toString());
  }


  public NeutralLossDataPoint getCursorPosition() {
    double xValue = neutralLossPlot.getXYPlot().getDomainCrosshairValue();
    double yValue = neutralLossPlot.getXYPlot().getRangeCrosshairValue();

    NeutralLossDataPoint point = dataset.getDataPoint(xValue, yValue);
    return point;

  }

  NeutralLossPlot getPlot() {
    return neutralLossPlot;
  }

  public void handleHighlight(String command) {
    Dialog dialog = new NeutralLossSetHighlightDialog(neutralLossPlot, command);
    dialog.show();
  }

  public void handleShowspectrum() {
    NeutralLossDataPoint pos = getCursorPosition();
    if (pos != null) {
      SpectraVisualizerModule.showNewSpectrumWindow(dataFile, pos.getScanNumber());
    }
  }
}
