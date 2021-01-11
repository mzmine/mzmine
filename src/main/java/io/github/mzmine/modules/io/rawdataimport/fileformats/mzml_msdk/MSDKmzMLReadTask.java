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

package io.github.mzmine.modules.io.rawdataimport.fileformats.mzml_msdk;

import com.google.common.collect.Range;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.rawdataimport.fileformats.mzml_msdk.msdk.data.MzMLMsScan;
import io.github.mzmine.modules.io.rawdataimport.fileformats.mzml_msdk.msdk.data.MzMLRawDataFile;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private IMSRawDataFile newImsFile;
  private int totalScans = 0, parsedScans;
  private int lastScanNumber = 0;
  private final File file;
  private String description;
  private SimpleFrame buildingFrame;
  private Map<Integer, Double> buildingMobilities;
  private final MSDKIndexingTask msdkTask;

  public MSDKmzMLReadTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    this.file = fileToOpen;
    this.project = project;
    if(newMZmineFile instanceof IMSRawDataFile) {
      this.newImsFile = (IMSRawDataFile) newMZmineFile;
    } else {
      this.newMZmineFile = newMZmineFile;
    }
    msdkTask = new MSDKIndexingTask(file);

    description = "Waiting for MSDK indexing of raw data file: " + fileToOpen.getName();
  }


  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    MZmineCore.getTaskController().addTask(msdkTask);
    setStatus(TaskStatus.WAITING);
    try {
      while (msdkTask.getStatus() != TaskStatus.FINISHED) {
        TimeUnit.MILLISECONDS.sleep(100);
        if (getStatus() == TaskStatus.CANCELED) {
          msdkTask.cancel();
        }
      }

      MzMLRawDataFile file = (MzMLRawDataFile) msdkTask.getImportMethod().getResult();
      if (file != null) {
        totalScans = file.getScans().size();
        if (newImsFile == null) {
          buildLCMSFile(file);
        }
        if(newImsFile != null) {
          ((IMSRawDataFileImpl)newImsFile).addSegment(Range.closed(1, file.getScans().size()));
          buildIonMobilityFile(file);
        }
      }

    } catch (Throwable e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);
    if(newImsFile != null) {
      project.addFile(newImsFile);
    } else {
      project.addFile(newMZmineFile);
    }
  }

  public void buildLCMSFile(io.github.msdk.datamodel.RawDataFile file) throws IOException {
    for (MsScan scan : file.getScans()) {
      newMZmineFile.addScan(ConversionUtils.msdkScanToSimpleScan(newMZmineFile, scan));
      parsedScans++;
      StringBuilder sb = new StringBuilder();
      sb.append("Importing ");
      sb.append(file.getName());
      sb.append(". Parsed ");
      sb.append(parsedScans);
      sb.append("/");
      sb.append(totalScans);
      sb.append(" scans");
      description = sb.toString();
    }
  }

  public void buildIonMobilityFile(io.github.msdk.datamodel.RawDataFile file) throws IOException {
    int mobilityScanNumberCounter = 1;
    int frameNumber = 1;
    SimpleFrame buildingFrame = null;
    for (MsScan scan : file.getScans()) {
      MzMLMsScan mzMLScan = (MzMLMsScan) scan;
      if (buildingFrame == null
          || Float.compare(scan.getRetentionTime(), buildingFrame.getRetentionTime()) != 0) {
        mobilityScanNumberCounter = 1;
        buildingMobilities = new HashMap<>();
        buildingFrame = new SimpleFrame(newImsFile, frameNumber, scan.getMsLevel(),
            scan.getRetentionTime(),
            0, 0, scan.getMzValues(),
            ConversionUtils.convertFloatsToDoubles(scan.getIntensityValues()),
            ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()),
            ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()),
            scan.getScanDefinition(), scan.getScanningRange(), mzMLScan.getMobility().mt(), 0,
            buildingMobilities, null);
        newImsFile.addScan(buildingFrame);
        frameNumber++;
      }
      buildingFrame.addMobilityScan(ConversionUtils.msdkScanToMobilityScan(newImsFile, scan, buildingFrame));
      buildingMobilities.put(mobilityScanNumberCounter, mzMLScan.getMobility().mobility());
      mobilityScanNumberCounter++;
      parsedScans++;
    }
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

}
