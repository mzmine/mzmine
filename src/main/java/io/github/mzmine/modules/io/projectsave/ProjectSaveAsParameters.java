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

package io.github.mzmine.modules.io.projectsave;

import java.awt.Window;
import java.io.File;
import java.util.concurrent.FutureTask;

import javax.swing.JOptionPane;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class ProjectSaveAsParameters extends SimpleParameterSet {

    public static final FileNameParameter projectFile = new FileNameParameter(
            "Project file", "File name of project to be saved");

    public ProjectSaveAsParameters() {
        super(new Parameter[] { projectFile });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        final File currentProjectFile = MZmineCore.getProjectManager()
                .getCurrentProject().getProjectFile();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open MZmine project");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("MZmine projects", "*.mzmine"),
                new ExtensionFilter("All Files", "*.*"));

        if (currentProjectFile != null) {
            File currentDir = currentProjectFile.getParentFile();
            if ((currentDir != null) && (currentDir.exists()))
                fileChooser.setInitialDirectory(currentDir);
        }

        final FutureTask<File> task = new FutureTask<>(
                () -> fileChooser.showSaveDialog(null));
        Platform.runLater(task);

        try {
            File selectedFile = task.get();
            if (selectedFile == null)
                return ExitCode.CANCEL;
            if (!selectedFile.getName().endsWith(".mzmine")) {
                selectedFile = new File(selectedFile.getPath() + ".mzmine");
            }
            if (selectedFile.exists()) {
                int selectedValue = JOptionPane.showConfirmDialog(
                        MZmineCore.getDesktop().getMainWindow(),
                        selectedFile.getName() + " already exists, overwrite ?",
                        "Question...", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (selectedValue != JOptionPane.YES_OPTION)
                    return ExitCode.CANCEL;
            }
            getParameter(projectFile).setValue(selectedFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ExitCode.OK;

    }
}
