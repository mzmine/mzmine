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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javax.annotation.Nonnull;

/**
 * Class extending ListView with possibility of items grouping.
 * @param <T> type of the contained items
 */
public class GroupableListView<T> extends ListView<GroupableListViewEntity<T>> {

  private final Map<String, ObservableList<GroupableListViewEntity<T>>> groups = FXCollections.observableHashMap();
  private final ObservableList<GroupableListViewEntity<T>> items = FXCollections.observableArrayList();

  private final ObservableList<T> selectedItems = FXCollections.observableArrayList();
  private final ObservableList<String> selectedGroupHeaders = FXCollections.observableArrayList();

  public GroupableListView() {
    setEditable(false);

    // Bind selected values and groups' headers to selected entities
    getSelectionModel().getSelectedItems().addListener(new ListChangeListener<>() {
      @Override
      public void onChanged(Change<? extends GroupableListViewEntity<T>> change) {
        while (change.next()) {
          if (change.getList() == null) {
            return;
          }
          ImmutableList<GroupableListViewEntity<T>> items = ImmutableList.copyOf(change.getList());
          selectedItems.clear();
          selectedGroupHeaders.clear();
          for (GroupableListViewEntity<T> item : items) {
            if (item.isGroupHeader()) {
              selectedGroupHeaders.add(item.getGroupHeader());
              selectedItems.addAll(groups.get(item.getGroupHeader()).stream()
                  .map(GroupableListViewEntity::getValue)
                  .collect(Collectors.toList()));
            } else {
              selectedItems.add(item.getValue());
            }
          }
        }
      }
    });
  }

  public final void setItemsValues(@Nonnull ObservableList<T> values) {
    items.clear();
    groups.clear();

    // Bind list view items to the content of the given list
    values.addListener(new ListChangeListener<T>() {
      @Override
      public void onChanged(Change<? extends T> change) {
        while (change.next()) {
          if (change.wasAdded()) {
            change.getAddedSubList().forEach(item -> items.add(new GroupableListViewEntity<>(item)));
          }
          if (change.wasRemoved()) {
            for (T removedItem : change.getRemoved()) {
              for (GroupableListViewEntity<T> item : items) {
                if (item.isValue() && item.getValue().equals(removedItem)) {
                  items.remove(item);
                  if (item.isGrouped()) {
                    groups.get(item.getGroup()).remove(item);
                    if (groups.get(item.getGroup()).isEmpty()) {
                      ungroupItems(item.getGroup());
                    }
                  }
                  break;
                }
              }
            }
          }
          setItems(items);
        }
      }
    });
  }

  public void groupSelectedItems() {
    List<Integer> selectedIndices = ImmutableList.copyOf(getSelectionModel().getSelectedIndices());
    groupItems(selectedIndices, gensymGroupHeader("New group"));

    getSelectionModel().clearSelection();
    int firstGroupIndex = Collections.min(selectedIndices) + 1;
    getSelectionModel().selectRange(firstGroupIndex, firstGroupIndex + selectedIndices.size());

    Platform.runLater(() -> {
      setEditable(true);
      edit(Collections.min(selectedIndices));
    });
  }

  public void groupItems(List<Integer> itemsIndices, String groupName) {
    ObservableList<GroupableListViewEntity<T>> groupItems = itemsIndices.stream()
        .map(index -> getItems().get(index))
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
    groupItems.forEach(item -> item.setGroup(groupName));

    groups.put(groupName, FXCollections.observableArrayList(groupItems));

    items.removeAll(groupItems);
    groupItems.add(0, new GroupableListViewEntity<>(groupName));
    items.addAll(Collections.min(itemsIndices), groupItems);
  }

  public void ungroupItems(List<String> groupNames) {
    groupNames.forEach(this::ungroupItems);
  }

  public void ungroupItems(String groupName) {
    GroupableListViewEntity<T> groupHeader = getGroupHeader(groupName);

    groups.get(groupName).forEach(item -> item.setGroup(null));
    if (groupHeader.isHidden()) {
      items.addAll(groups.get(groupName));
    }

    items.remove(groupHeader);
    groups.remove(groupName);
  }

  public ObservableList<T> getSelectedItems() {
    return selectedItems;
  }

  public ObservableList<String> getSelectedGroups() {
    return selectedGroupHeaders;
  }

  public void renameGroupHeader(GroupableListViewEntity<T> groupHeader, String newName) {
    String oldName = groupHeader.getGroupHeader();
    if (!groupHeader.isGroupHeader() || !groups.containsKey(oldName) || oldName.equals(newName)) {
      return;
    }
    final String finalName = gensymGroupHeader(newName);

    groupHeader.setGroupHeader(finalName);
    groups.get(oldName).forEach(item -> item.setGroup(finalName));
    groups.put(finalName, groups.remove(oldName));
  }

  public GroupableListViewEntity<T> getGroupHeader(String groupName) {
    for (GroupableListViewEntity<T> item : items) {
      if (item.isGroupHeader() && item.getGroupHeader().equals(groupName)) {
        return item;
      }
    }
    return null;
  }

  public ObservableList<GroupableListViewEntity<T>> getGroupItems(String groupName) {
    return groups.get(groupName);
  }

  public boolean onlyGroupHeadersSelected() {
    for (GroupableListViewEntity<T> selectedItem : getSelectionModel().getSelectedItems()) {
      if (!selectedItem.isGroupHeader()) {
        return false;
      }
    }
    return true;
  }

  public boolean onlyItemsSelected() {
    return selectedGroupHeaders.isEmpty() && !selectedItems.isEmpty();
  }

  public boolean anyGroupedItemSelected() {
    for (GroupableListViewEntity<T> selectedItem : getSelectionModel().getSelectedItems()) {
      if (selectedItem != null && selectedItem.isGrouped()) {
        return true;
      }
    }
    return false;
  }

  public void addToGroup(String group, GroupableListViewEntity<T> item) {
    if (group == null) {
      return;
    }

    item.setGroup(group);
    groups.get(group).add(item);
  }

  public void removeFromGroup(String group, GroupableListViewEntity<T> item) {
    if (group == null) {
      return;
    }

    item.setGroup(null);
    groups.get(group).remove(item);

    if (groups.get(group).isEmpty()) {
      ungroupItems(group);
    }
  }

  public int getGroupSize(String group) {
    if (!groups.containsKey(group)) {
      return 0;
    }
    return groups.get(group).size();
  }

  private String gensymGroupHeader(String groupName) {
    return gensymGroupHeader(groupName, 1);
  }

  private String gensymGroupHeader(String groupName, int n) {
    if (!groups.containsKey(groupName)) {
      return groupName;
    } else {
      return gensymGroupHeader(n == 1
          ? groupName + "(" + n + ")"
          : groupName.substring(0, groupName.length() - 2) + n + ")", n + 1);
    }
  }

}
