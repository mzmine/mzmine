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

package io.github.mzmine.modules.visualization.twod;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetChangeEvent;

import com.google.common.collect.Range;

/**
 * Created by owen myers (oweenm@gmail.com) on 4/5/17.
 */

public class BaseXYPlot extends XYPlot {
  private static final long serialVersionUID = 1L;

  public Logger logger = Logger.getLogger(this.getClass().getName());

  public Range<Double> totalMZRange;
  public Range<Float> totalRTRange;
  public BufferedImage zoomOutBitmap;

  public TwoDDataSet dataset;

  public TwoDPaletteType paletteType = TwoDPaletteType.PALETTE_RAINBOW;

  public PlotMode plotMode = PlotMode.UNDEFINED;

  public boolean logScale;
  public double maxValue = 0;

  BaseXYPlot(TwoDDataSet dataset, Range<Float> rtRange, Range<Double> mzRange,
      ValueAxis domainAxis, ValueAxis rangeAxis) {

    super(dataset, domainAxis, rangeAxis, null);

    this.dataset = dataset;

    totalRTRange = rtRange;
    totalMZRange = mzRange;

  }

  public boolean render(final Graphics2D g2, final Rectangle2D dataArea, int index,
      PlotRenderingInfo info, CrosshairState crosshairState) {
    return super.render(g2, dataArea, index, info, crosshairState);

  }

  Range<Double> getDomainRange() {
    return Range.closed(getDomainAxis().getRange().getLowerBound(),
        getDomainAxis().getRange().getUpperBound());
  }

  Range<Double> getAxisRange() {
    return Range.closed(getRangeAxis().getRange().getLowerBound(),
        getRangeAxis().getRange().getUpperBound());
  }

  void switchPalette() {
    TwoDPaletteType types[] = TwoDPaletteType.values();
    int newIndex = paletteType.ordinal() + 1;
    if (newIndex >= types.length)
      newIndex = 0;
    paletteType = types[newIndex];
    zoomOutBitmap = null;
    datasetChanged(new DatasetChangeEvent(dataset, dataset));
  }

  PlotMode getPlotMode() {
    return plotMode;
  }

  void setPlotMode(PlotMode plotMode) {
    this.plotMode = plotMode;

    // clear the zoom out image cache
    zoomOutBitmap = null;

    datasetChanged(new DatasetChangeEvent(dataset, dataset));
  }

  void setLogScale(boolean logscale) {
    logScale = logscale;

    // clear the zoom out image cache
    zoomOutBitmap = null;

    datasetChanged(new DatasetChangeEvent(dataset, dataset));
  }

  public void setDataset(TwoDDataSet dataset) {
    this.dataset = dataset;
    setDataset(0, dataset);
  }

}
