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

package io.github.mzmine.modules.visualization.scatterplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotChart;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

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
  private FeatureList featureList;

  public ScatterPlotTab(FeatureList featureList) {
    super("Scatter plot Visualizer", true, false);

    //setTitle("Scatter plot of " + featureList);
    this.featureList = featureList;

    mainPane = new BorderPane();
    //mainScene = new Scene(mainPane);

    // Use main CSS
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    //setScene(mainScene);

    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    topPanel = new ScatterPlotTopPanel();
    mainPane.setTop(topPanel);

    chart = new ScatterPlotChart(this, topPanel, featureList);
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

    bottomPanel = new ScatterPlotBottomPanel(this, chart, featureList);
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

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return featureList.getRawDataFiles();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return new ArrayList<>(Collections.singletonList((ModularFeatureList) featureList));
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists) {

  }
}
