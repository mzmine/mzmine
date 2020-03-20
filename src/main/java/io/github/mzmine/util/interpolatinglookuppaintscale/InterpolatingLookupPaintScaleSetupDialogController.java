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

package io.github.mzmine.util.interpolatinglookuppaintscale;

import java.awt.*;
import java.util.TreeMap;
import java.util.logging.Logger;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public class InterpolatingLookupPaintScaleSetupDialogController{

    public static final int VALUEFIELD_COLUMNS = 4;

    @FXML
    private AnchorPane main;

    @FXML
    private AnchorPane panelControlsAndList;

    @FXML
    private AnchorPane panelValueAndColor;

    @FXML
    private Label labelValue;

    @FXML
    private TextField fieldValue;

    @FXML
    private Button buttonColor;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private Button buttonAddModify;

    @FXML
    private Button buttonDelete;

    @FXML
    private AnchorPane panelOKCancelButton;

    @FXML
    private Button buttonOK;

    @FXML
    private Button buttonCancel;

    @FXML
    private TableView<InterpolatingLookupPaintScaleRow> tableLookupValues;

    @FXML
    private TableColumn<InterpolatingLookupPaintScaleRow, Double> valueColumn;

    @FXML
    private TableColumn<InterpolatingLookupPaintScaleRow, Color> colorColumn;

    @FXML
    private void initialize() {

        valueColumn.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getKey()));
        colorColumn.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getValue()));

        colorColumn.setCellFactory(e -> new TableCell<InterpolatingLookupPaintScaleRow, Color>() {
            @Override
            public void updateItem(Color item, boolean empty) {

                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle(null);
                    setGraphic(null);
                } else {

                    if(item!=null && !isEmpty()){
                        int r = item.getRed();
                        int g = item.getGreen();
                        int b = item.getBlue();
                        String hex = String.format("#%02x%02x%02x", r, g, b);
                        this.setStyle("-fx-background-color: " + hex + ";");
                    }
                }
            }
        });

    }

    private  TreeMap<Double, Color> lookupTable = new TreeMap<Double, Color>();
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private  ExitCode exitCode = ExitCode.CANCEL;
    private javafx.scene.paint.Color bColor = javafx.scene.paint.Color.WHITE;


    private ObservableList<InterpolatingLookupPaintScaleRow> observableTableList = FXCollections.observableArrayList();

    public void getObservableList(TreeMap<Double, Color> lookupTable, ObservableList<InterpolatingLookupPaintScaleRow> observableTableList){

        this.lookupTable = lookupTable;
        this.observableTableList = observableTableList;
        tableLookupValues.setItems(observableTableList);
    }

    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();

        if (src == colorPicker) {
            javafx.scene.paint.Color color = colorPicker.getValue();
            bColor = color;
        }

        if (src == buttonAddModify) {
            if (fieldValue.getText() == null) {
                MZmineCore.getDesktop().displayMessage("Please enter value first.");
                return;
            }
            Double d = Double.parseDouble(fieldValue.getText());

            java.awt.Color awtColor = new java.awt.Color((float) bColor.getRed(),
                    (float) bColor.getGreen(),
                    (float) bColor.getBlue(),
                    (float) bColor.getOpacity());

            lookupTable.put(d,awtColor);
            updateOBList(lookupTable);
        }

        if (src == buttonDelete) {
            InterpolatingLookupPaintScaleRow selected = tableLookupValues.getSelectionModel().getSelectedItem();
            if (selected != null) {
                observableTableList.remove(selected);
                lookupTable.remove(selected.getKey());

            }
        }

        if (src == buttonOK) {
            exitCode = ExitCode.OK;
            dispose();
        }
        if (src == buttonCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
        }
    }




    public ExitCode getExitCode() {
        return exitCode;
    }

    public InterpolatingLookupPaintScale getPaintScale() {
        InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
        for (Double value : lookupTable.keySet()) {
            paintScale.add(value, lookupTable.get(value));
        }
        return paintScale;
    }

    private void updateOBList(TreeMap<Double, Color> lookupTable){
        observableTableList.clear();
        for (Double value : lookupTable.keySet()) {
            InterpolatingLookupPaintScaleRow ir = new InterpolatingLookupPaintScaleRow(value, lookupTable.get(value));
            observableTableList.add(ir);
        }
    }

    public void dispose() {
        tableLookupValues.getScene().getWindow().hide();
    }

}