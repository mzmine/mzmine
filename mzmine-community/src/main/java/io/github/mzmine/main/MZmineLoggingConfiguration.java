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

package io.github.mzmine.main;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * MZmine logging support
 */
public final class MZmineLoggingConfiguration {

  private static final Logger logger = Logger.getLogger(MZmineLoggingConfiguration.class.getName());

  /**
   * Configures the logging properties according to the logging.properties file found in the jar
   * resources
   */
  public MZmineLoggingConfiguration() {

    try {
      ClassLoader cl = MZmineLoggingConfiguration.class.getClassLoader();
      try (InputStream loggingProperties = cl.getResourceAsStream("logging.properties")) {
        LogManager logMan = LogManager.getLogManager();
        logMan.readConfiguration(loggingProperties);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "error during logger setup: " + e.getMessage(), e);
      e.printStackTrace();
    }

  }

}
