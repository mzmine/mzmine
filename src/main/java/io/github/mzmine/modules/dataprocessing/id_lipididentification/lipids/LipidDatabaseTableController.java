package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import java.text.NumberFormat;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.LipidSearchParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidIdentity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

public class LipidDatabaseTableController {

  @FXML
  private TableView<TableModel> lipidDatabaseTableView;

  @FXML
  private TableColumn<TableModel, String> idColumn;

  @FXML
  private TableColumn<TableModel, String> lipidCoreClassColumn;

  @FXML
  private TableColumn<TableModel, String> lipidMainClassColumn;

  @FXML
  private TableColumn<TableModel, String> lipidClassColumn;

  @FXML
  private TableColumn<TableModel, String> formulaColumn;

  @FXML
  private TableColumn<TableModel, String> abbreviationColumn;

  @FXML
  private TableColumn<TableModel, String> ionizationColumn;

  @FXML
  private TableColumn<TableModel, String> exactMassColumn;

  @FXML
  private TableColumn<TableModel, String> infoColumn;

  @FXML
  private TableColumn<TableModel, String> statusColumn;

  @FXML
  private TableColumn<TableModel, String> fragmentsPosColumn;

  @FXML
  private TableColumn<TableModel, String> fragmentsNegColumn;

  @FXML
  private BorderPane kendrickPlotPanelCH2;

  @FXML
  private BorderPane kendrickPlotPanelH;

  ObservableList<TableModel> tableData = FXCollections.observableArrayList();

  private int minChainLength;
  private int maxChainLength;
  private int minDoubleBonds;
  private int maxDoubleBonds;
  private IonizationType ionizationType;
  private boolean useModification;
  private LipidModification[] lipidModification;
  private MZTolerance mzTolerance;

  public void initialize(ParameterSet parameters, LipidClasses[] selectedLipids) {
    System.out.println(selectedLipids.length);
    this.minChainLength =
        parameters.getParameter(LipidSearchParameters.chainLength).getValue().lowerEndpoint();
    this.maxChainLength =
        parameters.getParameter(LipidSearchParameters.chainLength).getValue().upperEndpoint();
    this.minDoubleBonds =
        parameters.getParameter(LipidSearchParameters.doubleBonds).getValue().lowerEndpoint();
    this.maxDoubleBonds =
        parameters.getParameter(LipidSearchParameters.doubleBonds).getValue().upperEndpoint();
    this.ionizationType =
        parameters.getParameter(LipidSearchParameters.ionizationMethod).getValue();
    this.useModification =
        parameters.getParameter(LipidSearchParameters.searchForModifications).getValue();
    if (useModification) {
      this.lipidModification = parameters.getParameter(LipidSearchParameters.searchForModifications)
          .getEmbeddedParameters().getParameter(LipidSearchModificationsParamters.modification)
          .getValue();
    }
    this.mzTolerance = parameters.getParameter(LipidSearchParameters.mzTolerance).getValue();

    NumberFormat numberFormat = MZmineCore.getConfiguration().getMZFormat();
    int id = 1;

    for (int i = 0; i < selectedLipids.length; i++) {
      int numberOfAcylChains = selectedLipids[i].getNumberOfAcylChains();
      int numberOfAlkylChains = selectedLipids[i].getNumberofAlkyChains();
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {

          // If we have non-zero fatty acid, which is shorter
          // than minimal length, skip this lipid
          if (((chainLength > 0) && (chainLength < minChainLength))) {
            continue;
          }

          // If we have more double bonds than carbons, it
          // doesn't make sense, so let's skip such lipids
          if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
            continue;
          }
          // Prepare a lipid instance
          LipidIdentity lipidChain = new LipidIdentity(selectedLipids[i], chainLength,
              chainDoubleBonds, numberOfAcylChains, numberOfAlkylChains);

          tableData.add(new TableModel(String.valueOf(id), // id
              selectedLipids[i].getCoreClass().getName(), // core class
              selectedLipids[i].getMainClass().getName(), // main class
              selectedLipids[i].getName(), // lipid class
              lipidChain.getFormula(), // molecular formula
              selectedLipids[i].getAbbr() + " (" + chainLength + ":" + chainDoubleBonds + ")", // abbr
              ionizationType.toString(), // ionization type
              numberFormat.format(lipidChain.getMass() + ionizationType.getAddedMass()), // exact
                                                                                         // mass
              "", // info
              "", // status
              String.join(", ", selectedLipids[i].getMsmsFragmentsPositiveIonization()), // msms
                                                                                         // fragments
                                                                                         // postive
              String.join(", ", selectedLipids[i].getMsmsFragmentsNegativeIonization()))); // msms
                                                                                           // fragments
                                                                                           // negative
          id++;
          // if (lipidModification.length > 0) {
          // for (int j = 0; j < lipidModification.length; j++) {
          // model.addRow(new Object[] {id, // id
          // selectedLipids[i].getCoreClass().getName(), // core class
          // selectedLipids[i].getMainClass().getName(), // main class
          // selectedLipids[i].getName() + " " + lipidModification[j].toString(), // lipid
          // // class
          // lipidChain.getFormula() + lipidModification[j].getLipidModificatio(), // sum
          // // formula
          // selectedLipids[i].getAbbr() + " (" + chainLength + ":" + chainDoubleBonds + ")"// abbr
          // + lipidModification[j].getLipidModificatio(),
          // ionizationType, // ionization type
          // numberFormat.format(lipidChain.getMass() + ionizationType.getAddedMass() // exact
          // // mass
          // + lipidModification[j].getModificationMass()),
          // "", // info
          // "", // status
          // "", // msms fragments postive
          // ""}); // msms fragments negative
          // id++;
          // }
          // }
        }
      }
    }

    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
    lipidCoreClassColumn.setCellValueFactory(new PropertyValueFactory<>("lipidCoreClass"));
    lipidMainClassColumn.setCellValueFactory(new PropertyValueFactory<>("lipidMainClass"));
    lipidClassColumn.setCellValueFactory(new PropertyValueFactory<>("lipidClass"));
    formulaColumn.setCellValueFactory(new PropertyValueFactory<>("molecularFormula"));
    abbreviationColumn.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));
    abbreviationColumn.setCellValueFactory(new PropertyValueFactory<>("ionization"));
    exactMassColumn.setCellValueFactory(new PropertyValueFactory<>("exactMass"));
    infoColumn.setCellValueFactory(new PropertyValueFactory<>("info"));
    statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    fragmentsPosColumn.setCellValueFactory(new PropertyValueFactory<>("msmsFragmentsPos"));
    fragmentsNegColumn.setCellValueFactory(new PropertyValueFactory<>("msmsFragmentsNeg"));

    lipidDatabaseTableView.setItems(tableData);

  }

}
