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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
class AddFilePopupMenu extends JMenu implements MenuListener, ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Hashtable<JMenuItem, RawDataFile> menuItemFiles;
    private TICVisualizerWindow visualizer;

    AddFilePopupMenu(TICVisualizerWindow visualizer) {
	super("Add plot of file...");
	addMenuListener(this);
	this.visualizer = visualizer;
    }

    /**
     * @see javax.swing.event.MenuListener#menuSelected(javax.swing.event.MenuEvent)
     */
    public void menuSelected(MenuEvent event) {

	// remove all menu items
	removeAll();

	// get all project files
	RawDataFile[] openFiles = MZmineCore.getProjectManager()
		.getCurrentProject().getDataFiles();
	List<RawDataFile> visualizedFiles = Arrays.asList(visualizer
		.getRawDataFiles());

	menuItemFiles = new Hashtable<JMenuItem, RawDataFile>();
	for (RawDataFile file : openFiles) {

	    // if this file is already added, skip it
	    if (visualizedFiles.contains(file))
		continue;

	    // add a menu item for each file
	    JMenuItem newItem = new JMenuItem(file.getName());
	    newItem.addActionListener(this);
	    menuItemFiles.put(newItem, file);
	    add(newItem);
	}

    }

    /**
     * @see javax.swing.event.MenuListener#menuDeselected(javax.swing.event.MenuEvent)
     */
    public void menuDeselected(MenuEvent arg0) {
	// do nothing
    }

    /**
     * @see javax.swing.event.MenuListener#menuCanceled(javax.swing.event.MenuEvent)
     */
    public void menuCanceled(MenuEvent arg0) {
	// do nothing
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
	Object src = event.getSource();
	RawDataFile file = menuItemFiles.get(src);
	if (file != null)
	    visualizer.addRawDataFile(file);
    }
}
