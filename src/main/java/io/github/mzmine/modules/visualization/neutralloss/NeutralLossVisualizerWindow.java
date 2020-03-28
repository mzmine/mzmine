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
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.awt.Dialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;


/**
 * Neutral loss visualizer using JFreeChart library
 */
public class NeutralLossVisualizerWindow extends BorderPane implements EventHandler<ActionEvent> {


  private NeutralLossToolBar toolBar;
  private NeutralLossPlot neutralLossPlot;

  private NeutralLossDataSet dataset;

  private RawDataFile dataFile;

  public NeutralLossVisualizerWindow(RawDataFile dataFile, ParameterSet parameters) {

//    super(dataFile.getName());

//    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setBackground(
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
    setCenter(neutralLossPlot);

    toolBar = new NeutralLossToolBar(this);
    setLeft(toolBar);

    MZmineCore.getTaskController().addTask(dataset, TaskPriority.HIGH);

    updateTitle();

    // Add the Windows menu
    MenuBar menuBar = new MenuBar();

    setTop(menuBar);

    // get the window settings parameter
    ParameterSet paramSet =
        MZmineCore.getConfiguration().getModuleParameters(NeutralLossVisualizerModule.class);
    WindowSettingsParameter settings = paramSet.getParameter(NeutralLossParameters.windowSettings);


  }

  void updateTitle() {

    StringBuffer title = new StringBuffer();
    title.append("[");
    title.append(dataFile.getName());
    title.append("]: neutral loss");

//    Stage s=(Stage)(this.getScene().getWindow());
//    s.setTitle(title.toString());

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

  @Override
  public void handle(ActionEvent event) {
//    String command = event.getActionCommand();
    String command = "";
    if (command.equals("HIGHLIGHT")) {
      Dialog dialog = new NeutralLossSetHighlightDialog(neutralLossPlot, command);
      dialog.setVisible(true);
    }

    if (command.equals("SHOW_SPECTRUM")) {
      NeutralLossDataPoint pos = getCursorPosition();
      if (pos != null) {
        SpectraVisualizerModule.showNewSpectrumWindow(dataFile, pos.getScanNumber());
      }
    }
  }

  public void handle(KeyEvent event) {
    System.out.println("keyevent method called");
  }
}
