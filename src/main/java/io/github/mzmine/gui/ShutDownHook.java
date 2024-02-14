/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shutdown hook - invoked on JRE shutdown. This method saves current configuration to XML and
 * closes (and removes) all opened temporary files.
 */
class ShutDownHook extends Thread {

  private static final Logger logger = Logger.getLogger(ShutDownHook.class.getName());

  @Override
  public void run() {

    // Cancel all running tasks - this is important because tasks can spawn
    // additional processes (such as ThermoRawDump.exe on Windows) and these
    // will block the shutdown of the JVM. If we cancel the tasks, the
    // processes will be killed immediately.
    try {
      MZmineCore.getTaskController().close();

    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not stop all tasks on shutdown", e);
    }

    // Save configuration
    try {
      MZmineCore.getConfiguration().saveConfiguration(MZmineConfiguration.CONFIG_FILE);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not save config on shutdown", e);
    }

    // Close all temporary files
    RawDataFile[] dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    for (RawDataFile dataFile : dataFiles) {
      dataFile.close();
    }

  }
}
