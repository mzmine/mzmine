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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import org.jetbrains.annotations.NotNull;

/**
 * Text and bubble styles of the plot for light and dark backgrounds.
 */
final class IntensityMapTheme {

  private IntensityMapTheme() {
  }

  static @NotNull String textColor(final boolean dark) {
    return dark ? "#e2e8f0" : "#1e293b";
  }

  static @NotNull String text(final boolean dark) {
    return "-fx-text-fill: " + textColor(dark) + ";";
  }

  /**
   * @return style of secondary text, e.g. a placeholder
   */
  static @NotNull String mutedText(final boolean dark) {
    return dark ? "-fx-text-fill: #94a3b8;" : "-fx-text-fill: #64748b;";
  }

  /**
   * @return style of a floating box, e.g. the hover readout or the loading indicator
   */
  static @NotNull String bubble(final boolean dark) {
    return (dark ? "-fx-background-color: rgba(30,35,42,0.94); -fx-border-color: #475569;"
        : "-fx-background-color: rgba(255,255,255,0.94); -fx-border-color: #cbd5e1;") + text(dark)
        + "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 9 6 9;";
  }

  /**
   * @return translucent background of text on top of the data
   */
  static @NotNull String labelBackground(final boolean dark) {
    return dark ? "rgba(30,35,42,0.85)" : "rgba(255,255,255,0.85)";
  }
}
