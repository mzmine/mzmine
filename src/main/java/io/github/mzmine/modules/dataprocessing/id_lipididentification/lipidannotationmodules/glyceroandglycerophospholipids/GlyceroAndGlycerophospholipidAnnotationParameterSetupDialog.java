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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.glyceroandglycerophospholipids;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidClassDescription;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidDatabaseCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Parameter setup dialog for lipid annotation module
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class GlyceroAndGlycerophospholipidAnnotationParameterSetupDialog extends
    ParameterSetupDialog {

  private Object[] selectedObjects;
  private ObservableList<LipidClassDescription> tableData = null;

  private static final Logger logger = Logger.getLogger(
      GlyceroAndGlycerophospholipidAnnotationParameterSetupDialog.class.getName());

  public GlyceroAndGlycerophospholipidAnnotationParameterSetupDialog(boolean valueCheckRequired,
      ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    // Add buttons
    Button showDatabaseTable = new Button("Show database");
    showDatabaseTable.setTooltip(
        new Tooltip("Show a database table for the selected classes and parameters"));
    showDatabaseTable.setOnAction(event -> {
      try {
        updateParameterSetFromComponents();

        // commit the changes to the parameter set
        selectedObjects = GlyceroAndGlycerophospholipidAnnotationParameters.lipidClasses.getValue();

        // Convert Objects to LipidClasses
        LipidClasses[] selectedLipids = Arrays.stream(selectedObjects)
            .filter(o -> o instanceof LipidClasses).map(o -> (LipidClasses) o)
            .toArray(LipidClasses[]::new);
        Task task = new AbstractTask(null, Instant.now()) {
          final double totalSteps = 100;
          double finishedSteps = 0;
          String taskDescription = "Open lipid database";

          @Override
          public String getTaskDescription() {
            return taskDescription;
          }

          @Override
          public double getFinishedPercentage() {
            if (totalSteps == 0) {
              return 0;
            }
            return (finishedSteps) / totalSteps;
          }

          @Override
          public void run() {
            setStatus(TaskStatus.PROCESSING);
            LipidDatabaseCalculator lipidDatabaseCalculator = new LipidDatabaseCalculator(
                parameters, selectedLipids);
            lipidDatabaseCalculator.createTableData();
            taskDescription = "Check interfering lipids";
            finishedSteps = 50;
            lipidDatabaseCalculator.checkInterferences();
            finishedSteps = 100;
            tableData = lipidDatabaseCalculator.getTableData();
            setStatus(TaskStatus.FINISHED);
            MZmineCore.runLater(() -> {
              FXMLLoader loader = new FXMLLoader(
                  (getClass().getResource("GlyceroAndGlycerophospholipidDatabaseTable.fxml")));
              Stage stage = new Stage();
              try {
                BorderPane root = loader.load();
                Scene scene = new Scene(root, 1200, 800);

                // get controller
                GlyceroAndGlycerophospholipidDatabaseTableController controller = loader.getController();
                controller.initialize(tableData, lipidDatabaseCalculator.getMzTolerance());

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
          }
        };
        MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

      } catch (Exception t) {
        logger.log(Level.WARNING, "Cannot show database table", t);
      }
    });
    getButtonBar().getButtons().add(showDatabaseTable);
  }

}
