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

import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.util.NumberFormatProvider;

/**
 * This interface represents the application GUI
 *
 */
public interface Desktop {

    public enum MZmineMenu {
        FILTERING, PEAKPICKING, ALIGNMENT, NORMALIZATION, BATCH, VISUALIZATION
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

    /**
     * Adds a listener for raw data file and alignment result selection changes
     * @param listener
     */
    public void addSelectionListener(ListSelectionListener listener);

    /**
     * Used to notify all registered listeners of an "artificial change" in raw data file or alignment result selection
     * For example: menu option availability may depend on properties of selected raw data file.
     * When selection doesn't change but properties of selected raw data file change, 
     * data processing method may call this method to update available menu options.
     */
    public void notifySelectionListeners();
    
    /**
     * 
     */
    public void addInternalFrame(JInternalFrame frame);

    public JInternalFrame[] getVisibleFrames(Class frameClass);
    public JInternalFrame[] getVisibleFrames();
    public JInternalFrame getSelectedFrame();

    public void setStatusBarText(String text);
    public void setStatusBarText(String text, Color textColor);

    public void displayMessage(String msg);
    public void displayErrorMessage(String msg);

    public boolean isDataFileSelected();
    
    public boolean isAlignmentResultSelected();

    /**
     * Returns array of currently selected raw data files in GUI
     */
    public OpenedRawDataFile[] getSelectedDataFiles();
    
    /**
     * Returns array of currently selected alignment results in GUI 
     */
    public AlignmentResult[] getSelectedAlignmentResults();

    /**
     * Selects one raw data file in GUI
     */
    public void setSelectedDataFile(OpenedRawDataFile dataFile);
    
    /**
     * Selects one alignment result in GUI
     */
    public void setSelectedAlignmentResult(AlignmentResult alignmentResult);
    
    /**
     * Adds a new raw data file to GUI
     */
    public void addDataFile(OpenedRawDataFile dataFile);
    
    /**
     * Adds a new alignment result to GUI
     */
    public void addAlignmentResult(AlignmentResult alignmentResult);
    
    /**
     * Removes raw data file from GUI
     */
    public void removeDataFile(OpenedRawDataFile dataFile);
    
    /**
     * Removes alignment result from GUI
     */
    public void removeAlignmentResult(AlignmentResult alignmentResult);
    
    public NumberFormatProvider getMZFormatProvider();

    public NumberFormatProvider getRTFormatProvider();
    
    public NumberFormatProvider getIntensityFormatProvider();

}
