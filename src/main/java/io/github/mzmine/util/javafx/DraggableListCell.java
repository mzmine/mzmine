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

package io.github.mzmine.util.javafx;

import com.google.common.collect.ImmutableList;
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
public class DraggableListCell<T> extends ListCell<T> {

  public DraggableListCell() {

    final DraggableListCell<T> thisCell = this; // necessary??

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
        dragDroppedAction(Integer.parseInt(db.getString()), getIndex());
      }
      if(db.hasFiles())
         MZmineGUI.activateSetOnDragDropped(event);

      event.setDropCompleted(true);

      event.consume();
    });

    setOnDragDone(DragEvent::consume);

  }

  /**
   * Places selected items to the new index. If new index is higher than dragged item index,
   * shifts old item up, else shifts it down. This method is supposed to be called in
   * setOnDragDropped event handler in case when {@link Dragboard#hasString()} is true.
   *
   * @param draggedIdx index of the dragged item
   * @param draggedToIdx new index for the dragged item
   */
  protected void dragDroppedAction(int draggedIdx, int draggedToIdx) {
    ObservableList<T> items = getListView().getItems();
    T draggedToItem = getListView().getItems().get(draggedToIdx);

    ImmutableList<T> selectedItems =  ImmutableList.copyOf(getListView().getSelectionModel().getSelectedItems());
    getListView().getSelectionModel().clearSelection();
    items.removeAll(selectedItems);

    // Calculate final index to place dragged item, after selected items were removed
    int finalIdx = items.indexOf(draggedToItem);

    // If dragged item is above this item, place it under this item
    if (draggedToIdx > draggedIdx) {
      finalIdx++;
    }
    items.addAll(finalIdx, selectedItems);

    getListView().getSelectionModel().selectRange(finalIdx, finalIdx + selectedItems.size());
  }

}


