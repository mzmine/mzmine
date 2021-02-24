/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.main;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import sun.misc.Unsafe;

public class TmpFileCleanup implements Runnable {

  private static Unsafe theUnsafe;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public void run() {

//    closeTabs();
    closeProject();

    logger.fine("Checking for old temporary files...");
    try {

      // Find all temporary files with the mask mzmine*.scans
      File tempDir = new File(System.getProperty("java.io.tmpdir"));
      File remainingTmpFiles[] = tempDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          if (name.matches("mzmine.*\\.tmp")) {
            return true;
          }
          return false;
        }
      });

      if (remainingTmpFiles != null) {
        for (File remainingTmpFile : remainingTmpFiles) {

          // Skip files created by someone else
          if (!remainingTmpFile.canWrite()) {
            continue;
          }

          // Try to obtain a lock on the file
          RandomAccessFile rac = new RandomAccessFile(remainingTmpFile, "rw");

          FileLock lock = rac.getChannel().tryLock();
          rac.close();

          if (lock != null) {
            // We locked the file, which means nobody is using it
            // anymore and it can be removed
            logger.finest(() -> "Removing unused temporary file " + remainingTmpFile);
            if (!remainingTmpFile.delete()) {
              logger.fine(() -> "Cannot delete temp file " + remainingTmpFile.getAbsolutePath());
            }
          } else {
            logger.fine(
                () -> "Cannot obtain lock on temp file " + remainingTmpFile.getAbsolutePath());
          }

        }
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error while checking for old temporary files", e);
    }

  }

  private void closeProject() {
    ProjectManager projectManager = MZmineCore.getProjectManager();
    if (projectManager == null) {
      return;
    }

    MZmineProject project = projectManager.getCurrentProject();
    if (project == null) {
      return;
    }

    if(theUnsafe == null) {
      theUnsafe = initUnsafe();
      if(theUnsafe == null) {
        return;
      }
    }

    project.getRawDataFiles().forEach(raw -> {
      try {
        MemoryMapStorage memoryMapStorage = raw.getMemoryMapStorage();
        memoryMapStorage.discard(theUnsafe);
      } catch (IOException e) {
        e.printStackTrace();
        logger.log(Level.SEVERE, "Cannot delete temp file for raw data file.", e);
      }
    });

    project.getFeatureLists().forEach(flist -> {
      try {
        MemoryMapStorage memoryMapStorage = ((ModularFeatureList) flist).getMemoryMapStorage();
        memoryMapStorage.discard(theUnsafe);
      } catch (IOException e) {
        e.printStackTrace();
        logger.log(Level.SEVERE, "Cannot delete temp file for feature list.", e);
      }
    });
  }

  /**
   * Taken from https://stackoverflow.com/a/48821002
   *
   * @return Instance {@link Unsafe} or null.
   * @author https://github.com/SteffenHeu
   */
  @Nullable
  private synchronized Unsafe initUnsafe() {
    try {
      Class unsafeClass = Class.forName("sun.misc.Unsafe");
//      unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
      Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
      clean.setAccessible(true);
      Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafeField.setAccessible(true);
      Object theUnsafe = theUnsafeField.get(null);

      return (Unsafe) theUnsafe;

    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
        NoSuchFieldException | ClassCastException e) {
      // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
      // but that method should be added if sun.misc.Unsafe is removed.
      e.printStackTrace();
    }
    return null;
  }
}
