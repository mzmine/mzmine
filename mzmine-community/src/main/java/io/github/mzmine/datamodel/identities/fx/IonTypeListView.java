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

import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonType.IonTypeStringFlavor;
import io.github.mzmine.datamodel.identities.iontype.IonTypeSorting;
import io.github.mzmine.javafx.components.FilterableListView;
import io.github.mzmine.javafx.components.MappingListCell;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.util.FxLayout.Position;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;

public class IonTypeListView extends FilterableListView<IonType> {

  // additional properties for the list view
  private final ObjectProperty<IonTypeSorting> listSorting = new SimpleObjectProperty<>(
      IonTypeSorting.getIonTypeDefault());

  public IonTypeListView(ObservableList<IonType> types) {
    this(types, true);
  }

  public IonTypeListView(ObservableList<IonType> types, boolean allowDeleteItems) {
    super(types, allowDeleteItems, SelectionMode.MULTIPLE);
    getListView().setPrefWidth(450);

    // create a list view with addtional controls for sorting and filtering
    // sorting:
    final HBox sortingCombo = FxComboBox.createLabeledComboBox("Sort by:",
        FXCollections.observableList(List.of(IonTypeSorting.values())), listSorting);

    final List<Node> additionalNodes = List.of(sortingCombo);
    final List<MenuControls> stdButtons = new ArrayList<>();

    if (allowDeleteItems) {
      stdButtons.add(MenuControls.CLEAR_BTN);
      stdButtons.add(MenuControls.REMOVE_BTN);
    }

    addMenuFlowPane(Position.TOP, Pos.CENTER_LEFT, stdButtons, additionalNodes);

    // create the list view
    getListView().setCellFactory(
        param -> new MappingListCell<>(ion -> ion.toString(IonTypeStringFlavor.FULL_WITH_MASS)));

    // set comparator and filter
    listSorting.subscribe(nv -> sortingComparatorProperty().set(nv.getComparator()));
  }

  public void setListSorting(IonTypeSorting listSorting) {
    this.listSorting.set(listSorting);
  }

  public ObjectProperty<IonTypeSorting> listSortingProperty() {
    return listSorting;
  }

  public IonTypeSorting getListSorting() {
    return listSorting.get();
  }
}
