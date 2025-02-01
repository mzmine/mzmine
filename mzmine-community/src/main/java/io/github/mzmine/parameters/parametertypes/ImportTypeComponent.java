/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

public class ImportTypeComponent extends BorderPane {

  final TableView<ImportType> table = new TableView<>();

  public ImportTypeComponent() {
    super();

    table.setEditable(true);
    setMaxHeight(400);
    setMinHeight(200);

    final TableColumn<ImportType, Boolean> importColumn = new TableColumn<>("Import");
    importColumn.setCellFactory(column -> new CheckBoxTableCell());
    importColumn.setCellValueFactory(cdf -> cdf.getValue().selectedProperty());
    importColumn.setEditable(true);

    final TableColumn<ImportType, String> nameInFile = new TableColumn<>("Column name (csv)");
    nameInFile.setCellValueFactory(cdf -> cdf.getValue().csvColumnName());
    nameInFile.setCellFactory(column -> new TextFieldTableCell<>(new StringConverter<String>() {
      @Override
      public String toString(String object) {
        return object.trim();
      }

      @Override
      public String fromString(String string) {
        return string.trim();
      }
    }));
    nameInFile.setEditable(true);

    final TableColumn<ImportType, String> dataTypeColumn = new TableColumn<>("Data type (MZmine)");
    dataTypeColumn.setCellValueFactory(
        cdf -> new SimpleStringProperty(cdf.getValue().getDataType().getHeaderString()));
    dataTypeColumn.setEditable(false);

    table.getColumns().addAll(importColumn, nameInFile, dataTypeColumn);

    setCenter(table);
  }

  public List<ImportType> getValue() {
    return new ArrayList<>(table.getItems());
  }

  public void setValue(List<ImportType> value) {
    table.getItems().clear();
    if (value == null) {
      return;
    }
    table.getItems().addAll(value);
  }
}
