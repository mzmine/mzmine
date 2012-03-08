/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import net.sf.mzmine.data.RawDataFile;

/**
 * Shutdown hook - invoked on JRE shutdown. This method saves current
 * configuration to XML and closes (and removes) all opened temporary files.
 */
class ShutDownHook extends Thread {

    public void start() {

	// Save configuration
	try {
	    MZmineCore.getConfiguration().saveConfiguration(
		    MZmineConfiguration.CONFIG_FILE);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// Close all temporary files
	RawDataFile dataFiles[] = MZmineCore.getCurrentProject().getDataFiles();
	for (RawDataFile dataFile : dataFiles) {
	    dataFile.close();
	}

    }
}