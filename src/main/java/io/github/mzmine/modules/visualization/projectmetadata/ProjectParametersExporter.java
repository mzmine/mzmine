/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import java.io.File;
import java.util.logging.Logger;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * This class exports project parameters and their values into a tsv-format file.
 */
public class ProjectParametersExporter {

  private static final Logger logger = Logger.getLogger(ProjectParametersExporter.class.getName());
  private final MZmineProject currentProject = MZmineCore.getProjectManager().getCurrentProject();
  private final MetadataTable metadataTable = currentProject.getProjectMetadata();
  private final Stage currentStage;

  public ProjectParametersExporter(Stage stage) {
    this.currentStage = stage;
  }

  public boolean exportParameters() {
    // Let user choose a file for importing
    File parameterFile = chooseFile();
    if (parameterFile == null) {
      logger.info("Parameter exporting cancelled.");
      return false;
    }

    // Read and interpret parameters
    return metadataTable.exportMetadata(parameterFile);
  }

  private File chooseFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Please select a file containing project parameter values for files.");
    fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"),
        new FileChooser.ExtensionFilter("text", "*.txt"),
        new FileChooser.ExtensionFilter("tsv", "*.tsv"));

    // setting the initial directory
    File currentFile = currentProject.getProjectFile();
    if (currentFile != null) {
      File currentDir = currentFile.getParentFile();
      if ((currentDir != null) && (currentDir.exists())) {
        fileChooser.setInitialDirectory(currentDir);
      }
    }

    return fileChooser.showSaveDialog(currentStage.getScene().getWindow());
  }
}
