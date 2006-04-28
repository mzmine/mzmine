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

package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.awt.Component;
import java.awt.Cursor;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


/**
 *
 */
class BasePeakPopupMenu extends JPopupMenu {
    
    private JMenuItem zoomOutMenuItem;
    private JMenuItem showSpectrumMenuItem;
    private JMenuItem annotationsMenuItem;
    
    BasePeakPopupMenu(BasePeakVisualizer masterFrame) {
        
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        zoomOutMenuItem = new JMenuItem("Zoom out");
        zoomOutMenuItem.addActionListener(masterFrame);
        zoomOutMenuItem.setActionCommand("ZOOM_OUT");
        zoomOutMenuItem.setEnabled(false);
        add(zoomOutMenuItem);
        
        addSeparator();
        
        annotationsMenuItem = new JMenuItem("Hide peak values");
        annotationsMenuItem.addActionListener(masterFrame);
        annotationsMenuItem.setActionCommand("SHOW_ANNOTATIONS");
        add(annotationsMenuItem);
        
        showSpectrumMenuItem = new JMenuItem("Show spectrum");
        showSpectrumMenuItem.addActionListener(masterFrame);
        showSpectrumMenuItem.setActionCommand("SHOW_SPECTRUM");
        showSpectrumMenuItem.setEnabled(false);
        add(showSpectrumMenuItem);
       
    }
    
    void setZoomOutMenuItem(boolean enabled) {
        zoomOutMenuItem.setEnabled(enabled);
    }
    
    void setAnnotationsMenuItem(String text) {
        annotationsMenuItem.setText(text);
    }
    
}
