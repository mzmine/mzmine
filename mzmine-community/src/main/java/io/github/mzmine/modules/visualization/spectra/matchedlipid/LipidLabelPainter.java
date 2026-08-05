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

import io.github.mzmine.main.ConfigService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for painting bordered text labels at a given anchor position on a {@link Graphics2D}
 * context, used to annotate lipid fragment signals in matched-lipid spectrum charts.
 */
public final class LipidLabelPainter {

  private static final double X_PADDING = 6d;
  private static final double Y_PADDING = 4d;

  private LipidLabelPainter() {
  }

  public static void drawLabel(final @NotNull Graphics2D g2,
      final @Nullable Rectangle2D bounds,
      final double anchorX, final double anchorY,
      final @NotNull String label,
      final @NotNull Paint borderPaint) {
    if (label.isBlank()) {
      return;
    }

    final Font oldFont = g2.getFont();
    final Paint oldPaint = g2.getPaint();
    final var oldStroke = g2.getStroke();
    try {
      final Rectangle2D boxSize = measureLabelBounds(g2, label);
      final double boxWidth = boxSize.getWidth();
      final double boxHeight = boxSize.getHeight();
      double left = anchorX + 10d;
      double top = anchorY - (boxHeight + 8d);

      if (bounds != null) {
        if (left + boxWidth > bounds.getMaxX()) {
          left = anchorX - (boxWidth + 10d);
        }
        if (left < bounds.getMinX()) {
          left = bounds.getMinX() + 2d;
        }
        if (top < bounds.getMinY()) {
          top = anchorY + 8d;
        }
        if (top + boxHeight > bounds.getMaxY()) {
          top = bounds.getMaxY() - boxHeight - 2d;
        }
      }

      drawLabelAt(g2, new Rectangle2D.Double(left, top, boxWidth, boxHeight), label, borderPaint);
    } finally {
      g2.setFont(oldFont);
      g2.setPaint(oldPaint);
      g2.setStroke(oldStroke);
    }
  }

  public static @NotNull Rectangle2D measureLabelBounds(final @NotNull Graphics2D g2,
      final @NotNull String label) {
    final Font oldFont = g2.getFont();
    try {
      final Font labelFont = resolveLabelFont();
      g2.setFont(labelFont);
      final FontMetrics fm = g2.getFontMetrics(labelFont);
      final String[] lines = label.split("\\R", -1);
      final int textWidth = Arrays.stream(lines).mapToInt(fm::stringWidth).max().orElse(0);
      final int lineHeight = fm.getHeight();
      final int textHeight = lineHeight * Math.max(1, lines.length);
      final double boxWidth = textWidth + 2d * X_PADDING;
      final double boxHeight = textHeight + 2d * Y_PADDING - 2d;
      return new Rectangle2D.Double(0d, 0d, boxWidth, boxHeight);
    } finally {
      g2.setFont(oldFont);
    }
  }

  public static void drawLabelAt(final @NotNull Graphics2D g2, final @NotNull Rectangle2D box,
      final @NotNull String label, final @NotNull Paint borderPaint) {
    if (label.isBlank()) {
      return;
    }

    final Font oldFont = g2.getFont();
    final Paint oldPaint = g2.getPaint();
    final var oldStroke = g2.getStroke();
    try {
      final Font labelFont = resolveLabelFont();
      g2.setFont(labelFont);
      final FontMetrics fm = g2.getFontMetrics(labelFont);
      final String[] lines = label.split("\\R", -1);
      final int lineHeight = fm.getHeight();

      g2.setPaint(labelBackgroundPaint());
      g2.fill(box);
      g2.setPaint(borderPaint);
      g2.setStroke(new BasicStroke(0.8f));
      g2.draw(box);
      g2.setPaint(labelTextPaint());

      float textY = (float) (box.getY() + Y_PADDING + fm.getAscent());
      final float textX = (float) (box.getX() + X_PADDING);
      for (final String line : lines) {
        g2.drawString(line, textX, textY);
        textY += lineHeight;
      }
    } finally {
      g2.setFont(oldFont);
      g2.setPaint(oldPaint);
      g2.setStroke(oldStroke);
    }
  }

  public static @NotNull Paint labelTextPaint() {
    return ConfigService.getConfiguration().isDarkMode() ? Color.WHITE : Color.BLACK;
  }

  public static @NotNull Paint labelBackgroundPaint() {
    return ConfigService.getConfiguration().isDarkMode() ? new Color(0, 0, 0, 160)
        : new Color(255, 255, 255, 175);
  }

  private static @NotNull Font resolveLabelFont() {
    final var theme = ConfigService.getConfiguration().getDefaultChartTheme();
    final Font itemLabelFont = theme.getItemLabelFont();
    return itemLabelFont != null ? itemLabelFont : theme.getRegularFont();
  }
}
