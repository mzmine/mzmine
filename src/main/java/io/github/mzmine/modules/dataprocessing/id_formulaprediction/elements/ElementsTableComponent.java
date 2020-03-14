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
import javax.swing.JPanel;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.embed.swing.JFXPanel;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.components.ComponentCellRenderer;
import io.github.mzmine.util.dialogs.PeriodicTableDialog;

import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;

public class ElementsTableComponent extends JPanel{

  private static final long serialVersionUID = 1L;

  private static ObservableList<ElementsValue> data = FXCollections.observableArrayList();

  public ElementsTableComponent() {

    VBox vBox = new VBox();
    TableView<ElementsValue> table = new TableView();
    Scene scene  =  new  Scene(vBox, Color.ALICEBLUE);

    table.maxWidth(200);
    table.setEditable(true);

    TableColumn<ElementsValue, IIsotope> elementCol = new TableColumn("Element");
    elementCol.setMaxWidth(70);
    elementCol.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getIsotope()));

    TableColumn<ElementsValue, Integer> maxCol = new TableColumn("Max");
    maxCol.setMaxWidth(70);
    maxCol.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getMax()));

    TableColumn<ElementsValue, Integer> minCol = new TableColumn("Min");
    minCol.setMaxWidth(70);
    minCol.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getMin()));
    table.setItems(data);

    final Button addButton = new javafx.scene.control.Button("Add");
    final Button removeButton = new Button("Remove");
    // Add event
    addButton.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
      @Override
      public void handle(ActionEvent t){
        PeriodicTableDialog dialog = new PeriodicTableDialog(null);
        dialog.setVisible(true);
        IIsotope chosenIsotope = dialog.getSelectedIsotope();
        if (chosenIsotope == null)
          return;
        data.add(new ElementsValue(chosenIsotope, 100, 0));
      }
    });

    // Remove event
    removeButton.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent t){
        ElementsValue element = table.getSelectionModel().getSelectedItem();
        data.remove(element);
      }
    });

    vBox.setPadding(new Insets(5, 0, 0, 5));
    vBox.setSpacing(5);

    table.getColumns().addAll(elementCol, maxCol, minCol);
    HBox hBox = new HBox();
    hBox.getChildren().addAll(addButton, removeButton);
    hBox.setSpacing(3);


    vBox.getChildren().addAll(table,hBox);


  }


  public void setElements(MolecularFormulaRange elements) {

    if (elements == null)
      return;

    for (IIsotope isotope : elements.isotopes()) {
      int minCount = elements.getIsotopeCountMin(isotope);
      int maxCount = elements.getIsotopeCountMax(isotope);
      data.add(new ElementsValue(isotope, maxCount, minCount));

    }
  }

}
