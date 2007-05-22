/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.alignmentresult;

import java.awt.Color;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

import net.sf.mzmine.util.GUIUtils;

/**
 *
 */
class AlignmentResultTableVisualizerToolBar extends JToolBar {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
    static final Icon zoomToPeakIcon = new ImageIcon("icons/annotationsicon.png");
    static final Icon changeFormatIcon = new ImageIcon("icons/tableselectionicon.png");

    AlignmentResultTableVisualizerToolBar(AlignmentResultTableVisualizerWindow masterFrame) {
    	
        super(JToolBar.VERTICAL);
        
        logger.info("Initializing alignment result table visualizer toolbar");

        setFloatable(false);
        setMargin(new Insets(5, 5, 5, 5));
        setBackground(Color.white);

        GUIUtils.addButton(this, null, zoomToPeakIcon, masterFrame,
                "ZOOM_TO_PEAK", "Zoom visualizers to selected peak");
        
        addSeparator();
        
        GUIUtils.addButton(this, null, changeFormatIcon, masterFrame,
                "CHANGE_FORMAT", "Change table column format");
        

    }

}
