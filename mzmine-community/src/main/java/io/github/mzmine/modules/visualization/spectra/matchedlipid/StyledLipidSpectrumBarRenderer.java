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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.main.ConfigService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

/**
 * Bar renderer for lipid matched-fragment spectrum plots that draws styled signal bars and
 * collision-avoiding, connector-linked labels for each annotated fragment peak.
 */
public class StyledLipidSpectrumBarRenderer extends ColoredXYBarRenderer {

  private static final double LABEL_CLIP_PADDING = 2d;
  private static final double LABEL_OVERLAP_PADDING = 2d;
  private static final double LABEL_X_OFFSET = 10d;
  private static final double LABEL_Y_OFFSET = 8d;
  private static final double SIGNAL_CONNECTOR_GAP = 3.5d;
  private static final double LABEL_CONNECTOR_GAP = 1.5d;
  private static final int MAX_VERTICAL_ATTEMPTS = 6;
  private final @NotNull Paint borderPaint;
  private final @NotNull List<Rectangle2D> placedLabelBounds = new ArrayList<>();

  public StyledLipidSpectrumBarRenderer(final boolean isTransparent,
      final @NotNull Paint borderPaint) {
    super(isTransparent);
    this.borderPaint = borderPaint;
  }

  @Override
  public @NotNull XYItemRendererState initialise(final @NotNull java.awt.Graphics2D g2,
      final @NotNull Rectangle2D dataArea, final @NotNull XYPlot plot,
      final @NotNull XYDataset dataset, final PlotRenderingInfo info) {
    placedLabelBounds.clear();
    return super.initialise(g2, dataArea, plot, dataset, info);
  }

  @Override
  protected void drawItemLabel(final @NotNull java.awt.Graphics2D g2,
      final @NotNull XYDataset dataset, final int series, final int item,
      final @NotNull XYPlot plot,
      final XYItemLabelGenerator generator,
      final @NotNull Rectangle2D bar, final boolean negative) {
    if (generator == null) {
      return;
    }
    final String label = generator.generateLabel(dataset, series, item);
    if (label == null || label.isBlank()) {
      return;
    }

    final PlotOrientation orientation = plot.getOrientation();
    final double anchorX = orientation.isHorizontal() ? bar.getMaxX() : bar.getCenterX();
    final double anchorY = orientation.isHorizontal() ? bar.getCenterY() : bar.getMinY();
    final Rectangle2D clipBounds = g2.getClipBounds();
    final Rectangle2D labelBounds = findPlacement(g2, label, clipBounds, anchorX, anchorY);
    if (labelBounds == null) {
      return;
    }
    drawConnector(g2, anchorX, anchorY, labelBounds);
    LipidLabelPainter.drawLabelAt(g2, labelBounds, label, borderPaint);
    placedLabelBounds.add(new Rectangle2D.Double(labelBounds.getX(), labelBounds.getY(),
        labelBounds.getWidth(), labelBounds.getHeight()));
  }

  private @Nullable Rectangle2D findPlacement(final @NotNull java.awt.Graphics2D g2,
      final @NotNull String label, final @Nullable Rectangle2D clipBounds, final double anchorX,
      final double anchorY) {
    final Rectangle2D labelSize = LipidLabelPainter.measureLabelBounds(g2, label);
    final double width = labelSize.getWidth();
    final double height = labelSize.getHeight();
    final double verticalStep = height + 4d;

    for (int offsetIndex = 0; offsetIndex <= MAX_VERTICAL_ATTEMPTS; offsetIndex++) {
      final double shift = offsetIndex * verticalStep;
      final Rectangle2D rightUp = candidateBounds(anchorX + LABEL_X_OFFSET,
          anchorY - (height + LABEL_Y_OFFSET) - shift, width, height, clipBounds);
      if (isPlacementFree(rightUp)) {
        return rightUp;
      }
      final Rectangle2D rightDown = candidateBounds(anchorX + LABEL_X_OFFSET,
          anchorY + LABEL_Y_OFFSET + shift, width, height, clipBounds);
      if (isPlacementFree(rightDown)) {
        return rightDown;
      }
      final Rectangle2D leftUp = candidateBounds(anchorX - (width + LABEL_X_OFFSET),
          anchorY - (height + LABEL_Y_OFFSET) - shift, width, height, clipBounds);
      if (isPlacementFree(leftUp)) {
        return leftUp;
      }
      final Rectangle2D leftDown = candidateBounds(anchorX - (width + LABEL_X_OFFSET),
          anchorY + LABEL_Y_OFFSET + shift, width, height, clipBounds);
      if (isPlacementFree(leftDown)) {
        return leftDown;
      }
    }

    return null;
  }

  private @NotNull Rectangle2D candidateBounds(final double left, final double top,
      final double width, final double height, final @Nullable Rectangle2D clipBounds) {
    if (clipBounds == null) {
      return new Rectangle2D.Double(left, top, width, height);
    }
    final double maxLeft = clipBounds.getMaxX() - width - LABEL_CLIP_PADDING;
    final double maxTop = clipBounds.getMaxY() - height - LABEL_CLIP_PADDING;
    final double clampedLeft = Math.max(clipBounds.getMinX() + LABEL_CLIP_PADDING,
        Math.min(left, maxLeft));
    final double clampedTop = Math.max(clipBounds.getMinY() + LABEL_CLIP_PADDING,
        Math.min(top, maxTop));
    return new Rectangle2D.Double(clampedLeft, clampedTop, width, height);
  }

  private boolean isPlacementFree(final @NotNull Rectangle2D candidate) {
    final Rectangle2D padded = new Rectangle2D.Double(candidate.getX() - LABEL_OVERLAP_PADDING,
        candidate.getY() - LABEL_OVERLAP_PADDING,
        candidate.getWidth() + 2d * LABEL_OVERLAP_PADDING,
        candidate.getHeight() + 2d * LABEL_OVERLAP_PADDING);
    for (final Rectangle2D existing : placedLabelBounds) {
      if (existing.intersects(padded)) {
        return false;
      }
    }
    return true;
  }

  private void drawConnector(final @NotNull java.awt.Graphics2D g2, final double anchorX,
      final double anchorY, final @NotNull Rectangle2D labelBounds) {
    final double rawToX = clamp(anchorX, labelBounds.getMinX(), labelBounds.getMaxX());
    final double rawToY = clamp(anchorY, labelBounds.getMinY(), labelBounds.getMaxY());
    final double dx = rawToX - anchorX;
    final double dy = rawToY - anchorY;
    final double length = Math.hypot(dx, dy);
    if (length <= 0d) {
      return;
    }

    final double unitX = dx / length;
    final double unitY = dy / length;
    final double fromX = anchorX + unitX * SIGNAL_CONNECTOR_GAP;
    final double fromY = anchorY + unitY * SIGNAL_CONNECTOR_GAP;
    final double toX = rawToX - unitX * LABEL_CONNECTOR_GAP;
    final double toY = rawToY - unitY * LABEL_CONNECTOR_GAP;
    final Paint oldPaint = g2.getPaint();
    final Stroke oldStroke = g2.getStroke();
    try {
      g2.setPaint(connectorPaint());
      g2.setStroke(new BasicStroke(0.7f));
      g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
    } finally {
      g2.setPaint(oldPaint);
      g2.setStroke(oldStroke);
    }
  }

  private static double clamp(final double value, final double min, final double max) {
    return Math.max(min, Math.min(max, value));
  }

  private static @NotNull Paint connectorPaint() {
    final Color neutral = ConfigService.getDefaultColorPalette().getNeutralColorAWT();
    return new Color(neutral.getRed(), neutral.getGreen(), neutral.getBlue(), 185);
  }
}
