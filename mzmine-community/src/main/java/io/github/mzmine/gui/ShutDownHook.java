/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.main.TmpFileCleanup;
import io.github.mzmine.modules.io.import_rawdata_wiff2.ClearcoreServer;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shutdown hook - invoked on JRE shutdown in GUI and headless (CLI) mode. Cancels all tasks, closes
 * (and removes) all opened temporary files and saves the current configuration to XML in GUI mode
 * only.
 */
public final class ShutDownHook extends Thread {

  private static final Logger logger = Logger.getLogger(ShutDownHook.class.getName());
  private static final AtomicBoolean registered = new AtomicBoolean(false);

  private ShutDownHook() {
    super("mzmine shutdown hook");
  }

  /**
   * Registers the mzmine shutdown hooks once. Call as early as possible during startup so that
   * every way of terminating mzmine runs them.
   */
  public static void register() {
    if (!registered.compareAndSet(false, true)) {
      return;
    }
    Runtime.getRuntime()
        .addShutdownHook(new Thread(new TmpFileCleanup(), "mzmine temp file cleanup"));
    Runtime.getRuntime().addShutdownHook(new ShutDownHook());
  }

  @Override
  public void run() {
    // decision: save the config first - it is fast and the most valuable step if the system kills
    // the process shortly after starting the shutdown
    if (MZmineCore.isGUI()) {
      try {
        // Save configuration only in GUI mode - headless batch runs must not change the config.
        // The desktop is headless until the GUI is launched, so early exits do not save either.
        if (!ConfigService.saveUserConfig()) {
          logger.log(Level.WARNING, "Could not save config on shutdown");
        }
      } catch (Throwable e) {
        logger.log(Level.WARNING, "Could not save user config on shutdown", e);
      }
    }

    try {
      ClearcoreServer.terminateSeverIfRunning();
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Could not stop clearcore server", e);
    }

    // Cancel all running tasks - this is important because tasks can spawn
    // additional processes (such as ThermoRawDump.exe on Windows) that would otherwise keep
    // running after the JVM exits. Waits only briefly for tasks to react to the cancellation.
    try {
      TaskService.getController().close();
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Could not stop all tasks on shutdown", e);
    }

    // Close all temporary files after tasks are canceled, tasks may still write to them
    RawDataFile[] dataFiles = ProjectService.getProjectManager().getCurrentProject().getDataFiles();
    for (RawDataFile dataFile : dataFiles) {
      try {
        dataFile.close();
      } catch (Throwable e) {
        logger.log(Level.WARNING, "Could not close data file: " + dataFile.getName(), e);
      }
    }

  }

}
