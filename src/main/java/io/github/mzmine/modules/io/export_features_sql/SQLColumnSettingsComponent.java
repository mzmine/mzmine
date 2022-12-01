/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_features_sql;

import java.util.ArrayList;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

public class SQLColumnSettingsComponent extends BorderPane {

    @NotNull
    private SQLColumnSettings value;
    private  TableView<SQLRowObject> columnsTable =new TableView<SQLRowObject>();
    private final Button addColumnButton,removeColumnButton;


    public SQLColumnSettingsComponent() {
        columnsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        value = new SQLColumnSettings();
        TableColumn<SQLRowObject,String> columnName=new TableColumn<SQLRowObject,String> (value.getColumnName(0));
        TableColumn<SQLRowObject,SQLExportDataType>  columnType=new TableColumn<SQLRowObject,SQLExportDataType> (value.getColumnName(1));
        TableColumn<SQLRowObject,String>  columnValue= new TableColumn<SQLRowObject,String> (value.getColumnName(2));

        columnName.setCellValueFactory(new PropertyValueFactory<>("Name")); //this is needed during connection to a database
        columnType.setCellValueFactory(new PropertyValueFactory<>("Type"));
        columnValue.setCellValueFactory(new PropertyValueFactory<>("Value"));

        columnsTable.getColumns().addAll(columnName,columnType,columnValue);  //added all the columns in the table
        setValue(value);
        columnsTable.setStyle("-fx-selection-bar: #3399FF; -fx-selection-bar-non-focused: #E3E3E3;"); //CSS color change on selection of row


        columnsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        columnName.setSortable(false);
        columnValue.setSortable(false);
        columnType.setSortable(false);

        columnName.setReorderable(false);
        columnValue.setReorderable(false);
        columnType.setReorderable(false);

        columnsTable.setPrefSize(550, 220);
        columnsTable.setFixedCellSize(columnsTable.getFixedCellSize()+20);
        columnsTable.setStyle("-fx-font: 10 \"Plain\"");

        //Setting action event on cells
        columnsTable.getSelectionModel().setCellSelectionEnabled(true);  //individual cell selection enabled
        columnsTable.setEditable(true);

        //Editors on each cell
      columnName.setCellFactory(TextFieldTableCell.<SQLRowObject>forTableColumn());
      columnName.setOnEditCommit(event -> {
        getValue().setValueAt(event.getNewValue(), event.getTablePosition().getRow(), 0);
        setValue(getValue()); //refresh the table
      });

      columnValue.setCellFactory(TextFieldTableCell.<SQLRowObject>forTableColumn());
      columnValue.setOnEditCommit(event -> {
        getValue().setValueAt(event.getNewValue().toUpperCase(), event.getTablePosition().getRow(), 2);
        setValue(getValue()); //refresh the table
      });

      ArrayList<SQLExportDataType> exportDataTypeValues=new ArrayList<SQLExportDataType>(Arrays.asList(SQLExportDataType.values()));

      columnType.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(SQLExportDataType.values())));
      columnType.setOnEditCommit(event -> {
        boolean selected = event.getNewValue().isSelectableValue();
        if(!selected){ //case of  invalid(Title) datatype selection
          getValue().setValueAt(exportDataTypeValues.get(exportDataTypeValues.indexOf(event.getNewValue())+1),event.getTablePosition().getRow(),1);
        }
        else {
          getValue().setValueAt(event.getNewValue(), event.getTablePosition().getRow(), 1);
        }
        setValue(getValue());
      });

        // Add buttons
        VBox buttonsPanel=new VBox(20);
        addColumnButton=new Button("Add");
        removeColumnButton=new Button("Remove");
        addColumnButton.setOnAction(this::actionPerformed);
        removeColumnButton.setOnAction(this::actionPerformed);
        buttonsPanel.getChildren().addAll(addColumnButton,removeColumnButton);


        this.setRight(buttonsPanel);
        this.setCenter(columnsTable);
        BorderPane.setMargin(buttonsPanel, new Insets(10));

    }


    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();
        if (src == addColumnButton) {
            value.addNewRow();
        }

        if (src == removeColumnButton) {
            SQLRowObject selectedRow= columnsTable.getSelectionModel().getSelectedItem();
            if (selectedRow ==null)
                return;
            value.removeRow(selectedRow);
        }
        setValue(value);

    }

    void setValue(@NotNull SQLColumnSettings newValue) {

        // Clear the table
        this.value = newValue;
        columnsTable.setItems(value.getTableData());
        columnsTable.refresh();
    }

    @NotNull
    synchronized SQLColumnSettings getValue() {
        return value;
    }
}


