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

package io.github.mzmine.modules.visualization.msms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYZDotRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.util.SortOrder;

public class MsMsChart extends SimpleXYZScatterPlot<MsMsDataProvider> {

  private MsMsDataProvider dataProvider;
  private ColoredXYZDataset dataset;
  private ColoredXYZDotRenderer renderer;
  private SortOrder zOrder;

  public MsMsChart(ParameterSet parameters) {
    super("MS/MS visualizer");

    MsMsXYAxisType xAxisType = parameters.getParameter(MsMsParameters.xAxisType).getValue();
    MsMsXYAxisType yAxisType = parameters.getParameter(MsMsParameters.yAxisType).getValue();
    RawDataFile[] dataFiles = parameters.getParameter(MsMsParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();

    setDomainAxisLabel(xAxisType.toString());
    setRangeAxisLabel(yAxisType.toString());
    setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getMZFormat());

    renderer = new ColoredXYZDotRenderer();

    dataProvider = new MsMsDataProvider(parameters);
    dataset = new ColoredXYZDataset(dataProvider);
    addDataset(dataset, renderer);

    getXYPlot().getDomainAxis().setUpperMargin(0);
    getXYPlot().getDomainAxis().setLowerMargin(0);
    getXYPlot().getRangeAxis().setUpperMargin(0);
    getXYPlot().getRangeAxis().setLowerMargin(0);

    getXYPlot().setBackgroundPaint(Color.WHITE);

    // Show spectrum of the clicked data point scan
    setOnMousePressed(event -> {

      if (!event.getButton().equals(MouseButton.PRIMARY) || event.getClickCount() != 2) {
        return;
      }

      MsMsDataPoint clickedDataPoint = dataProvider.getDataPoint(getCursorPosition().getValueIndex());

      // Run spectrum module
      ParameterSet spectrumParameters =
          MZmineCore.getConfiguration().getModuleParameters(SpectraVisualizerModule.class);
      spectrumParameters.getParameter(SpectraVisualizerParameters.dataFiles)
          .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, dataFiles);
      spectrumParameters.getParameter(SpectraVisualizerParameters.scanNumber)
          .setValue(clickedDataPoint.getScanNumber());
      MZmineCore.runMZmineModule(SpectraVisualizerModule.class, spectrumParameters);
    });

    // Do not show legend
    setLegendCanvas(new Canvas());
  }

  public MsMsXYAxisType getXAxisType() {
    return dataProvider.getXAxisType();
  }

  public MsMsXYAxisType getYAxisType() {
    return dataProvider.getYAxisType();
  }

  public void setXAxisType(MsMsXYAxisType xAxisType) {
    dataProvider.setXAxisType(xAxisType);
    dataProvider.sortZValues(zOrder);
    dataset.fireDatasetChanged();
  }

  public void setYAxisType(MsMsXYAxisType yAxisType) {
    dataProvider.setYAxisType(yAxisType);
    dataProvider.sortZValues(zOrder);
    dataset.fireDatasetChanged();
  }

  public void setZAxisType(MsMsZAxisType zAxisType) {
    dataProvider.setZAxisType(zAxisType);
    dataProvider.sortZValues(zOrder);
    dataset.fireDatasetChanged();
  }

  public void highlightPoints(MsMsXYAxisType valuesType1, @Nullable Range<Double> range1,
      MsMsXYAxisType valuesType2, @Nullable Range<Double> range2) {
    dataProvider.highlightPoints(valuesType1, range1, valuesType2, range2);
    dataProvider.sortZValues(zOrder);
    dataset.fireDatasetChanged();
  }

  public void setDataFile(RawDataFile dataFile) {
    dataProvider.setDataFile(dataFile);
    dataset.run();
    dataProvider.sortZValues(zOrder);
    dataset.fireDatasetChanged();
  }

  public void sortZValues(SortOrder zOrder) {
    this.zOrder = zOrder;
    dataProvider.sortZValues(zOrder);
    renderer.setZOrder(zOrder);
    dataset.fireDatasetChanged();
  }

  public SimpleObjectProperty<TaskStatus> datasetStatusProperty() {
    return dataset.statusProperty();
  }

}
