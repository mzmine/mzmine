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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.gui.MZmineGUI;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * A modified ListCell
 * to reorder ListView items by dragging with the mouse
 * to drag and drop files on the ListView
 */
public class DraggableListCellWithDraggableFiles<Type> extends ListCell<Type> {

  public DraggableListCellWithDraggableFiles() {

    final DraggableListCellWithDraggableFiles<Type> thisCell = this; // necessary??

    setOnDragDetected(event -> {
      if (getItem() == null) {
        return;
      }

      Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
      ClipboardContent content = new ClipboardContent();
      int itemIndex = getListView().getItems().indexOf(getItem());
      content.putString(String.valueOf(itemIndex));
      WritableImage snapshot = this.snapshot(null, null);
      dragboard.setDragView(snapshot);
      dragboard.setContent(content);
      event.consume();
    });

    setOnDragOver(event -> {
      if (event.getGestureSource() != thisCell && event.getDragboard().hasString()) {
        event.acceptTransferModes(TransferMode.MOVE);
      }
      if(event.getDragboard().hasFiles()) {
        event.acceptTransferModes(TransferMode.COPY);
      }
      event.consume();
    });

    setOnDragEntered(event -> {
      if (event.getGestureSource() != thisCell && event.getDragboard().hasString()) {
        setOpacity(0.3);
      }
    });

    setOnDragExited(event -> {
      if (event.getGestureSource() != thisCell && event.getDragboard().hasString()) {
        setOpacity(1);
      }
    });

    setOnDragDropped(event -> {
      if (getItem() == null) {
        return;
      }

      Dragboard db = event.getDragboard();

      if (db.hasString()) {
        ObservableList<Type> items = getListView().getItems();

        int draggedIdx = Integer.valueOf(db.getString());
        int thisIdx = items.indexOf(getItem());

        Type draggedItem = getListView().getItems().get(draggedIdx);

        items.set(draggedIdx, getItem());
        items.set(thisIdx, draggedItem);
        getListView().getSelectionModel().clearAndSelect(thisIdx);

      }
      if(db.hasFiles())
         MZmineGUI.activateSetOnDragDropped(event);

      event.setDropCompleted(true);

      event.consume();
    });

    setOnDragDone(DragEvent::consume);

  }

}


