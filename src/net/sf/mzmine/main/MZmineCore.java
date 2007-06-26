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

package net.sf.mzmine.main;

import javax.swing.JOptionPane;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;

/**
 * This interface represents MZmine core modules - I/O, task controller and GUI.
 */
public abstract class MZmineCore {

    protected static IOController ioController;
    protected static TaskController taskController;
    protected static Desktop desktop;
    protected static MZmineProject currentProject;
    protected static MZmineModule[] initializedModules;

    /**
     * Returns a reference to local IO controller.
     * 
     * @return IO controller reference
     */
    public static IOController getIOController() {
        return ioController;
    }

    /**
     * Returns a reference to local task controller.
     * 
     * @return TaskController reference
     */
    public static TaskController getTaskController() {
        return taskController;
    }

    /**
     * Returns a reference to Desktop. May return null on MZmine nodes with no
     * GUI.
     * 
     * @return Desktop reference or null
     */
    public static Desktop getDesktop() {
        return desktop;
    }

    /**
     * 
     */
    public static MZmineProject getCurrentProject() {
        return currentProject;
    }

    /**
     * Returns an array of all initialized MZmine modules
     * 
     * @return Array of all initialized MZmine modules
     */
    public static MZmineModule[] getAllModules() {
        return initializedModules;
    }

    /**
     * Saves configuration and exits the application.
     * 
     */
    public static void exitMZmine() {

        // If we have GUI, ask if use really wants to quit
        if (desktop != null) {
            int selectedValue = JOptionPane.showInternalConfirmDialog(
                    desktop.getMainFrame().getContentPane(),
                    "Are you sure you want to exit MZmine?", "Exiting...",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (selectedValue != JOptionPane.YES_OPTION)
                return;

            desktop.getMainFrame().dispose();
        }

        System.exit(0);

    }

}
