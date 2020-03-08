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

package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.application.Platform;

import java.text.NumberFormat;
import java.util.logging.Logger;


public class ResultWindowController {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private final NumberFormat massFormat = MZmineCore.getConfiguration().getMZFormat();
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();


    private ObservableList<ResultFormula> formulas;

    @FXML
    private TableView<ResultFormula>resultTable;
    @FXML
    private TableColumn<ResultFormula, String>Formula;
    @FXML
    private TableColumn<ResultFormula, String>MassDifference;
    @FXML
    private TableColumn<ResultFormula, String>RDBE;
    @FXML
    private TableColumn<ResultFormula, String>IsotopePattern;
    @FXML
    private TableColumn<ResultFormula, String>MSScore;

    @FXML
    private void initialize(){
        formulas = FXCollections.observableArrayList();
        Formula.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getFormulaAsString()));
        RDBE.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(String.valueOf(cell.getValue().getRDBE())));
        MassDifference.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(String.valueOf(cell.getValue().getExactMass())));
        IsotopePattern.setCellValueFactory(cell->new ReadOnlyObjectWrapper<>(String.valueOf(cell.getValue().getIsotopeScore())));
        MSScore.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(String.valueOf(cell.getValue().getMSMSScore())));
        resultTable.setItems(formulas);
    }

    public void addNewListItem(final ResultFormula formula)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                formulas.add(formula);

            }
        });

    }

    private  PeakListRow peakListRow;
    private  Task searchTask;
    private  String title;
    private double searchedMass;



    public void initValues(String title, PeakListRow peakListRow, double searchedMass, int charge,
                        Task searchTask) {

        this.title = title;
        this.peakListRow = peakListRow;
        this.searchTask = searchTask;
        this.searchedMass = searchedMass;
    }


    @FXML
    private Button ShowMS;
    @FXML
    private Button Copy;
    @FXML
    private Button ViewIsotope;
    @FXML
    private Button addIdentity;
    @FXML
    private Button Export;


    @FXML
    private void AddIdentityClick(ActionEvent ae){
// TODO: handle Button event
        Button button = ( Button ) ae.getSource();
        Alert a= new Alert(Alert.AlertType.CONFIRMATION);
        a.show();
    }

    @FXML
    private void exportClick(ActionEvent ae){
// TODO: handle Button event
    }

    @FXML
    private void viewIsotopeClick(ActionEvent ae){
// TODO: handle Button event
    }

    @FXML
    private void copyClick(ActionEvent ae){
// TODO: handle Button event
    }

    @FXML
    private void showsMSClick(ActionEvent ae){
// TODO: handle Button event
    }

    public void dispose() {

        // Cancel the search task if it is still running
        TaskStatus searchStatus = searchTask.getStatus();
        if ((searchStatus == TaskStatus.WAITING) || (searchStatus == TaskStatus.PROCESSING)) {
            searchTask.cancel();
        }

        resultTable.getScene().getWindow().hide();

    }


}
