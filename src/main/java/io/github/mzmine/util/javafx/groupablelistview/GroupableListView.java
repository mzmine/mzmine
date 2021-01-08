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
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javax.annotation.Nonnull;

/**
 * Class extending ListView with possibility of items grouping.
 *
 * @param <T> type of the contained items
 */
public class GroupableListView<T> extends ListView<GroupableListViewEntity> {

  private final Map<GroupEntity, ObservableList<ValueEntity<T>>> groups = FXCollections.observableHashMap();
  private final ObservableList<GroupableListViewEntity> items = FXCollections.observableArrayList();

  private final ObservableList<T> selectedItems = FXCollections.observableArrayList();
  private final ObservableList<GroupEntity> selectedGroups = FXCollections.observableArrayList();

  public GroupableListView() {
    setEditable(false);

    // Bind selected values and groups' to selected entities
    getSelectionModel().getSelectedItems().addListener(new ListChangeListener<>() {
      @Override
      public void onChanged(Change<? extends GroupableListViewEntity> change) {
        while (change.next()) {
          if (change.getList() == null) {
            return;
          }
          ImmutableList<GroupableListViewEntity> items = ImmutableList.copyOf(change.getList());
          selectedItems.clear();
          selectedGroups.clear();
          for (GroupableListViewEntity item : items) {
            if (item instanceof GroupEntity) {
              selectedGroups.add((GroupEntity) item);
              selectedItems.addAll(groups.get(item).stream()
                  .map(ValueEntity::getValue)
                  .collect(Collectors.toList()));
            } else {
              selectedItems.add(((ValueEntity<T>) item).getValue());
            }
          }
        }
      }
    });
  }

  /**
   * Binds the values of this {@link GroupableListView} to the given {@link ObservableList}.
   *
   * @param values list to be binded
   */
  public final void setValues(@Nonnull ObservableList<T> values) {
    items.clear();
    groups.clear();

    values.addListener(new ListChangeListener<T>() {
      @Override
      public void onChanged(Change<? extends T> change) {
        while (change.next()) {
          if (change.wasAdded()) {
            change.getAddedSubList().forEach(item -> items.add(new ValueEntity<T>(item)));
          }
          if (change.wasRemoved()) {
            for (T removedItem : change.getRemoved()) {
              for (GroupableListViewEntity item : items) {
                if (item instanceof ValueEntity && ((ValueEntity<?>) item).getValue().equals(removedItem)) {
                  items.remove(item);
                  if (((ValueEntity<?>) item).isGrouped()) {
                    groups.get(((ValueEntity<?>) item).getGroup()).remove(item);
                    if (groups.get(((ValueEntity<?>) item).getGroup()).isEmpty()) {
                      ungroupItems(((ValueEntity<?>) item).getGroup());
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
    groupItems(selectedIndices, generateNewGroupName("New group"));

    getSelectionModel().clearSelection();
    int firstGroupIndex = Collections.min(selectedIndices) + 1;
    getSelectionModel().selectRange(firstGroupIndex, firstGroupIndex + selectedIndices.size());

    Platform.runLater(() -> {
      setEditable(true);
      edit(Collections.min(selectedIndices));
    });
  }

  public void groupItems(List<Integer> itemsIndices, String groupName) {
    GroupEntity newGroup = new GroupEntity(groupName);

    ObservableList<ValueEntity<T>> groupItems = itemsIndices.stream()
        .map(index -> (ValueEntity<T>) getItems().get(index))
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
    groupItems.forEach(item -> item.setGroup(newGroup));

    groups.put(newGroup, FXCollections.observableArrayList(groupItems));

    items.removeAll(groupItems);
    int minIndex = Collections.min(itemsIndices);
    items.add(minIndex, newGroup);
    items.addAll(minIndex + 1, groupItems);
  }

  public void ungroupItems(List<GroupEntity> groupNames) {
    groupNames.forEach(this::ungroupItems);
  }

  public void ungroupItems(GroupEntity group) {
    groups.get(group).forEach(item -> item.setGroup(null));
    if (group.isHidden()) {
      items.addAll(groups.get(group));
    }

    items.remove(group);
    groups.remove(group);
  }

  public ObservableList<T> getSelectedItems() {
    return selectedItems;
  }

  public ObservableList<GroupEntity> getSelectedGroups() {
    return selectedGroups;
  }

  public void renameGroup(GroupEntity group, String newName) {
    String oldName = group.getGroupName();
    if (!groups.containsKey(group) || oldName.equals(newName)) {
      return;
    }

    // Modify new name to be unique among group names
    newName = generateNewGroupName(newName);

    group.setGroupName(newName);
  }

  public ObservableList<ValueEntity<T>> getGroupItems(GroupEntity group) {
    return groups.get(group);
  }

  public boolean onlyGroupsSelected() {
    for (GroupableListViewEntity selectedItem : getSelectionModel().getSelectedItems()) {
      if (!(selectedItem instanceof GroupEntity)) {
        return false;
      }
    }
    return true;
  }

  public boolean onlyItemsSelected() {
    return selectedGroups.isEmpty() && !selectedItems.isEmpty();
  }

  public boolean anyGroupedItemSelected() {
    for (GroupableListViewEntity selectedItem : getSelectionModel().getSelectedItems()) {
      if (selectedItem instanceof ValueEntity && ((ValueEntity<?>) selectedItem).isGrouped()) {
        return true;
      }
    }
    return false;
  }

  public void addToGroup(GroupEntity group, ValueEntity<T> item) {
    if (group == null || !groups.containsKey(group)) {
      return;
    }

    item.setGroup(group);
    groups.get(group).add(item);
  }

  public void removeFromGroup(GroupEntity group, ValueEntity<T> item) {
    if (group == null) {
      return;
    }

    item.setGroup(null);
    groups.get(group).remove(item);

    if (groups.get(group).isEmpty()) {
      ungroupItems(group);
    }
  }

  public Integer getGroupSize(GroupEntity group) {
    if (group == null || !groups.containsKey(group)) {
      return null;
    }
    return groups.get(group).size();
  }

  /**
   * Generates new unique group name. Examples:
   * "Group" -> "Group", if "Group" doesn't exist
   * "Group" -> "Group(1)", if "Group" exists
   * "Group(1)" -> "Group(2)", if "Group(1)" exists
   *
   * @param groupName initial group name
   * @return new group name
   */
  private String generateNewGroupName(String groupName) {
    Set<String> groupsNames = groups.keySet().stream()
        .map(GroupEntity::getGroupName)
        .collect(Collectors.toSet());

    return generateNewGroupName(groupName, groupsNames, 1);
  }

  private String generateNewGroupName(String groupName, Set<String> groupsNames, int n) {
    if (!groupsNames.contains(groupName)) {
      return groupName;
    } else {
      return generateNewGroupName(n == 1
          ? groupName + "(" + n + ")"
          : groupName.substring(0, groupName.length() - 2) + n + ")", groupsNames, n + 1);
    }
  }

}
