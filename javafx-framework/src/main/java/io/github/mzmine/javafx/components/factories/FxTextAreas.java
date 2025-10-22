/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.javafx.components.factories;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.Nullable;

public class FxTextAreas {

  public static TextArea newTextArea(@Nullable StringProperty textProperty,
      @Nullable String tooltip, boolean wrapText) {
    return newTextArea(TextArea.DEFAULT_PREF_COLUMN_COUNT, TextArea.DEFAULT_PREF_ROW_COUNT,
        wrapText, textProperty, tooltip);
  }

  public static TextArea newTextArea(int prefColumnCount, int prefRowCount, boolean wrapText,
      @Nullable StringProperty textProperty, @Nullable String tooltip) {
    return newTextArea(prefColumnCount, prefRowCount, wrapText, textProperty, null, tooltip);
  }

  public static TextArea newTextArea(int prefColumnCount, int prefRowCount, boolean wrapText,
      @Nullable StringProperty textProperty, @Nullable String promptText,
      @Nullable String tooltip) {
    final TextArea area = new TextArea();
    area.setWrapText(wrapText);
    area.setPrefColumnCount(prefColumnCount);
    area.setPrefRowCount(prefRowCount);
    if (promptText != null) {
      area.setPromptText(promptText);
    }
    if (textProperty != null) {
      area.textProperty().bindBidirectional(textProperty);
    }
    if (tooltip != null) {
      area.setTooltip(new Tooltip(tooltip));
    }
    return area;
  }
}
