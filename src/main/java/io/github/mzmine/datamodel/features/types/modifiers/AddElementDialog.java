/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.modifiers;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.beans.property.ListProperty;
import javafx.scene.control.TextInputDialog;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public interface AddElementDialog {

  static final String BUTTON_TEXT = "Add new...";

  /**
   * Shows a dialog to create a new element. e.g., to add an element to a {@link ListDataType}
   *
   * @param model
   * @param parentType
   * @param type
   * @param subColumnIndex
   */
  default void createNewElementDialog(ModularDataModel model,
      @Nullable SubColumnsFactory parentType, DataType type, int subColumnIndex,
      Consumer<Object> newElementConsumer) {
    assert type instanceof StringParser : "Cannot use the default dialog without a StringParser type";
    assert type instanceof ListDataType : "Cannot use the default dialog for a non ListDataType";
    TextInputDialog dialog = new TextInputDialog("Enter new element");
    Optional<String> result = dialog.showAndWait();
    if (result.isPresent()) {
      Object newElement = ((StringParser) type).fromString(result.get());
      // set via parent if available
      if (parentType != null) {
        parentType.valueChanged(model, (DataType) type, subColumnIndex, newElement);
      } else {
        ((ListProperty) model.get(type)).add(0, newElement);
      }
      if (newElementConsumer != null) {
        newElementConsumer.accept(newElement);
      }
    }
  }
}
