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

package io.github.mzmine.modules.visualization.twod;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerTab extends MZmineTab {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final Image paletteIcon =
      FxIconUtil.loadImageFromResources("icons/colorbaricon.png");
  private static final Image dataPointsIcon =
      FxIconUtil.loadImageFromResources("icons/datapointsicon.png");
  private static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");
  private static final Image centroidIcon =
      FxIconUtil.loadImageFromResources("icons/centroidicon.png");
  private static final Image continuousIcon =
      FxIconUtil.loadImageFromResources("icons/continuousicon.png");
  private static final Image tooltipsIcon =
      FxIconUtil.loadImageFromResources("icons/tooltips2dploticon.png");
  private static final Image notooltipsIcon =
      FxIconUtil.loadImageFromResources("icons/notooltips2dploticon.png");
  private static final Image logScaleIcon = FxIconUtil.loadImageFromResources("icons/logicon.png");

  //private final Scene mainScene;
  private final BorderPane mainPane;
  private final ToolBar toolBar;
  private final TwoDPlot twoDPlot;
  private final TwoDBottomPanel bottomPanel;

  private TwoDDataSet dataset;
  private RawDataFile dataFile;
  private final Range<Float> rtRange;
  private final Range<Double> mzRange;
  private final ParameterSet parameters;

  public TwoDVisualizerTab(RawDataFile dataFile, Scan scans[], Range<Float> rtRange,
      Range<Double> mzRange, ParameterSet parameters) {

    super("2D Visualizer", true, false);

    //setTitle("2D view: [" + dataFile.getName() + "]");

    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    // setBackground(Color.white);

    this.dataFile = dataFile;
    this.rtRange = rtRange;
    this.mzRange = mzRange;
    this.parameters = parameters;

    mainPane = new BorderPane();
    //mainScene = new Scene(mainPane);

    // Use main CSS
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    //setScene(mainScene);
    setContent(mainPane);

    dataset = new TwoDDataSet(dataFile, scans, rtRange, mzRange, this);
    if (parameters.getParameter(TwoDVisualizerParameters.plotType).getValue() == PlotType.FAST2D) {
      twoDPlot = new TwoDPlot(dataFile, this, dataset, rtRange, mzRange, "default");
    } else {
      twoDPlot = new TwoDPlot(dataFile, this, dataset, rtRange, mzRange, "point2D");
    }
    mainPane.setCenter(twoDPlot);

    toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);

    Button paletteBtn = new Button(null, new ImageView(paletteIcon));
    paletteBtn.setTooltip(new Tooltip("Switch palette"));
    paletteBtn.setOnAction(e -> {
      twoDPlot.getXYPlot().switchPalette();
    });


    Button toggleContinuousModeButton = new Button(null, new ImageView(dataPointsIcon));
    toggleContinuousModeButton
        .setTooltip(new Tooltip("Toggle displaying of data points in continuous mode"));
    toggleContinuousModeButton.setOnAction(e -> {
      twoDPlot.switchDataPointsVisible();
    });

    Button axesButton = new Button(null, new ImageView(axesIcon));
    axesButton.setTooltip(new Tooltip("Setup ranges for axes"));
    axesButton.setOnAction(e -> {
      AxesSetupDialog dialog = new AxesSetupDialog(getTabPane().getScene().getWindow(), twoDPlot.getXYPlot());
      dialog.showAndWait();
    });

    Button centroidContinuousButton = new Button(null, new ImageView(centroidIcon));
    centroidContinuousButton
        .setTooltip(new Tooltip("Switch between continuous and centroided mode"));
    centroidContinuousButton.setOnAction(e -> {
      if (twoDPlot.getPlotMode() == PlotMode.CENTROID) {
        centroidContinuousButton.setGraphic(new ImageView(centroidIcon));
        twoDPlot.setPlotMode(PlotMode.CONTINUOUS);
      } else {
        centroidContinuousButton.setGraphic(new ImageView(continuousIcon));
        twoDPlot.setPlotMode(PlotMode.CENTROID);
      }
    });

    ToggleButton toggleTooltipButton = new ToggleButton(null, new ImageView(tooltipsIcon));
    toggleTooltipButton.setTooltip(new Tooltip("Toggle displaying of tool tips on the peaks"));
    toggleTooltipButton.setSelected(true);
    toggleTooltipButton.setOnAction(e -> {
      if (toggleTooltipButton.isSelected()) {
        twoDPlot.showFeaturesTooltips(false);
        toggleTooltipButton.setGraphic(new ImageView(notooltipsIcon));
      } else {
        twoDPlot.showFeaturesTooltips(true);
        toggleTooltipButton.setGraphic(new ImageView(tooltipsIcon));
      }
      toggleTooltipButton.setSelected(!toggleTooltipButton.isSelected());
    });

    ToggleButton logScaleButton = new ToggleButton(null, new ImageView(logScaleIcon));
    logScaleButton.setTooltip(new Tooltip("Set log scale"));
    logScaleButton.setOnAction(e -> {
      boolean logScale = !logScaleButton.isSelected();
      logScaleButton.setSelected(logScale);
      twoDPlot.setLogScale(logScale);
    });

    toolBar.getItems().addAll(paletteBtn, toggleContinuousModeButton, axesButton,
        centroidContinuousButton, toggleTooltipButton, logScaleButton);

    mainPane.setRight(toolBar);

    bottomPanel = new TwoDBottomPanel(this, dataFile, parameters);
    mainPane.setBottom(bottomPanel);

    updateTitle();

    // After we have constructed everything, load the feature lists into the
    // bottom panel
    // bottomPanel.rebuildPeakListSelector();

    // MZmineCore.getDesktop().addPeakListTreeListener(bottomPanel);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(mainScene);

    // pack();

    // get the window settings parameter
    //ParameterSet paramSet =
    //    MZmineCore.getConfiguration().getModuleParameters(TwoDVisualizerModule.class);
    //WindowSettingsParameter settings =
    //    paramSet.getParameter(TwoDVisualizerParameters.windowSettings);

    // update the window and listen for changes
    // settings.applySettingsToWindow(this);
    // this.addComponentListener(settings);

  }

  void updateTitle() {
    StringBuffer title = new StringBuffer();
    title.append("[");
    title.append(dataFile.getName());
    title.append("]: 2D view");
    twoDPlot.setTitle(title.toString());
  }

  TwoDPlot getPlot() {
    return twoDPlot;
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return new ArrayList<>(Collections.singletonList(dataFile));
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @NotNull
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

    // add new dataset
    ScanSelection scanSel =
        parameters.getParameter(TwoDVisualizerParameters.scanSelection).getValue();
    Scan newScans[] = scanSel.getMatchingScans(newFile);
    TwoDDataSet newDataset = new TwoDDataSet(newFile, newScans, rtRange, mzRange, this);
    twoDPlot.addTwoDDataSet(newDataset);

    dataFile = newFile;
    dataset = newDataset;

    updateTitle();
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists) {

  }
}
