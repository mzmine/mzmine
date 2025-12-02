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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.javafx.components.FilterableListView;
import io.github.mzmine.javafx.components.FilterableListView.MenuControls;
import io.github.mzmine.javafx.components.util.FxLayout;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxListViews {

  /**
   * Creates a ListView that also enables filtering and sorting
   *
   * @param items           original items
   * @param enableDeleteKey activate deletion by backspace and del key
   * @param selectionMode   the selection mode of items
   * @param <T>             item class
   * @return a new FilterableListView
   */
  public static <T> FilterableListView<T> newFilterableListView(
      final @NotNull ObservableList<T> items, boolean enableDeleteKey,
      final @NotNull SelectionMode selectionMode) {
    return newFilterableListView(items, enableDeleteKey, selectionMode, null, Pos.CENTER_LEFT,
        List.of(), List.of());
  }

  /**
   * Creates a ListView that also enables filtering and sorting
   *
   * @param items            original items
   * @param enableDeleteKey  activate deletion by backspace and del key
   * @param selectionMode    the selection mode of items
   * @param menuPosition     the menu position - no menu if null
   * @param standardControls the default controls implemented by the {@link FilterableListView}
   * @param additionalNodes  the additional nodes to be added
   * @param <T>              item class
   * @return a new FilterableListView
   */
  public static <T> FilterableListView<T> newFilterableListView(
      final @NotNull ObservableList<T> items, boolean enableDeleteKey,
      final @NotNull SelectionMode selectionMode, final @Nullable FxLayout.Position menuPosition,
      final @NotNull Pos menuAlignment, final @NotNull List<MenuControls> standardControls,
      final @NotNull List<Node> additionalNodes) {
    final FilterableListView<T> view = new FilterableListView<>(items, enableDeleteKey,
        selectionMode);

    if (menuPosition != null) {
      view.addMenuFlowPane(menuPosition, menuAlignment, standardControls, additionalNodes);
    }

    return view;
  }

  public static <T> ListView<T> newListView(ObservableList<T> items, boolean enableDeleteKey,
      final SelectionMode selectionMode) {
    final ListView<T> view = new ListView<>(items);

    view.getSelectionModel().setSelectionMode(selectionMode);

    if (enableDeleteKey) {
      view.setOnKeyPressed(event -> {
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
          view.getItems().removeAll(view.getSelectionModel().getSelectedItems());
          view.getSelectionModel().clearSelection();
        }
      });
    }
    return view;
  }
}
