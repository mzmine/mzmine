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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.javafx.components.util.FxLayout;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntensityNormalizerComponent extends FlowPane {


  private final ComboBox<IntensityNormalizerOptions> combo;
  private final CheckBox cbFormat;

  public IntensityNormalizerComponent(final ObservableList<IntensityNormalizerOptions> choices,
      @NotNull final IntensityNormalizer value, final boolean allowScientificFormatSelection,
      final String description) {
    super(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    combo = new ComboBox<>(choices);
    combo.setTooltip(new Tooltip(description));
    cbFormat = new CheckBox("Scientific format");
    cbFormat.setVisible(allowScientificFormatSelection);
    cbFormat.setManaged(allowScientificFormatSelection);

    cbFormat.setTooltip(new Tooltip(
        "Scientific exponential format, e.g., 1.05E4, is best at preserving values over multiple magnitudes. Make sure to test parsing compatibility with other tools."));
    setValue(value);
    getChildren().addAll(combo, cbFormat);
  }

  @NotNull
  public IntensityNormalizer getValue() {
    return new IntensityNormalizer(combo.getValue(), cbFormat.isSelected());
  }

  public void setValue(@Nullable IntensityNormalizer value) {
    if (value == null) {
      value = IntensityNormalizer.createDefault();
    }

    combo.getSelectionModel().select(value.option());
    cbFormat.setSelected(value.scientificFormat());
  }
}
