/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.javafx.metadatatreeview;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <T> Value type of the items of the tree view
 * @param <G> Value type of the grouping choices
 */
public class MetadataTreeView<T, G> extends BorderPane {

  private final MetadataWrapper<T, G> rootItem = new MetadataWrapper<>(null,
      new SimpleStringProperty("root"), _ -> "rootItem");
  private final ObjectProperty<G> groupingSelection;
  private TreeView<T> treeView = new TreeView<>(rootItem);
  private String NO_GROUPING = "ungrouped";

  private BooleanProperty manualGrouping = new SimpleBooleanProperty(false);
  private final ObservableList<G> groupingChoices;

  public MetadataTreeView() {
    this(FXCollections.observableArrayList());
  }

  public MetadataTreeView(ObservableList<G> groupingChoices) {
    this.groupingChoices = groupingChoices;
    final Label title = FxLabels.newLabel("Group by");

    final ComboBox<G> comboBox = new ComboBox<>(groupingChoices);
    groupingSelection = comboBox.valueProperty();
    groupingSelection.subscribe(_ -> autoGroup());

    setTop(FxLayout.newVBox(Pos.CENTER, title, comboBox));
    setCenter(treeView);
  }

  public TreeView<T> getTreeView() {
    return treeView;
  }

  private void autoGroup() {
    if (manualGrouping.get()) {
      return;
    }

    Set<TreeItem<T>> allChildren = new LinkedHashSet<>(); // linked hash set to keep the order
    getAllChildren(allChildren, rootItem);

    rootItem.getChildren().clear();

    final Map<String, List<TreeItem<T>>> grouped = allChildren.stream()
        .collect(Collectors.groupingBy(item -> {
          if (item instanceof MetadataWrapper meta) {
            return Objects.requireNonNullElse(meta.getGrouping(groupingSelection.get()),
                NO_GROUPING);
          }
          return NO_GROUPING;
        }));

    for (Entry<String, List<TreeItem<T>>> grouping : grouped.entrySet()) {
      final MetadataWrapper<T, Object> group = new MetadataWrapper<>(null,
          new SimpleStringProperty(grouping.getKey()), _ -> null);
      final List<TreeItem<T>> filteredItems = removeAllGroupItems(grouping.getValue());

      if (NO_GROUPING.equals(group.getTitle())) {
        rootItem.getChildren().addAll(filteredItems);
      } else {
        group.getChildren().addAll(filteredItems);
        rootItem.getChildren().add(group);
      }
    }
  }

  /**
   * Extracts all child items recursively into the result set.
   */
  private void getAllChildren(Set<TreeItem<T>> result, TreeItem<T> item) {
    for (TreeItem<T> child : item.getChildren()) {
      if (!child.getChildren().isEmpty()) {
        getAllChildren(result, child);
      }
      result.add(child);
    }
  }

  public void addItem(MetadataWrapper<T, G> item) {
    // we could find the group for the new item manually, but this is less code and probably works the same way
    rootItem.getChildren().add(item);
    autoGroup();
  }

  public void addItems(Collection<MetadataWrapper<T, G>> items) {
    rootItem.getChildren().addAll(items);
    autoGroup();
  }

  public void groupSelected() {
    final ObservableList<TreeItem<T>> itemsToGroup = treeView.getSelectionModel()
        .getSelectedItems();

    final LinkedHashSet<TreeItem<T>> selected = new LinkedHashSet<>();

    // flatten the items
    for (TreeItem<T> item : itemsToGroup) {
      getAllChildren(selected, item);
    }

    final MetadataWrapper<T, G> group = new MetadataWrapper<>(null,
        new SimpleStringProperty("Group"), _ -> null);
    group.getChildren().addAll(removeAllGroupItems(selected));
    rootItem.getChildren().add(group);
    treeView.getSelectionModel().select(group);
  }

  public void ungroupSelected() {
    final ObservableList<TreeItem<T>> itemsToUngroup = treeView.getSelectionModel()
        .getSelectedItems();
    final LinkedHashSet<TreeItem<T>> selected = new LinkedHashSet<>();

    // flatten the items
    for (TreeItem<T> item : itemsToUngroup) {
      getAllChildren(selected, item);
    }

    rootItem.getChildren().addAll(removeAllGroupItems(selected));
    treeView.getSelectionModel().clearSelection();
  }

  private static <T> @NotNull List<TreeItem<T>> removeAllGroupItems(
      Collection<TreeItem<T>> selected) {
    return selected.stream().map(item -> {
      if (item instanceof MetadataWrapper<?, ?> meta && meta.isGroup()) {
        return null;
      }
      return item;
    }).filter(Objects::nonNull).toList();
  }

  public @NotNull List<@NotNull TreeItem<@Nullable T>> getSelectedTreeItems() {
    return List.copyOf(treeView.getSelectionModel().getSelectedItems());
  }

  public @NotNull List<@NotNull T> getSelectedItems() {
    return getSelectedTreeItems().stream().map(TreeItem::getValue).filter(Objects::nonNull)
        .toList();
  }

  public void removeItems(List<MetadataWrapper<T, G>> items) {
    for (MetadataWrapper<T, G> item : items) {
      item.getParent().getChildren().remove(item);
    }
  }

  public void removeItem(MetadataWrapper<T, G> item) {
    item.getParent().getChildren().remove(item);
  }

  public void removeItemsByValues(Collection<T> values) {

    LinkedHashSet<TreeItem<T>> allItems = new LinkedHashSet<>();
    getAllChildren(allItems, rootItem);

    // quickly map all items to values to their items, so we can find the tree items without
    // iterating over tree items + values
    final HashMap<T, TreeItem<T>> treeItemValues = new HashMap<>();
    for (TreeItem<T> item : allItems) {
      final T value = item.getValue();
      if (value != null) {
        treeItemValues.put(value, item);
      }
    }

    for (T value : values) {
      final TreeItem<T> treeItem = treeItemValues.get(value);
      if (treeItem != null) {
        treeItem.getParent().getChildren().remove(treeItem);
      }
    }
  }

  public void removeItemByValue(T value) {
    removeItemsByValues(List.of(value));
  }

  public ObservableList<G> getGroupingChoices() {
    return groupingChoices;
  }
}
