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

package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

public class SimpleToolTipGenerator implements XYToolTipGenerator {

  private final EChartViewer chart;
  private final XYPlot plot;

  public SimpleToolTipGenerator(EChartViewer chart){
    super();

    this.chart = chart;
    plot = chart.getChart().getXYPlot();

    if(plot == null) {
      throw new IllegalArgumentException("SimpleToolTipGenerator can only be used for XY-plots.");
    }

  }

  @Override
  public String generateToolTip(XYDataset dataset, int series, int item) {
    if(!(dataset instanceof ColoredXYDataset)) {
      return plot.getDomainAxis().getLabel() + ": " + dataset.getX(series, item) + "\n" +
          plot.getRangeAxis().getLabel() + ": " + dataset.getY(series, item);
    }
    return ((ColoredXYDataset) dataset).getToolTip(item);
  }
}
