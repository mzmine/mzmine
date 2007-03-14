/*
 * Copyright 2007 The MZmine Development Team
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

package net.sf.mzmine.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * Simple ActionListener handler to change the application Look&Feel
 */
public class LookAndFeelChanger implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
	
	/**
	 * Changes the current application L&F to the L&F specified as the event action command.
	 * The action command must contant L&F class name.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {
		
		try {
			
			logger.info("Changing L&F to " + event.getActionCommand());
			
			UIManager.setLookAndFeel(event.getActionCommand());
			
			MainWindow.getInstance().repaint();
			
		} catch (Exception e) {
			logger.log(Level.WARNING, "L&F change failed", e);
		}

	}

}
