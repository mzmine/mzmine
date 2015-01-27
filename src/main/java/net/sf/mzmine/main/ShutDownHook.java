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

package net.sf.mzmine.main;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.impl.WrappedTask;

/**
 * Shutdown hook - invoked on JRE shutdown. This method saves current
 * configuration to XML and closes (and removes) all opened temporary files.
 */
class ShutDownHook extends Thread {

    public void start() {

	// Cancel all running tasks - this is important because tasks can spawn
	// additional processes (such as ThermoRawDump.exe on Windows) and these
	// will block the shutdown of the JVM. If we cancel the tasks, the
	// processes will be killed immediately.
	for (WrappedTask wt : MZmineCore.getTaskController().getTaskQueue()
		.getQueueSnapshot()) {
	    Task t = wt.getActualTask();
	    if ((t.getStatus() == TaskStatus.WAITING)
		    || (t.getStatus() == TaskStatus.PROCESSING)) {
		t.cancel();
	    }
	}

	// Save configuration
	try {
	    MZmineCore.getConfiguration().saveConfiguration(
		    MZmineConfiguration.CONFIG_FILE);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// Close all temporary files
	RawDataFile dataFiles[] = MZmineCore.getProjectManager()
		.getCurrentProject().getDataFiles();
	for (RawDataFile dataFile : dataFiles) {
	    dataFile.close();
	}

    }
}