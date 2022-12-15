/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.javafx.groupablelistview;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.sun.istack.Nullable;
import io.github.mzmine.main.MZmineCore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import org.jetbrains.annotations.NotNull;

/**
 * Class extending ListView with possibility of items grouping.
 *
 * @param <T> type of the contained items
 */
public class GroupableListView<T> extends ListView<GroupableListViewEntity> {

  private final Map<GroupEntity, ObservableList<ValueEntity<T>>> listGroups = FXCollections.observableHashMap();
  private final ObservableList<GroupableListViewEntity> listItems = FXCollections.observableArrayList();

  private final ObservableList<T> selectedValues = FXCollections.observableArrayList();
  private final ObservableList<GroupEntity> selectedGroups = FXCollections.observableArrayList();

  private Function<T, String> grouping;

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
          var items = change.getList();
          selectedValues.clear();
          selectedGroups.clear();
          for (GroupableListViewEntity item : items) {
            if (item == null) {
              continue;
            }

            if (item instanceof GroupEntity) {
              selectedGroups.add((GroupEntity) item);
              selectedValues.addAll(listGroups.get(item).stream().map(ValueEntity::getValue)
                  .collect(Collectors.toList()));
            } else {
              if (!selectedValues.contains(((ValueEntity<T>) item).getValue())) {
                selectedValues.add(((ValueEntity<T>) item).getValue());
              }
            }
          }
        }
      }
    });
  }

  public void setGrouping(Function<T, String> grouping) {
    this.grouping = grouping;
  }

  /**
   * @param values initial values
   */
  public final void setValues(@NotNull List<T> values) {
    listItems.clear();
    listGroups.clear();

    addItems(values);
  }

  public void removeItems(List<? extends T> items) {
    MZmineCore.runLater(() -> {
      for (T removedValue : items) {
        for (GroupableListViewEntity item : listItems) {
          if (item instanceof ValueEntity && ((ValueEntity<?>) item).getValue()
              .equals(removedValue)) {
            listItems.remove(item);
            if (((ValueEntity<?>) item).isGrouped()) {
              listGroups.get(((ValueEntity<?>) item).getGroup()).remove(item);
              if (listGroups.get(((ValueEntity<?>) item).getGroup()).isEmpty()) {
                ungroupItems(((ValueEntity<?>) item).getGroup());
              }
            }
            break;
          }
        }
      }
      setItems(listItems);
    });
  }

  public void addItems(final List<? extends T> items) {
    MZmineCore.runLater(() -> {
      for (T addedValue : items) {
        ValueEntity<T> newItem = new ValueEntity<T>(addedValue);

        if (grouping == null) {
          listItems.add(newItem);
        } else {
          String groupName = grouping.apply(addedValue);
          GroupEntity group = getGroupByName(groupName);

          if (group == null) {
            group = new GroupEntity(groupName);
            listGroups.put(group, FXCollections.observableArrayList());
            listItems.add(group);
          }

          addToGroup(group, 0, newItem);
        }
      }
      setItems(listItems);
    });
  }

  public void groupSelectedItems() {
    List<Integer> selectedIndices = ImmutableList.copyOf(getSelectionModel().getSelectedIndices());
    getSelectionModel().clearSelection();

    groupItems(selectedIndices, generateNewGroupName("New group"));

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

    listGroups.put(newGroup, FXCollections.observableArrayList(groupItems));

    listItems.removeAll(groupItems);
    int minIndex = Collections.min(itemsIndices);
    listItems.add(minIndex, newGroup);
    listItems.addAll(minIndex + 1, groupItems);
  }

  public void ungroupItems(List<GroupEntity> groups) {
    if (groups.equals(selectedGroups)) {
      // Create list copy to avoid concurrent modification of selected groups
      groups = List.copyOf(groups);
    }

    groups.forEach(this::ungroupItems);
  }

  public void ungroupItems(GroupEntity group) {
    listGroups.get(group).forEach(item -> item.setGroup(null));
    if (group.isHidden()) {
      listItems.addAll(listGroups.get(group));
    }

    listItems.remove(group);
    listGroups.remove(group);
  }

  public ObservableList<T> getSelectedValues() {
    return selectedValues;
  }

  public ObservableList<GroupEntity> getSelectedGroups() {
    return selectedGroups;
  }

  public void renameGroup(GroupEntity group, String newName) {
    String oldName = group.getGroupName();
    if (!listGroups.containsKey(group) || oldName.equals(newName)) {
      return;
    }

    // Modify new name to be unique among group names
    newName = generateNewGroupName(newName);

    group.setGroupName(newName);
  }

  public ObservableList<ValueEntity<T>> getGroupItems(GroupEntity group) {
    return listGroups.get(group);
  }

  public boolean onlyGroupsSelected() {
    for (GroupableListViewEntity selectedItem : getSelectionModel().getSelectedItems()) {
      if (!(selectedItem instanceof GroupEntity)) {
        return false;
      }
    }
    return true;
  }

  public boolean onlyGroupedItemsSelected() {
    for (GroupableListViewEntity selectedItem : getSelectionModel().getSelectedItems()) {
      if (!(selectedItem instanceof ValueEntity && ((ValueEntity<?>) selectedItem).isGrouped())) {
        return false;
      }
    }
    return true;
  }

  public boolean onlyItemsSelected() {
    return selectedGroups.isEmpty() && !selectedValues.isEmpty();
  }

  public boolean anyGroupedItemSelected() {
    for (GroupableListViewEntity selectedItem : getSelectionModel().getSelectedItems()) {
      if (selectedItem instanceof ValueEntity && ((ValueEntity<?>) selectedItem).isGrouped()) {
        return true;
      }
    }
    return false;
  }


  public boolean anyGroupSelected() {
    return !selectedGroups.isEmpty();
  }

  public void addToGroup(GroupEntity group, int index, List<ValueEntity<T>> items) {
    if (group == null || !listGroups.containsKey(group)) {
      return;
    }

    items.forEach(item -> item.setGroup(group));
    listGroups.get(group).addAll(index, items);

    listItems.removeAll(items);
    listItems.addAll(listItems.indexOf(group) + 1 + index, items);
  }

  public void addToGroup(GroupEntity group, int index, ValueEntity<T> item) {
    addToGroup(group, index, List.of(item));
  }

  public void removeValuesFromGroup(List<T> values) {
    if (values.equals(selectedValues)) {
      // Create list copy to avoid concurrent modification of selected items
      values = List.copyOf(values);
    }

    // Remove values' entities from the list and place them to the end
    values.forEach(value -> removeFromGroup(listItems.size() - 1, getValueEntity(value)));

    // Select ungrouped items
    getSelectionModel().clearSelection();
    getSelectionModel().selectRange(listItems.size() - values.size(), listItems.size());
  }

  /**
   * Removes item from it's group and places it to the given index of the list view.
   *
   * @param index new index of the item
   * @param item  item to remove from it's group
   */
  public void removeFromGroup(int index, ValueEntity<T> item) {
    if (item == null || !item.isGrouped()) {
      return;
    }

    GroupEntity itemOldGroup = item.getGroup();

    removeFromGroup(item);
    listItems.remove(item);
    // Compensate removed item
    if (listItems.indexOf(item) > index) {
      index++;
    }

    // If item was last group element, group header was removed
    if (!listGroups.containsKey(itemOldGroup)) {
      index--;
    }

    listItems.add(index, item);

    getSelectionModel().clearAndSelect(index);
  }

  public void removeFromGroup(ValueEntity<T> item) {
    if (item == null || !item.isGrouped()) {
      return;
    }

    GroupEntity group = item.getGroup();

    item.setGroup(null);
    listGroups.get(group).remove(item);

    if (listGroups.get(group).isEmpty()) {
      ungroupItems(group);
    }
  }

  public Integer getGroupIndex(GroupEntity group) {
    if (group == null || !listGroups.containsKey(group)) {
      return null;
    }

    for (int index = 0; index < listItems.size(); index++) {
      if (listItems.get(index) instanceof GroupEntity && (listItems.get(index)).equals(group)) {
        return index;
      }
    }
    return null;
  }

  public List<Integer> getGroupItemsIndices(GroupEntity group) {
    Integer groupIndex = getGroupIndex(group);
    return IntStream.rangeClosed(groupIndex + 1, groupIndex + getGroupSize(group)).boxed()
        .collect(Collectors.toList());
  }

  public Integer getGroupSize(GroupEntity group) {
    if (group == null || !listGroups.containsKey(group)) {
      return null;
    }
    return listGroups.get(group).size();
  }

  @Nullable
  public GroupEntity getGroupByName(String groupName) {
    for (GroupEntity group : listGroups.keySet()) {
      if (group.getGroupName().equals(groupName)) {
        return group;
      }
    }

    return null;
  }

  public void sortSelectedItems() {
    sortItems(getSelectionModel().getSelectedIndices());
  }

  /**
   * Sorts items of the list alphabetically. Cases: - one item: * item is group header: sort group
   * items * item is not group header: do nothing - multiple items: * sort group headers and not
   * grouped elements together, sort grouped items within their groups
   * <p>
   * Allows sorting of any possible list of list's indices.
   *
   * @param indices indices of items to sort
   */
  public void sortItems(List<Integer> indices) {
    if (indices == null || indices.isEmpty()) {
      return;
    }

    // Get items corresponding to indices
    List<GroupableListViewEntity> itemsToSort = indices.stream().map(listItems::get)
        .collect(Collectors.toList());

    // One item is to be sorted
    if (indices.size() == 1) {

      // If item is group header, sort all group items, else do nothing
      if (itemsToSort.get(0) instanceof GroupEntity) {

        // Sort group items
        sortHomogeneousItems(getGroupItemsIndices((GroupEntity) itemsToSort.get(0)));
      }
      return;
    }

    // Split grouped and not grouped elements indices, distribute group elements by their groups
    List<Integer> notGroupedItemsIndices = new ArrayList<>();
    HashMap<GroupEntity, List<Integer>> groupedItemsIndices = new HashMap<>();
    for (Integer index : indices) {
      GroupableListViewEntity item = listItems.get(index);
      if (item instanceof ValueEntity && ((ValueEntity<?>) item).isGrouped()) {
        groupedItemsIndices.putIfAbsent(((ValueEntity<?>) item).getGroup(), new ArrayList<>());
        groupedItemsIndices.get(((ValueEntity<?>) item).getGroup()).add(index);
      } else {
        notGroupedItemsIndices.add(index);
      }
    }

    // Sort splitted items indices
    for (GroupEntity group : groupedItemsIndices.keySet()) {
      sortHomogeneousItems(groupedItemsIndices.get(group));
    }
    sortHomogeneousItems(notGroupedItemsIndices);

  }

  /**
   * Method designed to sort each part of splitted(e.g. : not grouped, belonging to group1,
   * belonging to group2...) in {@link GroupableListView#sortItems} items. Optimized to sort only
   * given indices, not affecting whole list.
   *
   * @param indices indices of items to sort
   */
  private void sortHomogeneousItems(List<Integer> indices) {
    if (indices == null || indices.size() < 2) {
      return;
    }

    // Get sorted items corresponding to indices
    List<GroupableListViewEntity> sortedItems = indices.stream().map(listItems::get)
        .sorted(Ordering.usingToString()).collect(Collectors.toList());

    // Place sorted items one by one to the initial indices

    // List to save expanded groups to fill the list with their elements after sort
    List<GroupEntity> expandedGroups = new ArrayList<>();
    int shift = 0;
    for (int i = 0; i < indices.size(); i++) {
      indices.set(i, indices.get(i) - shift);
      GroupableListViewEntity item = listItems.get(indices.get(i));
      if (item instanceof GroupEntity && ((GroupEntity) item).isExpanded()) {
        shift += getGroupSize((GroupEntity) item);
        listItems.removeAll(listGroups.get(item));
        expandedGroups.add((GroupEntity) item);
      }
    }

    // Loop through initial indexes
    int sortedItemsIndex = 0;
    for (int index : indices) {

      GroupableListViewEntity item = sortedItems.get(sortedItemsIndex);

      // Put sorted element to the current index of the list view
      listItems.set(index, item);

      // If item is grouped, sort the group
      if (item instanceof ValueEntity && ((ValueEntity<T>) item).isGrouped()) {
        GroupEntity group = ((ValueEntity<?>) item).getGroup();
        listGroups.get(group).set(index - getGroupIndex(group) - 1, (ValueEntity<T>) item);
      }
      sortedItemsIndex++;
    }

    // Fill the list view with sorted groups elements
    for (GroupEntity expandedGroup : expandedGroups) {
      listItems.addAll(getGroupIndex(expandedGroup) + 1, getGroupItems(expandedGroup));
    }

    setItems(listItems);
  }

  /**
   * Returns {@link ValueEntity} of the list view, containing given value.
   *
   * @param value value
   * @return {@link ValueEntity} containing given value or null, if such {@link ValueEntity} doesn't
   * exist
   */
  @Nullable
  private ValueEntity<T> getValueEntity(T value) {
    for (GroupableListViewEntity item : listItems) {
      if (item instanceof ValueEntity && ((ValueEntity<?>) item).getValue().equals(value)) {
        return (ValueEntity<T>) item;
      }
    }
    return null;
  }

  /**
   * Generates new unique group name. Examples: "Group" -> "Group", if "Group" doesn't exist "Group"
   * -> "Group(1)", if "Group" exists "Group(1)" -> "Group(2)", if "Group(1)" exists
   *
   * @param groupName initial group name
   * @return new group name
   */
  private String generateNewGroupName(String groupName) {
    Set<String> groupsNames = listGroups.keySet().stream().map(GroupEntity::getGroupName)
        .collect(Collectors.toSet());

    return generateNewGroupName(groupName, groupsNames, 1);
  }

  private String generateNewGroupName(String groupName, Set<String> groupsNames, int n) {
    if (!groupsNames.contains(groupName)) {
      return groupName;
    } else {
      return generateNewGroupName(n == 1 ? groupName + "(" + n + ")"
          : groupName.substring(0, groupName.length() - 2) + n + ")", groupsNames, n + 1);
    }
  }

  public void updateItems() {
  }
}
