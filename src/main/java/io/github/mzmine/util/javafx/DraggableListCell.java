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

package io.github.mzmine.util.javafx;

import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * A modified ListCell to reorder ListView items by dragging with the mouse.
 */
public class DraggableListCell<Type> extends ListCell<Type> {

  private static final DataFormat dragDataFormat = new DataFormat("application/octet-stream");

  public DraggableListCell() {

    final DraggableListCell<Type> thisCell = this; // necessary??

    setOnDragDetected(event -> {
      if (getItem() == null) {
        return;
      }

      Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
      ClipboardContent content = new ClipboardContent();
      content.put(dragDataFormat, getItem());
      // dragboard.setDragView();
      dragboard.setContent(content);
      event.consume();
    });

    setOnDragOver(event -> {
      if (event.getGestureSource() != thisCell && event.getDragboard().hasContent(dragDataFormat)) {
        event.acceptTransferModes(TransferMode.MOVE);
      }
      event.consume();
    });

    setOnDragEntered(event -> {
      if (event.getGestureSource() != thisCell && event.getDragboard().hasContent(dragDataFormat)) {
        setOpacity(0.3);
      }
    });

    setOnDragExited(event -> {
      if (event.getGestureSource() != thisCell && event.getDragboard().hasContent(dragDataFormat)) {
        setOpacity(1);
      }
    });

    setOnDragDropped(event -> {
      if (getItem() == null) {
        return;
      }

      Dragboard db = event.getDragboard();
      boolean success = false;

      if (db.hasContent(dragDataFormat)) {
        ObservableList<Type> items = getListView().getItems();

        int draggedIdx = items.indexOf(db.getString());
        int thisIdx = items.indexOf(getItem());

        @SuppressWarnings("unchecked")
        Type temp = (Type) db.getContent(dragDataFormat);

        items.set(draggedIdx, getItem());
        items.set(thisIdx, temp);

        // List<Object> itemscopy = new ArrayList<>(getListView().getItems());
        // getListView().getItems().setAll(itemscopy);

        success = true;
      }
      event.setDropCompleted(success);

      event.consume();
    });

    setOnDragDone(DragEvent::consume);

  }

}


