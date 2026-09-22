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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Creates tag checkboxes with the shared palette-based appearance.
 */
public final class TagCheckBoxFactory {

  private TagCheckBoxFactory() {
  }

  public static @NotNull CheckBox create(final int tagIndex, @NotNull final String label) {
    if (tagIndex < 0) {
      throw new IllegalArgumentException("Tag index must not be negative: " + tagIndex);
    }

    final Color color = ConfigService.getDefaultColorPalette().get(tagIndex);
    final String markColor = color.getBrightness() < 0.55 ? "#FFFFFF" : "#000000";
    final CheckBox checkBox = new CheckBox();
    checkBox.getStyleClass().add("tag-check-box");
    checkBox.setStyle(
        "-tag-color: %s; -tag-mark-color: %s;".formatted(FxColorUtil.colorToHex(color), markColor));
    setLabel(checkBox, label);
    return checkBox;
  }

  public static void setLabel(@NotNull final CheckBox checkBox, @NotNull final String label) {
    checkBox.setTooltip(new Tooltip(label));
    checkBox.setAccessibleText(label);
  }
}
