/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYSmallBlockRenderer;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;

/**
 * @author https://github.com/SteffenHeu & https://github.com/Annexhc
 */
public class SimpleXYZScatterPlot<T extends PlotXYZDataProvider> extends EChartViewer {

  protected static final Logger logger = Logger.getLogger(SimpleXYZScatterPlot.class.getName());

  protected static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
  protected final JFreeChart chart;
  private final XYPlot plot;
  private final TextTitle chartTitle;
  private final TextTitle chartSubTitle;
  protected ColoredXYSmallBlockRenderer blockRenderer;

  public SimpleXYZScatterPlot(@Nonnull String title) {

    super(ChartFactory.createScatterPlot("", "x", "y", null,
        PlotOrientation.VERTICAL, true, false, true));

    chart = getChart();
    chartTitle = new TextTitle(title);
    chart.setTitle(chartTitle);
    chartSubTitle = new TextTitle();
    chart.addSubtitle(chartSubTitle);
    plot = chart.getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    blockRenderer = new ColoredXYSmallBlockRenderer();
    initializePlot();

    chart.addChangeListener(new ChartChangeListener() {
      @Override
      public void chartChanged(ChartChangeEvent event) {
        logger.info("Chart changed: " + event.getSource().toString());
      }
    });

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
  }

  private PaintScaleLegend prepareLegend(double min, double max, LookupPaintScale scale) {
    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    PaintScaleLegend newLegend = new PaintScaleLegend(scale, scaleAxis);
    newLegend.setPadding(5, 0, 5, 0);
    newLegend.setStripOutlineVisible(false);
    newLegend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    newLegend.setAxisOffset(5.0);
    newLegend.setSubdivisionCount(500);
    newLegend.setPosition(RectangleEdge.RIGHT);
    newLegend.getAxis().setLabelFont(legendFont);
    newLegend.getAxis().setTickLabelFont(legendFont);
    return newLegend;
  }

  public XYPlot getPlot() {
    return plot;
  }

  private void initializePlot() {
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
  }

  public void setDataset(ColoredXYZDataset dataset) {
    plot.setDataset(dataset);

    plot.setRenderer(blockRenderer);
    blockRenderer.setBlockHeight(dataset.getBoxHeight());
    blockRenderer.setBlockWidth(dataset.getBoxWidth());
    PaintScaleLegend legend = null;
    if (dataset.getPaintScale() == null) {
      LookupPaintScale ps = new LookupPaintScale(0, 10000, Color.RED);
      blockRenderer.setPaintScale(ps);
      legend = prepareLegend(0, 1000, ps);
    } else {
      blockRenderer.setPaintScale(dataset.getPaintScale());
      legend = prepareLegend(dataset.getMinZValue(), dataset.getMaxZValue(),
          dataset.getPaintScale());
    }
    chart.clearSubtitles();
    chart.addSubtitle(legend);
  }

  public void setDataset(T datasetProvider) {
    ColoredXYZDataset dataset = new ColoredXYZDataset(datasetProvider);
    setDataset(dataset);
  }
}
