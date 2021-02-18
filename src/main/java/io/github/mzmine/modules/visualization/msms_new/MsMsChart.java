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
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYZDotRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;

public class MsMsChart extends SimpleXYZScatterPlot<MsMsDataProvider> {

  MsMsDataProvider dataset;
  ColoredXYZDotRenderer renderer;

  public MsMsChart(ParameterSet parameters) {
    super("MS/MS visualizer");

    MsMsXYAxisType xAxisType = parameters.getParameter(MsMsParameters.xAxisType).getValue();
    MsMsXYAxisType yAxisType = parameters.getParameter(MsMsParameters.yAxisType).getValue();

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
