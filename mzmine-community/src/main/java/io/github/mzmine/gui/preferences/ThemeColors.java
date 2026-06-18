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

/**
 * Defines the color palette applied to the UI. Each value references a CSS file that only contains
 * {@code .root} variable definitions for colors. The structural styling is provided by
 * {@link ThemeStyle}. A color palette loaded after the style overrides the style's default color
 * variables.
 */
public enum ThemeColors {

  LIGHT("Light", "themes/colors_light.css", false), DARK("Dark", "themes/colors_dark.css",
      true), DARK_HIGH_CONTRAST("Dark (High Contrast)", "themes/colors_dark_high_contrast.css",
      true), DARK_GREY("Dark Grey", "themes/colors_dark_grey.css", true), MIDNIGHT("Midnight",
      "themes/colors_midnight.css", true), NORD("Nord", "themes/colors_nord.css", true), TWILIGHT(
      "Twilight", "themes/colors_twilight.css", true), CRISP_LIGHT("Crisp Light",
      "themes/colors_crisp_light.css", false), DARK_BLUE("Dark blue",
      "themes/colors_dark_blue.css", true);

  private final String displayName;
  private final String stylesheet;
  private final boolean dark;

  ThemeColors(final String displayName, final String stylesheet, final boolean dark) {
    this.displayName = displayName;
    this.stylesheet = stylesheet;
    this.dark = dark;
  }

  public String getStylesheet() {
    return stylesheet;
  }

  public boolean isDark() {
    return dark;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
