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
