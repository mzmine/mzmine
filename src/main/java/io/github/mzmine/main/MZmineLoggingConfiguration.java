/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
      InputStream loggingProperties = cl.getResourceAsStream("logging.properties");
      LogManager logMan = LogManager.getLogManager();
      logMan.readConfiguration(loggingProperties);
      loggingProperties.close();
    } catch (Exception e) {
      logger.log(Level.WARNING, "error during logger setup: " + e.getMessage(), e);
      e.printStackTrace();
    }

  }

}
