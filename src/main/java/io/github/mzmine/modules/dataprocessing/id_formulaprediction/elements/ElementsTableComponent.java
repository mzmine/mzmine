/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_formulaprediction.elements;
import com.panemu.tiwulfx.control.NumberField;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import io.github.mzmine.util.dialogs.PeriodicTableDialog;

import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;

public class ElementsTableComponent extends VBox{

  private final ObservableList<ElementsValue> elementsValues = FXCollections.observableArrayList();
  private final TableView<ElementsValue> elementsValueTable = new TableView();


  public ElementsTableComponent() {

    elementsValueTable.setEditable(true);
    this.maxWidth(200);
    this.maxHeight(200);
// allows the individual cells to be selected
    elementsValueTable.getSelectionModel().cellSelectionEnabledProperty().set(true);
    TableColumn<ElementsValue, IIsotope> elementCol = new TableColumn("Element");
    TableColumn<ElementsValue, String> maxCol = new TableColumn("Max");
    TableColumn<ElementsValue, String> minCol = new TableColumn("Min");

    // Make Column editibale
    maxCol.setCellFactory(TextFieldTableCell.forTableColumn());
    maxCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().
            getRow()).setMax(event.getNewValue()));
    minCol.setCellFactory(TextFieldTableCell.forTableColumn());
    minCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().
            getRow()).setMin(event.getNewValue()));

    elementCol.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getIsotope()));
    maxCol.setCellValueFactory(col-> {
      String max = String.valueOf(col.getValue().getMax());
      return new ReadOnlyObjectWrapper<>(max);
    });




    minCol.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getMin()));

    minCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().
            getRow()).setMin(event.getNewValue()));

    elementsValueTable.setItems(elementsValues);

    final Button addButton = new javafx.scene.control.Button("Add");
    final Button removeButton = new Button("Remove");
    // Add event
    addButton.setOnAction(t -> {
      PeriodicTableDialog dialog = new PeriodicTableDialog(null);
      dialog.setVisible(true);
      IIsotope chosenIsotope = dialog.getSelectedIsotope();
      if (chosenIsotope == null)
        return;
      elementsValues.add(new ElementsValue(chosenIsotope, "100", "0"));
    });

    // Remove event
    removeButton.setOnAction(t -> {
      ElementsValue element = elementsValueTable.getSelectionModel().getSelectedItem();
      elementsValues.remove(element);
    });

    this.setPadding(new Insets(5, 0, 0, 5));
    this.setSpacing(5);

    elementsValueTable.getColumns().addAll(elementCol, maxCol, minCol);
    HBox hBox = new HBox();
    hBox.getChildren().addAll(addButton, removeButton);
    hBox.setSpacing(3);


    this.getChildren().addAll(elementsValueTable,hBox);

  }


  public void setElements(MolecularFormulaRange elements) {

    if (elements == null)
      return;

    for (IIsotope isotope : elements.isotopes()) {
      int minCount = elements.getIsotopeCountMin(isotope);
      int maxCount = elements.getIsotopeCountMax(isotope);
      elementsValues.add(new ElementsValue(isotope, String.valueOf(maxCount), String.valueOf(minCount)));

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
