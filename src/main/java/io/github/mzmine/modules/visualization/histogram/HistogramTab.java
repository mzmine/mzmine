/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.histogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class HistogramTab extends MZmineTab {

  //private final Scene mainScene;
  private final BorderPane mainPane;

  private HistogramChart histogram;

  private RawDataFile rawDataFiles[];
  private FeatureList featureList;

  private HistogramDataType dataType;
  private int numOfBins;
  private Range<Double> range;

  public HistogramTab(ParameterSet parameters) {
    super("Histogram Visualizer", true, false);

    featureList = parameters.getParameter(HistogramParameters.featureList).getValue().getMatchingFeatureLists()[0];

    //this.setTitle("Histogram of " + peakList.getName());

    mainPane = new BorderPane();
    //mainScene = new Scene(mainPane);

    // Use main CSS
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    //setScene(mainScene);

    rawDataFiles = parameters.getParameter(HistogramParameters.dataFiles).getValue();

    dataType = parameters.getParameter(HistogramParameters.dataRange).getType();
    numOfBins = parameters.getParameter(HistogramParameters.numOfBins).getValue();
    range = parameters.getParameter(HistogramParameters.dataRange).getValue();

    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    // setBackground(Color.white);

    // Creates plot and toolbar
    histogram = new HistogramChart();

    // Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    // Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);

    // BorderPane pnlPlot = new BorderPane();
    // pnlPlot.setBorder(BorderFactory.createCompoundBorder(one, two));
    // pnlPlot.setBackground(Color.white);

    // pnlPlot.add(histogram, BorderLayout.CENTER);

    mainPane.setCenter(histogram);
    setContent(mainPane);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(mainScene);

    // pack();

    // get the window settings parameter
    //ParameterSet paramSet =
    //    MZmineCore.getConfiguration().getModuleParameters(HistogramVisualizerModule.class);
    //WindowSettingsParameter settings = paramSet.getParameter(HistogramParameters.windowSettings);

    // update the window and listen for changes
    // settings.applySettingsToWindow(this);
    // this.addComponentListener(settings);

    //setMinWidth(600.0);
    //setMinHeight(400.0);

    if (featureList != null) {
      HistogramPlotDataset dataSet =
          new HistogramPlotDataset(featureList, rawDataFiles, numOfBins, dataType, range);
      histogram.addDataset(dataSet, dataType);
    }

  }

  HistogramChart getChart() {
    return histogram;
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return new ArrayList<>(Arrays.asList(rawDataFiles));
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
