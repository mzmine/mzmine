/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.projectload;

import java.awt.Window;
import java.io.File;
import java.util.concurrent.FutureTask;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class ProjectLoaderParameters extends SimpleParameterSet {

    private static final FileFilter filters[] = new FileFilter[] {
            new FileNameExtensionFilter("MZmine projects", "mzmine") };

    public static final FileNameParameter projectFile = new FileNameParameter(
            "Project file", "File name of project to be loaded");

    public ProjectLoaderParameters() {
        super(new Parameter[] { projectFile });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open MZmine project");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("MZmine projects", "*.mzmine"),
                new ExtensionFilter("All Files", "*.*"));

        File currentFile = getParameter(projectFile).getValue();
        if (currentFile != null) {
            File currentDir = currentFile.getParentFile();
            if ((currentDir != null) && (currentDir.exists()))
                fileChooser.setInitialDirectory(currentDir);
        }

        final FutureTask<File> task = new FutureTask<>(
                () -> fileChooser.showOpenDialog(null));
        Platform.runLater(task);

        try {
            File selectedFile = task.get();
            if (selectedFile == null)
                return ExitCode.CANCEL;
            getParameter(projectFile).setValue(selectedFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ExitCode.OK;

    }

}
