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
import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javax.annotation.Nonnull;

/**
 * Neutral loss visualizer using JFreeChart library.
 */
public class NeutralLossVisualizerTab extends MZmineTab {

  private static final Image PRECURSOR_MASS_ICON =
      FxIconUtil.loadImageFromResources("icons/datapointsicon.png");
  private ToolBar toolBar;
  private NeutralLossPlot neutralLossPlot;
  private BorderPane borderPane;
  private Scene scene;
  private NeutralLossDataSet dataset;
  private RawDataFile dataFile;

  // Parameters
  private Range<Float> rtRange;
  private Range<Double> mzRange;
  private int numOfFragments;
  private Object xAxisType;

  /**
   * Constructor.
   *
   * @param dataFile   file containing the data of one sample
   * @param parameters plot parameters set by the user
   */
  public NeutralLossVisualizerTab(RawDataFile dataFile, ParameterSet parameters) {

    super("NeutralLoss Visualizer", true, false);

    this.dataFile = dataFile;

    // Retrieve parameter's values
    rtRange = RangeUtils.toFloatRange(parameters.getParameter(NeutralLossParameters.retentionTimeRange).getValue());
    mzRange = parameters.getParameter(NeutralLossParameters.mzRange).getValue();
    numOfFragments = parameters.getParameter(NeutralLossParameters.numOfFragments).getValue();
    xAxisType = parameters.getParameter(NeutralLossParameters.xAxisType).getValue();

    // Set window components
    dataset = new NeutralLossDataSet(dataFile, xAxisType, rtRange, mzRange, numOfFragments, this);

    borderPane = new BorderPane();
    setContent(borderPane);
    //scene = new Scene(borderPane);


    //Use main CSS
    //scene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    //setScene(scene);

    //setMinWidth(400.0);
    //setMinHeight(300.0);

    neutralLossPlot = new NeutralLossPlot();
    neutralLossPlot.setAxisTypes(xAxisType);
    neutralLossPlot.addNeutralLossDataSet(dataset);
    neutralLossPlot.setVisualizer(this);
    neutralLossPlot.setMenuItems();
    borderPane.setCenter(neutralLossPlot);

    toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);

    Button highlightPrecursorBtn = new Button(null, new ImageView(PRECURSOR_MASS_ICON));
    highlightPrecursorBtn.setTooltip(new Tooltip("Highlight precursor m/z range..."));
    highlightPrecursorBtn.setOnAction(e -> {
      assert MZmineCore.getDesktop() != null;

      NeutralLossSetHighlightDialog dialog =
          new NeutralLossSetHighlightDialog(MZmineCore.getDesktop().getMainWindow(),
              neutralLossPlot, "HIGHLIGHT_PRECURSOR");
      dialog.show();
    });

    toolBar.getItems().add(highlightPrecursorBtn);
    borderPane.setRight(toolBar);

    //WindowsMenu.addWindowsMenu(scene);

    MZmineCore.getTaskController().addTask(dataset, TaskPriority.HIGH);

    updateTitle();

    // get the window settings parameter
    ParameterSet paramSet =
        MZmineCore.getConfiguration().getModuleParameters(NeutralLossVisualizerModule.class);
    //WindowSettingsParameter settings = paramSet.getParameter(NeutralLossParameters.windowSettings);

    // update the window and listen for changes
    //settings.applySettingsToWindow(this);

  }

  void updateTitle() {

    StringBuffer title = new StringBuffer();
    title.append("[");
    title.append(dataFile.getName());
    title.append("]: neutral loss");

    //setTitle(title.toString());
    neutralLossPlot.setTitle(title.toString());

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

  public RawDataFile getDataFile() {
    return this.dataFile;
  }

  @Nonnull
  @Override
  public Collection<RawDataFile> getRawDataFiles() {
    return new ArrayList<>(Collections.singletonList(dataFile));
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if(rawDataFiles == null || rawDataFiles.isEmpty()) {
      return;
    }

    // get first raw data file
    RawDataFile newFile = rawDataFiles.iterator().next();
    if (dataFile.equals(newFile)) {
      return;
    }

    // remove old dataset
    neutralLossPlot.getXYPlot().setDataset(
        neutralLossPlot.getXYPlot().indexOf(dataset),null);

    // add new dataset
    NeutralLossDataSet newDataset = new NeutralLossDataSet(
        newFile, xAxisType, rtRange, mzRange, numOfFragments, this);
    neutralLossPlot.addNeutralLossDataSet(newDataset);

    dataFile = newFile;
    dataset = newDataset;

    MZmineCore.getTaskController().addTask(newDataset, TaskPriority.HIGH);
    updateTitle();
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featurelists) {

  }
}
