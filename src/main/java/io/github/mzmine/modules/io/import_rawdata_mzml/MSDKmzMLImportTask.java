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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import io.github.msdk.datamodel.MsScan;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.MsdkScanWrapper;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.MzMLFileImportMethod;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLMsScan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class MSDKmzMLImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MSDKmzMLImportTask.class.getName());
  private final File file;
  // advanced processing will apply mass detection directly to the scans
  private final boolean applyMassDetection;
  private MzMLFileImportMethod msdkTask = null;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private int totalScans = 0, parsedScans;
  private String description;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private MZmineProcessingStep<MassDetector> ms1Detector = null;
  private MZmineProcessingStep<MassDetector> ms2Detector = null;

  public MSDKmzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    this(project, fileToOpen, newMZmineFile, null, module, parameters, moduleCallDate);
  }

  public MSDKmzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      AdvancedSpectraImportParameters advancedParam, @NotNull final Class<? extends MZmineModule> module,
      @NotNull final ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(newMZmineFile.getMemoryMapStorage(), moduleCallDate); // storage in raw data file
    this.file = fileToOpen;
    this.project = project;
    this.newMZmineFile = newMZmineFile;
    description = "Importing raw data file: " + fileToOpen.getName();
    this.parameters = parameters;
    this.module = module;

    if (advancedParam != null) {
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
        this.ms1Detector = advancedParam
            .getParameter(AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter()
            .getValue();
      }
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
        this.ms2Detector = advancedParam
            .getParameter(AdvancedSpectraImportParameters.msMassDetection).getEmbeddedParameter()
            .getValue();
      }
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

    newMZmineFile.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
    project.addFile(newMZmineFile);
    setStatus(TaskStatus.FINISHED);
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
        } else if (ms2Detector != null && wrapper.getMSLevel() >= 2) {
          mzIntensities = applyMassDetection(ms2Detector, wrapper);
        }

        if (mzIntensities != null) {
          // create mass list and scan. Override data points and spectrum type
          newScan = ConversionUtils
              .msdkScanToSimpleScan(newMZmineFile, mzMLScan, mzIntensities[0], mzIntensities[1],
                  MassSpectrumType.CENTROIDED);
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
    Set<PasefMsMsInfo> finishedImsMsMsInfos = null;
    final IMSRawDataFile newImsFile = (IMSRawDataFile) newMZmineFile;

    Integer lastScanId = null;

    for (MsScan scan : file.getScans()) {
      MzMLMsScan mzMLScan = (MzMLMsScan) scan;
      if (buildingFrame == null
          || Float.compare((scan.getRetentionTime() / 60f), buildingFrame.getRetentionTime())
          != 0) {
        mobilityScanNumberCounter = 0; // mobility scan numbers start with 0!
        // waters uses different numbering for ms1 and ms2, so we need to reset if we start a new frame.
        lastScanId = null;

        if (buildingFrame != null) { // finish the frame
          final SimpleFrame finishedFrame = buildingFrame;
          finishedFrame.setMobilityScans(mobilityScans, false);
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
            scan.getRetentionTime() / 60f, null, null,
            ConversionUtils.msdkToMZmineSpectrumType(scan.getSpectrumType()),
            ConversionUtils.msdkToMZminePolarityType(scan.getPolarity()), scan.getScanDefinition(),
            scan.getScanningRange(), mzMLScan.getMobility().mt(), null);
        frameNumber++;

        description =
            "Importing " + file.getName() + ", parsed " + parsedScans + "/" + totalScans + " scans";
      }

      // I'm not proud of this piece of code, but some manufactures or conversion tools leave out
      // empty scans. however, we need that info for proper processing ~SteffenHeu
      if (lastScanId == null) {
        lastScanId = mzMLScan.getScanNumber();
      } else {
        Integer newScanId = mzMLScan.getScanNumber();
        final int missingScans = newScanId - lastScanId;
        if (missingScans > 1) {
          final Double lastMobility = mobilities.get(mobilities.size() - 1);
          final double nextMobility = mzMLScan.getMobility().mobility();
          final double deltaMobility = nextMobility - lastMobility;
          final double stepSize = deltaMobility / (missingScans + 1);

          for (int i = 0; i < missingScans; i++) {
            // make up for data saving options leaving out empty scans.
            // todo check if this works properly
            mobilityScans.add(
                new BuildingMobilityScan(mobilityScanNumberCounter, new double[0], new double[0]));

            final Double calculatedMobility = lastMobility + (i + 1) * stepSize;
            mobilities.add(calculatedMobility);
            mobilityScanNumberCounter++;
          }
        }
        lastScanId = mzMLScan.getScanNumber();
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
    if (msdkTask == null || msdkTask.getFinishedPercentage() == null) {
      return 0.0;
    }
    final double msdkProgress = msdkTask.getFinishedPercentage().doubleValue();
    final double parsingProgress = totalScans == 0 ? 0.0 : (double) parsedScans / totalScans;
    return (msdkProgress * 0.25) + (parsingProgress * 0.75);
  }

}
