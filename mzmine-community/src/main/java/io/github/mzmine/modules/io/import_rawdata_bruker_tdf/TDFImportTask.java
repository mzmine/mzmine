/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSImagingRawDataFile;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.DIAImsMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.IMSImagingRawDataFileImpl;
import io.github.mzmine.datamodel.impl.PasefMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.BuildingPASEFMsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.DiaFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.DiaFrameMsMsWindowTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.PrmFrameTargetTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameLaserInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFPasefFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFPrecursorTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_uv.BrukerUvReader;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 */
public class TDFImportTask extends AbstractTask implements RawDataImportTask {

  private static final Logger logger = Logger.getLogger(TDFImportTask.class.getName());
  private final MZmineProject project;
  private final ScanImportProcessorConfig scanProcessorConfig;
  private final Class<? extends MZmineModule> module;
  private final ParameterSet parameters;
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
  private DiaFrameMsMsWindowTable diaFrameMsMsWindowTable;
  private DiaFrameMsMsInfoTable diaFrameMsMsInfoTable;
  private IMSRawDataFile newMZmineFile = null;
  private boolean isMaldi;
  private String description;
  private double finishedPercentage;
  private double lastFinishedPercentage;
  private int loadedFrames;

  /**
   * Bruker tims format: - Folder - contains multiple files - one folder per analysis - .d extension
   * - *.tdf - SQLite database; contains metadata - *.tdf_bin - contains peak data
   * <p>
   * - *.tdf_bin - list of frames - frame: - set of spectra at one specific time - single spectrum
   * for "each" mobility - spectrum: - intensity vs m/z
   */

  public TDFImportTask(MZmineProject project, File file, MemoryMapStorage storage,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    this(project, file, storage, ScanImportProcessorConfig.createDefault(), module, parameters,
        moduleCallDate);
  }

  public TDFImportTask(MZmineProject project, File file, MemoryMapStorage storage,
      final @NotNull ScanImportProcessorConfig scanProcessorConfig,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.fileNameToOpen = file;
    this.project = project;
    this.scanProcessorConfig = scanProcessorConfig;
    this.module = module;
    this.parameters = parameters;
    setDescription("Importing raw data file %s".formatted(file.getName()));
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

    diaFrameMsMsInfoTable = new DiaFrameMsMsInfoTable();
    diaFrameMsMsWindowTable = new DiaFrameMsMsWindowTable();

    readMetadata();
    if (isMaldi) {
      newMZmineFile = new IMSImagingRawDataFileImpl(tdf.getParentFile().getName(),
          tdf.getParentFile().getAbsolutePath(), getMemoryMapStorage());
      ((IMSImagingRawDataFile) newMZmineFile).setImagingParam(
          new ImagingParameters(metaDataTable, maldiFrameInfoTable, maldiFrameLaserInfoTable));
    } else {
      newMZmineFile = new IMSRawDataFileImpl(tdf.getParentFile().getName(),
          tdf.getParentFile().getAbsolutePath(), getMemoryMapStorage());
    }

    newMZmineFile.setStartTimeStamp(metaDataTable.getAcquisitionDateTime());

    rawDataFileName = tdfBin.getParentFile().getName();
    synchronized (org.sqlite.JDBC.class) {
      BrukerUvReader.loadAndAddForFile(tdfBin.getParentFile(), (RawDataFileImpl) newMZmineFile,
          getMemoryMapStorage());
    }

    if (!(newMZmineFile instanceof IMSRawDataFileImpl)) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Raw data file was not recognised as IMSRawDataFile.");
      return;
    }

    final TDFUtils tdfUtils = new TDFUtils();
    logger.finest(() -> "Opening tdf file " + tdfBin.getAbsolutePath());
    final long handle = tdfUtils.openFile(tdfBin);
//    newMZmineFile.setName(rawDataFileName);
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
    List<SimpleFrame> frames = new ArrayList<>();

    final boolean importProfile = ConfigService.isTdfPseudoProfile();

    try {
      for (int i = 0; i < numFrames; i++) {
        int frameId = frameTable.getFrameIdColumn().get(i).intValue();
        setFinishedPercentage((double) (loadedFrames) / numFrames);
        setDescription(
            "Importing " + rawDataFileName + ": Importing Frame " + frameId + "/" + numFrames);
        final SimpleFrame frame;
        if (!importProfile) {
          frame = tdfUtils.extractCentroidScanForTimsFrame(newMZmineFile, frameId, metaDataTable,
              frameTable, framePrecursorTable, maldiFrameInfoTable, scanProcessorConfig);
        } else {
          frame = tdfUtils.extractProfileScanForFrame(newMZmineFile, frameId, metaDataTable,
              frameTable, framePrecursorTable, maldiFrameInfoTable, scanProcessorConfig);
        }

        // frame might be null when filtered out - just continue with next
        if (frame == null) {
          loadedFrames++;
          continue;
        }

        if (scanProcessorConfig.isMassDetectActive(frame.getMSLevel())) {
          frame.addMassList(new ScanPointerMassList(frame));
        }

        if (isMaldi && frame instanceof ImagingFrame imgFrame) {
          final MaldiSpotInfo maldiSpotInfo = maldiFrameInfoTable.getMaldiSpotInfo(
              frame.getFrameId());
          imgFrame.setMaldiSpotInfo(maldiSpotInfo);
        }

        loadMobilityScansForFrame(tdfUtils, frameTable, frame);

        newMZmineFile.addScan(frame);
        frames.add(frame);
        loadedFrames++;
        if (isCanceled()) {
          tdfUtils.close();
          return;
        }
      }
    } catch (IndexOutOfBoundsException e) {
      // happens on corrupt data
      logger.warning("Cannot import raw data from " + tdf.getName() + ", data is corrupt.");
      setStatus(TaskStatus.FINISHED);
      return;
    }

    // now assign MS/MS infos
    constructMsMsInfo(newMZmineFile, framePrecursorTable);
    assignDiaMsMsInfo(newMZmineFile, diaFrameMsMsWindowTable, diaFrameMsMsInfoTable);
    assignBbCidMsMsInfo(newMZmineFile, frameTable, frameMsMsInfoTable, metaDataTable);
    assignTimsAutoMsMsInfo(newMZmineFile, frameTable, frameMsMsInfoTable);

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
      try (Connection connection = DriverManager.getConnection(
          "jdbc:sqlite:" + tdf.getAbsolutePath())) {
        logger.finest(() -> "Connection established. " + connection.toString());

        setDescription("Reading metadata for " + tdf.getName());
        metaDataTable.executeQuery(connection);

        setDescription("Reading frame data for " + tdf.getName());
        frameTable.executeQuery(connection);

        isMaldi = frameTable.getScanModeColumn()
            .contains(Integer.toUnsignedLong(BrukerScanMode.MALDI.getNum()));

        setDescription("Reading precursor info for " + tdf.getName());
        precursorTable.executeQuery(connection);

        setDescription("Reading MS/MS-Precursor info for " + tdf.getName());
        framePrecursorTable.executeQuery(connection);

        setDescription("Reading PRM Target info for " + tdf.getName());
        prmFrameTargetTable.executeQuery(connection);

        if (isMaldi) {
          setDescription("MALDI info for " + tdf.getName());
          maldiFrameInfoTable.executeQuery(connection);
          maldiFrameInfoTable.process();
          maldiFrameLaserInfoTable.executeQuery(connection);
        }

        setDescription("Reading DIA info for " + tdf.getName());
        diaFrameMsMsInfoTable.executeQuery(connection);
        diaFrameMsMsWindowTable.executeQuery(connection);

        setDescription("Reading bbCID info for " + tdf.getName());
        frameMsMsInfoTable.executeQuery(connection);

      } catch (Throwable t) {
        logger.log(Level.FINE, t.getMessage(), t);
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

  private void loadMobilityScansForFrame(@Nonnull final TDFUtils tdfUtils,
      @NotNull final TDFFrameTable tdfFrameTable, SimpleFrame frame) {

    final List<BuildingMobilityScan> spectra = tdfUtils.loadSpectraForTIMSFrame(frame, frameTable,
        scanProcessorConfig);
    if (spectra.isEmpty()) {
      spectra.add(new BuildingMobilityScan(0, new double[]{}, new double[]{}));
    }

    boolean useAsMassList = scanProcessorConfig.isMassDetectActive(frame.getMSLevel());
    frame.setMobilityScans(spectra, useAsMassList);
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

  private void assignDiaMsMsInfo(IMSRawDataFile file, DiaFrameMsMsWindowTable diaWindows,
      DiaFrameMsMsInfoTable diaInfo) {
    final Map<Long, Long> frameToGroup = diaInfo.getFrameToWindowGroupMap();
    final Map<Long, Set<DIAImsMsMsInfoImpl>> groupInfoMap = diaWindows.getWindowGroupMsMsInfoMap();

    if (frameToGroup.isEmpty() || groupInfoMap.isEmpty()) {
      return;
    }

    for (Frame frame : file.getFrames()) {
      final Long group = frameToGroup.get((long) frame.getFrameId());
      if (group != null) {
        final Set<DIAImsMsMsInfoImpl> infos = groupInfoMap.get(group);
        // set the parent frame
        final Set<IonMobilityMsMsInfo> newInfos = infos.stream()
            .<IonMobilityMsMsInfo>mapMulti((info, c) -> {
              final IonMobilityMsMsInfo copy = !isMaldi ? info.createCopy()
                  : Objects.requireNonNullElse(mapToPasefInfo(info), info.createCopy());
              copy.setMsMsScan(frame);
              c.accept(copy);
            }).collect(Collectors.toSet());

        ((SimpleFrame) frame).setPrecursorInfos(newInfos);
      }
    }
  }

  /**
   * With timsControl > 6.1 the SIMSEF data is saved as DIA window. We have to map it to a regular
   * MS2 info.
   */
  private static @Nullable PasefMsMsInfo mapToPasefInfo(IonMobilityMsMsInfo i) {
    return switch (i) {
      case PasefMsMsInfo pasef -> pasef;
      case DIAImsMsMsInfoImpl dia -> {
        if (dia.getIsolationWindow() == null) {
          yield null;
        }
        final double isoWidth = RangeUtils.rangeLength(dia.getIsolationWindow()).doubleValue();
        if (isoWidth
            <= 5) { // is arbitrarily chosen as a cutoff for what can be deemed as a SIMSEF dda MS2.
          yield new PasefMsMsInfoImpl(RangeUtils.rangeCenter(dia.getIsolationWindow()),
              dia.getSpectrumNumberRange(), dia.getActivationEnergy(), null, null,
              dia.getMsMsFrame(), dia.getIsolationWindow());
        }
        yield null;
      }
      default -> null;
    };
  }

  /**
   * bbCID is Bruker's version of all ion fragmentation (AIF) or MSe. Alternating between low (MS1)
   * and high collision energies (MS2) without quad isolation.
   */
  private void assignBbCidMsMsInfo(IMSRawDataFile newMZmineFile, TDFFrameTable frameTable,
      TDFFrameMsMsInfoTable frameMsMsInfoTable, TDFMetaDataTable metadataTable) {
    List<? extends Frame> frames = newMZmineFile.getFrames();

    final int firstFrameId = (int) frameTable.getFirstFrameNumber();
    final Range<Double> mzRange = metadataTable.getMzRange();

    for (int i = 0; i < frames.size(); i++) {
      Frame frame = frames.get(i);
      final int frameTableIndex = frame.getFrameId() - firstFrameId;

      if (frameTable.getScanModeColumn().get(frameTableIndex).intValue()
          != BrukerScanMode.BROADBAND_CID.getNum()
          || frameTable.getMsMsTypeColumn().get(frameTableIndex).intValue() != 2) {
        continue;
      }

      final int frameMsMsTableIndex = BinarySearch.binarySearch(frameMsMsInfoTable.getFrameId(),
          (double) frame.getFrameId(), DefaultTo.MINUS_INSERTION_POINT, Long::doubleValue);
      if (frameMsMsTableIndex < 0) {
        continue;
      }

      final float ce = frameMsMsInfoTable.getCe().get(frameMsMsTableIndex).floatValue();
      final DIAImsMsMsInfoImpl diaImsMsMsInfo = new DIAImsMsMsInfoImpl(
          Range.closed(0, frame.getNumberOfMobilityScans() - 1), ce, frame, mzRange);
      frame.getImsMsMsInfos().add(diaImsMsMsInfo);
    }
  }

  /**
   * timsTOF classic only supports auto msms and not pasef.
   */
  private void assignTimsAutoMsMsInfo(IMSRawDataFile newMZmineFile, TDFFrameTable frameTable,
      TDFFrameMsMsInfoTable frameMsMsInfoTable) {
    List<? extends Frame> frames = newMZmineFile.getFrames();

    final int firstFrameId = (int) frameTable.getFirstFrameNumber();

    for (int i = 0; i < frames.size(); i++) {
      Frame frame = frames.get(i);
      final int frameTableIndex = frame.getFrameId() - firstFrameId;

      if (frameTable.getScanModeColumn().get(frameTableIndex).intValue()
          != BrukerScanMode.AUTO_MSMS.getNum()
          || frameTable.getMsMsTypeColumn().get(frameTableIndex).intValue() != 2) {
        continue;
      }

      final int frameMsMsTableIndex = BinarySearch.binarySearch(frameMsMsInfoTable.getFrameId(),
          (double) frame.getFrameId(), DefaultTo.MINUS_INSERTION_POINT, Long::doubleValue);
      if (frameMsMsTableIndex < 0) {
        continue;
      }

      final PasefMsMsInfo ddaImsMsMsInfo = frameMsMsInfoTable.getImsAutoMsMsInfo(
          frameMsMsTableIndex, frame, null);
      frame.getImsMsMsInfos().add(ddaImsMsMsInfo);
    }
  }

  @Nullable
  private Frame getParentFrame(IMSRawDataFile file, Integer parentFrameNumber) {
    if (parentFrameNumber == null) {
      return null;
    }
    Optional<Frame> optionalFrame = (Optional<Frame>) file.getFrames().stream()
        .filter(f -> f.getFrameId() == parentFrameNumber).findFirst();
    Frame parentFrame = optionalFrame.orElseGet(() -> null);
    return parentFrame;
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? newMZmineFile : null;
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
