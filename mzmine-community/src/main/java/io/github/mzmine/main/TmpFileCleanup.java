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

package io.github.mzmine.main;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.io.import_rawdata_thermo_raw.ThermoRawImportTask;
import io.github.mzmine.modules.io.projectload.version_3_0.FeatureListLoadTask;
import io.github.mzmine.modules.io.projectload.version_3_0.RawDataFileOpenHandler_3_0;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.MemoryMapStorages;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryNotEmptyException;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

public class TmpFileCleanup implements Runnable {

  private static Unsafe theUnsafe;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public void run() {

//    closeProject();

    logger.fine("Checking for old temporary files...");
    try {
      // Find all temporary files with the mask mzmine*.scans
      File[] tempDir = {FileAndPathUtil.getTempDir(),
          new File(System.getProperty("java.io.tmpdir"))};
      File[] remainingTmpFiles = Arrays.stream(tempDir).map(f -> f.listFiles((dir, name) -> {
        if (name.matches("mzmine.*\\.tmp") || name.matches(
            STR."(.)*\{RawDataFileOpenHandler_3_0.TEMP_RAW_DATA_FOLDER}(.)*") || name.matches(
            STR."(.)*\{FeatureListLoadTask.TEMP_FLIST_DATA_FOLDER}(.)*") || name.matches(
            STR."(.)*\{ThermoRawImportTask.THERMO_RAW_PARSER_DIR}(.)*")) {
          return true;
        }
        return false;
      })).filter(Objects::nonNull).flatMap(Arrays::stream).toArray(File[]::new);

      if (remainingTmpFiles != null) {
        for (File remainingTmpFile : remainingTmpFiles) {

          // Skip files created by someone else
          if (!remainingTmpFile.canWrite()) {
            continue;
          }

          if (remainingTmpFile.isDirectory()) {
            // delete directory we used to store raw files on project import.
            try {
              FileUtils.deleteDirectory(remainingTmpFile);
            } catch (DirectoryNotEmptyException e) {
              logger.info(
                  () -> STR."Unable to delete directory \{remainingTmpFile}, it might be used by another mzmine instance.");
            }
            continue;
          }

          // Try to obtain a lock on the file
          RandomAccessFile rac = new RandomAccessFile(remainingTmpFile, "rw");

          FileLock lock = null;
          try {
            lock = rac.getChannel().tryLock();
          } catch (OverlappingFileLockException e) {
            logger.finest("The lock for a temporary file " + remainingTmpFile.getAbsolutePath()
                + " can not be acquired");
          }
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
    ProjectManager projectManager = ProjectService.getProjectManager();
    if (projectManager == null) {
      return;
    }

    MZmineProject project = projectManager.getCurrentProject();
    if (project == null) {
      return;
    }

    if (theUnsafe == null) {
      theUnsafe = initUnsafe();
      if (theUnsafe == null) {
        return;
      }
    }

    /*
    for (final MemoryMapStorage storage : MemoryMapStorages.getStorageList()) {
      try {
        storage.discard(theUnsafe);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }*/

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
