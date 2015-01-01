/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import java.io.File;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.projectmethods.projectload.ProjectLoadModule;
import net.sf.mzmine.modules.projectmethods.projectload.ProjectLoaderParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.ProjectManager;

/**
 * Project manager implementation
 */
public class ProjectManagerImpl implements ProjectManager {

    private static ProjectManagerImpl myInstance;

    MZmineProject currentProject;

    /**
     * @see net.sf.mzmine.modules.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {
	currentProject = new MZmineProjectImpl();
	myInstance = this;
    }

    public MZmineProject getCurrentProject() {
	return currentProject;
    }

    public void setCurrentProject(MZmineProject project) {

	if (project == currentProject)
	    return;

	// Close previous data files
	if (currentProject != null) {
	    RawDataFile prevDataFiles[] = currentProject.getDataFiles();
	    for (RawDataFile prevDataFile : prevDataFiles) {
		prevDataFile.close();
	    }
	}

	this.currentProject = project;

	// This is a hack to keep correct value of last opened directory (this
	// value was overwritten when configuration file was loaded from the new
	// project)
	if (project.getProjectFile() != null) {
	    File projectFile = project.getProjectFile();
	    ParameterSet loaderParams = MZmineCore.getConfiguration()
		    .getModuleParameters(ProjectLoadModule.class);
	    loaderParams.getParameter(ProjectLoaderParameters.projectFile)
		    .setValue(projectFile);
	}

	// Notify the GUI about project structure change
	((MZmineProjectImpl) project).activateProject();

    }

    public static ProjectManagerImpl getInstance() {
	return myInstance;
    }

}
