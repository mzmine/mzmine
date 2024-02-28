/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.logging;

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

    StringBuilder output = new StringBuilder(512);
    Date eventTime = new Date(record.getMillis());

    output.append("[");
    output.append(format.format(eventTime));
    output.append('|');
    output.append(record.getLevel());
    output.append('|');
    output.append(record.getLoggerName());
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
