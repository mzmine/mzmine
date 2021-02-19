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

package io.github.mzmine.modules.visualization.msms_new;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYZDotRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;

public class MsMsChart extends SimpleXYZScatterPlot<MsMsDataProvider> {

  MsMsDataProvider dataset;
  ColoredXYZDotRenderer renderer;

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
    setDefaultRenderer(renderer);

    dataset = new MsMsDataProvider(parameters);
    Platform.runLater(() -> addDataset(dataset));

    getXYPlot().getDomainAxis().setUpperMargin(0);
    getXYPlot().getDomainAxis().setLowerMargin(0);
    getXYPlot().getRangeAxis().setUpperMargin(0);
    getXYPlot().getRangeAxis().setLowerMargin(0);

    // Show spectrum of the clicked data point scan
    setOnMousePressed(event -> {

      if (!event.getButton().equals(MouseButton.PRIMARY) || event.getClickCount() != 2) {
        return;
      }

      MsMsDataPoint clickedDataPoint = dataset.getDataPoint(getCursorPosition().getValueIndex());

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

  public void setXAxisType(MsMsXYAxisType xAxisType) {
    dataset.setXAxisType(xAxisType);
    setDataset(dataset);
  }

  public void setYAxisType(MsMsXYAxisType yAxisType) {
    dataset.setYAxisType(yAxisType);
    setDataset(dataset);
  }

  public void setZAxisType(MsMsZAxisType zAxisType) {
    dataset.setZAxisType(zAxisType);
    setDataset(dataset);
  }

  public void highlightPrecursorMz(Range<Double> closed) {
    dataset.highlightPrecursorMz(closed);
    setDataset(dataset);
  }

}
