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

package net.sf.mzmine.desktop.impl;

import java.awt.Font;
import java.util.Enumeration;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/**
 * This class has just a single static method which sets Swing properties for MZmine
 */
class SwingParameters {

	static void initSwingParameters() {

        // Get tooltip manager instance
        ToolTipManager tooltipManager = ToolTipManager.sharedInstance();
        
        // Set tooltip display after 100 ms 
        tooltipManager.setInitialDelay(100);
        
        // Never dismiss tooltips
        tooltipManager.setDismissDelay(Integer.MAX_VALUE);

        // Prepare default fonts
		Font defaultFont = new Font("SansSerif", Font.PLAIN, 13);
		Font smallFont = new Font("SansSerif", Font.PLAIN, 11);
		Font tinyFont = new Font("SansSerif", Font.PLAIN, 10);
        
        // Set default font
		Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof Font)
				UIManager.put(key, defaultFont);
		}
        
        // Set small font where necessary
		UIManager.put("List.font", smallFont);
		UIManager.put("Table.font", smallFont);
		UIManager.put("ToolTip.font", tinyFont);

	}


}
