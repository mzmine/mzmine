/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * Combines a {@link ThemeStyle} (structural UI styling) with a {@link ThemeColors} (color palette)
 * to produce the full list of CSS stylesheets for the application.
 * <p>
 * Loading order:
 * <ol>
 *   <li>Style base stylesheet (structural controls)</li>
 *   <li>Color palette stylesheet (.root color variables, overrides style defaults)</li>
 *   <li>Dark overrides (if dark palette, structural rules that differ for dark mode)</li>
 *   <li>Common additions</li>
 *   <li>Light or dark additions</li>
 * </ol>
 */
public record Themes(@NotNull ThemeStyle style, @NotNull ThemeColors colors) {

  private static final String DARK_OVERRIDES = "themes/mzmine_dark_overrides.css";
  private static final String ADDITIONS_COMMON = "themes/jabref_additions_common.css";
  private static final String ADDITIONS_LIGHT = "themes/jabref_additions_light.css";
  private static final String ADDITIONS_DARK = "themes/jabref_additions_dark.css";

  /**
   * Applies this theme's stylesheets to the given observable list (typically a Scene's stylesheet
   * list). Clears any existing stylesheets first.
   */
  public void apply(@NotNull final ObservableList<String> sheets) {
    List<String> styles = new ArrayList<>();
    styles.add(style.getStylesheet());
    styles.add(colors.getStylesheet());
    if (colors.isDark()) {
      styles.add(DARK_OVERRIDES);
    }
    styles.add(ADDITIONS_COMMON);
    styles.add(colors.isDark() ? ADDITIONS_DARK : ADDITIONS_LIGHT);
    sheets.setAll(styles);
  }

  public boolean isDark() {
    return colors.isDark();
  }

  @Override
  public String toString() {
    return style + " - " + colors;
  }
}
