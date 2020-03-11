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

import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;
import java.util.logging.Logger;


public class ResultWindowController {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private final NumberFormat massFormat = MZmineCore.getConfiguration().getMZFormat();
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    private final ObservableList<ResultFormula> formulas = FXCollections.observableArrayList();

    @FXML
    private TableView<ResultFormula>resultTable;
    @FXML
    private TableColumn<ResultFormula, String>Formula;
    @FXML
    private TableColumn<ResultFormula, Double>MassDifference;
    @FXML
    private TableColumn<ResultFormula, Double>RDBE;
    @FXML
    private TableColumn<ResultFormula, String>IsotopePattern;
    @FXML
    private TableColumn<ResultFormula, String>MSScore;

    @FXML
    private void initialize(){
        Formula.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getFormulaAsString()));

        RDBE.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getRDBE()));

        MassDifference.setCellValueFactory(cell-> {
            double ExactMass = cell.getValue().getExactMass();
            double MassDiff = searchedMass - ExactMass;

            return new ReadOnlyObjectWrapper<>(Double.parseDouble(massFormat.format(MassDiff)));
        });

        IsotopePattern.setCellValueFactory(cell->{
            String isotopeScore = String.valueOf(cell.getValue().getIsotopeScore());
            String cellVal = "";
            if(cell.getValue().getIsotopeScore() != null)
            {
                cellVal = percentFormat.format(Double.parseDouble(isotopeScore));
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });

        MSScore.setCellValueFactory(cell-> {
            String Msscore = String.valueOf(cell.getValue().getMSMSScore());
            String cellVal = "";
            if(cell.getValue().getMSMSScore() !=null)
            {
                cellVal = percentFormat.format(Double.parseDouble(Msscore));
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });

        resultTable.setItems(formulas);
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
    private void AddIdentityClick(ActionEvent ae){

        ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();

        if(formula == null) {
            MZmineCore.getDesktop().displayMessage(null,
                    "Select one result to add as compound identity");
            return;
        }

        SimplePeakIdentity newIdentity = new SimplePeakIdentity(formula.getFormulaAsString());
        peakListRow.addPeakIdentity(newIdentity, false);

        dispose();
    }

    @FXML
    private void exportClick(ActionEvent ae) throws IOException {

        // Ask for filename
        FileChooser fileChooser = new FileChooser();
        File result = fileChooser.showSaveDialog(null);
        if(result==null)
        {
            return;
        }
        fileChooser.setTitle("Export");

        try {
            FileWriter fileWriter = new FileWriter(result);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write("Formula,Mass,RDBE,Isotope pattern score,MS/MS score");
            writer.newLine();

            for (int row = 0; row < resultTable.getItems().size(); row++) {
                ResultFormula formula = resultTable.getItems().get(row);
                writer.write(formula.getFormulaAsString());
                writer.write(",");
                writer.write(String.valueOf(formula.getExactMass()));
                writer.write(",");
                if (formula.getRDBE() != null)
                    writer.write(String.valueOf(formula.getRDBE()));
                writer.write(",");
                if (formula.getIsotopeScore() != null)
                    writer.write(String.valueOf(formula.getIsotopeScore()));
                writer.write(",");
                if (formula.getMSMSScore() != null)
                    writer.write(String.valueOf(formula.getMSMSScore()));
                writer.newLine();
            }

            writer.close();

        } catch (Exception ex) {
            MZmineCore.getDesktop().displayErrorMessage(
                    "Error writing to file " + result + ": " + ExceptionUtils.exceptionToString(ex));
        }
        return;



    }

    @FXML
    private void viewIsotopeClick(ActionEvent ae){
        ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
        if(formula == null)
        {
            MZmineCore.getDesktop().displayMessage(null,
                    "Select one result to copy");
            return;
        }

        logger.finest("Showing isotope pattern for formula " + formula.getFormulaAsString());
        IsotopePattern predictedPattern = formula.getPredictedIsotopes();

        if (predictedPattern == null)
            return;

        Feature peak = peakListRow.getBestPeak();

        RawDataFile dataFile = peak.getDataFile();
        int scanNumber = peak.getRepresentativeScanNumber();
        SpectraVisualizerModule.showNewSpectrumWindow(dataFile, scanNumber, null,
                peak.getIsotopePattern(), predictedPattern);
    }

    @FXML
    private void copyClick(ActionEvent ae){
        ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
        if(formula == null)
        {
            MZmineCore.getDesktop().displayMessage(null,
                    "Select one result to copy");
            return;
        }

        String formulaString = formula.getFormulaAsString();
        StringSelection stringSelection = new StringSelection(formulaString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    @FXML
    private void showsMSClick(ActionEvent ae){
        ResultFormula formula = resultTable.getSelectionModel().getSelectedItem();
        if(formula == null){
            MZmineCore.getDesktop().displayMessage(null,
                    "Select one result to show MS score");
            return;
        }

        Feature bestPeak = peakListRow.getBestPeak();

        RawDataFile dataFile = bestPeak.getDataFile();
        int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();

        if (msmsScanNumber < 1)
            return;

        SpectraVisualizerWindow msmsPlot =
                SpectraVisualizerModule.showNewSpectrumWindow(dataFile, msmsScanNumber);

        if (msmsPlot == null)
            return;
        Map<DataPoint, String> annotation = formula.getMSMSannotation();

        if (annotation == null)
            return;
        msmsPlot.addAnnotation(annotation);
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
    public void dispose() {

        // Cancel the search task if it is still running
        TaskStatus searchStatus = searchTask.getStatus();
        if ((searchStatus == TaskStatus.WAITING) || (searchStatus == TaskStatus.PROCESSING)) {
            searchTask.cancel();
        }

        resultTable.getScene().getWindow().hide();

    }


}
