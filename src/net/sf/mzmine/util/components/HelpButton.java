/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.components;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.swing.JButton;

import net.sf.mzmine.desktop.helpsystem.MZmineHelpMap;
import net.sf.mzmine.desktop.helpsystem.MZmineHelpSet;
import net.sf.mzmine.desktop.helpsystem.MZmineTOCView;

/**
 * This class extends JButton class to implement Help system generated
 * programatically. This class permits to get the help system in a dialog modal
 * window, and assign the HelpSystem Action Listener to this button.
 * 
 * 
 */
public class HelpButton extends JButton {

	/**
	 * This constructor receives as parameter a help ID stored in HelpSet.
	 * 
	 * @param helpID
	 */
	public HelpButton(String helpID) {
		super("Help");
		try {
			// Construct help
			MZmineHelpMap helpMap = new MZmineHelpMap();

			File file = new File(System.getProperty("user.dir")
					+ File.separator + "dist" + File.separator + "MZmine.jar");
			JarFile jarFile = new JarFile(file);
			Enumeration<JarEntry> e = jarFile.entries();
			while (e.hasMoreElements()) {
				JarEntry entry = e.nextElement();
				String name = entry.getName();
				if (name.contains("htm")) {
					helpMap.setTarget(name);
				}
			}

			helpMap.setTargetImage("topic.png");

			MZmineHelpSet hs = new MZmineHelpSet();
			MZmineTOCView myTOC = new MZmineTOCView(hs, "TOC",
					"Table Of Contents", helpMap);

			hs.setLocalMap(helpMap);
			hs.addTOCView(myTOC);
			hs.setTitle("MZmine 2 - LC/MS Toolbox ");

			HelpBroker hb = hs.createHelpBroker();
			hs.setHomeID(helpID);

			this.addActionListener(new CSH.DisplayHelpFromSource(hb));

		} catch (Exception event) {
			event.printStackTrace();
		}
	}

}
