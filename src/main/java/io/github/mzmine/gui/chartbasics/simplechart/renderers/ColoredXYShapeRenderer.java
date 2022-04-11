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

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ColorProvider;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

/**
 * The standard shape renderer for {@link SimpleXYChart}s.
 * <p></p>
 * This renderer has been modified to draw a dataset, generate labels and legend items based on the
 * color specified by the dataset.
 */
public class ColoredXYShapeRenderer extends XYAreaRenderer {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private static final float OPACITY = 0.45f;
  private XYDataset currentDataset;

  public ColoredXYShapeRenderer() {
    super();
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  private static Composite makeComposite(final float alpha) {

    return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
  }

  @Override
  public void drawItem(final Graphics2D g2, final XYItemRendererState state,
      final Rectangle2D dataArea, final PlotRenderingInfo info, final XYPlot plot,
      final ValueAxis domainAxis, final ValueAxis rangeAxis, final XYDataset dataSet,
      final int series, final int item, final CrosshairState crosshairState, final int pass) {

    currentDataset = dataSet;

    /*Paint seriesColor = (dataSet instanceof ColoredXYDataset) ?
        ((ColoredXYDataset) dataSet).getAWTColor() : getItemPaint(series, item);
    System.out.println(
        "Drawing dataset " + dataSet.getSeriesKey(0) + "with color " + seriesColor.toString());

    g2.setPaint(seriesColor);*/

    g2.setComposite(makeComposite(OPACITY));

    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataSet, series, item,
        crosshairState, pass);
  }

  @Override
  public LegendItem getLegendItem(int datasetIndex, int series) {
    LegendItem result = null;
    XYPlot xyplot = getPlot();
    if (xyplot != null) {
      XYDataset dataset = xyplot.getDataset(datasetIndex);
      if (dataset != null) {
        XYSeriesLabelGenerator lg = getLegendItemLabelGenerator();
        String label = lg.generateLabel(dataset, series);
        String description = label;
        String toolTipText = null;
        if (getLegendItemToolTipGenerator() != null) {
          toolTipText = getLegendItemToolTipGenerator().generateLabel(dataset, series);
        }
        String urlText = null;
        if (getLegendItemURLGenerator() != null) {
          urlText = getLegendItemURLGenerator().generateLabel(dataset, series);
        }
        Paint paint = lookupSeriesPaint(series);
        if (dataset instanceof ColorProvider) {
          paint = ((ColorProvider) dataset).getAWTColor();
        }
        result = new LegendItem(label, description, toolTipText, urlText, super.getLegendArea(),
            paint);
        result.setLabelFont(lookupLegendTextFont(series));
        Paint labelPaint = lookupLegendTextPaint(series);
        if (labelPaint != null) {
          result.setLabelPaint(labelPaint);
        }
        result.setDataset(dataset);
        result.setDatasetIndex(datasetIndex);
        result.setSeriesKey(dataset.getSeriesKey(series));
        result.setSeriesIndex(series);
      }
    }

    return result;
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    if (currentDataset instanceof ColoredXYDataset) {
      return ((ColoredXYDataset) currentDataset).getAWTColor();
    }
    return lookupSeriesPaint(row);
  }

}

