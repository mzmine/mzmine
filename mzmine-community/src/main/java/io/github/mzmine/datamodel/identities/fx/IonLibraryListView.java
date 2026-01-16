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

import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonLibrarySorting;
import io.github.mzmine.javafx.components.FilterableListView;
import io.github.mzmine.javafx.components.MappingListCell;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.util.FxLayout.Position;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;

/**
 * List of ion libraries
 */
public class IonLibraryListView extends FilterableListView<IonLibrary> {

  private final ObjectProperty<IonLibrarySorting> listSorting = new SimpleObjectProperty<>(
      IonLibrarySorting.getDefault());
  private final ReadOnlyObjectProperty<IonLibrary> selectedIonLibrary;
  private final ObservableValue<Boolean> selectedLibraryIsInternal;


  public IonLibraryListView(ObservableList<IonLibrary> libraries, boolean allowRemoveItem,
      boolean allowEdit, boolean allowCreateNew) {
    super(libraries, allowRemoveItem, SelectionMode.SINGLE);
    setAskBeforeRemove(true);
    getListView().setPrefWidth(450);

    // create a list view with addtional controls for sorting and filtering
    // sorting:
    final HBox sortingCombo = FxComboBox.createLabeledComboBox("Sort by:",
        FXCollections.observableList(List.of(IonLibrarySorting.values())), listSorting);

    final List<Node> additionalNodes = List.of(sortingCombo);
    final List<MenuControls> stdButtons = new ArrayList<>();
    if (allowRemoveItem) {
      stdButtons.add(MenuControls.REMOVE_BTN);
    }
    if (allowEdit) {
      stdButtons.add(MenuControls.EDIT_BTN);
    }
    if (allowCreateNew) {
      stdButtons.add(MenuControls.CREATE_NEW_BTN);
    }

    addMenuFlowPane(Position.TOP, Pos.CENTER_LEFT, stdButtons, additionalNodes);

    // create the list view
    getListView().setCellFactory(_ -> new MappingListCell<>(Object::toString));

    // set comparator and filter
    listSorting.subscribe(nv -> sortingComparatorProperty().set(nv.getComparator()));

    selectedIonLibrary = getListView().getSelectionModel().selectedItemProperty();
    selectedLibraryIsInternal = selectedIonLibrary.map(IonLibraries::isInternalLibrary)
        .orElse(false);
    // internal libraries can only be duplicated
    editButtonTextProperty().bind(
        selectedLibraryIsInternal.map(isInternal -> isInternal ? "Copy" : "Edit"));

    disableRemoveProperty().bind(selectedLibraryIsInternal);
  }

  public static IonLibraryListView createImmutableMultiSelect(
      ObservableList<IonLibrary> libraries) {
    final IonLibraryListView view = new IonLibraryListView(libraries, false, false, false);
    view.getListView().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    return view;
  }

  public void setListSorting(IonLibrarySorting listSorting) {
    this.listSorting.set(listSorting);
  }

  public ObjectProperty<IonLibrarySorting> listSortingProperty() {
    return listSorting;
  }

  public IonLibrarySorting getListSorting() {
    return listSorting.get();
  }

  public IonLibrary getSelectedIonLibrary() {
    return selectedIonLibrary.get();
  }

  public ReadOnlyObjectProperty<IonLibrary> selectedIonLibraryProperty() {
    return selectedIonLibrary;
  }
}
