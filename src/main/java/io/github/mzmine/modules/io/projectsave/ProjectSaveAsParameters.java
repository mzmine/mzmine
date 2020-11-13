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

package io.github.mzmine.modules.io.projectsave;

import java.io.File;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.ExitCode;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class ProjectSaveAsParameters extends SimpleParameterSet {

  public static final FileNameParameter projectFile = new FileNameParameter("Project file",
      "File name of project to be saved", FileSelectionType.SAVE);

  public ProjectSaveAsParameters() {
    super(new Parameter[] {projectFile});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    final File currentProjectFile =
        MZmineCore.getProjectManager().getCurrentProject().getProjectFile();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open MZmine project");
    fileChooser.getExtensionFilters().addAll(new ExtensionFilter("MZmine projects", "*.mzmine"),
        new ExtensionFilter("All Files", "*.*"));

    if (currentProjectFile != null) {
      File currentDir = currentProjectFile.getParentFile();
      if ((currentDir != null) && (currentDir.exists()))
        fileChooser.setInitialDirectory(currentDir);
    }

    try {
      File selectedFile = fileChooser.showSaveDialog(null);
      if (selectedFile == null)
        return ExitCode.CANCEL;
      if (!selectedFile.getName().endsWith(".mzmine")) {
        selectedFile = new File(selectedFile.getPath() + ".mzmine");
      }
      if (selectedFile.exists()) {
        Alert alert = new Alert(AlertType.CONFIRMATION,
            selectedFile.getName() + " already exists, overwrite ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() != ButtonType.YES) {
          return ExitCode.CANCEL;
        }
      }
      getParameter(projectFile).setValue(selectedFile);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return ExitCode.OK;

  }
}
