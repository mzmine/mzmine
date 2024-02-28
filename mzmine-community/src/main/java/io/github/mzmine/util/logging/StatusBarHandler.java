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

import io.github.mzmine.gui.Desktop;
import io.github.mzmine.main.MZmineCore;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javafx.scene.paint.Color;

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
    if (desktop == null) {
      return;
    }
    if (desktop.getMainWindow() == null) {
      return;
    }

    Date recordTime = new Date(record.getMillis());

    // format the message
    String formattedMessage = "[" + timeFormat.format(recordTime) + "]: " + record.getMessage();

    Color messageColor = (MZmineCore.getConfiguration().isDarkMode() ? Color.LIGHTGRAY
        : Color.BLACK);

    // display severe errors in red
    if (record.getLevel().equals(Level.SEVERE)) {
      messageColor = Color.RED;
    }

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
