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

package io.github.mzmine.modules.dataprocessing.id_sirius;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.awt.datatransfer.Clipboard;
import com.google.common.base.Strings;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.SiriusCompound;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.db.DBFrame;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import io.github.mzmine.datamodel.PeakListRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

public class ResultWindowController{

    private final ObservableList<SiriusCompound> compounds = FXCollections.observableArrayList();
    private PeakListRow peakListRow;
    private Task searchTask;

    @FXML
    private TableView<SiriusCompound>compoundsTable;
    @FXML
    private TableColumn<SiriusCompound, String>nameCol;
    @FXML
    private TableColumn<SiriusCompound, String>formulaCol;
    @FXML
    private TableColumn<SiriusCompound, Node>dbsCol;
    @FXML
    private TableColumn<SiriusCompound, String>siriusScoreCol;
    @FXML
    private TableColumn<SiriusCompound, String>fingerldScoreCol;
    @FXML
    private TableColumn<SiriusCompound, Structure2DComponent>chemicalStructureCol;
    @FXML
    private void initialize(){
        formulaCol.setCellValueFactory(cell-> {
            String formula = cell.getValue().getStringFormula();
            String cellVal = "";
            if(cell.getValue().getStringFormula()!=null)
            {
                cellVal = formula;
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });

        nameCol.setCellValueFactory(cell->{
            String name = cell.getValue().getAnnotationDescription();
            String cellVal = "";
            if(cell.getValue().getAnnotationDescription()!=null)
            {
                cellVal = name;
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });
        dbsCol.setCellValueFactory(c-> c.getValue().getDBSNode());

        siriusScoreCol.setCellValueFactory(cell->{
            String sirius = cell.getValue().getSiriusScore();
            String cellVal = "";
            if(cell.getValue().getSiriusScore()!=null)
            {
                cellVal = sirius;
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });
        fingerldScoreCol.setCellValueFactory(cell->{
            String sirius = cell.getValue().getFingerIdScore();
            String cellVal = "";
            if(cell.getValue().getSiriusScore()!=null)
            {
                cellVal = sirius;
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });

        chemicalStructureCol.setCellValueFactory(c-> {
            try {
                return c.getValue().getChemicalStructureNode();
            } catch (CDKException e) {
                e.printStackTrace();
            }
            return null;
        });

    chemicalStructureCol.setCellFactory(
        x -> new TableCell<>() {
            @Override
            protected void updateItem(Structure2DComponent item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText("");
                } else {
                    setGraphic(item);
                    Tooltip tooltip = new Tooltip();

                    this.setOnMouseEntered(e -> {

                        IAtomContainer container = item.getContainer();
                        try {
                            if (container == null) return;
                            Structure2DComponent newItem = new Structure2DComponent(container);
                            newItem.resize(500, 500);
                            tooltip.setGraphic(newItem);
                        } catch (CDKException ex) {
                            ex.printStackTrace();
                        }

                        tooltip.show(this, e.getScreenX() + 50, e.getScreenY());
                    });

                    this.setOnMouseExited(e -> {
                        tooltip.hide();
                    });

                }
            }
        });

     compoundsTable.setItems(compounds);
    }

    public void initValues(PeakListRow peakListRow, Task searchTask)
    {
        this.peakListRow = peakListRow;
        this.searchTask = searchTask;
    }



@FXML
private void addIdentityOnClick(ActionEvent ae){
    SiriusCompound compound = compoundsTable.getSelectionModel().getSelectedItem();
    if(compound == null)
    {
        MZmineCore.getDesktop().displayMessage(null,
                "Select one result to add as compound identity");
        return;
    }
    peakListRow.addPeakIdentity(compound, false);
    dispose();

}
@FXML
private void displayDBOnClick(ActionEvent ae){

    SiriusCompound compound = compoundsTable.getSelectionModel().getSelectedItem();

    if (compound == null) {
        MZmineCore.getDesktop().displayMessage(null, "Select one row to display the list DBs");
        return;
    }
  DBFrame dbFrame = new DBFrame(compound);
}

@FXML
private void copyFormulaOnClick(ActionEvent ae){
    SiriusCompound compound = compoundsTable.getSelectionModel().getSelectedItem();

    if  (compound == null) {
        MZmineCore.getDesktop().displayMessage(null, "Select one result to copy FORMULA value");
        return;
    }

    String formula = compound.getStringFormula();
    copyToClipboard(formula, "Formula value is null...");
}

@FXML
private void copySmilesOnClick(ActionEvent ae){
    SiriusCompound compound = compoundsTable.getSelectionModel().getSelectedItem();

    if (compound == null) {
        MZmineCore.getDesktop().displayMessage(null, "Select one result to copy SMILES value");
        return;
    }
    String smiles = compound.getSMILES();
    copyToClipboard(smiles, "Selected compound does not contain identified SMILES");
}

    /**
     * Method sets value of clipboard to `content`
     *
     * @param content - Formula or SMILES string
     * @param errorMessage - to print in a message if value of `content` is null
     */
    private void copyToClipboard(String content, String errorMessage) {
    if (Strings.isNullOrEmpty(content)) {
            MZmineCore.getDesktop().displayMessage(null, errorMessage);
            return;
        }

        StringSelection stringSelection = new StringSelection(content);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     * Update content of a table using swing-thread
     *
     * @param compound
     */
    public void addNewListItem( final SiriusCompound compound) {
        Platform.runLater(() -> {
            compounds.add(compound);     // todo : add here
            // link to cell
            // then i will get
            // sizes from it
        });
    }

    /**
     * Method adds a new SiriusCompound to a table
     *
     * @param annotations - SiriusIonAnnotation results processed by Sirius/FingerId methods
     */
    public void addListofItems(final List<IonAnnotation> annotations) {
        for (IonAnnotation ann : annotations) {
            SiriusIonAnnotation annotation = (SiriusIonAnnotation) ann;
            SiriusCompound compound = new SiriusCompound(annotation);
            addNewListItem(compound);
        }
    }

    /**
     * Releases the list of subtasks and disposes windows related to it.
     */
    public void dispose() {
        // Cancel the search task if it is still running
        TaskStatus searchStatus = searchTask.getStatus();
        if ((searchStatus == TaskStatus.WAITING) || (searchStatus == TaskStatus.PROCESSING)) {
            searchTask.cancel();
        }

        compoundsTable.getScene().getWindow().hide();
    }
}
