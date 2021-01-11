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

package io.github.mzmine.modules.io.rawdataimport.fileformats.mzmlnew;

import io.github.msdk.MSDKException;
import io.github.msdk.io.mzml.MzMLFileImportMethod;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.io.File;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class MSDKmzMLReadTask extends AbstractTask {

  private static final Pattern SCAN_PATTERN = Pattern.compile("scan=([0-9]+)");
  /*
   * This stack stores at most 20 consecutive scans. This window serves to find possible fragments
   * (current scan) that belongs to any of the stored scans in the stack. The reason of the size
   * follows the concept of neighborhood of scans and all his fragments. These solution is
   * implemented because exists the possibility to find fragments of one scan after one or more full
   * scans.
   */
  private static final int PARENT_STACK_SIZE = 20;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private IMSRawDataFile newImsFile;
  private int totalScans = 0, parsedScans;
  private int lastScanNumber = 0;
  private Map<String, Integer> scanIdTable = new Hashtable<String, Integer>();
  private LinkedList<io.github.mzmine.datamodel.Scan> parentStack = new LinkedList<>();

  private SimpleFrame buildingFrame;
  private Map<Integer, Double> buildingMobilities;

  public MSDKmzMLReadTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    this.file = fileToOpen;
    this.project = project;
    this.newMZmineFile = newMZmineFile;
  }


  @Override
  public String getTaskDescription() {
    return null;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started parsing file " + file);

    try {

      MzMLFileImportMethod importMethod = new MzMLFileImportMethod(file);

      Task task = new AbstractTask() {
        @Override
        public String getTaskDescription() {
          return "Import";
        }

        @Override
        public double getFinishedPercentage() {
          return importMethod.getFinishedPercentage().doubleValue();
        }

        @Override
        public void run() {
          try {
            importMethod.execute();
          } catch (MSDKException e) {
            e.printStackTrace();
          }
        }
      };
      task.run();

    } catch (Throwable e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      e.printStackTrace();
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);

  }
}
