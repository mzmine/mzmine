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

package io.github.mzmine.parameters.parametertypes.elements;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import io.github.mzmine.util.dialogs.PeriodicTableDialog;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;

import java.util.HashSet;
import java.util.Set;

public class ElementsTableComponent extends FlowPane {

  private final ObservableList<ElementsValue> elementsValues = FXCollections.observableArrayList();
  private final TableView<ElementsValue> elementsValueTable = new TableView();


  public ElementsTableComponent() {

    elementsValueTable.setEditable(true);
    this.setMaxHeight(200);
    elementsValueTable.setMaxHeight(200);
    elementsValueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);



// allows the individual cells to be selected
    elementsValueTable.getSelectionModel().cellSelectionEnabledProperty().set(true);
    TableColumn<ElementsValue, String> elementCol = new TableColumn("Element");
    TableColumn<ElementsValue, String> maxCol = new TableColumn("Max");
    TableColumn<ElementsValue, String> minCol = new TableColumn("Min");

    // Make Column editable
    maxCol.setCellFactory(TextFieldTableCell.forTableColumn());
    maxCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().
            getRow()).setMax(event.getNewValue()));
    minCol.setCellFactory(TextFieldTableCell.forTableColumn());
    minCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().
            getRow()).setMin(event.getNewValue()));

    elementCol.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getIsotope().getSymbol()));
    maxCol.setCellValueFactory(col-> {
      String max = String.valueOf(col.getValue().getMax());
      return new ReadOnlyObjectWrapper<>(max);
    });

    minCol.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getMin()));

    minCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().
            getRow()).setMin(event.getNewValue()));


    elementsValueTable.setItems(elementsValues);

    final Button addButton = new Button("Add");
    final Button removeButton = new Button("Remove");
    // Add event
    addButton.setOnAction(
        t -> {
          PeriodicTableDialog dialog = new PeriodicTableDialog();
          dialog.show();
          IIsotope chosenIsotope = dialog.getSelectedIsotope();
          if (chosenIsotope == null) return;
          ElementsValue elementsValue = new ElementsValue(chosenIsotope, "100", "0");
            elementsValues.add(elementsValue);
        });

    // Remove event
    removeButton.setOnAction(t -> {
      ElementsValue element = elementsValueTable.getSelectionModel().getSelectedItem();
        elementsValues.remove(element);

    });

    this.setPadding(new Insets(5, 0, 0, 5));

    elementsValueTable.getColumns().addAll(elementCol, minCol, maxCol);
    VBox vBox = new VBox();
    vBox.getChildren().addAll(addButton, removeButton);
    vBox.setSpacing(10);

    this.getChildren().addAll(elementsValueTable,vBox);
    this.setHgap(10d);
    this.setAlignment(Pos.BASELINE_RIGHT);
  }


  public void setElements(MolecularFormulaRange elements) {

    if (elements == null)
      return;
    elementsValues.clear();
    for (IIsotope isotope : elements.isotopes()) {
      int minCount = elements.getIsotopeCountMin(isotope);
      int maxCount = elements.getIsotopeCountMax(isotope);
      ElementsValue elementsValue = new ElementsValue(isotope, String.valueOf(maxCount), String.valueOf(minCount));
       elementsValues.add(elementsValue);

    }

  }

  public MolecularFormulaRange getElements() {

    MolecularFormulaRange newValue = new MolecularFormulaRange();

    for (int row = 0; row < elementsValueTable.getItems().size(); row++) {

      ElementsValue elementsValue = elementsValueTable.getItems().get(row);

      IIsotope isotope = elementsValue.getIsotope();
      String minCount = elementsValue.getMin();
      String maxCount = elementsValue.getMax();
      newValue.addIsotope(isotope, Integer.parseInt(minCount), Integer.parseInt(maxCount));
    }
    return newValue;
  }

}
