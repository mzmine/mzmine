/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import net.sf.mzmine.desktop.Desktop;

public class NewVersionCheck implements Runnable {
    public enum CheckType {DESKTOP, MENU};
    public CheckType info;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    public NewVersionCheck(CheckType type) {
	info = type;
    }
    
    public void run() {
	// Check for updated version
	String currentVersion = "", newestVersion = "", msg = "";
	currentVersion = MZmineCore.getMZmineVersion();
	if (info.equals(CheckType.MENU)) {
	    logger.info("Checking for updates...");
	}
	try {
	    URL url = new URL("http://mzmine.sourceforge.net/version.txt");
	    // Open the stream and put it into BufferedReader
	    BufferedReader buffer = new BufferedReader(new InputStreamReader(url.openStream()));

	    newestVersion = buffer.readLine();
	    buffer.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    newestVersion = "0";
	}

	if (newestVersion == "0") {
	    if (info.equals(CheckType.MENU)) {
		msg = "An error occured. Please make sure that you are connected to the internet or try again later.";
		logger.info(msg);
		MZmineCore.getDesktop().displayMessage(msg);
	    }
	}
	else if (currentVersion == newestVersion || currentVersion == "0.0") {
	    if (info.equals(CheckType.MENU)) {
		msg = "No updated version of MZmine is available.";
		logger.info(msg);
		MZmineCore.getDesktop().displayMessage(msg);
	    }
	}
	else {
	    msg = "An updated version is available: MZmine "+newestVersion;
	    logger.info(msg);
	    if (info.equals(CheckType.MENU)) {
		MZmineCore.getDesktop().displayMessage(msg +"\nPlease download the newest version from: http://mzmine.sourceforge.net.");
	    }
	    else if (info.equals(CheckType.DESKTOP)) {
		Desktop desktop = MZmineCore.getDesktop();
		desktop.setStatusBarText(msg +". Please download the newest version from: http://mzmine.sourceforge.net.", Color.red);
	    }
	}
    }
}
