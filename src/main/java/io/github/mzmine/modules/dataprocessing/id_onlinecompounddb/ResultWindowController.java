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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.data.Feature;
import io.github.mzmine.datamodel.data.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.molstructure.MolStructureViewer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.text.NumberFormat;
import java.util.logging.Logger;

public class ResultWindowController {


    private Logger logger = Logger.getLogger(this.getClass().getName());
    private final NumberFormat massFormat = MZmineCore.getConfiguration().getMZFormat();
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    private FeatureListRow peakListRow;
    private Task searchTask;
    private double searchedMass;


    private final ObservableList<DBCompound> compoundList = FXCollections.observableArrayList();

    @FXML
    private TableView<DBCompound> IDList;
    @FXML
    private TableColumn<DBCompound, String> colID;
    @FXML
    private TableColumn <DBCompound, String> colName ;
    @FXML
    private TableColumn <DBCompound, String> colFormula ;
    @FXML
    private TableColumn <DBCompound, String> colMassDiff ;
    @FXML
    private TableColumn <DBCompound, String> colIPS ;


    @FXML
    private void initialize(){
        colID.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getPropertyValue(PeakIdentity.PROPERTY_ID)));
        colName.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getName()));
        colFormula.setCellValueFactory(cell-> new ReadOnlyObjectWrapper<>(cell.getValue().getPropertyValue(PeakIdentity.PROPERTY_FORMULA)));
        colMassDiff.setCellValueFactory(cell-> {
            String compFormula = cell.getValue().getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
            String cellVar = "";
            if (compFormula != null) {
                double compMass = FormulaUtils.calculateExactMass(compFormula);
                double massDifference = Math.abs(searchedMass - compMass);
                cellVar =  massFormat.format(massDifference);
            }
            return new ReadOnlyObjectWrapper<>(cellVar);
        });
        colIPS.setCellValueFactory(cell->{

            Double score = cell.getValue().getIsotopePatternScore();
            String cellVar ="";
            if (score != null)
                cellVar = percentFormat.format(score);
            return new ReadOnlyObjectWrapper<>(cellVar);
        });
        IDList.setItems(compoundList);
    }

    /**
     * Initialize values for calculations used in buttons
     *
     * @param peakListRow
     * @param searchTask
     * @param searchedMass
     */
    public void initValues(FeatureListRow peakListRow, Task searchTask, double searchedMass){
        this.peakListRow = peakListRow;
        this.searchTask = searchTask;
        this.searchedMass = searchedMass;
    }

    /**
     * Add compound to the list for viewing in the Table
     *
     * @param compound
     */
    public void addNewListItem(final DBCompound compound) {
        assert Platform.isFxApplicationThread();
        compoundList.add(compound);
    }

    /**
     * Hide the TableView, cancel Task
     */
    public void dispose() {

        // Cancel the search task if it is still running
        TaskStatus searchStatus = searchTask.getStatus();
        if ((searchStatus == TaskStatus.WAITING) || (searchStatus == TaskStatus.PROCESSING)){
            searchTask.cancel();
        }
        IDList.getScene().getWindow().hide();

    }

    @FXML
    public void handleAddAction(ActionEvent actionEvent) {
        try {
            DBCompound compound = IDList.getSelectionModel().getSelectedItem();

            if (compound == null) {
                MZmineCore.getDesktop().displayMessage(null,
                        "Select one result to add as compound identity");
                return;

            }

            peakListRow.addPeakIdentity(compound, false);
            dispose();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @FXML
    public void handleViewStructureAction(ActionEvent actionEvent) {

        DBCompound compound = IDList.getSelectionModel().getSelectedItem();

        if (compound==null) {
            MZmineCore.getDesktop().displayMessage(null,
                    "Select one result to display molecule structure");
            return;
        }

        URL url2D = compound.get2DStructureURL();
        URL url3D = compound.get3DStructureURL();
        String name =
                compound.getName() + " (" + compound.getPropertyValue(PeakIdentity.PROPERTY_ID) + ")";
        MolStructureViewer viewer = new MolStructureViewer(name, url2D, url3D);
        viewer.show();
    }

    @FXML
    public void handleViewIPAction(ActionEvent actionEvent) {
        DBCompound compound = IDList.getSelectionModel().getSelectedItem();

        if (compound == null) {
            MZmineCore.getDesktop().displayMessage(null,
                    "Select one result to display the isotope pattern");
            return;
        }

        final IsotopePattern predictedPattern =compound.getIsotopePattern();

        if (predictedPattern == null)
            return;

        Feature peak = peakListRow.getBestFeature();

        RawDataFile dataFile = peak.getRawDataFile();
        int scanNumber = peak.getRepresentativeScanNumber();
        SpectraVisualizerModule.addNewSpectrumTab(dataFile, scanNumber, null,
                peak.getIsotopePattern(), predictedPattern);
    }

    @FXML
    public void handleOpenBrowserAction(ActionEvent actionEvent) {
        DBCompound compound = IDList.getSelectionModel().getSelectedItem();

        if (compound == null) {
            MZmineCore.getDesktop().displayMessage(null,
                    "Select one compound to display in a web browser");
            return;
        }

        logger.finest("Launching default browser to display compound details");

        String urlString = compound.getPropertyValue(PeakIdentity.PROPERTY_URL);

        if ((urlString == null) || (urlString.length() == 0))
            return;

        try{
            MZmineCore.getDesktop().openWebPage(new URL(urlString));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
