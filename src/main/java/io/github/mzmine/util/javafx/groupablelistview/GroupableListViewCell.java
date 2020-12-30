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

package io.github.mzmine.util.javafx.groupablelistview;

import io.github.mzmine.util.javafx.DraggableListCellWithDraggableFiles;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Class designed to be used as a cell of {@link GroupableListView}.
 * @param <T> type of the cell content
 */
public class GroupableListViewCell<T> extends
    DraggableListCellWithDraggableFiles<GroupableListViewEntity<T>> {

  private static final int INDENT = 20;
  private final String POSTFIX = "files";

  private final Text expandButton = new Text("▼");
  private final Text hiddenButton = new Text("▶");

  private final TextField renameTextField = new TextField();
  private Node renameSavedGraphic;

  public GroupableListViewCell(MenuItem groupUngroupMenuItem) {
    setEditable(true);

    // Setup renaming text fields
    renameTextField.setOnAction(event -> commitEdit(getItem()));
    renameTextField.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        cancelEdit();
      }
    });

    // Setup group headers expanding
    expandButton.setOnMouseClicked(event -> {
      getListView().getItems().removeAll(((GroupableListView<T>) getListView())
          .getGroupItems(getItem().getGroupHeader()));
      setGraphic(hiddenButton);
      getItem().invertState();
    });
    hiddenButton.setOnMouseClicked(event -> {
      getListView().getItems().addAll(getIndex() + 1, ((GroupableListView<T>) getListView())
          .getGroupItems(getItem().getGroupHeader()));
      setGraphic(expandButton);
      getItem().invertState();
    });

    // Setup grouping context menu item
    Platform.runLater(() -> {
      getListView().getSelectionModel().getSelectedItems().addListener(new ListChangeListener<GroupableListViewEntity<T>>() {
        @Override
        public void onChanged(Change<? extends GroupableListViewEntity<T>> change) {
          if (((GroupableListView<T>) getListView()).onlyGroupHeadersSelected()) {
            groupUngroupMenuItem.setText("Ungroup " + POSTFIX);
            groupUngroupMenuItem.setDisable(false);
          } else if (((GroupableListView<T>) getListView()).onlyItemsSelected()
              // TODO: do we need inherited grouping?
              && !((GroupableListView<T>) getListView()).anyGroupedItemSelected()) {
            groupUngroupMenuItem.setText("Group " + POSTFIX);
            groupUngroupMenuItem.setDisable(false);
          } else {
            groupUngroupMenuItem.setText("Group/Ungroup " + POSTFIX);
            groupUngroupMenuItem.setDisable(true);
          }
        }
      });
    });
  }

  @Override
  protected void updateItem(GroupableListViewEntity<T> item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || (item == null)) {
      setText("");
      setGraphic(null);
      return;
    }

    setText(item.toString());
    if (item.isGrouped()) {
      setStyle("-fx-padding: 3 3 3 " + INDENT + ";");
    } else {
      setStyle("-fx-padding: 3 3 3 5;");
    }

    if (item.isGroupHeader()) {
      setGraphic(item.isExpanded() ? expandButton : hiddenButton);
      textFillProperty().unbind();
      textFillProperty().setValue(Color.BLACK);
    }
  }

  @Override
  public void startEdit() {
    setEditable(true);
    if (!isEditable() || !getListView().isEditable() || getItem() == null) {
      return;
    }
    super.startEdit();
    renameSavedGraphic = getGraphic();

    renameTextField.setText(getItem().toString());
    setText(null);
    HBox hbox = new HBox();
    hbox.getChildren().add(getGraphic());
    hbox.getChildren().add(renameTextField);
    hbox.setAlignment(Pos.CENTER_LEFT);
    setGraphic(hbox);

    renameTextField.selectAll();
    renameTextField.requestFocus();
  }

  @Override
  public void cancelEdit() {
    if (getItem() == null) {
      return;
    }
    super.cancelEdit();

    setText(getItem().toString());
    setGraphic(renameSavedGraphic);
    getListView().setEditable(false);
  }

  @Override
  public void commitEdit(GroupableListViewEntity<T> item) {
    if (item == null) {
      return;
    }
    super.commitEdit(item);

    if (item.isGroupHeader()) {
      ((GroupableListView<T>) getListView()).renameGroupHeader(item, renameTextField.getText());
    }
    setGraphic(renameSavedGraphic);
    setText(renameTextField.getText());
    getListView().setEditable(false);

    getListView().getSelectionModel().clearSelection();
    getListView().getSelectionModel().select(getIndex());
  }

  @Override
  protected void dragDroppedAction(int draggedIdx) {
    int thisIndex = getIndex();
    GroupableListViewEntity<T> draggedItem = getListView().getItems().get(draggedIdx);
    GroupableListViewEntity<T> thisItem = getListView().getItems().get(thisIndex);

    // Define drop behavior depending on the active items
    if (draggedItem.isValue()) {
      if (thisItem.isGroupHeader() && thisIndex > draggedIdx) {
        thisIndex += ((GroupableListView<T>) getListView()).getGroupSize(thisItem.getGroupHeader());
      }

      super.dragDroppedAction(draggedIdx);
      ((GroupableListView<T>) getListView()).removeFromGroup(draggedItem.getGroup(), draggedItem);
      ((GroupableListView<T>) getListView()).addToGroup(thisItem.getGroup(), draggedItem);
    } else if (draggedItem.isGroupHeader() && !thisItem.isGrouped()) {
      List<GroupableListViewEntity<T>> groupItems
          = new ArrayList<>(((GroupableListView<T>) getListView()).getGroupItems(draggedItem.getGroupHeader()));

      if (thisIndex > draggedIdx) {
        thisIndex -= groupItems.size();
        if (thisItem.isGroupHeader()) {
          thisIndex += ((GroupableListView<T>) getListView()).getGroupSize(thisItem.getGroupHeader());
        }
      }

      getListView().getItems().remove(draggedItem);
      getListView().getItems().removeAll(groupItems);
      groupItems.add(0, draggedItem);
      getListView().getItems().addAll(thisIndex, groupItems);
    }

    getListView().getSelectionModel().clearAndSelect(thisIndex);
  }

}
