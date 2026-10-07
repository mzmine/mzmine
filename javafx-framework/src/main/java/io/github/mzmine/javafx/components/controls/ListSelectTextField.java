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

package io.github.mzmine.javafx.components.controls;

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.formatters.ListStringConverter;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.validation.FxValidation;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ListSelectTextField<T> extends TextField {

  private static final Logger logger = Logger.getLogger(ListSelectTextField.class.getName());
  private final @NotNull ObservableList<T> items;

  private final @NotNull ObjectProperty<@Nullable T> value = new SimpleObjectProperty<>();
  private final ObservableValue<String> validationError;

  public ListSelectTextField(boolean autoGrow, @NotNull ObservableList<T> items) {
    this.items = items;
    if (autoGrow) {
      FxTextFields.autoGrowFitText(this);
    }

    FxTextFields.bindAutoCompletion(this, items);

    final ReadOnlyBooleanProperty validText = PropertyUtils.bindBidirectionalKeepText(
        textProperty(), value, null, new ListStringConverter<>(items));
    validationError = validText.map(
        valid -> valid ? null : "Text could not be converted to a valid value.");
    FxValidation.registerErrorValidator(this, validationError);
  }

  public ObjectProperty<@Nullable T> selectedItemProperty() {
    return value;
  }

  public @Nullable T getSelectedItem() {
    return selectedItemProperty().getValue();
  }

  public @NotNull ObservableList<T> getItems() {
    return items;
  }
}
