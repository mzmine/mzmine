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

package io.github.mzmine.modules.visualization.combinedModule;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.neutralloss.NeutralLossParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class CombinedModuleVisualizerWindow extends Stage {

  private static final Image PRECURSOR_MASS_ICON =
      FxIconUtil.loadImageFromResources("icons/datapointsicon.png");

  @FXML
  private ToolBar toolbar;

  @FXML
  private HBox bottomPanel;

  @FXML
  private BorderPane mainPane;

  @FXML
  private Pane centerPane;

  private RawDataFile dataFile;

  public CombinedModuleVisualizerWindow(ParameterSet parameters) {

    Button highlightPrecursorBtn = new Button(null, new ImageView(PRECURSOR_MASS_ICON));
    toolbar.getItems().add(highlightPrecursorBtn);

    Range<Double> rtRange =
        parameters.getParameter(CombinedModuleParameters.retentionTimeRange).getValue();
    Range<Double> mzRange = parameters.getParameter(CombinedModuleParameters.mzRange).getValue();
    Object xAxisType = parameters.getParameter(NeutralLossParameters.xAxisType).getValue();
    WindowsMenu.addWindowsMenu(getScene());
    dataFile = parameters.getParameter(CombinedModuleParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];


  }

}
