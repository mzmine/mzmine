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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl.helpsystem;

import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.ExceptionUtils;

public class HelpImpl {

    private static Logger logger = Logger.getLogger(MZmineCore.class.getName());

    private MZmineHelpSet hs;

    public HelpImpl() {

	MZmineHelpMap helpMap;
	JarFile jarFile;
	try {
	    String path = this.getClass().getProtectionDomain().getCodeSource()
		    .getLocation().getPath();
	    String decodedPath = URLDecoder.decode(path, "UTF-8");
	    helpMap = new MZmineHelpMap(decodedPath);
	    jarFile = new JarFile(decodedPath);
	} catch (Exception e) {
	    logger.warning("Could not load help files: "
		    + ExceptionUtils.exceptionToString(e));
	    return;
	}

	Enumeration<JarEntry> e = jarFile.entries();
	while (e.hasMoreElements()) {
	    JarEntry entry = e.nextElement();
	    String name = entry.getName();
	    if ((name.endsWith("htm")) || (name.endsWith("html"))) {
		helpMap.setTarget(name);
	    }
	}

	helpMap.setTargetImage("topic.png");

	hs = new MZmineHelpSet();
	hs.setLocalMap(helpMap);

	MZmineTOCView myTOC = new MZmineTOCView(hs, "TOC", "Table Of Contents",
		helpMap, jarFile);

	hs.setTitle("MZmine 2");
	hs.addTOCView(myTOC);

    }

    public MZmineHelpSet getHelpSet() {
	return hs;
    }

}
