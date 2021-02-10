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
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYZDotRenderer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;

public class MsMsChart extends SimpleXYZScatterPlot<MsMsDataset> {

  MsMsDataset dataset;
  ColoredXYZDotRenderer renderer;

  public MsMsChart(ParameterSet parameters) {
    super("MS/MS visualizer");

    MsMsAxisType xAxisType = parameters.getParameter(MsMsParameters.xAxisType).getValue();
    MsMsAxisType yAxisType = parameters.getParameter(MsMsParameters.yAxisType).getValue();

    setDomainAxisLabel(xAxisType.toString());
    setRangeAxisLabel(yAxisType.toString());
    setDomainAxisNumberFormatOverride(MZmineCore.getConfiguration().getRTFormat());
    setRangeAxisNumberFormatOverride(MZmineCore.getConfiguration().getMZFormat());

    renderer = new ColoredXYZDotRenderer();
    renderer.setDefaultToolTipGenerator(new SimpleToolTipGenerator());
    setDefaultRenderer(renderer);

    dataset = new MsMsDataset(parameters);
    Platform.runLater(() -> addDataset(dataset));

    setLegendCanvas(new Canvas());
  }

  public void setXAxisType(MsMsAxisType xAxisType) {
    dataset.setXAxisType(xAxisType);
    setDataset(dataset);
  }

  public void setYAxisType(MsMsAxisType yAxisType) {
    dataset.setYAxisType(yAxisType);
    setDataset(dataset);
  }

  public void highlightPrecursorMz(Range<Double> closed) {
    dataset.highlightPrecursorMz(closed);
    setDataset(dataset);
  }

}
