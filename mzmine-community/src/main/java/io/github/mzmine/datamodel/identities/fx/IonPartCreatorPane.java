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

package io.github.mzmine.datamodel.identities.fx;

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldTitle;
import static io.github.mzmine.javafx.components.util.FxLayout.DEFAULT_PADDING_INSETS;

import io.github.mzmine.datamodel.identities.IonPartDefinition;
import io.github.mzmine.datamodel.identities.IonPartSorting;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.javafx.components.FilterableListView;
import io.github.mzmine.javafx.components.FilterableListView.MenuControls;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxListViews;
import io.github.mzmine.javafx.components.util.FxLayout.Position;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class IonPartCreatorPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(IonPartCreatorPane.class.getName());

  private final FilterableListView<IonPartDefinition> partListView;

  // additional properties for the list view
  private final ObjectProperty<IonPartSorting> listSorting = new SimpleObjectProperty<>(
      IonPartSorting.ALPHABETICAL);

  private final IonPartDefinitionPane ionPartDefinitionPane;


  public IonPartCreatorPane(ObservableList<IonPartDefinition> parts) {
    setTop(newBoldTitle("List of ion building blocks"));

    ionPartDefinitionPane = new IonPartDefinitionPane(this::addPart, true);

    partListView = createListView(parts);
    partListView.setCenter(ionPartDefinitionPane);
    partListView.setLeft(partListView.getListView());
    setCenter(partListView);
  }

  private @NotNull FilterableListView<IonPartDefinition> createListView(
      final ObservableList<IonPartDefinition> parts) {

    setPadding(DEFAULT_PADDING_INSETS);
    // create a list view with addtional controls for sorting and filtering
    // sorting:
    final HBox sortingCombo = FxComboBox.createLabeledComboBox("Sort by:",
        FXCollections.observableList(IonPartSorting.valuesForDefinitions()), listSorting);

    final List<Node> additionalNodes = List.of(sortingCombo);
    final List<MenuControls> stdButtons = List.of(MenuControls.REMOVE_BTN);

    // create the list view
    final FilterableListView<IonPartDefinition> partListView = FxListViews.newFilterableListView(
        parts, true, SelectionMode.MULTIPLE, Position.TOP, Pos.CENTER_LEFT, stdButtons,
        additionalNodes);

    partListView.onRemoveItem(this::removePart);

    // set comparator and filter
    listSorting.subscribe(
        nv -> partListView.sortingComparatorProperty().set(nv.getDefinitionComparator()));

    return partListView;
  }

  private void removePart(IonPartDefinition removed) {
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    global.removePartDefinition(removed);
  }

  /**
   * Adds the part to the final list
   */
  private void addPart(final @Nullable IonPartDefinition part) {
    if (part == null) {
      return;
    }
    // need to directly push into service for paring purposes
    GlobalIonLibraryService.getGlobalLibrary().addPartDefinition(part);

    var items = partListView.getOriginalItems();
    if (!items.contains(part)) {
      items.add(part);

      final ListView<IonPartDefinition> listView = partListView.getListView();
      listView.getSelectionModel().clearSelection();
      listView.getSelectionModel().select(part);
      listView.scrollTo(part);
    }
  }

}
