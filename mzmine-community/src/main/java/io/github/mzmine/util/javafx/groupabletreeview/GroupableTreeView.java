/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.javafx.groupabletreeview;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A flexible TreeView component that supports pluggable grouping strategies, manual
 * grouping/ungrouping, hierarchical sorting, and drag & drop reordering.
 *
 * @param <T> the type of items displayed in the tree
 */
public class GroupableTreeView<T> extends BorderPane {

  private final TreeItem<T> rootItem = new TreeItem<>();
  private final TreeView<T> treeView = new TreeView<>(rootItem);
  private final ObservableList<T> allItems = FXCollections.observableArrayList();
  private final ObservableList<GroupingStrategy<T>> availableStrategies = FXCollections.observableArrayList();
  private final ObjectProperty<GroupingStrategy<T>> activeStrategy;
  private final ComboBox<GroupingStrategy<T>> strategyComboBox;
  private boolean suppressRegroup = false;

  public GroupableTreeView() {
    rootItem.setExpanded(true);
    treeView.setShowRoot(false);

    strategyComboBox = new ComboBox<>(availableStrategies);
    strategyComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(final @Nullable GroupingStrategy<T> strategy) {
        return strategy == null ? "" : strategy.displayName();
      }

      @Override
      public @Nullable GroupingStrategy<T> fromString(final String string) {
        return null;
      }
    });
    activeStrategy = strategyComboBox.valueProperty();
    activeStrategy.subscribe((_, _) -> {
      if (!suppressRegroup) {
        regroup();
      }
    });

    setTop(FxLayout.newHBox(Pos.CENTER_LEFT, FxLabels.newLabel("Group by"), strategyComboBox));
    setCenter(treeView);
  }

  public @NotNull TreeView<T> getTreeView() {
    return treeView;
  }

  // -- ContextMenu delegation (needed for FXML compatibility since BorderPane has no contextMenu) --

  public @Nullable ContextMenu getContextMenu() {
    return treeView.getContextMenu();
  }

  public void setContextMenu(@Nullable final ContextMenu contextMenu) {
    treeView.setContextMenu(contextMenu);
  }

  public @NotNull ObjectProperty<ContextMenu> contextMenuProperty() {
    return treeView.contextMenuProperty();
  }

  public @NotNull ObservableList<GroupingStrategy<T>> getAvailableStrategies() {
    return availableStrategies;
  }

  public @Nullable GroupingStrategy<T> getActiveStrategy() {
    return activeStrategy.get();
  }

  public void setActiveStrategy(@Nullable final GroupingStrategy<T> strategy) {
    activeStrategy.set(strategy);
  }

  public @NotNull ObjectProperty<GroupingStrategy<T>> activeStrategyProperty() {
    return activeStrategy;
  }

  // -- Item management --

  public void addItem(@NotNull final T item) {
    allItems.add(item);
    regroup();
  }

  public void addItems(@NotNull final Collection<T> items) {
    allItems.addAll(items);
    regroup();
  }

  public void removeItemByValue(@NotNull final T value) {
    allItems.remove(value);
    regroup();
  }

  public void removeItemsByValues(@NotNull final Collection<T> values) {
    allItems.removeAll(values);
    regroup();
  }

  // -- Grouping --

  /**
   * Rebuilds the tree structure from allItems using the active strategy. Builds the new tree
   * offline to avoid concurrent modification, then sets all children at once on the FX thread.
   */
  public void regroup() {
    final GroupingStrategy<T> strategy = activeStrategy.get();
    final List<TreeItem<T>> newChildren = buildTreeChildren(strategy);
    // apply on the FX thread to avoid ConcurrentModificationException from task threads
    FxThread.runLater(() -> rootItem.getChildren().setAll(newChildren));
  }

  /**
   * Rebuilds the tree synchronously. Use when already on the FX thread and the result must be
   * visible immediately (e.g., after a drag & drop move).
   */
  private void regroupNow() {
    final GroupingStrategy<T> strategy = activeStrategy.get();
    final List<TreeItem<T>> newChildren = buildTreeChildren(strategy);
    rootItem.getChildren().setAll(newChildren);
  }

  private @NotNull List<TreeItem<T>> buildTreeChildren(
      @Nullable final GroupingStrategy<T> strategy) {
    final List<TreeItem<T>> result = new ArrayList<>();

    if (strategy == null) {
      for (final T item : allItems) {
        result.add(new TreeItem<>(item));
      }
      return result;
    }

    // decision: use LinkedHashMap to preserve insertion order of groups
    final Map<String, GroupTreeItem<T>> groups = new LinkedHashMap<>();

    for (final T item : allItems) {
      final String groupName = strategy.getGroupName(item);
      if (groupName == null) {
        result.add(new TreeItem<>(item));
      } else {
        // add group to result on first encounter, then just add child items to it
        final GroupTreeItem<T> group = groups.computeIfAbsent(groupName, name -> {
          final GroupTreeItem<T> g = new GroupTreeItem<>(name);
          result.add(g);
          return g;
        });
        group.getChildren().add(new TreeItem<>(item));
      }
    }

    return result;
  }

  /**
   * Groups the currently selected items under a new group. If an auto-strategy is active, snapshots
   * to custom first.
   */
  public void groupSelected() {
    final List<T> selectedValues = getSelectedItems();
    if (selectedValues.isEmpty()) {
      return;
    }

    if (!isCustomStrategy()) {
      snapshotToCustom();
    }

    final GroupingStrategy<T> strategy = activeStrategy.get();
    if (strategy instanceof CustomGroupingStrategy<T> custom) {
      for (final T item : selectedValues) {
        custom.assignToGroup(item, "Group");
      }
      regroup();
    }
  }

  /**
   * Removes the currently selected items from their groups, placing them at top level. If an
   * auto-strategy is active, snapshots to custom first.
   */
  public void ungroupSelected() {
    final List<T> selectedValues = getSelectedItems();
    if (selectedValues.isEmpty()) {
      return;
    }

    if (!isCustomStrategy()) {
      snapshotToCustom();
    }

    final GroupingStrategy<T> strategy = activeStrategy.get();
    if (strategy instanceof CustomGroupingStrategy<T> custom) {
      for (final T item : selectedValues) {
        custom.removeFromGroup(item);
      }
      regroup();
    }
  }

  /**
   * Captures the current tree structure as a {@link CustomGroupingStrategy} and switches to it.
   * This allows the user to manually edit an auto-derived grouping.
   */
  public @NotNull CustomGroupingStrategy<T> snapshotToCustom() {
    final CustomGroupingStrategy<T> custom = snapshotToCustomSilent();
    // setting active strategy triggers regroup via the property listener
    activeStrategy.set(custom);
    return custom;
  }

  /**
   * Like {@link #snapshotToCustom()} but does not set the active strategy, avoiding a regroup. Used
   * by {@link #moveItem} which handles regrouping itself.
   */
  private @NotNull CustomGroupingStrategy<T> snapshotToCustomSilent() {
    final CustomGroupingStrategy<T> custom = new CustomGroupingStrategy<>();

    for (final TreeItem<T> child : rootItem.getChildren()) {
      if (child instanceof GroupTreeItem<T> group) {
        final String groupName = group.getGroupName();
        for (final TreeItem<T> grandChild : group.getChildren()) {
          final T value = grandChild.getValue();
          if (value != null) {
            custom.assignToGroup(value, groupName);
          }
        }
      }
      // top-level items don't need assignment (null = top level)
    }

    // add to available strategies if not already present
    @SuppressWarnings("unchecked") final GroupingStrategy<T> castCustom = custom;
    availableStrategies.removeIf(s -> s instanceof CustomGroupingStrategy);
    availableStrategies.add(castCustom);
    // set the strategy without triggering a regroup
    suppressRegroup = true;
    try {
      activeStrategy.set(castCustom);
    } finally {
      suppressRegroup = false;
    }

    return custom;
  }

  private boolean isCustomStrategy() {
    final GroupingStrategy<T> strategy = activeStrategy.get();
    return strategy == null || strategy.isCustom();
  }

  // -- Sorting --

  /**
   * Sorts the tree hierarchically: groups are sorted alphabetically, items within each group (and
   * top-level items) are sorted by the provided comparator.
   */
  public void sortItems(@NotNull final Comparator<T> comparator) {
    final Comparator<TreeItem<T>> treeComparator = (a, b) -> {
      // groups sort by name
      if (a instanceof GroupTreeItem<T> ga && b instanceof GroupTreeItem<T> gb) {
        return ga.getGroupName().compareToIgnoreCase(gb.getGroupName());
      }
      // groups before leaf items
      if (a instanceof GroupTreeItem) {
        return -1;
      }
      if (b instanceof GroupTreeItem) {
        return 1;
      }
      // leaf items sort by comparator
      return comparator.compare(a.getValue(), b.getValue());
    };

    FXCollections.sort(rootItem.getChildren(), treeComparator);

    // sort children within each group
    final Comparator<TreeItem<T>> leafComparator = (a, b) -> comparator.compare(a.getValue(),
        b.getValue());
    for (final TreeItem<T> child : rootItem.getChildren()) {
      if (child instanceof GroupTreeItem) {
        FXCollections.sort(child.getChildren(), leafComparator);
      }
    }
  }

  /**
   * Sorts items to match the given order. Items not in the list keep their current position.
   * Backward compat for MZmineGUI.sortRawDataFilesAlphabetically.
   */
  public void sortItemObjects(@NotNull final List<T> sortedOrder) {
    final Map<T, Integer> orderMap = new LinkedHashMap<>();
    for (int i = 0; i < sortedOrder.size(); i++) {
      orderMap.put(sortedOrder.get(i), i);
    }
    sortItems(Comparator.comparingInt(item -> orderMap.getOrDefault(item, Integer.MAX_VALUE)));
  }

  // -- Selection --

  /**
   * @return selected item values, excluding group nodes
   */
  public @NotNull List<@NotNull T> getSelectedItems() {
    return getSelectedTreeItems().stream().map(TreeItem::getValue).filter(Objects::nonNull)
        .toList();
  }

  public @NotNull List<@NotNull TreeItem<T>> getSelectedTreeItems() {
    return List.copyOf(treeView.getSelectionModel().getSelectedItems());
  }

  // -- Drag & drop support for tree reordering --

  /**
   * Moves an item from one position to another within the tree. If an auto-strategy is active,
   * snapshots to custom first.
   *
   * @param item         the item to move
   * @param targetParent the target parent (root or a GroupTreeItem)
   * @param index        insertion index within the target parent's children
   */
  public void moveItem(@NotNull final T item, @NotNull final TreeItem<T> targetParent,
      final int index) {
    if (!isCustomStrategy()) {
      // snapshot without triggering regroup - we'll regroup manually below
      snapshotToCustomSilent();
    }

    // update the custom strategy with the new group assignment
    final GroupingStrategy<T> strategy = activeStrategy.get();
    if (strategy instanceof CustomGroupingStrategy<T> custom) {
      if (targetParent instanceof GroupTreeItem<T> group) {
        custom.assignToGroup(item, group.getGroupName());
      } else {
        custom.removeFromGroup(item);
      }
    }

    // reorder in allItems to reflect the drop position
    allItems.remove(item);
    final int insertPos = computeInsertPosition(item, targetParent, index);
    allItems.add(insertPos, item);

    // rebuild tree synchronously so the move is visible immediately
    regroupNow();

    // select the moved item
    final TreeItem<T> movedNode = findTreeItem(item);
    if (movedNode != null) {
      treeView.getSelectionModel().clearSelection();
      treeView.getSelectionModel().select(movedNode);
    }
  }

  /**
   * Computes the insert position in allItems based on the target parent and index within that
   * parent's children.
   */
  private int computeInsertPosition(@NotNull final T item, @NotNull final TreeItem<T> targetParent,
      final int index) {
    final List<TreeItem<T>> siblings = targetParent.getChildren();

    // find the item currently at the target index (or the last item if index is beyond end)
    if (siblings.isEmpty()) {
      // empty group or root: insert at end
      return allItems.size();
    }

    if (index >= siblings.size()) {
      // insert after the last sibling
      final TreeItem<T> lastSibling = siblings.getLast();
      final T lastValue = lastSibling instanceof GroupTreeItem<T> ? getLastLeafValue(lastSibling)
          : lastSibling.getValue();
      if (lastValue != null) {
        final int pos = allItems.indexOf(lastValue);
        return pos >= 0 ? pos + 1 : allItems.size();
      }
      return allItems.size();
    }

    // insert before the sibling at the target index
    final TreeItem<T> targetSibling = siblings.get(index);
    final T siblingValue =
        targetSibling instanceof GroupTreeItem<T> ? getFirstLeafValue(targetSibling)
            : targetSibling.getValue();
    if (siblingValue != null) {
      final int pos = allItems.indexOf(siblingValue);
      return pos >= 0 ? pos : allItems.size();
    }
    return allItems.size();
  }

  @Nullable
  private T getFirstLeafValue(@NotNull final TreeItem<T> group) {
    for (final TreeItem<T> child : group.getChildren()) {
      if (child.getValue() != null) {
        return child.getValue();
      }
    }
    return null;
  }

  @Nullable
  private T getLastLeafValue(@NotNull final TreeItem<T> group) {
    final List<TreeItem<T>> children = group.getChildren();
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).getValue() != null) {
        return children.get(i).getValue();
      }
    }
    return null;
  }

  @Nullable
  private TreeItem<T> findTreeItem(@NotNull final T value) {
    for (final TreeItem<T> child : rootItem.getChildren()) {
      if (value.equals(child.getValue())) {
        return child;
      }
      if (child instanceof GroupTreeItem<T>) {
        for (final TreeItem<T> grandChild : child.getChildren()) {
          if (value.equals(grandChild.getValue())) {
            return grandChild;
          }
        }
      }
    }
    return null;
  }

  /**
   * @return all items in the tree (the master flat list)
   */
  public @NotNull ObservableList<T> getAllItems() {
    return allItems;
  }
}
