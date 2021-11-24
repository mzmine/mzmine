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

package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser.ExtensionFilter;

public class ProjectSaveAsParameters extends SimpleParameterSet {

  public static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("MZmine project file", "*.mzmine"), //
      new ExtensionFilter("All files", "*.*") //
  );
  public static final ComboParameter<ProjectSaveOption> option = new ComboParameter<>(
      "Project type",
      "Referencing projects point to the original directory of raw data files (with those projects "
      + "files should not be moved or renamed). Standalone copies the raw data files into the project, "
      + "creating a large but flexible project that can be shared.", ProjectSaveOption.values(),
      ProjectSaveOption.REFERENCING);
  public static final FileNameParameter projectFile = new FileNameParameter("Project file",
      "File name of project to be saved", extensions, FileSelectionType.SAVE);
  private static final Logger logger = Logger.getLogger(ProjectSaveAsParameters.class.getName());

  public ProjectSaveAsParameters() {
    super(new Parameter[]{projectFile, option});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    final String message = """
        There are currently two project formats supported:</br>
        <b>Standalone</b>: Adds the raw data files into a project (large but flexible)</br>
        <b>Referencing</b>: The project will point to the current files used. Any rename, move, or 
        remove of a file from their current directory might lead to incompatibility of the project.</br>
        <b>WARNING:</b> If this is an existing project, it is recommended to save it in the same way.""";

    // set parameters to current project if already saved to file
    final MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    final File currentProjectFile = project.getProjectFile();

    if ((currentProjectFile != null) && (currentProjectFile.canWrite())) {
      final ProjectSaveOption projectType =
          Objects.requireNonNullElse(project.isStandalone(), true) ? ProjectSaveOption.STANDALONE
              : ProjectSaveOption.REFERENCING;
      setParameter(projectFile, currentProjectFile);
      setParameter(option, projectType);
    }

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    final ExitCode exitCode = dialog.getExitCode();

    if (!ExitCode.OK.equals(exitCode)) {
      return exitCode;
    }

    // check if file exists and alert
    try {
      File file = getValue(projectFile);
      if (file == null) {
        return ExitCode.CANCEL;
      }

      if (!file.getName().endsWith(".mzmine")) {
        file = FileAndPathUtil.getRealFilePath(file, ".mzmine");
      }

      if (file.exists()) {
        Alert alert = new Alert(AlertType.CONFIRMATION,
            file.getName() + " already exists, overwrite ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() != ButtonType.YES) {
          return ExitCode.CANCEL;
        }
      }
      setParameter(projectFile, file);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Problem with checking project file during saving", e);
    }
    return ExitCode.OK;
  }
}
