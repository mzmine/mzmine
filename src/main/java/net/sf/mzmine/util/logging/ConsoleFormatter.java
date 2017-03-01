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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Console log formatter
 */
public class ConsoleFormatter extends Formatter {

    private static final DateFormat format = new SimpleDateFormat("H:mm:ss");
    private static final String lineSep = System.getProperty("line.separator");

    public String format(LogRecord record) {

	String loggerNameElements[] = record.getLoggerName().split("\\.");
	String loggerName = loggerNameElements[loggerNameElements.length - 1];

	StringBuilder output = new StringBuilder(512);
	Date eventTime = new Date(record.getMillis());

	output.append("[");
	output.append(format.format(eventTime));
	output.append('|');
	output.append(record.getLevel());
	output.append('|');
	output.append(loggerName);
	output.append("]: ");
	output.append(record.getMessage());

	if (record.getThrown() != null) {
	    output.append("(");
	    output.append(record.getThrown().toString());

	    Object[] stackTrace = record.getThrown().getStackTrace();
	    if (stackTrace.length > 0) {
		output.append("@");
		output.append(stackTrace[0].toString());
	    }

	    output.append(")");
	}

	output.append(lineSep);

	return output.toString();
    }

}