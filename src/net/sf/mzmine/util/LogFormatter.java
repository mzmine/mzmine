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

package net.sf.mzmine.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
 
public class LogFormatter extends Formatter {
    
    private static final DateFormat format = new SimpleDateFormat("H:mm:ss");
    private static final String lineSep = System.getProperty("line.separator");
    
    /**
     * A Custom format implementation that is designed for brevity.
     */
    public String format(LogRecord record) {
        String l[] = record.getLoggerName().split("\\.");
        
        String loggerName = l[l.length - 1];
        
        if(loggerName == null) {
            loggerName = "root";
        }
        StringBuilder output = new StringBuilder()
            .append("[")
          //  .append(Thread.currentThread().getName()).append('|')
            .append(format.format(new Date(record.getMillis())))
            .append('|')
            .append(record.getLevel()).append('|')
                        .append(loggerName)
            .append("]: ")
            .append(record.getMessage()); //.append(' ')
     
        if (record.getThrown() != null) output.append("(" + record.getThrown().toString() + ")");
        output.append(lineSep);
        return output.toString();       
    }
 
}