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

import com.google.common.collect.Range;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.MsdkScanWrapper;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_all_data_files.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_all_data_files.MassDetectionSubParameters;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.MzMLFileImportMethod;
import io.github.mzmine.modules.io.import_mzml_msdk.msdk.data.MzMLMsScan;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;

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

  // advanced processing will apply mass detection directly to the scans
  private final boolean applyMassDetection;
  private MZmineProcessingStep<MassDetector> ms1Detector = null;
  private MZmineProcessingStep<MassDetector> ms2Detector = null;

  public MSDKmzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    this(project, fileToOpen, newMZmineFile, null);
  }

  public MSDKmzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      AdvancedSpectraImportParameters advancedParam) {
    super(newMZmineFile.getMemoryMapStorage()); // storage in raw data file
    this.file = fileToOpen;
    this.project = project;
    this.newMZmineFile = newMZmineFile;
    description = "Importing raw data file: " + fileToOpen.getName();

    if (advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
      this.ms1Detector = advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection)
          .getEmbeddedParameter().getValue();
    }
    if (advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
      this.ms2Detector = advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection)
          .getEmbeddedParameter().getValue();
    }

    this.applyMassDetection = ms1Detector != null || ms2Detector != null;
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

  private double[][] applyMassDetection(MZmineProcessingStep<MassDetector> msDetector,
      MsdkScanWrapper scan) {
    // run mass detection on data object
    // [mzs, intensities]
    return msDetector.getModule().getMassValues(scan, msDetector.getParameterSet());
  }

  @Override
  public void cancel() {
    if (msdkTask != null) {
      msdkTask.cancel();
    }
    super.cancel();
  }

  public void buildLCMSFile(io.github.msdk.datamodel.RawDataFile file) throws IOException {
    for (MsScan scan : file.getScans()) {
      MzMLMsScan mzMLScan = (MzMLMsScan) scan;

      Scan newScan = null;

      if (applyMassDetection) {
        // wrap scan
        MsdkScanWrapper wrapper = new MsdkScanWrapper(scan);
        double[][] mzIntensities = null;

        // apply mass detection
        if (ms1Detector != null && wrapper.getMSLevel() == 1) {
          mzIntensities = applyMassDetection(ms1Detector, wrapper);
        } else if (ms2Detector != null && wrapper.getMSLevel() == 2) {
          mzIntensities = applyMassDetection(ms2Detector, wrapper);
        }

        if(mzIntensities != null) {
          // create mass list and scan. Override data points and spectrum type
          newScan = ConversionUtils.msdkScanToSimpleScan(newMZmineFile, mzMLScan, mzIntensities[0],
              mzIntensities[1], MassSpectrumType.CENTROIDED);
          ScanPointerMassList newMassList = new ScanPointerMassList(newScan);
          newScan.addMassList(newMassList);
        }
      }

      if (newScan == null) {
        newScan = ConversionUtils.msdkScanToSimpleScan(newMZmineFile, mzMLScan);
      }

      newMZmineFile.addScan(newScan);
      parsedScans++;
      description =
          "Importing " + file.getName() + ", parsed " + parsedScans + "/" + totalScans + " scans";
    }
  }

  public void buildIonMobilityFile(io.github.msdk.datamodel.RawDataFile file) throws IOException {
    int mobilityScanNumberCounter = 0;
    int frameNumber = 1;
    SimpleFrame buildingFrame = null;

    final List<BuildingMobilityScan> mobilityScans = new ArrayList<>();
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
          finishedFrame.setMobilityScans(mobilityScans);
          finishedFrame
              .setMobilities(mobilities.stream().mapToDouble(Double::doubleValue).toArray());
          newImsFile.addScan(buildingFrame);
          mobilityScans.clear();
          mobilities.clear();
          if (!buildingImsMsMsInfos.isEmpty()) {
            finishedImsMsMsInfos = new HashSet<>();
            for (BuildingImsMsMsInfo info : buildingImsMsMsInfos) {
              finishedImsMsMsInfos.add(info.build(null, buildingFrame));
            }
            finishedFrame.setPrecursorInfos(finishedImsMsMsInfos);
          }
          buildingImsMsMsInfos.clear();
        }

        buildingFrame = new SimpleFrame(newImsFile, frameNumber, scan.getMsLevel(),
            scan.getRetentionTime() / 60f, 0, 0, new double[]{}, new double[]{},
            ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()),
            ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()), scan.getScanDefinition(),
            scan.getScanningRange(), mzMLScan.getMobility().mt(), null);
        frameNumber++;

        description =
            "Importing " + file.getName() + ", parsed " + parsedScans + "/" + totalScans + " scans";
      }

      mobilityScans.add(ConversionUtils.msdkScanToMobilityScan(mobilityScanNumberCounter, scan));
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
