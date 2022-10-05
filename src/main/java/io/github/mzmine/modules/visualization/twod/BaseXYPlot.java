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
