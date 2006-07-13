/*
 * Copyright 2006 The MZmine Development Team
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

/**
 * 
 */
package net.sf.mzmine.userinterface.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.visualizers.rawdata.MultipleRawDataVisualizer;


/**
 *
 */
public class RemoveFilePopupMenu extends JMenu implements MenuListener, ActionListener {

    private Hashtable<JMenuItem,RawDataFile> menuItemFiles;
    MultipleRawDataVisualizer visualizer;
    
    public RemoveFilePopupMenu(MultipleRawDataVisualizer visualizer) {
        super("Remove plot of file...");
        addMenuListener(this);
        this.visualizer = visualizer;
    }

    /**
     * @see javax.swing.event.MenuListener#menuSelected(javax.swing.event.MenuEvent)
     */
    public void menuSelected(MenuEvent event) {
        removeAll();
        RawDataFile[] files = visualizer.getRawDataFiles();
        
        // if we have only one file, we cannot remove it
        if (files.length == 1) return;
        
        menuItemFiles = new Hashtable<JMenuItem,RawDataFile>();
        for (RawDataFile file : files) {
            JMenuItem newItem = new JMenuItem(file.toString());
            newItem.addActionListener(this);
            menuItemFiles.put(newItem, file);
            add(newItem);
        }
        
    }

    /**
     * @see javax.swing.event.MenuListener#menuDeselected(javax.swing.event.MenuEvent)
     */
    public void menuDeselected(MenuEvent arg0) {
    }

    /**
     * @see javax.swing.event.MenuListener#menuCanceled(javax.swing.event.MenuEvent)
     */
    public void menuCanceled(MenuEvent arg0) {
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
         Object src = event.getSource();
         RawDataFile file = menuItemFiles.get(src);
         if (file != null) visualizer.removeRawDataFile(file);
    }
}
