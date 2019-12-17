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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes;

import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * A modified JList that can reorder items in DefaultListModel by dragging with mouse
 *
 */
public class OrderComponent<ValueType> extends ListView<ValueType> {

  public OrderComponent() {

    // for some reason, plain JList does not have a border (at least on Mac)
    // Border border = BorderFactory.createEtchedBorder();
    // setBorder(border);


    this.setCellFactory(listView -> {

      var newCell = new ListCell<ValueType>();

      // Drag/drop feature
      newCell.setOnDragDetected(event -> {
        if (newCell.getItem() == null)
          return;

        Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();

        ObservableList<ValueType> items = this.getItems();
        int dragIndex = items.indexOf(newCell.getItem());
        content.putString(String.valueOf(dragIndex));

        dragboard.setContent(content);

        event.consume();
      });

      newCell.setOnDragOver(event -> {
        if (event.getGestureSource() != newCell && event.getDragboard().hasString()) {
          event.acceptTransferModes(TransferMode.MOVE);
        }
        event.consume();
      });

      newCell.setOnDragEntered(event -> {
        if (event.getGestureSource() != newCell && event.getDragboard().hasString()) {
          setOpacity(0.3);
        }
      });

      newCell.setOnDragExited(event -> {
        if (event.getGestureSource() != newCell && event.getDragboard().hasString()) {
          setOpacity(1);
        }
      });

      newCell.setOnDragDropped(event -> {
        if (newCell.getItem() == null)
          return;

        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasString()) {
          ObservableList<ValueType> items = this.getItems();

          int draggedIdx = Integer.valueOf(db.getString());
          int thisIdx = items.indexOf(newCell.getItem());

          ValueType oldCell = items.get(draggedIdx);
          items.set(draggedIdx, newCell.getItem());
          items.set(thisIdx, oldCell);

          // List<ValueType> itemscopy = new ArrayList<>(getItems());
          // getItems().setAll(itemscopy);

          success = true;
        }
        event.setDropCompleted(success);

        event.consume();
      });

      newCell.setOnDragDone(DragEvent::consume);
      return newCell;
    });


  }

  public void setValues(ValueType newValues[]) {
    setItems(FXCollections.observableArrayList(Arrays.asList(newValues)));

    // Adjust the size of the component
    setPrefWidth(150);
  }

  @SuppressWarnings("unchecked")
  public ValueType[] getValues() {
    return (ValueType[]) getItems().toArray();
  }


}
