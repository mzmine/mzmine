/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.util.javafx.DraggableListCell;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;

/**
 * A modified ListView that can reorder items by dragging with mouse
 *
 */
public class OrderComponent<ValueType> extends ListView<ValueType> {

  public OrderComponent() {

    // for some reason, plain JList does not have a border (at least on Mac)
    // Border border = BorderFactory.createEtchedBorder();
    // setBorder(border);

    setCellFactory(param -> new DraggableListCell<>() {
      @Override
      protected void updateItem(ValueType item, boolean empty) {
        super.updateItem(item, empty);
        if(!empty && item != null) {
          setText(item.toString());
        }
      }
    });

    // Adjust the size of the component
    setPrefWidth(150);
  }

  public void setValues(ValueType newValues[]) {
    setItems(FXCollections.observableArrayList(Arrays.asList(newValues)));
  }

  @SuppressWarnings("unchecked")
  public ValueType[] getValues() {
    return (ValueType[]) getItems().toArray();
  }


}
