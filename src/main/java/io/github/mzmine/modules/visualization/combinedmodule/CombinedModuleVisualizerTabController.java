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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.util.RangeUtils;
import java.util.Collection;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;

public class CombinedModuleVisualizerTabController {

  @FXML
  private ToolBar toolbar;
  @FXML
  private HBox bottomPanel;
  @FXML
  private BorderPane mainPane;
  @FXML
  private Button highlightPrecursorBtn;
  @FXML
  private CombinedModulePlot plot;

  private ParameterSet parameters;
  private RawDataFile dataFile;
  private Range<Float> rtRange;
  private Range<Double> mzRange;
  private AxisType xAxisType;
  private AxisType yAxisType;
  private String massList;
  private Double noiseLevel;
  private ColorScale colorScale;
  private CombinedModuleDataset dataset;


  public void setParameters(Window window, ParameterSet parameters) {
    highlightPrecursorBtn.setOnAction(event -> {
      CombinedModuleSetHighlightDialog dialog = new CombinedModuleSetHighlightDialog(window, plot,
          "HIGHLIGHT_PRECURSOR");
      dialog.show();
    });
    this.parameters = parameters;
    rtRange = RangeUtils.toFloatRange(
        parameters.getParameter(CombinedModuleParameters.retentionTimeRange).getValue());
    mzRange = parameters.getParameter(CombinedModuleParameters.mzRange).getValue();
    xAxisType = parameters.getParameter(CombinedModuleParameters.xAxisType).getValue();
    yAxisType = parameters.getParameter(CombinedModuleParameters.yAxisType).getValue();
    dataFile = parameters.getParameter(CombinedModuleParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];
    massList = parameters.getParameter(CombinedModuleParameters.massList).getValue();
    noiseLevel = parameters.getParameter(CombinedModuleParameters.noiseLevel).getValue();
    colorScale = parameters.getParameter(CombinedModuleParameters.colorScale).getValue();
    dataset = new CombinedModuleDataset(dataFile, rtRange, mzRange, this, xAxisType, yAxisType,
        noiseLevel, colorScale,massList);
    plot.setPlot(dataFile, this, rtRange, mzRange, xAxisType, yAxisType, massList,
        noiseLevel, colorScale);
    plot.addDataset(dataset);

  }

  public CombinedModulePlot getPlot() {
    return plot;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  public void updateVisualizedFiles(Collection<? extends RawDataFile> rawDataFiles) {
    if(rawDataFiles == null || rawDataFiles.isEmpty()) {
      return;
    }

    // get first raw features file
    RawDataFile newFile = rawDataFiles.iterator().next();
    if (dataFile.equals(newFile)) {
      return;
    }

    // remove old dataset
    plot.getXYPlot().setDataset(
        plot.getXYPlot().indexOf(dataset),null);

    // add new dataset
    CombinedModuleDataset newDataset = new CombinedModuleDataset(dataFile, rtRange, mzRange,
        this, xAxisType, yAxisType, noiseLevel, colorScale, massList);
    plot.addDataset(newDataset);

    dataFile = newFile;
    dataset = newDataset;

    newDataset.run();
  }
}
