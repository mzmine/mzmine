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

package io.github.mzmine.modules.visualization.rawdataoverview;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/*
 * Raw data overview task class
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class RawDataOverviewTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private RawDataFile rawDataFiles[];

  private int totalSteps = 0;
  private int appliedSteps = 0;

  public RawDataOverviewTask(ParameterSet parameters) {

    this.rawDataFiles = parameters.getParameter(RawDataOverviewParameters.rawDataFiles).getValue()
        .getMatchingRawDataFiles();

    this.totalSteps = rawDataFiles.length;
  }

  @Override
  public String getTaskDescription() {
    return "Create raw data overview for " + rawDataFiles.length + " raw data files";
  }

  @Override
  public double getFinishedPercentage() {
    return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled())
      return;
    for (RawDataFile rawDataFile : rawDataFiles) {
      logger.info("Create raw data overview for" + rawDataFile.getName());

      Platform.runLater(() -> {
        FXMLLoader loader = new FXMLLoader((getClass().getResource("RawDataOverviewWindow.fxml")));
        Stage stage = new Stage();
        try {
          BorderPane root = (BorderPane) loader.load();
          Scene scene = new Scene(root, 1200, 800);

          // get controller
          RawDataOverviewWindowController controller = loader.getController();
          controller.initialize();
          controller.setRawDataFiles(Arrays.asList(rawDataFile));

          // Use main CSS
          scene.getStylesheets()
              .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
          stage.setScene(scene);
          logger.finest("Stage has been successfully loaded from the FXML loader.");
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        stage.setTitle("Raw data overview");
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
      });

      appliedSteps++;
    }
  }
}
