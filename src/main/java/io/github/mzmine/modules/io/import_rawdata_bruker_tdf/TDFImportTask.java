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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.IMSImagingRawDataFileImpl;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.BuildingPASEFMsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.PrmFrameTargetTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameLaserInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFPasefFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFPrecursorTable;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class TDFImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(TDFImportTask.class.getName());

  private final MZmineProject project;
  @Nullable
  private final MassDetector ms1Detector;
  @Nullable
  private final MassDetector ms2Detector;
  @Nullable
  private final ParameterSet ms1DetectorParam;
  @Nullable
  private final ParameterSet ms2DetectorParam;

  private final boolean denoising = false;
  private static final double NOISE_THRESHOLD = 9E0;

  private File fileNameToOpen;
  private File tdf, tdfBin;
  private String rawDataFileName;
  private TDFMetaDataTable metaDataTable;
  private TDFFrameTable frameTable;
  private TDFPrecursorTable precursorTable;
  private TDFPasefFrameMsMsInfoTable pasefFrameMsMsInfoTable;
  private TDFFrameMsMsInfoTable frameMsMsInfoTable;
  private FramePrecursorTable framePrecursorTable;
  private PrmFrameTargetTable prmFrameTargetTable;
  private TDFMaldiFrameInfoTable maldiFrameInfoTable;
  private TDFMaldiFrameLaserInfoTable maldiFrameLaserInfoTable;
  private IMSRawDataFile newMZmineFile;
  private final Class<? extends MZmineModule> module;
  private final ParameterSet parameters;
  private boolean isMaldi;
  private String description;
  private double finishedPercentage;
  private double lastFinishedPercentage;
  private int loadedFrames;

  /**
   * Bruker tims format: - Folder - contains multiple files - one folder per analysis - .d extension
   * - *.tdf - SQLite database; contains metadata - *.tdf_bin - contains peak data
   *
   * - *.tdf_bin
   *   - list of frames
   *     - frame:
   *       - set of spectra at one specific time
   *         - single spectrum
   * for "each" mobility - spectrum: - intensity vs m/z
   */

  /**
   * @param project
   * @param file
   * @param newMZmineFile needs to be created as {@link IMSRawDataFileImpl} via {@link
   *                      MZmineCore#createNewIMSFile}.
   */
  public TDFImportTask(MZmineProject project, File file, IMSRawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    this(project, file, newMZmineFile, null, module, parameters, moduleCallDate);
  }

  public TDFImportTask(MZmineProject project, File file, IMSRawDataFile newMZmineFile,
      @Nullable final AdvancedSpectraImportParameters advancedParam,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(newMZmineFile.getMemoryMapStorage(), moduleCallDate);
    this.fileNameToOpen = file;
    this.project = project;
    this.newMZmineFile = newMZmineFile;
    this.module = module;
    this.parameters = parameters;

    if (advancedParam != null && advancedParam.getParameter(
        AdvancedSpectraImportParameters.msMassDetection).getValue()) {
      ms1Detector = advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection)
          .getEmbeddedParameter().getValue().getModule();
      ms1DetectorParam = advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection)
          .getEmbeddedParameter().getValue().getParameterSet();
    } else {
      if (denoising) {
        ms1Detector = MassDetectionParameters.centroid;
        ms1DetectorParam = MZmineCore.getConfiguration()
            .getModuleParameters(CentroidMassDetector.class).cloneParameterSet();
        ms1DetectorParam.getParameter(CentroidMassDetectorParameters.noiseLevel)
            .setValue(NOISE_THRESHOLD);
        ms1DetectorParam.setParameter(CentroidMassDetectorParameters.detectIsotopes, false);
      } else {
        ms1Detector = null;
        ms1DetectorParam = null;
      }
    }
    if (advancedParam != null && advancedParam.getParameter(
        AdvancedSpectraImportParameters.msMassDetection).getValue()) {
      ms2Detector = advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection)
          .getEmbeddedParameter().getValue().getModule();
      ms2DetectorParam = advancedParam.getParameter(
              AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter().getValue()
          .getParameterSet();
    } else {
      if (denoising) {
        ms2Detector = ms1Detector;
        ms2DetectorParam = ms1DetectorParam;
      } else {
        ms2Detector = null;
        ms2DetectorParam = null;
      }
    }
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  private void setFinishedPercentage(double percentage) {
    if (percentage - lastFinishedPercentage > 0.1) {
      logger.finest(() -> String.format("%s - %d", description, (int) (percentage * 100)) + "%");
      lastFinishedPercentage = percentage;
    }
    finishedPercentage = percentage;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    File[] files;
    if (fileNameToOpen.isDirectory() || fileNameToOpen.getAbsolutePath().endsWith(".d")) {
      files = getDataFilesFromDir(fileNameToOpen);
    } else {
      files = getDataFilesFromDir(fileNameToOpen.getParentFile());
    }
    if (files == null || files[0] == null || files[1] == null) {
      setErrorMessage("Cannot find or open file " + fileNameToOpen.toString());
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (files == null || files.length != 2 || files[0] == null || files[1] == null) {
      setErrorMessage("Could not find .tdf and .tdf_bin in " + fileNameToOpen.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      return;
    }

    this.tdf = files[0];
    this.tdfBin = files[1];

    if (!tdf.exists() || !tdf.canRead() || !tdfBin.exists() || !tdfBin.canRead()) {
      setErrorMessage("Cannot open sql or bin files: " + tdf.getName() + "; " + tdfBin.getName());
      setStatus(TaskStatus.ERROR);
      return;
    }

    metaDataTable = new TDFMetaDataTable();
    frameTable = new TDFFrameTable();
    precursorTable = new TDFPrecursorTable();
    pasefFrameMsMsInfoTable = new TDFPasefFrameMsMsInfoTable();
    prmFrameTargetTable = new PrmFrameTargetTable();
    frameMsMsInfoTable = new TDFFrameMsMsInfoTable();
    framePrecursorTable = new FramePrecursorTable();

    maldiFrameInfoTable = new TDFMaldiFrameInfoTable();
    maldiFrameLaserInfoTable = new TDFMaldiFrameLaserInfoTable();
    isMaldi = false;

    readMetadata();
    if (isMaldi) {
      try {
        newMZmineFile = new IMSImagingRawDataFileImpl(newMZmineFile.getName(),
            newMZmineFile.getAbsolutePath(), newMZmineFile.getMemoryMapStorage());
        ((IMSImagingRawDataFile) newMZmineFile).setImagingParam(
            new ImagingParameters(metaDataTable, maldiFrameInfoTable, maldiFrameLaserInfoTable));
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
    }

    rawDataFileName = tdfBin.getParentFile().getName();

    if (!(newMZmineFile instanceof IMSRawDataFileImpl)) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Raw data file was not recognised as IMSRawDataFile.");
      return;
    }

    final TDFUtils tdfUtils = new TDFUtils();
    logger.finest(() -> "Opening tdf file " + tdfBin.getAbsolutePath());
    final long handle = tdfUtils.openFile(tdfBin);
    newMZmineFile.setName(rawDataFileName);
    if (handle == 0L) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Failed to open the file " + tdfBin + " using the Bruker TDF library");
      return;
    }

    loadedFrames = 0;
    final int numFrames = frameTable.getFrameIdColumn().size();

    logger.finest("Starting frame import.");

    loadedFrames = 0;
    // collect average spectra for each frame
    Set<SimpleFrame> frames = new LinkedHashSet<>();

   final boolean importProfile = MZmineCore.getInstance().isTdfPseudoProfile();

    try {
      for (int i = 0; i < numFrames; i++) {
        int frameId = frameTable.getFrameIdColumn().get(i).intValue();
        setFinishedPercentage(0.1 * (loadedFrames) / numFrames);
        setDescription(
            "Importing " + rawDataFileName + ": Averaging Frame " + frameId + "/" + numFrames);
        final SimpleFrame frame;
        if (!importProfile) {
          frame = tdfUtils.extractCentroidScanForTimsFrame(newMZmineFile, frameId, metaDataTable,
              frameTable, framePrecursorTable, maldiFrameInfoTable, ms1Detector, ms1DetectorParam,
              ms2Detector, ms2DetectorParam);
        } else {
          frame = tdfUtils.extractProfileScanForFrame(newMZmineFile, frameId, metaDataTable,
              frameTable, framePrecursorTable, maldiFrameInfoTable, ms1Detector, ms1DetectorParam,
              ms2Detector, ms2DetectorParam);
        }

        if (frame.getMSLevel() == 1 && ms1Detector != null && ms1DetectorParam != null) {
          frame.addMassList(new ScanPointerMassList(frame));
        } else if (frame.getMSLevel() == 2 && ms2Detector != null && ms2DetectorParam != null) {
          frame.addMassList(new ScanPointerMassList(frame));
        }

        newMZmineFile.addScan(frame);
        frames.add(frame);
        loadedFrames++;
        if (isCanceled()) {
          tdfUtils.close();
          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // extract mobility scans
    appendScansFromTimsSegment(tdfUtils, frameTable, frames);

    // now assign MS/MS infos
    constructMsMsInfo(newMZmineFile, framePrecursorTable);

    tdfUtils.close();

    if (isCanceled()) {
      return;
    }

    setDescription("Importing " + rawDataFileName + ": Writing raw data file...");
    newMZmineFile.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));
    setFinishedPercentage(1.0);
    logger.info(
        "Imported " + rawDataFileName + ". Loaded " + newMZmineFile.getNumOfScans() + " scans and "
            + newMZmineFile.getNumberOfFrames() + " frames.");
    project.addFile(newMZmineFile);
    // compareMobilities(newMZmineFile);

    setStatus(TaskStatus.FINISHED);

  }

  private void readMetadata() {
    setDescription("Initializing SQL...");

    logger.finest(() -> "Initialising SQL...");
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      logger.info("Could not load sqlite.JDBC.");
      setStatus(TaskStatus.ERROR);
      return;
    }
    logger.finest(() -> "SQl initialised.");

    setDescription("Establishing SQL connection to " + tdf.getName());
    logger.finest(() -> "Establishing SQL connection to " + tdf.getName());

    synchronized (org.sqlite.JDBC.class) {
      Connection connection;
      try {
        connection = DriverManager.getConnection("jdbc:sqlite:" + tdf.getAbsolutePath());
        logger.finest(() -> "Connection established. " + connection.toString());

        setDescription("Reading metadata for " + tdf.getName());
        metaDataTable.executeQuery(connection);
        // metaDataTable.print();

        setDescription("Reading frame data for " + tdf.getName());
        frameTable.executeQuery(connection);
        // frameTable.print();

        isMaldi = frameTable.getScanModeColumn()
            .contains(Integer.toUnsignedLong(BrukerScanMode.MALDI.getNum()));

//        if (frameTable.getMsMsTypeColumn()
//            .contains(Integer.toUnsignedLong(BrukerScanMode.PASEF.getNum()))) {
        setDescription("Reading precursor info for " + tdf.getName());
        precursorTable.executeQuery(connection);

        setDescription("Reading MS/MS-Precursor info for " + tdf.getName());
        framePrecursorTable.executeQuery(connection);

        setDescription("Reading PRM Target info for " + tdf.getName());
        prmFrameTargetTable.executeQuery(connection);
//        }

        if (isMaldi) {
          setDescription("MALDI info for " + tdf.getName());
          maldiFrameInfoTable.executeQuery(connection);
          maldiFrameInfoTable.process();
          maldiFrameLaserInfoTable.executeQuery(connection);
          // maldiFrameInfoTable.print();
        }

        connection.close();
      } catch (Throwable t) {
        t.printStackTrace();
        logger.info("If stack trace contains \"out of memory\" the file was not found.");
        setStatus(TaskStatus.ERROR);
        setErrorMessage(t.toString());
      }
    }

    logger.info("Metadata read successfully for " + rawDataFileName);
  }

  private void setDescription(String desc) {
    description = desc;
  }

  /**
   * Adds all scans from the pasef segment to a raw data file. Does not add the frame spectra!
   *
   * @param tdfFrameTable {@link TDFFrameTable} of the tdf file
   * @param frames        the frames to load mobility spectra for
   */
  private void appendScansFromTimsSegment(@Nonnull final TDFUtils tdfUtils,
      @NotNull final TDFFrameTable tdfFrameTable, Set<SimpleFrame> frames) {

    loadedFrames = 0;
    final long numFrames = tdfFrameTable.lastFrameId();

    for (SimpleFrame frame : frames) {
      setDescription(
          "Loading mobility scans of " + rawDataFileName + ": Frame " + frame.getFrameId() + "/"
              + numFrames);
      setFinishedPercentage(0.1 + (0.9 * ((double) loadedFrames / numFrames)));

      final int msLevel = frame.getMSLevel();
      final MassDetector detector = msLevel == 1 ? ms1Detector : ms2Detector;
      final ParameterSet param = msLevel == 1 ? ms1DetectorParam : ms2DetectorParam;
      final List<BuildingMobilityScan> spectra = tdfUtils.loadSpectraForTIMSFrame(
          frame.getFrameId(), frameTable, detector, param);

      frame.setMobilityScans(spectra, detector != null);

      if (isCanceled()) {
        return;
      }
      loadedFrames++;
    }

  }

  private File[] getDataFilesFromDir(File dir) {

    if (!dir.exists() || !dir.isDirectory()) {
      setStatus(TaskStatus.ERROR);
      throw new IllegalArgumentException("Invalid directory.");
    }

    if (!dir.getAbsolutePath().endsWith(".d")) {
      setStatus(TaskStatus.ERROR);
      throw new IllegalArgumentException("Invalid directory ending.");
    }

    File[] files = dir.listFiles(pathname -> {
      if (pathname.getAbsolutePath().endsWith(".tdf") || pathname.getAbsolutePath()
          .endsWith(".tdf_bin")) {
        return true;
      }
      return false;
    });

    if (files.length != 2) {
      return null;
    }

    File tdf = Arrays.stream(files).filter(c -> c.getAbsolutePath().endsWith(".tdf")).findAny()
        .orElse(null);
    File tdf_bin = Arrays.stream(files).filter(c -> c.getAbsolutePath().endsWith(".tdf_bin"))
        .findAny().orElse(null);

    return new File[]{tdf, tdf_bin};
  }

  private void constructMsMsInfo(IMSRawDataFile file, FramePrecursorTable precursorTable) {
    setDescription("Assigning MS/MS precursor info for " + rawDataFileName + ".");

    Date start = new Date();
    int constructed = 0;
    for (Frame frame : file.getFrames()) {
      if (frame.getMSLevel() == 1) {
        continue;
      }

      Set<BuildingPASEFMsMsInfo> pasefBuildingInfo = precursorTable.getMsMsInfoForFrame(
          frame.getFrameId());
      final Set<BuildingPASEFMsMsInfo> prmBuildingInfo = prmFrameTargetTable.getMsMsInfoForFrame(
          frame.getFrameId());
      if (pasefBuildingInfo != null && prmBuildingInfo != null) {
        pasefBuildingInfo.addAll(prmBuildingInfo);
      } else if (pasefBuildingInfo == null && prmBuildingInfo != null) {
        pasefBuildingInfo = prmBuildingInfo;
      }

      if (pasefBuildingInfo == null) {
        continue;
      }

      for (BuildingPASEFMsMsInfo building : pasefBuildingInfo) {
        Integer parentFrameNumber = building.getParentFrameNumber();

        final Frame parentFrame = getParentFrame(file, parentFrameNumber);

        PasefMsMsInfo info = new PasefMsMsInfoImpl(building.getLargestPeakMz(),
            Range.closedOpen(building.getSpectrumNumberRange().lowerEndpoint() - 1,
                // -1 bc we work with indices later on
                building.getSpectrumNumberRange().upperEndpoint() - 1),
            building.getCollisionEnergy(), building.getPrecursorCharge(), parentFrame, frame,
            building.getIsolationWindow());

        frame.getImsMsMsInfos().add(info);
        constructed++;
      }
    }

    Date end = new Date();
    logger.info(
        "Construced " + constructed + " ImsMsMsInfos for " + file.getFrames().size() + " in " + (
            end.getTime() - start.getTime()) + " ms");
  }

  @Nullable
  private Frame getParentFrame(IMSRawDataFile file, Integer parentFrameNumber) {
    if(parentFrameNumber == null) {
      return null;
    }
    Optional<Frame> optionalFrame = (Optional<Frame>) file.getFrames().stream()
        .filter(f -> f.getFrameId() == parentFrameNumber).findFirst();
    Frame parentFrame = optionalFrame.orElseGet(() -> null);
    return parentFrame;
  }

 /*private void compareMobilities(IMSRawDataFile rawDataFile) {
    for (int i = 1; i < rawDataFile.getNumberOfFrames() - 1; i++) {
      Frame thisFrame = rawDataFile.getFrame(i);
      Frame nextFrame = rawDataFile.getFrame(i + 1);

      if (nextFrame == null) {
        break;
      }

      Set<Integer> nums = thisFrame.getMobilityScanNumbers();
      for (Integer num : nums) {
        if (Double.compare(thisFrame.getMobilityForMobilityScanNumber(num),
            nextFrame.getMobilityForMobilityScanNumber(num)) != 0) {
          logger.info("Mobilities for num " + num + " dont match 1: "
              + thisFrame.getMobilityForMobilityScanNumber(num) + " 2: "
              + nextFrame.getMobilityForMobilityScanNumber(num));
        }
      }
    }
  }*/
}
