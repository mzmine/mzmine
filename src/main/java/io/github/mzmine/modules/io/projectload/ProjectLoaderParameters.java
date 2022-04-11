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

package io.github.mzmine.modules.io.projectload;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.util.List;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class ProjectLoaderParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("MZmine project file", "*.mzmine"), //
      new ExtensionFilter("All files", "*.*") //
  );

  public static final FileNameParameter projectFile = new FileNameParameter("Project file",
      "File name of project to be loaded", extensions, FileSelectionType.OPEN);

  public ProjectLoaderParameters() {
    super(new Parameter[]{projectFile});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open MZmine project");
    fileChooser.getExtensionFilters().addAll(extensions);

    File currentFile = getParameter(projectFile).getValue();
    if (currentFile != null) {
      File currentDir = currentFile.getParentFile();
      if ((currentDir != null) && (currentDir.exists()))
        fileChooser.setInitialDirectory(currentDir);
    }

    File selectedFile = fileChooser.showOpenDialog(null);
    if (selectedFile == null)
      return ExitCode.CANCEL;
    getParameter(projectFile).setValue(selectedFile);

    return ExitCode.OK;

  }

}
