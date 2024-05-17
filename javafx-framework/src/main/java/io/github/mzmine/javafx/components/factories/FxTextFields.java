/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.Nullable;

public class FxTextFields {

  public static TextField newTextField(@Nullable Integer columnCount,
      @Nullable StringProperty textProperty, @Nullable String tooltip) {
    return newTextField(columnCount, textProperty, tooltip);
  }

  public static TextField newTextField(@Nullable Integer columnCount,
      @Nullable StringProperty textProperty, @Nullable String prompt, @Nullable String tooltip) {
    var field = new TextField();
    if (textProperty != null) {
      field.textProperty().bindBidirectional(textProperty);
    }
    if (prompt != null) {
      field.setPromptText(prompt);
    }
    if (tooltip != null) {
      field.setTooltip(new Tooltip(tooltip));
    }
    if (columnCount == null) {
      field.setPrefColumnCount(columnCount);
    }
    return field;
  }

}
