/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidDatabaseTableController;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Parameter setup dialog for lipid annotation module
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidSearchParameterSetupDialog extends ParameterSetupDialog {

  private final Button showDatabaseTable;
  private Object[] selectedObjects;
  private CheckBoxTreeItem<Object> items;

  private static final Logger logger = Logger.getLogger(
      LipidSearchParameterSetupDialog.class.getName());

  public LipidSearchParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    // Add buttons
    showDatabaseTable = new Button("Show database");
    showDatabaseTable.setTooltip(
        new Tooltip("Show a database table for the selected classes and parameters"));
    showDatabaseTable.setOnAction(event -> {
      try {
        updateParameterSetFromComponents();

        // commit the changes to the parameter set
        selectedObjects = LipidSearchParameters.lipidClasses.getValue();

        // Convert Objects to LipidClasses
        LipidClasses[] selectedLipids = Arrays.stream(selectedObjects)
            .filter(o -> o instanceof LipidClasses).map(o -> (LipidClasses) o)
            .toArray(LipidClasses[]::new);

        Platform.runLater(() -> {
          FXMLLoader loader = new FXMLLoader(
              (getClass().getResource("lipids/LipidDatabaseTable.fxml")));
          Stage stage = new Stage();
          try {
            BorderPane root = loader.load();
            Scene scene = new Scene(root, 1200, 800);

            // get controller
            LipidDatabaseTableController controller = loader.getController();
            controller.initialize(parameters, selectedLipids);

            // Use main CSS
            scene.getStylesheets()
                .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
            stage.setScene(scene);
            logger.finest("Stage has been successfully loaded from the FXML loader.");
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }
          stage.setTitle("Lipid database");
          stage.show();
          stage.setMinWidth(stage.getWidth());
          stage.setMinHeight(stage.getHeight());
        });

      } catch (Exception t) {
        logger.log(Level.WARNING, "Cannot show database table", t);
      }
    });

    getButtonBar().getButtons().add(showDatabaseTable);
  }

}
