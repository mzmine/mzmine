/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.logging;

import java.awt.Color;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;

/**
 * java.util.logging handler that displays last logged message on the status bar
 */
public class StatusBarHandler extends Handler {

    static final DateFormat timeFormat = DateFormat.getTimeInstance();

    static final int infoLevel = Level.INFO.intValue();

    /**
     * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
     */
    public void publish(LogRecord record) {

	// if the event level is below INFO, ignore it
	if (record.getLevel().intValue() < infoLevel)
	    return;

	// get Desktop instance
	Desktop desktop = MZmineCore.getDesktop();
	if (desktop == null)
	    return;
	if (desktop.getMainWindow() == null)
	    return;

	Date recordTime = new Date(record.getMillis());

	// format the message
	String formattedMessage = "[" + timeFormat.format(recordTime) + "]: "
		+ record.getMessage();

	// default color is black
	Color messageColor = Color.black;

	// display severe errors in red
	if (record.getLevel().equals(Level.SEVERE))
	    messageColor = Color.red;

	// set status bar text
	desktop.setStatusBarText(formattedMessage, messageColor);

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
