/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingUtilities;

import net.sf.mzmine.modules.projectmethods.projectload.ProjectLoader;
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

	// We use WeakReference to keep the reference to windows which want to be
	// notified about project change. The reason is that we don't want to
	// prevent the garbage collector from collecting these windows.
	private Vector<WeakReference<ProjectListener>> listeners;

	MZmineProject currentProject;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {
		listeners = new Vector<WeakReference<ProjectListener>>();
		currentProject = new MZmineProjectImpl();
		myInstance = this;
	}

	public MZmineProject getCurrentProject() {
		return currentProject;
	}

	public void setCurrentProject(MZmineProject project) {
		this.currentProject = project;

		// Fire the project listeners in swing thread
		Runnable swingCode = new Runnable() {
			public void run() {
				fireProjectListeners(new ProjectEvent(
						ProjectEventType.ALL_CHANGED));
			}
		};
		SwingUtilities.invokeLater(swingCode);

		// This is a hack to keep correct value of last opened directory (this
		// value was overwritten when configuration file was loaded from the new
		// project)
		String lastPath = project.getProjectFile().getParentFile().getPath();
		ProjectLoader.setLastProjectOpenPath(lastPath);
	}

	public static ProjectManagerImpl getInstance() {
		return myInstance;
	}

	public void addProjectListener(ProjectListener listener) {
		synchronized (listeners) {
			WeakReference<ProjectListener> newReference = new WeakReference<ProjectListener>(
					listener);
			listeners.add(newReference);
		}
	}

	public void removeProjectListener(ProjectListener listener) {
		synchronized (listeners) {
			Iterator<WeakReference<ProjectListener>> it = listeners.iterator();
			while (it.hasNext()) {
				WeakReference<ProjectListener> ref = it.next();
				ProjectListener refList = ref.get();
				if ((refList == null) || (refList == listener)) {
					it.remove();
					continue;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void fireProjectListeners(ProjectEvent event) {
		WeakReference<ProjectListener> listenersCopy[];
		synchronized (listeners) {
			listenersCopy = listeners.toArray(new WeakReference[0]);
		}
		// Now we released the lock of listeners, so we can safely call the
		// actual methods
		for (WeakReference<ProjectListener> ref : listenersCopy) {
			ProjectListener listener = ref.get();
			if (listener != null)
				listener.projectModified(event);
		}

	}

}
