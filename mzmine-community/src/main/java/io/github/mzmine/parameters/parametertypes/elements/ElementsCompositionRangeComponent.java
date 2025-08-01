/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.elements;

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.util.dialogs.PeriodicTableDialog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IIsotope;

public class ElementsCompositionRangeComponent extends BorderPane {

  private final ObservableList<ElementsCompositionRangeValue> elementsValues = FXCollections.observableArrayList();
  private final TableView<ElementsCompositionRangeValue> elementsValueTable = new TableView<>();

  public ElementsCompositionRangeComponent() {

    elementsValueTable.setEditable(true);
    this.setMaxHeight(200);
    elementsValueTable.setMaxHeight(200);
    elementsValueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

    // Allows the individual cells to be selected
    elementsValueTable.getSelectionModel().cellSelectionEnabledProperty().set(true);
    TableColumn<ElementsCompositionRangeValue, String> elementCol = new TableColumn<>("Element");
    TableColumn<ElementsCompositionRangeValue, String> maxCol = new TableColumn<>("Max");
    TableColumn<ElementsCompositionRangeValue, String> minCol = new TableColumn<>("Min");

    // Make Column editable
    maxCol.setCellFactory(TextFieldTableCell.forTableColumn());
    maxCol.setOnEditCommit(
        event -> event.getTableView().getItems().get(event.getTablePosition().getRow())
            .setMax(event.getNewValue()));
    minCol.setCellFactory(TextFieldTableCell.forTableColumn());
    minCol.setOnEditCommit(
        event -> event.getTableView().getItems().get(event.getTablePosition().getRow())
            .setMin(event.getNewValue()));

    elementCol.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getIsotope().getSymbol()));
    maxCol.setCellValueFactory(col -> {
      String max = String.valueOf(col.getValue().getMax());
      return new ReadOnlyObjectWrapper<>(max);
    });

    minCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMin()));

    minCol.setOnEditCommit(
        event -> event.getTableView().getItems().get(event.getTablePosition().getRow())
            .setMin(event.getNewValue()));

    elementsValueTable.setItems(elementsValues);

    final Button addButton = new Button("Add");
    final Button removeButton = new Button("Remove");
    // Add event
    addButton.setOnAction(t -> {
      PeriodicTableDialog dialog = new PeriodicTableDialog();
      dialog.show();
      dialog.setOnHiding(e -> {
        IIsotope chosenIsotope = dialog.getSelectedIsotope();
        if (chosenIsotope == null) {
          return;
        }
        ElementsCompositionRangeValue elementsValue = new ElementsCompositionRangeValue(
            chosenIsotope, "100", "0");
        elementsValues.add(elementsValue);
      });
    });

    // Remove event
    removeButton.setOnAction(t -> {
      ElementsCompositionRangeValue element = elementsValueTable.getSelectionModel()
          .getSelectedItem();
      elementsValues.remove(element);

    });

    this.setPadding(new Insets(5, 0, 0, 5));

    elementsValueTable.getColumns().addAll(elementCol, minCol, maxCol);
    VBox vBox = FxLayout.newVBox(Pos.TOP_LEFT, addButton, removeButton);

    setCenter(elementsValueTable);
    setRight(vBox);
  }

  public MolecularFormulaRange getElements() {

    MolecularFormulaRange newValue = new MolecularFormulaRange();

    for (int row = 0; row < elementsValueTable.getItems().size(); row++) {

      ElementsCompositionRangeValue elementsValue = elementsValueTable.getItems().get(row);

      IIsotope isotope = elementsValue.getIsotope();
      String minCount = elementsValue.getMin();
      String maxCount = elementsValue.getMax();
      newValue.addIsotope(isotope, Integer.parseInt(minCount), Integer.parseInt(maxCount));
    }
    return newValue;
  }

  public void setElements(MolecularFormulaRange elements) {
    elementsValues.clear();
    if (elements == null) {
      return;
    }

    List<IIsotope> isotopes = new ArrayList<>();
    elements.isotopes().forEach(isotopes::add);
    isotopes.sort(Comparator.comparing(IElement::getSymbol));

    for (IIsotope isotope : isotopes) {
      int minCount = elements.getIsotopeCountMin(isotope);
      int maxCount = elements.getIsotopeCountMax(isotope);
      ElementsCompositionRangeValue elementsValue = new ElementsCompositionRangeValue(isotope,
          String.valueOf(maxCount), String.valueOf(minCount));
      elementsValues.add(elementsValue);
    }

  }

}
