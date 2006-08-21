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

package net.sf.mzmine.main;

import javax.swing.JOptionPane;

/**
 * This main class is executed when the user simply double-clicks on MZmine JAR
 * file. Only displays error message.
 */
public class DummyMain {

    /**
     * @param args
     */
    public static void main(String[] args) {

        String msg = "Please run MZmine using a provided starting script (startMZmine.bat or startMZmine.sh)";

        System.out.println(msg);

        try {
            JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);

        } catch (Exception e) {
            // do nothing
        }

    }

}
