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
import io.github.mzmine.datamodel.data.Feature;
import io.github.mzmine.datamodel.data.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.logging.Logger;


public class ResultWindowController {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private final NumberFormat massFormat = MZmineCore.getConfiguration().getMZFormat();
    private final DecimalFormat percentFormat = new DecimalFormat("##.##%");
    private final NumberFormat ppmFormat = new DecimalFormat("0.0");

    private final ObservableList<ResultFormula> formulas = FXCollections.observableArrayList();

    @FXML
    private TableView<ResultFormula>resultTable;
    @FXML
    private TableColumn<ResultFormula, String>Formula;
    @FXML
    private TableColumn<ResultFormula, Double>absoluteMassDifference;
    @FXML
    private TableColumn<ResultFormula, Double>massDifference;
    @FXML
    private TableColumn<ResultFormula, Double>RDBE;
    @FXML
    private TableColumn<ResultFormula, String>isotopePattern;
    @FXML
    private TableColumn<ResultFormula, String>msScore;

    @FXML
    private void initialize(){
        Formula.setCellValueFactory(cell-> {
            String formula = cell.getValue().getFormulaAsString();
            String cellVal = "";
            if(cell.getValue().getFormulaAsString()!=null)
            {
                cellVal = formula;
            }

            return new ReadOnlyObjectWrapper<>(cellVal);
        });

        RDBE.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getRDBE()));

        absoluteMassDifference.setCellValueFactory(cell-> {
            double exactMass = cell.getValue().getExactMass();
            double massDiff = searchedMass - exactMass;

            return new ReadOnlyObjectWrapper<>(Double.parseDouble(massFormat.format(massDiff)));
        });
        massDifference.setCellValueFactory(cell-> {
            double ExactMass = cell.getValue().getExactMass();
            double MassDiff = searchedMass - ExactMass;
            MassDiff = ( MassDiff / ExactMass ) * 1E6;

            return new ReadOnlyObjectWrapper<>(Double.parseDouble(ppmFormat.format(MassDiff)));
        });


        isotopePattern.setCellValueFactory(cell->{
            String isotopeScore = String.valueOf(cell.getValue().getIsotopeScore());
            String cellVal = "";
            if(cell.getValue().getIsotopeScore() != null)
            {
                cellVal = percentFormat.format(Double.parseDouble(isotopeScore));
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });

        msScore.setCellValueFactory(cell-> {
            String msScore = String.valueOf(cell.getValue().getMSMSScore());
            String cellVal = "";
            if(cell.getValue().getMSMSScore() !=null)
            {
                cellVal = percentFormat.format(Double.parseDouble(msScore));
            }
            return new ReadOnlyObjectWrapper<>(cellVal);

        });

        resultTable.setItems(formulas);
    }


    private FeatureListRow peakListRow;
    private  Task searchTask;
    private  String title;
    private double searchedMass;

    public void initValues(String title, FeatureListRow peakListRow, double searchedMass, int charge,
                        Task searchTask) {

        this.title = title;
        this.peakListRow = peakListRow;
        this.searchTask = searchTask;
        this.searchedMass = searchedMass;
    }

    @FXML
    private void addIdentityClick(ActionEvent ae){

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

        Feature peak = peakListRow.getBestFeature();

        RawDataFile dataFile = peak.getRawDataFile();
        int scanNumber = peak.getRepresentativeScanNumber();
        SpectraVisualizerModule.addNewSpectrumTab(dataFile, scanNumber, null,
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

        Feature bestPeak = peakListRow.getBestFeature();

        RawDataFile dataFile = bestPeak.getRawDataFile();
        int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();

        if (msmsScanNumber < 1)
            return;

        SpectraVisualizerTab msmsPlot =
                SpectraVisualizerModule.addNewSpectrumTab(dataFile, msmsScanNumber);

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
