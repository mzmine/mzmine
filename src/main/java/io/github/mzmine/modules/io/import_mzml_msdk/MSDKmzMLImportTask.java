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

package io.github.mzmine.modules.io.import_mzml_msdk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.MzMLFileImportMethod;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLMsScan;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class MSDKmzMLImportTask extends AbstractTask {

  private final File file;
  private MzMLFileImportMethod msdkTask = null;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private int totalScans = 0, parsedScans;
  private String description;

  public MSDKmzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    this.file = fileToOpen;
    this.project = project;
    this.newMZmineFile = newMZmineFile;
    description = "Importing raw data file: " + fileToOpen.getName();
  }


  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {

      msdkTask = new MzMLFileImportMethod(file);
      msdkTask.execute();
      io.github.msdk.datamodel.RawDataFile file = msdkTask.getResult();

      if (file == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("MSDK returned null");
        return;
      }
      totalScans = file.getScans().size();
      if (newMZmineFile instanceof IMSRawDataFileImpl) {
        ((IMSRawDataFileImpl) newMZmineFile).addSegment(Range.closed(1, file.getScans().size()));
        buildIonMobilityFile(file);
      } else {
        buildLCMSFile(file);
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
    project.addFile(newMZmineFile);
  }

  @Override
  public void cancel() {
    if (msdkTask != null)
      msdkTask.cancel();
    super.cancel();
  }

  public void buildLCMSFile(io.github.msdk.datamodel.RawDataFile file) throws IOException {
    for (MsScan scan : file.getScans()) {
      newMZmineFile.addScan(ConversionUtils.msdkScanToSimpleScan(newMZmineFile, (MzMLMsScan) scan));
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
    int mobilityScanNumberCounter = 0;
    int frameNumber = 1;
    SimpleFrame buildingFrame = null;

    final List<MobilityScan> mobilityScans = new ArrayList<>();
    final List<Double> mobilities = new ArrayList<>();
    final List<BuildingImsMsMsInfo> buildingImsMsMsInfos = new ArrayList<>();
    Set<ImsMsMsInfo> finishedImsMsMsInfos = null;
    final IMSRawDataFile newImsFile = (IMSRawDataFile) newMZmineFile;

    for (MsScan scan : file.getScans()) {
      MzMLMsScan mzMLScan = (MzMLMsScan) scan;
      if (buildingFrame == null || Float.compare((scan.getRetentionTime() / 60f),
          buildingFrame.getRetentionTime()) != 0) {
        mobilityScanNumberCounter = 0;

        if (buildingFrame != null) { // finish the frame
          final SimpleFrame finishedFrame = buildingFrame;
          mobilityScans.forEach(s -> finishedFrame.addMobilityScan(s));
          finishedFrame
              .setMobilities(mobilities.stream().mapToDouble(Double::doubleValue).toArray());
          newImsFile.addScan(buildingFrame);
          mobilityScans.clear();
          mobilities.clear();
          if (!buildingImsMsMsInfos.isEmpty()) {
            finishedImsMsMsInfos = new HashSet<>();
            for (BuildingImsMsMsInfo info : buildingImsMsMsInfos) {
              finishedImsMsMsInfos.add(info.build());
            }
            finishedFrame.setPrecursorInfos(finishedImsMsMsInfos);
          }
          buildingImsMsMsInfos.clear();
        }

        buildingFrame = new SimpleFrame(newImsFile, frameNumber, scan.getMsLevel(),
            scan.getRetentionTime() / 60f, 0, 0, new double[] {}, new double[] {},
            ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()),
            ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()), scan.getScanDefinition(),
            scan.getScanningRange(), mzMLScan.getMobility().mt(), null);
        frameNumber++;

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

      mobilityScans.add(ConversionUtils.msdkScanToMobilityScan(newImsFile,
          mobilityScanNumberCounter, scan, buildingFrame));
      mobilities.add(mzMLScan.getMobility().mobility());
      ConversionUtils.extractImsMsMsInfo(mzMLScan, buildingImsMsMsInfos, frameNumber,
          mobilityScanNumberCounter);
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
    final double msdkProgress =
        msdkTask == null ? 0.0 : msdkTask.getFinishedPercentage().doubleValue();
    final double parsingProgress = totalScans == 0 ? 0.0 : (double) parsedScans / totalScans;
    return (msdkProgress * 0.25) + (parsingProgress * 0.75);
  }

}
