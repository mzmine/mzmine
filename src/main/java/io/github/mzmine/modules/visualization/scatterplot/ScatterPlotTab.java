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

package io.github.mzmine.modules.visualization.scatterplot;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotChart;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
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
import javafx.stage.Stage;
import javax.annotation.Nonnull;

/**
 * Main window of the scatter plot visualizer.
 *
 */
public class ScatterPlotTab extends MZmineTab {

  static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");

  //private final Scene mainScene;
  private final BorderPane mainPane;
  private final ToolBar toolbar;
  private final Button axesButton;
  private final ScatterPlotChart chart;
  private final ScatterPlotTopPanel topPanel;
  private final ScatterPlotBottomPanel bottomPanel;
  private PeakList peakList;

  public ScatterPlotTab(PeakList peakList) {
    super("Scatter plot Visualizer", true, false);

    //setTitle("Scatter plot of " + peakList);
    this.peakList = peakList;

    mainPane = new BorderPane();
    //mainScene = new Scene(mainPane);

    // Use main CSS
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    //setScene(mainScene);

    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    topPanel = new ScatterPlotTopPanel();
    mainPane.setTop(topPanel);

    chart = new ScatterPlotChart(this, topPanel, peakList);
    // Border border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    // chart.setBorder(border);
    // chart.setBackground(Color.white);
    mainPane.setCenter(chart);

    toolbar = new ToolBar();
    toolbar.setOrientation(Orientation.VERTICAL);

    axesButton = new Button(null, new ImageView(axesIcon));
    axesButton.setTooltip(new Tooltip("Setup ranges for axes"));
    axesButton.setOnAction(e -> {
      AxesSetupDialog dialog = new AxesSetupDialog(MZmineCore.getDesktop().getMainWindow(), chart.getPlot());
      dialog.show();
    });
    toolbar.getItems().add(axesButton);

    mainPane.setRight(toolbar);

    // JComponent leftMargin = (JComponent) Box.createRigidArea(new Dimension(10, 10));
    // leftMargin.setOpaque(false);
    // add(leftMargin, BorderLayout.WEST);

    bottomPanel = new ScatterPlotBottomPanel(this, chart, peakList);
    mainPane.setBottom(bottomPanel);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(mainScene);


    // get the window settings parameter
    //ParameterSet paramSet =
    //    MZmineCore.getConfiguration().getModuleParameters(ScatterPlotVisualizerModule.class);
    //WindowSettingsParameter settings = paramSet.getParameter(ScatterPlotParameters.windowSettings);

    // update the window and listen for changes
    // settings.applySettingsToWindow(this);
    // this.addComponentListener(settings);

    //setMinWidth(500.0);
    //setMinHeight(400.0);
    setContent(mainPane);

  }

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return peakList.getRawDataFiles();
  }

  @Nonnull
  @Override
  public Collection<? extends ModularFeatureList> getFeatureLists() {
    return new ArrayList<>(Collections.singletonList((ModularFeatureList)peakList));
  }

  @Nonnull
  @Override
  public Collection<? extends ModularFeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends ModularFeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends ModularFeatureList> featurelists) {

  }
}
