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

import java.util.Vector;

import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectEvent;
import net.sf.mzmine.project.ProjectListener;
import net.sf.mzmine.project.ProjectManager;
import net.sf.mzmine.project.ProjectEvent.ProjectEventType;

/**
 * Project manager implementation 
 */
public class ProjectManagerImpl implements ProjectManager {

	private static ProjectManagerImpl myInstance;

	private Vector<ProjectListener> listeners;

	MZmineProject currentProject;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {
		listeners = new Vector<ProjectListener>();
		currentProject = new MZmineProjectImpl();
		myInstance = this;
	}

	public MZmineProject getCurrentProject() {
		return currentProject;
	}

	public void setCurrentProject(MZmineProject project) {
		this.currentProject = project;
		fireProjectListeners(new ProjectEvent(ProjectEventType.ALL_CHANGED));
	}

	public static ProjectManagerImpl getInstance() {
		return myInstance;
	}

	public void addProjectListener(ProjectListener listener) {
		listeners.add(listener);
	}

	public void removeProjectListener(ProjectListener listener) {
		listeners.remove(listener);
	}

	public synchronized void fireProjectListeners(ProjectEvent event) {
		for (ProjectListener listener : listeners)
			listener.projectModified(event);
	}

}
