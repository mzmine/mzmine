/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
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
    table.getItems().addAll(value);
  }
}
