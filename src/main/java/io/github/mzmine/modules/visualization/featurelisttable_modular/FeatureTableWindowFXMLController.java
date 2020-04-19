package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.DataType;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class FeatureTableWindowFXMLController {

  private static final Logger logger = Logger
      .getLogger(FeatureTableWindowFXMLController.class.getName());

  @FXML
  private BorderPane pnMain;

  @FXML
  private CheckMenuItem miShowXIC;

  @FXML
  private CheckMenuItem miShowSpectrum;

  @FXML
  private SplitPane pnBottomSplit;

  @FXML
  private MenuItem miParameters;

  @FXML
  private StackPane pnMainCenter;

  @FXML
  private ChoiceBox<Class<? extends DataType>> cmbFilter;

  @FXML
  private TextField txtSearch;

  @FXML
  private FeatureTableFX featureTable;


  @FXML
  void miParametersOnAction(ActionEvent event) {

  }

  @FXML
  void miShowXICOnAction(ActionEvent event) {

  }

  @FXML
  void miShowSpectrumOnAction(ActionEvent event) {

  }

  public void setFeatureList(ModularFeatureList featureList) {
    featureTable.addData(featureList);
    setupFilter();
  }

  private void setupFilter() {
    ModularFeatureList flist = featureTable.getFeatureList();
    if (flist == null) {
      logger.info("Cannot setup filters for feature list window. Feature list not loaded.");
    }

    for (DataType dataType : flist.getRowTypes().values()) {
      cmbFilter.getItems().add(dataType.getClass());
    }

    txtSearch.setOnKeyReleased(keyEvent -> {
      Class<? extends DataType> type = cmbFilter.getValue();
      if (type == null) {
        return;
      }

      featureTable.getFilteredRowItems().setPredicate(item -> {

        ModularFeatureListRow row = item.getValue();

        Object value = row.getValue(type);

//        DataType<?> dt = row.get(type); // das geht nicht
//        row.getTypeColumn(type); // das auch nicht

        String lowValue = value.toString().toLowerCase(); // dt.getFormattedString(value);
        String filter = txtSearch.getText().toLowerCase().trim();

        return lowValue.contains(filter);
      });

      featureTable.getRoot().getChildren().clear();
      featureTable.getRoot().getChildren().addAll(featureTable.getFilteredRowItems());
    });
  }
}
