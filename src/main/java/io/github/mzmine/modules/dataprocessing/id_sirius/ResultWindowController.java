package io.github.mzmine.modules.dataprocessing.id_sirius;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.awt.datatransfer.Clipboard;

import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.ResultTable;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.SiriusCompound;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.db.DBFrame;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import io.github.mzmine.datamodel.PeakListRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.openscience.cdk.interfaces.IAtomContainer;

import javax.annotation.Nonnull;
import javax.swing.*;


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
    private TableColumn<SiriusCompound, String[]>dbsCol;
    @FXML
    private TableColumn<SiriusCompound, String>siriusScoreCol;
    @FXML
    private TableColumn<SiriusCompound, String>fingerldScoreCol;
    @FXML
    private TableColumn<SiriusCompound, IAtomContainer>chemicalStructureCol;
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
            String name = cell.getValue().getName();
            String cellVal = "";
            if(cell.getValue().getName()!=null)
            {
                cellVal = name;
            }
            return new ReadOnlyObjectWrapper<>(cellVal);
        });
        dbsCol.setCellValueFactory(cell->{
            String dbs[] = cell.getValue().getDBS();
            return new ReadOnlyObjectWrapper<>(dbs);
        });
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
        chemicalStructureCol.setCellValueFactory(cell->{
            IAtomContainer atomContainer = cell.getValue().getContainer();
            if(cell.getValue().getContainer()!=null)
            {
                return new ReadOnlyObjectWrapper<>();
            }
            return new ReadOnlyObjectWrapper<>(atomContainer);
        });


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

}

@FXML
private void displayDBOnClick(ActionEvent ae){

    SiriusCompound compound = compoundsTable.getSelectionModel().getSelectedItem();

    if (compound == null) {
        MZmineCore.getDesktop().displayMessage(null, "Select one row to display the list DBs");
        return;
    }
   DBFrame dbFrame = new DBFrame(compound, null);
    dbFrame.setVisible(true);
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
        if (content == null) {
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
    public void addNewListItem(@Nonnull final SiriusCompound compound) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                compounds.add(compound);     // todo : add here
                // link to cell
                // then i will get
                // sizes from it
            }
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
