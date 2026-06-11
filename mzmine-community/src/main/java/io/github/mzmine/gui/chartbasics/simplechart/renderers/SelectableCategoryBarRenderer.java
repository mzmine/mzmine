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

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.main.ConfigService;
import java.awt.Color;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

/**
 * Category bar renderer that highlights a selected bar (or a set of bars with explicit color
 * overrides) using the application colour palette, while drawing all other bars in a neutral
 * colour.
 */
public class SelectableCategoryBarRenderer extends BarRenderer {

  private final @NotNull Color selectedColor = ConfigService.getDefaultColorPalette()
      .getPositiveColorAWT();
  private final @NotNull Color defaultColor = ConfigService.getDefaultColorPalette()
      .getNeutralColorAWT();
  private @NotNull Map<String, Color> selectedCategoryColors = Map.of();
  private @Nullable String selectedCategoryKey;

  public SelectableCategoryBarRenderer() {
    setBarPainter(new StandardBarPainter());
    setShadowVisible(false);
  }

  public void setSelectedCategoryKey(final @Nullable String selectedCategoryKey) {
    this.selectedCategoryKey = selectedCategoryKey;
  }

  public void setSelectedCategoryColors(final @NotNull Map<String, Color> selectedCategoryColors) {
    this.selectedCategoryColors = selectedCategoryColors;
  }

  @Override
  public @NotNull java.awt.Paint getItemPaint(final int row, final int column) {
    final var plot = getPlot();
    if (plot != null && plot.getDataset() != null) {
      final String key = Objects.toString(plot.getDataset().getColumnKey(column), null);
      if (key != null) {
        final Color selectedCategoryColor = selectedCategoryColors.get(key);
        if (selectedCategoryColor != null) {
          return selectedCategoryColor;
        }
      }
      if (selectedCategoryKey != null && selectedCategoryKey.equals(key)) {
        return selectedColor;
      }
    }
    return defaultColor;
  }
}
