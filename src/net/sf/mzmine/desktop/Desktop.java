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

package net.sf.mzmine.desktop;

import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineModule;

/**
 * This interface represents the application GUI
 * 
 */
public interface Desktop extends MZmineModule {

    /**
     * Returns a reference to main application window
     * 
     * @return Main window frame
     */
    public JFrame getMainFrame();

    /**
     * Creates a new menu item in the main application menu.
     * 
     * @param parentMenu MZmineMenu where to create the new item
     * @param text Item text
     * @param toolTip Item's tooltip
     * @param mnemonic Item's keyboard shortcut
     * @param listener ActionListener to receive the new menu item's events
     * @param actionCommand Action command for action listener or null
     * @return Newly created JMenuItem
     */
    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            String toolTip, int mnemonic, ActionListener listener, String actionCommand);


    /**
     * Adds a new internal frame (JInternalFrame) to the desktop pane
     * 
     * @param frame Internal frame to add
     */
    public void addInternalFrame(JInternalFrame frame);

    /**
     * Returns all internal frames in the desktop pane
     * 
     * @return Array of all internal frames
     */
    public JInternalFrame[] getInternalFrames();

    /**
     * Returns the currently selected frame or null if no frame is selected
     * 
     * @return Selected frame
     */
    public JInternalFrame getSelectedFrame();

    /**
     * Displays a given text on the application status bar in black color
     * 
     * @param text Text to show
     */
    public void setStatusBarText(String text);

    /**
     * Displays a given text on the application status bar in a given color
     * 
     * @param text Text to show
     * @param textColor Text color
     */
    public void setStatusBarText(String text, Color textColor);

    /**
     * Displays a message box with a given text
     * 
     * @param msg Text to show
     */
    public void displayMessage(String msg);

    /**
     * Displays an error message box with a given text
     * 
     * @param msg Text to show
     */
    public void displayErrorMessage(String msg);

    /**
     * Returns array of currently selected raw data files in GUI
     * 
     * @return Array of selected raw data files
     */
    public RawDataFile[] getSelectedDataFiles();

    /**
     * Returns array of currently selected peak lists in GUI
     * 
     * @return Array of selected peak lists
     */
    public PeakList[] getSelectedPeakLists();

}
