package io.github.mzmine.modules.visualization.lipidannotationoverview;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.io.IOException;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LipidAnnotationOverviewWindow extends Stage {

  private LipidAnnotationOverviewPaneController controller;

  public LipidAnnotationOverviewWindow(List<ModularFeatureListRow> rows,
      List<ModularFeature> selectedFeatures, FeatureTableFX table) {
    setTitle("Lipid annotation overview" + table.getFeatureList().getName());

    try {
      // Load the window FXML
      FXMLLoader loader = new FXMLLoader(
          (getClass().getResource("lipidannotationoverviewpane.fxml")));
      BorderPane rootPane = loader.load();
      // Get controller
      controller = loader.getController();
      controller.initialize(rows, selectedFeatures, table);

      Scene mainScene = new Scene(rootPane);
      mainScene.getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      this.setScene(mainScene);
    } catch (IOException e) {
      Scene mainScene = new Scene(
          new BorderPane(new Label("Could not load pane, see log for more information")));
      mainScene.getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      this.setScene(mainScene);
    }
  }

  public LipidAnnotationOverviewPaneController getController() {
    return controller;
  }
}
