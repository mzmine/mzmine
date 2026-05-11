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

package io.github.mzmine.gui.preferences;

/**
 * Defines the structural UI style applied to all controls. Each style references a base CSS
 * stylesheet that defines control shapes, sizes, padding, borders, and layout—independent of
 * colors. Colors are supplied separately by {@link ThemeColors}.
 */
public enum ThemeStyle {

  JABREF("Classic (JabRef)", "themes/jabref_light.css"), MODERN("Modern",
      "themes/style_modern.css");

  private final String displayName;
  private final String stylesheet;

  ThemeStyle(final String displayName, final String stylesheet) {
    this.displayName = displayName;
    this.stylesheet = stylesheet;
  }

  public String getStylesheet() {
    return stylesheet;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
