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
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;

/**
 * Extends {@link StackedBarRenderer} to overlay individual-sample jitter dots on each stacked bar
 * segment. The dataset layout is: series = isotopologues (M+0, M+1, …), categories = groups
 * (Labeled / Unlabeled). Dots are positioned at y = mean_stack_base + per_sample_value, and use a
 * contrasting color so they remain visible against any bar color including black and white.
 */
public class BarJitterRenderer extends StackedBarRenderer {

  private static final double DOT_RADIUS = 3.5;

  // categoryKey (group name) → isotopologueKey ("M+0", …) → per-sample values
  private final Map<String, Map<String, double[]>> jitterValues;

  public BarJitterRenderer(Map<String, Map<String, double[]>> jitterValues) {
    this.jitterValues = jitterValues;
    setBarPainter(new StandardBarPainter());
    setShadowVisible(false);
    setDrawBarOutline(false);
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    Paint base = getSeriesPaint(row);
    // Unlabeled categories get a lighter, less-saturated shade of the same hue.
    CategoryPlot p = getPlot();
    if (p != null && base instanceof Color c) {
      String catKey = (String) p.getDataset().getColumnKey(column);
      if (catKey.contains("Unlabeled") || catKey.contains("unlabeled")) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] * 0.45f,
            Math.min(1.0f, hsb[2] * 1.35f + 0.1f));
      }
    }
    return base;
  }

  @Override
  public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
      CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset,
      int row, int column, int pass) {

    super.drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column, pass);

    // Draw dots only in the last rendering pass so they sit on top of all bar segments.
    if (pass != getPassCount() - 1 || jitterValues == null) {
      return;
    }

    String categoryKey = (String) dataset.getColumnKey(column);
    String isotopologueKey = (String) dataset.getRowKey(row);

    Map<String, double[]> catMap = jitterValues.get(categoryKey);
    if (catMap == null) {
      return;
    }
    double[] values = catMap.get(isotopologueKey);
    if (values == null || values.length == 0) {
      return;
    }

    // Stack base: sum of mean values for earlier series in this category column.
    double stackBase = 0;
    for (int s = 0; s < row; s++) {
      Number v = dataset.getValue(s, column);
      if (v != null) {
        stackBase += v.doubleValue();
      }
    }

    // Bar x-centre: the centre of the category on the domain axis.
    RectangleEdge domainEdge = plot.getDomainAxisEdge();
    double barCenterX = domainAxis.getCategoryMiddle(column, dataset.getColumnCount(), dataArea,
        domainEdge);

    Paint paint = getItemPaint(row, column);
    Color baseColor = (paint instanceof Color c) ? c : Color.GRAY;
    Color dotColor = contrastColor(baseColor);

    RectangleEdge rangeEdge = plot.getRangeAxisEdge();
    Random rng = new Random((long) row * 31 + column);
    double halfJitter = state.getBarWidth() * 0.25;

    g2.setColor(dotColor);
    for (double value : values) {
      double dotX = barCenterX + (rng.nextDouble() - 0.5) * 2.0 * halfJitter;
      double dotY = rangeAxis.valueToJava2D(stackBase + value, dataArea, rangeEdge);
      g2.fill(new Ellipse2D.Double(dotX - DOT_RADIUS, dotY - DOT_RADIUS,
          DOT_RADIUS * 2, DOT_RADIUS * 2));
    }
  }

  /**
   * Returns a contrasting dot color for any bar color, including black and white extremes.
   * Uses fixed white/charcoal for the extremes, and a fixed +/-80 RGB offset for mid-range colors.
   */
  private static Color contrastColor(Color base) {
    double brightness = 0.299 * base.getRed() + 0.587 * base.getGreen() + 0.114 * base.getBlue();
    if (brightness < 60) {
      return new Color(255, 255, 255, 220); // very dark (black etc.) → white dots
    }
    if (brightness > 200) {
      return new Color(40, 40, 40, 220);   // very bright (white etc.) → charcoal dots
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
