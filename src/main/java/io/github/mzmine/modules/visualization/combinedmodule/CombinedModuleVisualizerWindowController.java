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

package io.github.mzmine.modules.visualization.combinedmodule;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.neutralloss.NeutralLossParameters;
import io.github.mzmine.parameters.ParameterSet;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class CombinedModuleVisualizerWindowController {


  @FXML
  private ToolBar toolbar;

  @FXML
  private HBox bottomPanel;

  @FXML
  private BorderPane mainPane;

  @FXML
  private Pane centerPane;

  @FXML
  private Button highlightPrecursorBtn;

  private ParameterSet parameters;
  private RawDataFile dataFile;
  private Range<Double> rtRange;
  private Range<Double> mzRange;
  private Object xAxisType;
  private CombinedModulePLot plot;


  public void setParameters(ParameterSet parameters) {
    this.parameters = parameters;
    rtRange =
        parameters.getParameter(CombinedModuleParameters.retentionTimeRange).getValue();
    mzRange = parameters.getParameter(CombinedModuleParameters.mzRange).getValue();
    xAxisType = parameters.getParameter(NeutralLossParameters.xAxisType).getValue();
    dataFile = parameters.getParameter(CombinedModuleParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];
  }

  public void initialize() {
    plot = new CombinedModulePLot();
    centerPane.getChildren().add(plot);


  }

}
