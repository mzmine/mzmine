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

package io.github.mzmine.javafx.dialogs;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A popup that
 *
 * @param <T>
 */
public abstract class FilterableMenuPopup<T> extends Popup {

  private final BorderPane contentPane;
  private final StringProperty searchText = new SimpleStringProperty("");
  private final TextField searchField;
  private final ListView<T> listView;
  private final ObservableList<T> originalItems;
  private final ObjectProperty<Comparator<T>> comparator = new SimpleObjectProperty<>(
      Comparator.comparing(Object::toString));
  private final ObjectProperty<Predicate<T>> filterPredicate = new SimpleObjectProperty<>(
      _ -> true);
  private final Pane top;

  public FilterableMenuPopup(ObservableList<T> items, boolean addRemoveButton, Node... buttons) {
    super();
    this.originalItems = items;

    listView = createListView();

    searchField = FxTextFields.newTextField(10, searchText, "Search...");

    if (addRemoveButton) {
      buttons = buttons == null ? new Node[1] : Arrays.copyOf(buttons, buttons.length + 1);
      buttons[buttons.length - 1] = FxButtons.createButton("Remove", FxIcons.X_CIRCLE,
          "Remove selected preset (use arrow keys up/down to select) or press delete key to remove.",
          this::askRemoveSelected); //
    }

    top = createTopMenu(buttons);

    // property.map somehow did not work
    searchText.subscribe((_, nv) -> filterPredicate.setValue(createPredicate(nv)));

    contentPane = FxLayout.newBorderPane(listView);
    contentPane.setTop(top);
    getContent().add(contentPane);

    PropertyUtils.onChange(this::recalcHeight, items, top.heightProperty());

    setAutoHide(true);
    initEventListeners();
  }

  /**
   * @return the removed item or null if none was removed
   */
  public T askRemoveSelected() {
    final T selectedItem = listView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      return null;
    }
    final String name = selectedItem.toString();
    boolean remove = DialogLoggerUtil.showDialogYesNo("Remove preset %s?".formatted(name),
        "Are you sure you want to remove " + name);
    if (remove) {
      return removeItem(selectedItem);
    }
    return null;
  }

  public String getSearchText() {
    return searchText.get();
  }

  /**
   * Default just removes from originalItems list. This will not work for unmodifiable lists - then
   * overwrite
   *
   * @return the removed item or null if none was removed
   */
  public @Nullable T removeItem(@Nullable T selectedItem) {
    if (selectedItem == null) {
      return null;
    }
    try {
      originalItems.remove(selectedItem);
      return selectedItem;
    } catch (Exception ex) {
      // silent as this is the most likely unmodifiable list exception
    }
    return null;
  }

  private void recalcHeight() {
    final double height = top.getHeight();
    contentPane.setPrefHeight(height + originalItems.size() * 25 + 40);
  }

  private @NotNull Pane createTopMenu(Node[] buttons) {
    final Insets insets = new Insets(0, 2, 5, 2);
    final HBox searchBox = FxLayout.newHBox(insets,
        FxIconUtil.newIconButton(FxIcons.X_CIRCLE, () -> searchText.setValue("")), searchField);
    HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);

    final var top = new BorderPane(searchBox);

    if (buttons.length > 0) {
      var buttonPane = FxLayout.newFlowPane(Pos.CENTER, insets, buttons);
      top.setTop(buttonPane);
    }
    return top;
  }

  private @NotNull ListView<T> createListView() {
    final SortedList<T> sortedList = new SortedList<>(originalItems, comparator.get());
    sortedList.comparatorProperty().bind(comparator);

    // filtered list needs to happen from sorted list.
    // filtered list does not trigger change event if there is no change listener attached
    final FilteredList<T> filteredItems = new FilteredList<>(sortedList);
    filteredItems.predicateProperty().bind(filterPredicate);

    final ListView<T> listView = new ListView<>(filteredItems);
    return listView;
  }

  public ListView<T> getListView() {
    return listView;
  }

  public void setComparator(Comparator<T> comparator) {
    this.comparator.set(comparator);
  }

  private void initEventListeners() {
    listView.setOnMouseClicked(event -> {
      // Check for a double-click
      if (event.getClickCount() == 1
          && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
        T selectedItem = listView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
          itemClickedAndHide(selectedItem);
          event.consume();
        }
      }
    });
    contentPane.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        hide();
        event.consume();
      } else if (event.getCode() == KeyCode.ENTER) {
        final T selectedItem = listView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
          itemClickedAndHide(selectedItem);
        }
        event.consume();
      } else if (event.getCode() == KeyCode.DELETE) {
        askRemoveSelected();
        event.consume();
      }
    });
    EventHandler<KeyEvent> arrowHandler = event -> {
      if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.PAGE_UP) {
        event.consume();
        shiftSelection(-1);
      }
      if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.PAGE_DOWN) {
        event.consume();
        shiftSelection(1);
      }
    };
    searchField.addEventHandler(KeyEvent.KEY_PRESSED, arrowHandler);

    // auto close when clicked somewhere
    focusedProperty().addListener((_, _, isNowFocused) -> {
      if (!isNowFocused) {
        hide();
      }
    });
  }

  private void shiftSelection(int add) {
    final int newSelectedIndex = listView.getSelectionModel().getSelectedIndex() + add;
    if (newSelectedIndex < 0 || newSelectedIndex >= listView.getItems().size()) {
      return;
    }
    listView.getSelectionModel().select(newSelectedIndex);
    listView.scrollTo(newSelectedIndex);
//    listView.getFocusModel().focus(newIndex);
  }

  public void show(Node node) {
    final Bounds boundsInScreen = node.localToScreen(node.getBoundsInLocal());
    show(node, boundsInScreen.getMinX(), boundsInScreen.getMinY());
  }

  @Override
  public void show(Node node, double v, double v1) {
    if (isShowing()) {
      return;
    }
    searchText.setValue("");
    super.setAnchorLocation(AnchorLocation.CONTENT_BOTTOM_LEFT);
    super.show(node, v, v1);

    searchField.requestFocus();
  }

  private void itemClickedAndHide(final @NotNull T item) {
    searchText.setValue("");
    hide();
    onItemActivated(item);
  }

  public abstract void onItemActivated(@NotNull T item);

  public abstract @NotNull Predicate<T> createPredicate(String searchText);


  public void setSearchText(String text) {
    searchText.setValue(text);
  }
}
