/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.isotope_labeling;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Random;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;

/**
 * Extends {@link StatisticalBarRenderer} to overlay individual-sample jitter dots. Keeps the
 * original error-bar rendering and group-based coloring; replaces the old
 * {@code LineAndShapeRenderer} overlay that used incorrect x positions.
 *
 * <p>Dataset layout: series (rows) = sample groups ("Labeled", "Unlabeled"), categories (columns)
 * = isotopologues ("M+0", "M+1", …).
 *
 * <p>Jitter map: {@code isotopologueKey → groupKey → per-sample double[]}.
 */
public class GroupedBarJitterRenderer extends StatisticalBarRenderer {

  private static final double DOT_RADIUS = 3.5;

  // isotopologueKey (column/category) → groupKey (row/series) → per-sample values
  private final Map<String, Map<String, double[]>> jitterValues;

  public GroupedBarJitterRenderer(Map<String, Map<String, double[]>> jitterValues) {
    this.jitterValues = jitterValues;
    setErrorIndicatorPaint(Color.BLACK);
    setItemMargin(0.05);
    setShadowVisible(false);
  }

  @Override
  public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
      CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset,
      int row, int column, int pass) {

    super.drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column, pass);

    // getPassCount() == 1 for StatisticalBarRenderer → pass is always 0, but guard anyway
    if (pass != getPassCount() - 1 || jitterValues == null) {
      return;
    }

    String isotopologueKey = (String) dataset.getColumnKey(column);
    String groupKey = (String) dataset.getRowKey(row);

    Map<String, double[]> isoMap = jitterValues.get(isotopologueKey);
    if (isoMap == null) {
      return;
    }
    double[] values = isoMap.get(groupKey);
    if (values == null || values.length == 0) {
      return;
    }

    // x = centre of this series' bar within this category
    RectangleEdge domainEdge = plot.getDomainAxisEdge();
    double barCenterX = domainAxis.getCategorySeriesMiddle(column, dataset.getColumnCount(),
        row, dataset.getRowCount(), getItemMargin(), dataArea, domainEdge);

    Paint seriesPaint = getItemPaint(row, column);
    Color baseColor = (seriesPaint instanceof Color c) ? c : Color.GRAY;
    Color dotColor = contrastColor(baseColor);

    RectangleEdge rangeEdge = plot.getRangeAxisEdge();
    Random rng = new Random((long) row * 31 + column);
    double halfJitter = state.getBarWidth() * 0.25;

    g2.setColor(dotColor);
    for (double value : values) {
      double dotX = barCenterX + (rng.nextDouble() - 0.5) * 2.0 * halfJitter;
      double dotY = rangeAxis.valueToJava2D(value, dataArea, rangeEdge);
      g2.fill(new Ellipse2D.Double(dotX - DOT_RADIUS, dotY - DOT_RADIUS,
          DOT_RADIUS * 2, DOT_RADIUS * 2));
    }
  }

  private static Color contrastColor(Color base) {
    double brightness = 0.299 * base.getRed() + 0.587 * base.getGreen() + 0.114 * base.getBlue();
    if (brightness < 60) {
      return new Color(255, 255, 255, 220);
    }
    if (brightness > 200) {
      return new Color(40, 40, 40, 220);
    }
    int r, g, b;
    if (brightness >= 128) {
      r = Math.max(0, base.getRed() - 80);
      g = Math.max(0, base.getGreen() - 80);
      b = Math.max(0, base.getBlue() - 80);
    } else {
      r = Math.min(255, base.getRed() + 80);
      g = Math.min(255, base.getGreen() + 80);
      b = Math.min(255, base.getBlue() + 80);
    }
    return new Color(r, g, b, 220);
  }
}
