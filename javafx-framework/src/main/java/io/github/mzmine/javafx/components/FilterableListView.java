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

package io.github.mzmine.javafx.components;

import static io.github.mzmine.util.StringUtils.allWordsSubMatchPredicate;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxListViews;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.components.util.FxLayout.Position;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilterableListView<T> extends BorderPane {

  private final ListView<T> listView;

  // this property disables both the button and key
  private final BooleanProperty disableRemove = new SimpleBooleanProperty(false);
  // only handles the remove key - button still works
  private final BooleanProperty removeKeyActive = new SimpleBooleanProperty(false);
  private final BooleanProperty searchFieldVisible = new SimpleBooleanProperty(true);

  private final StringProperty editButtonText = new SimpleStringProperty("Edit");
  // an externally defined filter
  private final ObjectProperty<@NotNull Predicate<T>> externalFilterPredicate = new SimpleObjectProperty<>(
      o -> true);
  // search predicate
  private final ObjectProperty<@NotNull Predicate<T>> searchFilterPredicate = new SimpleObjectProperty<>(
      o -> true);

  private final BooleanProperty askBeforeRemove = new SimpleBooleanProperty(false);
  private final ObjectProperty<Comparator<T>> sortingComparator = new SimpleObjectProperty<>();

  // search field
  private final StringProperty searchText;

  // on edit button
  private final ObjectProperty<Consumer<T>> editSelectedAction = new SimpleObjectProperty<>();
  // on create new button
  private final ObjectProperty<Runnable> createNewAction = new SimpleObjectProperty<>();

  // original items are input into filtering and sorting
  private final ObservableList<T> originalItems;
  private final SortedList<T> sortedFilteredItems;
  private Consumer<T> onRemoveItemListener;

  // top flow pane

  /**
   *
   */
  public FilterableListView(final ObservableList<T> items, boolean enableDeleteKey,
      SelectionMode selectionMode) {
    this(items, enableDeleteKey, selectionMode, true);
  }

  public FilterableListView(final ObservableList<T> items, boolean enableDeleteKey,
      SelectionMode selectionMode, boolean showSearchField) {
    this.removeKeyActive.set(enableDeleteKey);
    this.originalItems = items;
    this.searchFieldVisible.set(showSearchField);

    sortedFilteredItems = new SortedList<>(items);
    sortedFilteredItems.comparatorProperty().bind(sortingComparator);

    // filtered list needs to happen from sorted list.
    // filtered list does not trigger change event if there is no change listener attached
    final FilteredList<T> filteredItems = new FilteredList<>(sortedFilteredItems);
    // the combined filter from search and from externalFilterPredicate
    ObjectBinding<Predicate<T>> filterPredicate = Bindings.createObjectBinding(this::combineFilters,
        searchFilterPredicate, externalFilterPredicate);
    filteredItems.predicateProperty().bind(filterPredicate);

    // need to add delete key handling separately not through the factory
    this.listView = FxListViews.newListView(filteredItems, false, selectionMode);

    // search text field
    final TextField searchField = TextFields.createClearableTextField();
    searchField.setPromptText("Search...");
    searchField.setTooltip(new Tooltip("Enter text to search in list."));

    searchField.visibleProperty().bind(searchFieldVisible);
    FxLayout.bindManagedToVisible(searchField);

    searchText = searchField.textProperty();
    searchFilterPredicate.bind(searchText.map(this::createSearchPredicate));

    // layout
    List<RowConstraints> rows = List.of(FxLayout.newGridRowConstraints(),
        FxLayout.newFillHeightRow());
    final GridPane mainGrid = FxLayout.newGrid1ColFillW(Insets.EMPTY, FxLayout.DEFAULT_SPACE, rows,
        searchField, listView);
    setCenter(mainGrid);

    addKeyEventListener();
  }

  private @NotNull Predicate<T> createSearchPredicate(String query) {
    return allWordsSubMatchPredicate(query, T::toString);
  }

  private void addKeyEventListener() {
    listView.setOnKeyPressed(event -> {
      if (removeKeyActive.get() && disableRemove.get() && (event.getCode() == KeyCode.DELETE
          || event.getCode() == KeyCode.BACK_SPACE)) {
        removeSelectedItems();
      }
      if (event.getCode() == KeyCode.ESCAPE) {
        getListView().getSelectionModel().clearSelection();
      }
    });
  }

  /**
   * Creates a menu with options for standard controls and additional nodes
   *
   * @param standardControls standard controls to be added
   * @param menuAlignment
   * @return the flow pane
   */
  public FlowPane addMenuFlowPane(final @NotNull Pos menuAlignment,
      @NotNull List<MenuControls> standardControls) {
    return addMenuFlowPane(Position.TOP, menuAlignment, standardControls, List.of());
  }

  /**
   * Creates a menu with options for standard controls and additional nodes
   *
   * @param position         position of the flow pane, never center
   * @param menuAlignment
   * @param standardControls standard controls to be added
   * @param additionalNodes  added after the standard controls
   * @return the flow pane
   */
  public FlowPane addMenuFlowPane(@NotNull Position position, final @NotNull Pos menuAlignment,
      @NotNull List<MenuControls> standardControls, @NotNull List<Node> additionalNodes) {
    if (position == Position.CENTER) {
      throw new IllegalArgumentException(
          "Cannot place node in the center as this will remove the ListView");
    }

    final BooleanBinding nothingSelectedBinding = listView.getSelectionModel()
        .selectedItemProperty().isNull();

    List<Node> nodesToAdd = new ArrayList<>();

    for (final MenuControls control : standardControls) {
      switch (control) {
        case CLEAR_BTN -> nodesToAdd.add(FxButtons.createButton("Clear", FxIcons.CLEAR,
            "Clear and remove all elements that are currently shown. This only includes those that match any filter (if available).",
            this::clearFilteredItems));
        case CREATE_NEW_BTN -> nodesToAdd.add(
            FxButtons.createButton("Create new", FxIcons.ADD, "Create a new item",
                this::createNewItem));
        case REMOVE_BTN -> {
          final Button btn = FxButtons.createDisabledButton("Remove", FxIcons.X_CIRCLE,
              "Removes all selected items", disableRemove.or(nothingSelectedBinding),
              this::removeSelectedItems);
          nodesToAdd.add(btn);
        }
        case EDIT_BTN -> {
          final Button btn = FxButtons.createDisabledLabelButton(editButtonText, FxIcons.EDIT,
              "Edit the selected item", nothingSelectedBinding, this::editSelectedItem);
          nodesToAdd.add(btn);
        }
      }
    }

    nodesToAdd.addAll(additionalNodes);

    final FlowPane flowPane = FxLayout.newFlowPane(menuAlignment, nodesToAdd.toArray(Node[]::new));
    FxLayout.addNode(this, flowPane, position);
    return flowPane;
  }

  private void editSelectedItem() {
    final T selected = listView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    final Consumer<T> editAction = editSelectedAction.get();
    if (editAction == null) {
      throw new IllegalStateException("No edit action defined for this list view.");
    }
    editAction.accept(selected);
  }

  private void createNewItem() {
    final Runnable createAction = createNewAction.get();
    if (createAction == null) {
      throw new IllegalStateException("No create new item action defined for this list view.");
    }
    createAction.run();
  }

  public void setEditSelectedAction(Consumer<T> editSelectedAction) {
    this.editSelectedAction.set(editSelectedAction);
  }

  public void setCreateNewAction(Runnable createNewAction) {
    this.createNewAction.set(createNewAction);
  }

  public void removeSelectedItems() {

    // have to remove items from the original list
    // sorted list and filtered list are immutable
    final int selectedIndex = listView.getSelectionModel().getSelectedIndex();

    // skip
    if (selectedIndex < 0 || (askBeforeRemove.get() && !DialogLoggerUtil.showDialogYesNo(
        "Remove items?", "Are you sure you want to remove all selected items?"))) {
      return;
    }

    removeItems(listView.getSelectionModel().getSelectedItems());
    // selection automatically moves to the previous index or is cleared when empty. seems fine
  }

  /**
   * Remove items and notify listener if available
   */
  public void removeItems(List<T> toRemove) {
    toRemove = List.copyOf(toRemove);

    originalItems.removeAll(toRemove);

    if (onRemoveItemListener != null) {
      for (T r : toRemove) {
        onRemoveItemListener.accept(r);
      }
    }
  }

  /**
   * only clears the filtered items currently visible
   */
  public void clearFilteredItems() {
    // need to accept
    final boolean clear = DialogLoggerUtil.showDialogYesNo("Clear all shown items?",
        "Are you sure you want to remove all items, this includes all items matching the current filters.");
    if (clear) {
      removeItems(sortedFilteredItems);
    }
  }

  public ListView<T> getListView() {
    return listView;
  }

  public BooleanProperty searchFieldVisibleProperty() {
    return searchFieldVisible;
  }

  public void setSearchFieldVisible(boolean visible) {
    searchFieldVisible.set(visible);
  }

  public Predicate<T> getExternalFilterPredicate() {
    return externalFilterPredicate.get();
  }

  public ObjectProperty<Predicate<T>> externalFilterPredicateProperty() {
    return externalFilterPredicate;
  }

  public boolean isRemoveKeyActive() {
    return removeKeyActive.get();
  }

  public BooleanProperty removeKeyActiveProperty() {
    return removeKeyActive;
  }

  public Comparator<T> getSortingComparator() {
    return sortingComparator.get();
  }

  public ObjectProperty<Comparator<T>> sortingComparatorProperty() {
    return sortingComparator;
  }

  public ObservableList<T> getOriginalItems() {
    return originalItems;
  }

  public SortedList<T> getSortedFilteredItems() {
    return sortedFilteredItems;
  }

  public boolean isAskBeforeRemove() {
    return askBeforeRemove.get();
  }

  public BooleanProperty askBeforeRemoveProperty() {
    return askBeforeRemove;
  }

  public void setAskBeforeRemove(boolean askBeforeRemove) {
    this.askBeforeRemove.set(askBeforeRemove);
  }

  public StringProperty editButtonTextProperty() {
    return editButtonText;
  }

  public BooleanProperty disableRemoveProperty() {
    return disableRemove;
  }

  /**
   * Removes the top menu and returns it to allow customization of the layout. The top menu may be
   * added into another layout.
   *
   * @return the top menu or null if there was no menu
   */
  @Nullable
  public Node removeTopMenu() {
    final Node top = getTop();
    setTop(null);
    return top;
  }

  public void onRemoveItem(Consumer<T> onRemoveItemListener) {
    this.onRemoveItemListener = onRemoveItemListener;
  }

  private Predicate<T> combineFilters() {
    final Predicate<T> search = searchFilterPredicate.get();
    final Predicate<T> external = externalFilterPredicate.get();
    return external.and(search);
  }


  public enum MenuControls {
    REMOVE_BTN, CLEAR_BTN, EDIT_BTN, CREATE_NEW_BTN
  }
}
