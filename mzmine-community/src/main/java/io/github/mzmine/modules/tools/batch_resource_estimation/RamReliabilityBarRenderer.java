/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleEdge;

/**
 * Keeps each run's color while distinguishing reliable RAM values from uncertain ones per step.
 */
final class RamReliabilityBarRenderer extends BarRenderer {

  private static final Stroke SOLID = new BasicStroke(1f);
  private static final Stroke HATCH = new BasicStroke(1.25f);
  private static final double HATCH_SPACING = 7;
  private static final Shape HATCHED_LEGEND = createHatchedLegend();

  private @NotNull List<BatchResourcePrediction> runs = List.of();

  RamReliabilityBarRenderer() {
    setBarPainter(new StandardBarPainter() {
      @Override
      public void paintBar(final Graphics2D g2, final BarRenderer renderer, final int row,
          final int column, final RectangularShape bar, final RectangleEdge base) {
        if (isReliable(row, column)) {
          super.paintBar(g2, renderer, row, column, bar, base);
          return;
        }
        // Trim line endpoints geometrically. FXGraphics2D.create() shares its canvas and
        // dispose() does not restore clipping, so clipping a child graphics hides later bars.
        // TexturePaint is also unsupported by the JavaFX canvas.
        g2.setPaint(renderer.getItemPaint(row, column));
        g2.setStroke(HATCH);
        for (double x = bar.getMinX() - bar.getHeight(); x < bar.getMaxX(); x += HATCH_SPACING) {
          final double startX = Math.max(x, bar.getMinX());
          final double endX = Math.min(x + bar.getHeight(), bar.getMaxX());
          g2.draw(new Line2D.Double(startX, bar.getMinY() + startX - x, endX,
              bar.getMinY() + endX - x));
        }
        g2.setPaint(renderer.getItemOutlinePaint(row, column));
        g2.setStroke(SOLID);
        g2.draw(bar);
      }
    });
    setShadowVisible(false);
    setDrawBarOutline(true);
  }

  void setRuns(@NotNull final List<BatchResourcePrediction> runs) {
    this.runs = List.copyOf(runs);
    fireChangeEvent();
  }

  private boolean isReliable(final int row, final int column) {
    return runs.get(row).steps().get(column).heapReliable();
  }

  @Override
  public Paint getItemOutlinePaint(final int row, final int column) {
    return super.getItemPaint(row, column);
  }

  @Override
  public Stroke getItemOutlineStroke(final int row, final int column) {
    return SOLID;
  }

  @Override
  public LegendItemCollection getLegendItems() {
    final LegendItemCollection items = super.getLegendItems();
    final Paint paint =
        getPlot() == null ? Color.GRAY : getPlot().getDomainAxis().getTickLabelPaint();
    final LegendItem reliable = new LegendItem("Reliable RAM", paint);
    final LegendItem unreliable = new LegendItem("Unreliable RAM", null, null, null, HATCHED_LEGEND,
        paint);
    reliable.setLabelPaint(paint);
    unreliable.setLabelPaint(paint);
    items.add(reliable);
    items.add(unreliable);
    return items;
  }

  private static Shape createHatchedLegend() {
    final Rectangle2D box = new Rectangle2D.Double(-6, -5, 12, 10);
    final Area shape = new Area(SOLID.createStrokedShape(box));
    for (double x = box.getMinX() - box.getHeight(); x < box.getMaxX(); x += HATCH_SPACING) {
      shape.add(new Area(HATCH.createStrokedShape(
          new Line2D.Double(x, box.getMinY(), x + box.getHeight(), box.getMaxY()))));
    }
    shape.intersect(new Area(box));
    return shape;
  }
}
