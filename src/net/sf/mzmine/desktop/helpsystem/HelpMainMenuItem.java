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

package net.sf.mzmine.desktop.helpsystem;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.help.CSH;
import javax.help.HelpBroker;

import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.desktop.impl.MainMenu;

public class HelpMainMenuItem {

	public void addMenuItem(MainMenu menu) {

		try {
			
			// Construct help
			MZmineHelpMap helpMap = new MZmineHelpMap();
			
			File file = new File(System.getProperty("user.dir") + File.separator + "dist" + File.separator
					+ "MZmine.jar");
			JarFile jarFile = new JarFile(file);
		    Enumeration<JarEntry> e = jarFile.entries();
		       while (e.hasMoreElements()) {
		           JarEntry entry = e.nextElement();
		           String name = entry.getName();
		           if ( name.contains("htm") ){
		        	   helpMap.setTarget(name);
		           }
		       }
		       
		    helpMap.setTargetImage("topic.png");
			
		    MZmineHelpSet hs = new MZmineHelpSet();
	        hs.setLocalMap(helpMap);

	        MZmineTOCView myTOC = new MZmineTOCView(hs, "TOC", "Table Of Contents", helpMap);
	        
			hs.setHomeID("net/sf/mzmine/desktop/helpsystem/AboutText.html");
			hs.setTitle("MZmine 2 - LC/MS Toolbox");
			hs.addTOCView(myTOC);
			
			HelpBroker hb = hs.createHelpBroker();
			ActionListener helpListener = new CSH.DisplayHelpFromSource(hb);

			menu.addMenuItem(MZmineMenu.HELPSYSTEM, "About MZmine 2 ...",
					"Help system contents", KeyEvent.VK_C, helpListener, null);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
