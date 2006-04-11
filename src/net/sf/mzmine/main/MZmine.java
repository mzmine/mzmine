/*
 * Copyright 2005 VTT Biotechnology
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

import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.util.Logger;

public class MZmine {

    private static MainWindow guiMain;

    /**
     * Main method
     */
    public static void main(String argz[]) {

        if (argz.length > 0) {
            if ((argz[0].compareToIgnoreCase(new String("quiet"))) == 0) {
                Logger.disableOutput();
            } else {
                Logger.setOutputOnScreen();
            }
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                guiMain = new MainWindow("MZmine", null);
                guiMain.setVisible(true);
            }
        });

    }

}