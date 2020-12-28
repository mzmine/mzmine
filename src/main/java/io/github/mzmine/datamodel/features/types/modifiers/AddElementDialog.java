/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.modifiers;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.Optional;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.scene.control.TextInputDialog;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public interface AddElementDialog {

  static final String BUTTON_TEXT = "Add new...";

  /**
   * Shows a dialog to create a new element. e.g., to add an element to a {@link ListDataType}
   * @param model
   * @param type
   * @param <T>
   */
  default <T extends Property<?>>  void createNewElementDialog(ModularDataModel model, DataType<T> type) {
    assert type instanceof StringParser : "Cannot use the default dialog without a StringParser type";
    assert type instanceof ListDataType : "Cannot use the default dialog for a non ListDataType";
    TextInputDialog dialog = new TextInputDialog("Enter new element");
    Optional<String> result = dialog.showAndWait();
    if(result.isPresent()) {
      Object newElement = ((StringParser) type).fromString(result.get());
      ((ListProperty)model.get(type)).add(0, newElement);
    }
  }
}
