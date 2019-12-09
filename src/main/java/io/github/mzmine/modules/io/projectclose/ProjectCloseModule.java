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

package io.github.mzmine.modules.io.projectclose;

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.JOptionPane;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.GUIUtils;

/**
 * This is a very simple module which adds the option to close a current project
 * 
 */
public class ProjectCloseModule implements MZmineProcessingModule {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private static final String MODULE_NAME = "Close project";
    private static final String MODULE_DESCRIPTION = "Close project";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        int selectedValue = JOptionPane.showInternalConfirmDialog(
                MZmineCore.getDesktop().getMainWindow().getContentPane(),
                "Are you sure you want to close the current project?",
                "Close project", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (selectedValue != JOptionPane.YES_OPTION)
            return ExitCode.CANCEL;

        // Close all windows related to previous project
        GUIUtils.closeAllWindows();

        // Create a new, empty project
        MZmineProject newProject = new MZmineProjectImpl();

        // Replace the current project with the new one
        ProjectManager projectManager = MZmineCore.getProjectManager();
        projectManager.setCurrentProject(newProject);

        // Ask the garbage collector to free the previously used memory
        System.gc();

        logger.info("Project closed.");
        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PROJECTIO;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return SimpleParameterSet.class;
    }

}
