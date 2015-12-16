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

import java.awt.Color;
import java.net.URL;
import java.util.logging.Logger;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.util.InetUtils;

public class NewVersionCheck implements Runnable {

    private static final String newestVersionAddress = "http://mzmine.github.io/version.txt";

    public enum CheckType {
	DESKTOP, MENU
    };

    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final CheckType checkType;

    public NewVersionCheck(CheckType type) {
	checkType = type;
    }

    public void run() {

	// Check for updated version
	String currentVersion, newestVersion;
	currentVersion = MZmineCore.getMZmineVersion();

	if (checkType.equals(CheckType.MENU)) {
	    logger.info("Checking for updates...");
	}

	final Desktop desktop = MZmineCore.getDesktop();

	try {
	    final URL newestVersionURL = new URL(newestVersionAddress);
	    newestVersion = InetUtils.retrieveData(newestVersionURL);
	    newestVersion = newestVersion.trim();
	} catch (Exception e) {
	    if (checkType.equals(CheckType.MENU)) { e.printStackTrace(); }
	    newestVersion = null;
	}

	if (newestVersion == null) {
	    if (checkType.equals(CheckType.MENU)) {
		final String msg = "An error occured. Please make sure that you are connected to the internet or try again later.";
		logger.info(msg);
		desktop.displayMessage(MZmineCore.getDesktop().getMainWindow(),
			msg);
	    }
	} else if (currentVersion.equals(newestVersion)
		|| currentVersion.equals("0.0")) {
	    if (checkType.equals(CheckType.MENU)) {
		final String msg = "No updated version of MZmine is available.";
		logger.info(msg);
		desktop.displayMessage(MZmineCore.getDesktop().getMainWindow(),
			msg);
	    }
	} else {
	    final String msg = "An updated version is available: MZmine "
		    + newestVersion;
	    final String msg2 = "Please download the newest version from: http://mzmine.github.io";
	    logger.info(msg);
	    if (checkType.equals(CheckType.MENU)) {
		desktop.displayMessage(MZmineCore.getDesktop().getMainWindow(),
			msg + "\n" + msg2);
	    } else if (checkType.equals(CheckType.DESKTOP)) {
		desktop.setStatusBarText(msg + ". " + msg2, Color.red);
	    }
	}
    }
}
