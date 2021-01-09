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

import io.github.mzmine.util.javafx.DraggableListCell;
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
 *
 * @param <T> type of the cell content
 */
public class GroupableListViewCell<T> extends
    DraggableListCell<GroupableListViewEntity> {

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

    // Setup group headers expanding/hiding
    expandButton.setOnMouseClicked(event -> {
      getListView().getItems().removeAll(((GroupableListView<T>) getListView())
          .getGroupItems((GroupEntity) getItem()));
      setGraphic(hiddenButton);
      ((GroupEntity) getItem()).invertState();
    });
    hiddenButton.setOnMouseClicked(event -> {
      getListView().getItems().addAll(getIndex() + 1, ((GroupableListView<T>) getListView())
          .getGroupItems((GroupEntity) getItem()));
      setGraphic(expandButton);
      ((GroupEntity) getItem()).invertState();
    });

    // Setup grouping context menu item
    Platform.runLater(() -> {
      getListView().getSelectionModel().getSelectedItems().addListener(new ListChangeListener<GroupableListViewEntity>() {
        @Override
        public void onChanged(Change<? extends GroupableListViewEntity> change) {
          if (getGroupableListView().onlyGroupsSelected()) {
            groupUngroupMenuItem.setText("Ungroup " + POSTFIX);
            groupUngroupMenuItem.setDisable(false);
          } else if (((GroupableListView<T>) getListView()).onlyItemsSelected()
              // TODO: do we need inherited grouping?
              && !getGroupableListView().anyGroupedItemSelected()) {
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
  protected void updateItem(GroupableListViewEntity item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || (item == null)) {
      setText("");
      setGraphic(null);
      return;
    }

    setText(item.toString());

    // Shift item to the right, if it's grouped
    if (item instanceof ValueEntity && ((ValueEntity<T>) item).isGrouped()) {
      setStyle("-fx-padding: 3 3 3 " + INDENT + ";");
    } else {
      setStyle("-fx-padding: 3 3 3 5;");
    }

    // Set expand/hide button as item's graphic. if it's group header
    if (item instanceof GroupEntity) {
      setGraphic(((GroupEntity) item).isExpanded() ? expandButton : hiddenButton);
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

    // Create HBox with renaming text field
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
  public void commitEdit(GroupableListViewEntity item) {
    if (item == null) {
      return;
    }
    super.commitEdit(item);

    if (item instanceof GroupEntity) {
      getGroupableListView().renameGroup((GroupEntity) item, renameTextField.getText());
    }
    setGraphic(renameSavedGraphic);
    setText(renameTextField.getText());
    getListView().setEditable(false);

    getListView().getSelectionModel().clearSelection();
    getListView().getSelectionModel().select(getIndex());
  }

  /**
   * Swaps {@link GroupableListView} items, according to the dragged and this items' types and indices.
   *
   * @param draggedIdx index of the dragged item
   * @param newIdx new index for the dragged item
   */
  @Override
  protected void dragDroppedAction(int draggedIdx, int newIdx) {
    GroupableListViewEntity draggedItem = getListView().getItems().get(draggedIdx);
    GroupableListViewEntity thisItem = getListView().getItems().get(newIdx);

    // Define drop behavior depending on the active items

    if (draggedItem instanceof ValueEntity) {

      // Calculate new index as an index under group, if it's dragged to the group header
      if (thisItem instanceof GroupEntity && ((GroupEntity) thisItem).isExpanded() && newIdx > draggedIdx) {
        newIdx += getGroupableListView().getGroupSize((GroupEntity) thisItem);
      }

      // Swap dragged and this items
      super.dragDroppedAction(draggedIdx, newIdx);

      // Remove dragged item from group
      getGroupableListView().removeFromGroup(((ValueEntity<?>) draggedItem).getGroup(),
          ((ValueEntity<T>) draggedItem));

      // Add dragged item to group, if it's dragged inside the group
      if (thisItem instanceof ValueEntity && ((ValueEntity<?>) thisItem).isGrouped()) {
        GroupEntity group = ((ValueEntity<?>) thisItem).getGroup();
        getGroupableListView().addToGroup(group,
            newIdx - getGroupableListView().getGroupIndex(group) - 1, ((ValueEntity<T>) draggedItem));
      }
    } else if (draggedItem instanceof GroupEntity
        && !(thisItem instanceof ValueEntity && ((ValueEntity<?>) thisItem).isGrouped())) {

      // If dragged group header is hidden, just swap it with this item
      if (((GroupEntity) draggedItem).isHidden()) {

        // Calculate new index as an index under group, if it's dragged to the group header
        if (thisItem instanceof GroupEntity && ((GroupEntity) thisItem).isExpanded() && newIdx > draggedIdx) {
          newIdx += getGroupableListView().getGroupSize((GroupEntity) thisItem);
        }
        super.dragDroppedAction(draggedIdx, newIdx);
      } else {
        List<GroupableListViewEntity> groupItems
            = new ArrayList<>(
            getGroupableListView().getGroupItems((GroupEntity) draggedItem));

        // Calculate new group header index
        if (newIdx > draggedIdx) {
          newIdx -= groupItems.size();
          if (thisItem instanceof GroupEntity && ((GroupEntity) thisItem).isExpanded()) {
            newIdx += getGroupableListView()
                .getGroupSize((GroupEntity) thisItem);
          }
        }

        // Place all group items under their group header
        getListView().getItems().remove(draggedItem);
        getListView().getItems().removeAll(groupItems);
        groupItems.add(0, draggedItem);
        getListView().getItems().addAll(newIdx, groupItems);
      }
    }

    getListView().getSelectionModel().clearAndSelect(newIdx);
  }

  public GroupableListView<T> getGroupableListView() {
    return (GroupableListView<T>) getListView();
  }
}
