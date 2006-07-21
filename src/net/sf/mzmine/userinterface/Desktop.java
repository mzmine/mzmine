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

package net.sf.mzmine.userinterface;

import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.io.OpenedRawDataFile;

/**
 * This interface represents the application GUI
 * 
 */
public interface Desktop {

    public enum MZmineMenu {
        FILTERING, PEAKPICKING, ALIGNMENT, NORMALIZATION, VISUALIZATION
    };

    /**
     * Returns a reference to main application window
     * @return Main window frame
     */
    public JFrame getMainFrame();

    public JMenuItem addMenuItem(MZmineMenu parentMenu, String text,
            ActionListener listener, String actionCommand, int mnemonic,
            boolean setAccelerator, boolean enabled);

    /**
     * Adds a separator to a given MZmine menu
     * @param parentMenu Menu where to add a separator
     */
    public void addMenuSeparator(MZmineMenu parentMenu);
    
    public void addSelectionListener(ListSelectionListener listener);

    public void addInternalFrame(JInternalFrame frame);

    public JInternalFrame[] getVisibleFrames(Class frameClass);
    public JInternalFrame[] getVisibleFrames();
    public JInternalFrame getSelectedFrame();

    public void setStatusBarText(String text);
    
    public void displayErrorMessage(String msg);

    public boolean isDataFileSelected();

    public OpenedRawDataFile[] getSelectedDataFiles();

    public OpenedRawDataFile getFirstSelectedDataFile();

    public void exitMZmine();

}
