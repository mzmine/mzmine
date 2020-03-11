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
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Logger;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class InterpolatingLookupPaintScaleSetupDialogController extends Stage implements Initializable {


    private static final long serialVersionUID = 1L;

    public static final int VALUEFIELD_COLUMNS = 4;

    private static final TreeMap<Double, Color> lookupTable = new TreeMap<Double, Color>();

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static InterpolatingLookupPaintScaleSetupDialogTableModel tableModel;

    private static Scene mainScene;





    // Load the window FXML
    FXMLLoader loader = new FXMLLoader((getClass().getResource("InterpolatingLookupPaintScaleSetupDialog.fxml")));


    //default constructor
    public InterpolatingLookupPaintScaleSetupDialogController(){
        logger.info("defalut constructor");
    }


    public InterpolatingLookupPaintScaleSetupDialogController(Object parent,
                                                    InterpolatingLookupPaintScale paintScale) {


        Double[] lookupValues = paintScale.getLookupValues();
        for (Double lookupValue : lookupValues) {
            Color color = (Color) paintScale.getPaint(lookupValue);
            lookupTable.put(lookupValue, color);
        }


        logger.info("cunstroctor of dialog");


        //load fxml
        try {
            Parent root = loader.load();
            mainScene  = new Scene(root);
        } catch (IOException e) {
            e.printStackTrace();
        }



        logger.info("cunstroctor of second");



        setScene(mainScene);

    }


    private ExitCode exitCode = ExitCode.CANCEL;

    // Load the window FXML


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
    private ScrollPane scrollpaneLookupValues;

    @FXML
    private TableView<InterpolatingLookupPaintScaleSetupDialogTableModel> tableLookupValues;


    private TableColumn<InterpolatingLookupPaintScaleSetupDialogTableModel, Double> value;


    private TableColumn<InterpolatingLookupPaintScaleSetupDialogTableModel, javafx.scene.paint.Color> color;



    @Override
    public void initialize(URL location, ResourceBundle resources) {

        logger.info("initilize_of_controller");
        tableModel = new InterpolatingLookupPaintScaleSetupDialogTableModel(lookupTable);

        tableLookupValues.getItems().addAll(tableModel);
        value = new TableColumn<>("value");
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        color = new TableColumn<>("color");
        color.setCellValueFactory(new PropertyValueFactory<>("color"));
        tableLookupValues.getColumns().addAll(value,color);

        //tableLookupValues.setItems(getdata(tableModel));
    }

    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();


        if (src == buttonColor) {
            logger.info("select color is called");
        }

        if (src == buttonAddModify) {
            logger.info("button addmodify is pressed");
            if (fieldValue.getText() == null) {
                MZmineCore.getDesktop().displayMessage("Please enter value first.");
                return;
            }

            scrollpaneLookupValues.requestLayout();
        }

        if (src == buttonDelete) {
            logger.info("button delete is pressed");
        }

        if (src == buttonOK) {
            exitCode = ExitCode.OK;
            logger.info("ok button pressed");
        }
        if (src == buttonCancel) {
            exitCode = ExitCode.CANCEL;
            logger.info("cancle button pressed");
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

    private ObservableList<InterpolatingLookupPaintScaleSetupDialogTableModel> getdata(InterpolatingLookupPaintScaleSetupDialogTableModel tModel) {
        ObservableList<InterpolatingLookupPaintScaleSetupDialogTableModel> tm = FXCollections.observableArrayList(tModel);
        tm.add(tModel);
        return tm;
    }
}
