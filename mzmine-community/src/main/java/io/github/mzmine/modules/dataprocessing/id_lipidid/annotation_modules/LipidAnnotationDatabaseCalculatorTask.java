/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClassDescription;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidDatabaseCalculator;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;

public class LipidAnnotationDatabaseCalculatorTask extends AbstractTask {


  private final double totalSteps = 100;
  private double finishedSteps = 0;
  private String taskDescription = "Open lipid database";
  private ObservableList<LipidClassDescription> tableData;
  private LipidDatabaseTableController controller;
  private LipidDatabaseCalculator lipidDatabaseCalculator;
  private Button showDatabaseTable;

  public LipidAnnotationDatabaseCalculatorTask(ObservableList<LipidClassDescription> tableData,
      LipidDatabaseTableController controller, LipidDatabaseCalculator lipidDatabaseCalculator,
      Button showDatabaseTable) {
    super(null, Instant.now());
    this.tableData = tableData;
    this.controller = controller;
    this.lipidDatabaseCalculator = lipidDatabaseCalculator;
    this.showDatabaseTable = showDatabaseTable;
  }

  @Override
  public String getTaskDescription() {
    return "Open lipid database";
  }

  @Override
  public double getFinishedPercentage() {
    return finishedSteps / totalSteps;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    tableData = lipidDatabaseCalculator.createTableData();
    taskDescription = "Check interfering lipids";
    finishedSteps = 50;
    lipidDatabaseCalculator.checkInterferences();
    finishedSteps = 100;
    setStatus(TaskStatus.FINISHED);
    Platform.runLater(() -> {
      assert controller != null;
      controller.initialize(tableData, lipidDatabaseCalculator.getMzTolerance());
      showDatabaseTable.setDisable(false);
    });
  }


}
