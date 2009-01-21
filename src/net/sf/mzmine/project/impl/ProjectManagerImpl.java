/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;

import net.sf.mzmine.desktop.impl.DesktopParameters;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.MZmineProject;

import com.sun.java.ExampleFileFilter;

/**
 * project manager implementation Using reflection to support different
 * implementation of actual tasks
 */
public class ProjectManagerImpl implements ActionListener {

    private static ProjectManagerImpl myInstance;

    MZmineProject currentProject;

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
     */
    public void initModule() {
        currentProject = new MZmineProjectImpl();
        myInstance = this;
    }

    public void actionPerformed(ActionEvent event) {

        String cmd = event.getActionCommand();

        if (cmd.equals("OPEN_PROJECT")) {

        }

        if (cmd.equals("SAVE_PROJECT")) {

        }

        if (cmd.equals("SAVE_PROJECT_AS")) {
            saveProjectAs();
        }
    }

    public MZmineProject getCurrentProject() {
        return currentProject;
    }

    void setCurrentProject(MZmineProject project) {
        this.currentProject = project;
    }

    public static ProjectManagerImpl getInstance() {
        return myInstance;
    }

    public void openProject() {
        DesktopParameters parameters = (DesktopParameters) MZmineCore.getDesktop().getParameterSet();
        String lastPath = parameters.getLastOpenProjectPath();
        JFileChooser chooser = new JFileChooser();
        if (lastPath != null)
            chooser.setCurrentDirectory(new File(lastPath));

        ExampleFileFilter filter = new ExampleFileFilter();
        filter.addExtension("mzmine");
        filter.setDescription("MZmine 2 projects");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(MZmineCore.getDesktop().getMainFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            ProjectOpeningTask task = new ProjectOpeningTask(selectedFile);
            MZmineCore.getTaskController().addTask(task);
            DesktopParameters newDesktopParams = (DesktopParameters) MZmineCore.getDesktop().getParameterSet();
            newDesktopParams.setLastOpenProjectPath(selectedFile.getParent());
        }
    }

    public void saveProject() {
        File projectFile = MZmineCore.getCurrentProject().getProjectFile();
        if (projectFile == null) {
            saveProjectAs();
            return;
        }
        ProjectSavingTask task = new ProjectSavingTask(projectFile);
        MZmineCore.getTaskController().addTask(task);
    }

    public void saveProjectAs() {
        DesktopParameters parameters = (DesktopParameters) MZmineCore.getDesktop().getParameterSet();
        String lastPath = parameters.getLastOpenProjectPath();
        JFileChooser chooser = new JFileChooser();
        if (lastPath != null)
            chooser.setCurrentDirectory(new File(lastPath));

        ExampleFileFilter filter = new ExampleFileFilter();
        filter.addExtension("mzmine");
        filter.setDescription("MZmine 2 projects");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showSaveDialog(MZmineCore.getDesktop().getMainFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            parameters.setLastOpenProjectPath(selectedFile.getParent());
            String extension = filter.getExtension(selectedFile);
            if ((extension == null) || (!extension.equals("mzmine"))) {
                selectedFile = new File(selectedFile.getPath() + ".mzmine");
            }
            ProjectSavingTask task = new ProjectSavingTask(selectedFile);
            MZmineCore.getTaskController().addTask(task);
        }
    }

}
