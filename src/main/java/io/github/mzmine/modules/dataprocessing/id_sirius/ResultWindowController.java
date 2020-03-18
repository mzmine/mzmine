package io.github.mzmine.modules.dataprocessing.id_sirius;

import java.net.URL;
import java.util.ResourceBundle;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.ResultTable;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.SiriusCompound;
import io.github.mzmine.taskcontrol.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import io.github.mzmine.datamodel.PeakListRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;


public class ResultWindowController implements Initializable {

    private final ObservableList<SiriusCompound> compounds = FXCollections.observableArrayList();
    private PeakListRow peakListRow;
    private Task searchTask;

    @FXML
    private TableView<SiriusCompound>compoundsTable;
    @FXML
    private TableColumn<SiriusCompound, String>nameCol;
    @FXML
    private TableColumn<SiriusCompound, Double>formulaCol;
    @FXML
    private TableColumn<SiriusCompound, Double>dbsCol;
    @FXML
    private TableColumn<SiriusCompound, Double>siriusScoreCol;
    @FXML
    private TableColumn<SiriusCompound, String>fingerldScoreCol;
    @FXML
    private TableColumn<SiriusCompound, String>chemicalStructureCol;

@Override
public void initialize(URL url, ResourceBundle rb) {
// initialize
}

@FXML
private void addIdentityOnClick(ActionEvent ae){
// TODO: handle Button event
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
// TODO: handle Button event
}

@FXML
private void copyFormulaOnClick(ActionEvent ae){
// TODO: handle Button event
}

@FXML
private void copySmilesOnClick(ActionEvent ae){
// TODO: handle Button event
}
}
