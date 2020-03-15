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

package io.github.mzmine.modules.io.sqlexport;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import javax.annotation.Nonnull;

public class SQLColumnSettingsComponent extends BorderPane {

    @Nonnull
    private SQLColumnSettings value;
    private final TableView<SQLRowObject> columnsTable =new TableView<SQLRowObject>();
    private final Button addColumnButton,removeColumnButton;


    public SQLColumnSettingsComponent() {
        columnsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        value = new SQLColumnSettings();
        TableColumn<SQLRowObject,String> columnName=new TableColumn<SQLRowObject,String> (value.getColumnName(0));
        TableColumn<SQLRowObject,String>  columnType=new TableColumn<SQLRowObject,String> (value.getColumnName(1));
        TableColumn<SQLRowObject,SQLExportDataType>  columnValue= new TableColumn<SQLRowObject,SQLExportDataType> (value.getColumnName(2));

        columnName.setCellValueFactory(new PropertyValueFactory<>("Name")); //this is needed during connection to a database
        columnValue.setCellValueFactory(new PropertyValueFactory<>("Value"));
        columnType.setCellValueFactory(new PropertyValueFactory<>("Type"));

        columnsTable.getColumns().addAll(columnName,columnType,columnValue);  //added all the columns in the table
        columnsTable.setItems(value.getlist());
        columnsTable.setStyle("-fx-selection-bar: #3399FF; -fx-selection-bar-non-focused: #E3E3E3;"); //CSS color change on selection of row
        columnsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if(newSelection!=null){

            }

        });

//        columnsTable = new JTable(value) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
//                Component c = super.prepareRenderer(renderer, row, column);
//                if (!isCellEditable(row, column)) {
//                    if (isCellSelected(row, column)) {
//                        c.setBackground(Color.decode("#3399FF"));
//                        c.setForeground(Color.white);
//                    } else {
//                        c.setBackground(Color.decode("#E3E3E3"));
//                    }
//                } else {
//                    if (isCellSelected(row, column)) {
//                        c.setBackground(Color.decode("#3399FF"));
//                        c.setForeground(Color.white);
//                    } else {
//                        c.setBackground(Color.white);
//                        c.setForeground(Color.black);
//                    }
//                }
//                return c;
//            }
//        };

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

        ObservableList<SQLExportDataType> list= FXCollections.observableArrayList(SQLExportDataType.values());
        ChoiceBox<SQLExportDataType> dataTypeChoice=new ChoiceBox<SQLExportDataType>(list);

//        DefaultCellEditor dataTypeEditor = new DefaultCellEditor(dataTypeCombo);
//        columnsTable.setDefaultEditor(SQLExportDataType.class, dataTypeEditor);

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

    }

    void setValue(@Nonnull SQLColumnSettings newValue) {

        // Clear the table
        this.value = newValue;
        columnsTable.setItems(value.getlist());
    }

    @Nonnull
    synchronized SQLColumnSettings getValue() {
        return value;
    }

}
