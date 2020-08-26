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

package io.github.mzmine.modules.visualization.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramCursorPosition;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;

/**
 * MS/MS visualizer using JFreeChart library
 */
public class MsMsVisualizerWindow extends Stage {

  private static final Image axesIcon = FxIconUtil.loadImageFromResources("icons/axesicon.png");
  private static final Image dataPointsIcon =
      FxIconUtil.loadImageFromResources("icons/datapointsicon.png");
  private static final Image tooltipsIcon =
      FxIconUtil.loadImageFromResources("icons/tooltips2dploticon.png");
  private static final Image notooltipsIcon =
      FxIconUtil.loadImageFromResources("icons/notooltips2dploticon.png");
  private static final Image findIcon = FxIconUtil.loadImageFromResources("icons/search.png");

  private final Scene mainScene;
  private final BorderPane mainPane;
  private final ToolBar toolBar;
  private final Button toggleContinuousModeButton, findButton;
  private final ToggleButton toggleTooltipButton;
  private final MsMsPlot IDAPlot;
  private final MsMsBottomPanel bottomPanel;
  private MsMsDataSet dataset;
  private RawDataFile dataFile;

  public MsMsVisualizerWindow(RawDataFile dataFile, ParameterSet parameters) {
    Range<Double> rtRange = parameters.getParameter(MsMsParameters.retentionTimeRange).getValue();
    Range<Double> mzRange = parameters.getParameter(MsMsParameters.mzRange).getValue();
    final IntensityType intensityType =
        parameters.getParameter(MsMsParameters.intensityType).getValue();
    final NormalizationType normalizationType =
        parameters.getParameter(MsMsParameters.normalizationType).getValue();
    Double minPeakInt = parameters.getParameter(MsMsParameters.minPeakInt).getValue();
    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(mainScene);

    this.dataFile = dataFile;

    dataset = new MsMsDataSet(dataFile, rtRange, mzRange, intensityType, normalizationType,
        minPeakInt, this);

    IDAPlot = new MsMsPlot(dataFile, this, dataset, rtRange, mzRange);
    mainPane.setCenter(IDAPlot);

    toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);

    toggleContinuousModeButton = new Button(null, new ImageView(dataPointsIcon));
    toggleContinuousModeButton
        .setTooltip(new Tooltip("Toggle displaying of data points for the peaks"));
    toggleContinuousModeButton.setOnAction(e -> {
      IDAPlot.switchDataPointsVisible();
    });

    toggleTooltipButton = new ToggleButton(null, new ImageView(tooltipsIcon));
    toggleTooltipButton.setSelected(true);
    toggleTooltipButton.setTooltip(new Tooltip("Toggle displaying of tool tips on the peaks"));
    toggleTooltipButton.setOnAction(e -> {
      if (toggleTooltipButton.isSelected()) {
        IDAPlot.showPeaksTooltips(false);
        toggleTooltipButton.setSelected(false);
      } else {
        IDAPlot.showPeaksTooltips(true);
        toggleTooltipButton.setSelected(true);
      }
    });

    findButton = new Button(null, new ImageView(findIcon));
    findButton.setTooltip(new Tooltip("Search for MS/MS spectra with specific ions"));
    findButton.setOnAction(e -> {
      // Parameters
      final DoubleParameter inputMZ =
          new DoubleParameter("Ion m/z", "m/z value of ion to search for.");

      final MZToleranceParameter inputMZTolerance = new MZToleranceParameter();

      final DoubleParameter inputIntensity = new DoubleParameter("Min. ion intensity",
          "Only ions with intensities above this value will be searched for.");

      final BooleanParameter inputNL = new BooleanParameter("Neutral Loss",
          "If selected, the ion to be searched for will be a neutral loss ion.\nIn this case, only ions above the min. intensity will be examined.",
          false);

      final ComboParameter<Colors> inputColors = new ComboParameter<Colors>("Color",
          "The color which the data points will be marked with.", Colors.values());

      Parameter<?>[] findParams = new Parameter<?>[5];
      findParams[0] = inputMZ;
      findParams[1] = inputMZTolerance;
      findParams[2] = inputIntensity;
      findParams[3] = inputNL;
      findParams[4] = inputColors;

      final ParameterSet parametersSearch = new SimpleParameterSet(findParams);
      ExitCode exitCode = parametersSearch.showSetupDialog(true);

      if (exitCode != ExitCode.OK) {
        return;
      }

      double searchMZ = parametersSearch.getParameter(inputMZ).getValue();
      MZTolerance searchMZTolerance = parametersSearch.getParameter(inputMZTolerance).getValue();
      double minIntensity = parametersSearch.getParameter(inputIntensity).getValue();
      boolean neutralLoss = parametersSearch.getParameter(inputNL).getValue();

      Color highligtColor = Color.red;

      if (parametersSearch.getParameter(inputColors).getValue().equals(Colors.green)) {
        highligtColor = Color.green;
      }
      if (parametersSearch.getParameter(inputColors).getValue().equals(Colors.blue)) {
        highligtColor = Color.blue;
      }

      // Find and highlight spectra with specific ion
      dataset.highlightSpectra(searchMZ, searchMZTolerance, minIntensity, neutralLoss,
          highligtColor);

      // Add legend entry
      LegendItemCollection chartLegend = IDAPlot.getXYPlot().getLegendItems();
      chartLegend.add(new LegendItem("Ion: " + searchMZ, "",
          "MS/MS spectra which contain the " + searchMZ + " ion\nTolerance: "
              + searchMZTolerance.toString() + "\nMin intensity: " + minIntensity,
          "", new Ellipse2D.Double(0, 0, 7, 7), highligtColor));
      IDAPlot.getXYPlot().setFixedLegendItems(chartLegend);
    });

    toolBar.getItems().addAll(toggleContinuousModeButton, toggleTooltipButton, findButton);
    mainPane.setRight(toolBar);

    bottomPanel = new MsMsBottomPanel(this, dataFile, parameters);
    bottomPanel.setPadding(new Insets(10));
    mainPane.setBottom(bottomPanel);

    updateTitle();

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);

    // get the window settings parameter
    ParameterSet paramSet =
        MZmineCore.getConfiguration().getModuleParameters(MsMsVisualizerModule.class);
    WindowSettingsParameter settings = paramSet.getParameter(MsMsParameters.windowSettings);

    // update the window and listen for changes
    settings.applySettingsToWindow(this);
    // this.addComponentListener(settings);
  }

  void updateTitle() {
    StringBuffer title = new StringBuffer();
    title.append("Time vs. m/z for precursor ions\n");
    title.append(dataFile.getName());
    IDAPlot.setTitle(title.toString());
  }

  /**
   * @return current cursor position
   */
  public ChromatogramCursorPosition getCursorPosition() {
    double selectedRT = IDAPlot.getXYPlot().getDomainCrosshairValue();
    double selectedMZ = IDAPlot.getXYPlot().getRangeCrosshairValue();

    int index = dataset.getIndex(selectedRT, selectedMZ);

    if (index >= 0) {
      double intensity = (double) dataset.getZ(0, index);
      ChromatogramCursorPosition pos = new ChromatogramCursorPosition(selectedRT, selectedMZ,
          intensity,
          dataset.getDataFile(), dataset.getScanNumber(index));
      return pos;
    }

    return null;
  }


  MsMsPlot getPlot() {
    return IDAPlot;
  }
}
