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

package net.sf.mzmine.util.logging;

import java.awt.Color;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * java.util.logging handler that displays last logged message on the status bar
 */
public class StatusBarHandler extends Handler {

    private static final Formatter statusBarFormatter = new StatusBarFormatter();
    private static final int infoLevel = Level.INFO.intValue();
    
    /**
     * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
     */
    public void publish(LogRecord record) {

        // if the event level is below INFO, just return
        if (record.getLevel().intValue() < infoLevel) return;
        
        // get Desktop instance from MainWindow
        Desktop desktop = MainWindow.getInstance();
        if (desktop != null) {

            // format the message
            String formattedMessage = statusBarFormatter.format(record);

            // default color is black
            Color messageColor = Color.black;
            
            // display severe errors in red
            if (record.getLevel().equals(Level.SEVERE)) messageColor = Color.red;
            
            // display warnings in blue
            if (record.getLevel().equals(Level.WARNING)) messageColor = Color.blue;

            // set status bar text
            desktop.setStatusBarText(formattedMessage, messageColor);
        }

    }

    /**
     * @see java.util.logging.Handler#flush()
     */
    public void flush() {
        // do nothing
    }

    /**
     * @see java.util.logging.Handler#close()
     */
    public void close() throws SecurityException {
        // do nothing
    }

    /**
     * @see java.util.logging.Handler#getFormatter()
     */
    public Formatter getFormatter() {
        return statusBarFormatter;
    }
    
    /**
     * @see java.util.logging.Handler#isLoggable(java.util.logging.LogRecord)
     */
    public boolean isLoggable(LogRecord record) {
        return (record.getLevel().intValue() >= infoLevel); 
    }
        
    /**
     * @see java.util.logging.Handler#getLevel()
     */
    public Level getLevel() {
        return Level.INFO;
    }

}
