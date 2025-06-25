/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClassDescription;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidDatabaseCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

/**
 * Parameter setup dialog for lipid annotation module
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidAnnotationParameterSetupDialog extends ParameterSetupDialog {

  private Object[] selectedObjects;
  private ILipidClass[] selectedCustomLipidClasses;
  private ObservableList<LipidClassDescription> tableData = FXCollections.observableArrayList();

  private static final Logger logger = Logger.getLogger(
      LipidAnnotationParameterSetupDialog.class.getName());

  public LipidAnnotationParameterSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);

    // Add buttons
    Button showDatabaseTable = new Button("Show database");
    showDatabaseTable.setTooltip(
        new Tooltip("Show a database table for the selected classes and parameters"));
    showDatabaseTable.setOnAction(event -> {
      showDatabaseTable.setDisable(true);
      try {
        updateParameterSetFromComponents();
        LipidDatabaseTableController controller = null;
        tableData.clear();
        selectedObjects = parameters.getValue(LipidAnnotationParameters.lipidClasses);
        this.selectedCustomLipidClasses = null;
        if (parameters.getValue(LipidAnnotationParameters.customLipidClasses)) {
          this.selectedCustomLipidClasses = parameters.getEmbeddedParameterValue(LipidAnnotationParameters.customLipidClasses)
              .getValue(CustomLipidClassParameters.customLipidClassChoices);
        }

        Stream<ILipidClass> selectedObjectsStream = Arrays.stream(selectedObjects)
            .filter(o -> o instanceof LipidClasses).map(o -> (LipidClasses) o);

        Stream<ILipidClass> selectedCustomLipidClassesStream =
            selectedCustomLipidClasses != null ? Arrays.stream(selectedCustomLipidClasses)
                : Stream.empty();

        ILipidClass[] selectedLipids = Stream.concat(selectedObjectsStream,
            selectedCustomLipidClassesStream).toArray(ILipidClass[]::new);

        LipidDatabaseCalculator lipidDatabaseCalculator = new LipidDatabaseCalculator(parameters,
            selectedLipids);
        FXMLLoader loader = new FXMLLoader((getClass().getResource("LipidDatabaseTable.fxml")));
        try {
          BorderPane root = loader.load();
          LipidDatabaseTab tab = new LipidDatabaseTab("Lipid database");
          // get controller
          controller = loader.getController();
          controller.initialize(tableData, lipidDatabaseCalculator.getMzTolerance());
          controller.getLipidDatabaseTableView()
              .setPlaceholder(new Label("Calculating lipid database, please wait."));
          tab.setContent(root);
          MZmineCore.getDesktop().addTab(tab);
        } catch (IOException e) {
          e.printStackTrace();
        }
        LipidAnnotationDatabaseCalculatorTask task = new LipidAnnotationDatabaseCalculatorTask(
            tableData, controller, lipidDatabaseCalculator, showDatabaseTable);
        MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

      } catch (Exception t) {
        logger.log(Level.WARNING, "Cannot show database table", t);
      }
    });
    getButtonBar().getButtons().add(showDatabaseTable);
  }

}
