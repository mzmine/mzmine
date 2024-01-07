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
