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

package io.github.mzmine.modules.io.import_bruker_tdf;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.ImsMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.BuildingPASEFMsMsInfo;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFPasefFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.sql.TDFPrecursorTable;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * @author https://github.com/SteffenHeu
 */
public class TDFImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(TDFImportTask.class.getName());

  private final MZmineProject project;
  private File fileNameToOpen;
  private File tdf, tdfBin;
  private String rawDataFileName;
  private TDFMetaDataTable metaDataTable;
  private TDFFrameTable frameTable;
  private TDFPrecursorTable precursorTable;
  private TDFPasefFrameMsMsInfoTable pasefFrameMsMsInfoTable;
  private TDFFrameMsMsInfoTable frameMsMsInfoTable;
  private FramePrecursorTable framePrecursorTable;
  private TDFMaldiFrameInfoTable maldiFrameInfoTable;
  private IMSRawDataFile newMZmineFile;
  private boolean isMaldi;

  private String description;
  private double finishedPercentage;
  private double lastFinishedPercentage;
  private int loadedFrames;

  /**
   * Bruker tims format: - Folder - contains multiple files - one folder per analysis - .d extension
   * - *.tdf - SQLite database; contains metadata - *.tdf_bin - contains peak data
   *
   * - *.tdf_bin - list of frames - frame: - set of spectra at one specific time - single spectrum
   * for "each" mobility - spectrum: - intensity vs m/z
   */

  /**
   * @param project
   * @param file
   * @param newMZmineFile needs to be created as {@link IMSRawDataFileImpl} via {@link
   *                      MZmineCore#createNewIMSFile}.
   */
  public TDFImportTask(MZmineProject project, File file, IMSRawDataFile newMZmineFile) {
    super(newMZmineFile.getMemoryMapStorage());
    this.fileNameToOpen = file;
    this.project = project;
    this.newMZmineFile = newMZmineFile;
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
    if(files == null || files[0] == null || files [1] == null) {
      setErrorMessage("Cannot find or open file " + fileNameToOpen.toString());
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
    frameMsMsInfoTable = new TDFFrameMsMsInfoTable();
    framePrecursorTable = new FramePrecursorTable(frameTable);
    maldiFrameInfoTable = new TDFMaldiFrameInfoTable();
    isMaldi = false;

    readMetadata();
    /*if (isMaldi) {
      try {
        newMZmineFile = new IMSImagingRawDataFileImpl(newMZmineFile.getName(),
            newMZmineFile.getMemoryMapStorage());
        ((IMSImagingRawDataFile) newMZmineFile)
            .setImagingParam(new ImagingParameters(maldiFrameInfoTable));
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
    }*/

    rawDataFileName = tdfBin.getParentFile().getName();

    if (!(newMZmineFile instanceof IMSRawDataFileImpl)) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Raw data file was not recognised as IMSRawDataFile.");
      return;
    }

    logger.finest(() -> "Opening tdf file " + tdfBin.getAbsolutePath());
    final long handle = TDFUtils.openFile(tdfBin);
    newMZmineFile.setName(rawDataFileName);

    loadedFrames = 0;

    if (handle == 0l) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Failed to open the file " + tdfBin + " using the Bruker TDF library");
      return;
    }

    final int numFrames = frameTable.getFrameIdColumn().size();

    Date start = new Date();

    identifySegments((IMSRawDataFileImpl) newMZmineFile);

    logger.finest("Starting frame import.");

    loadedFrames = 0;
    // collect average spectra for each frame
    Set<SimpleFrame> frames = new LinkedHashSet<>();
    try {
      for (int i = 0; i < numFrames; i++) {
        int frameId = frameTable.getFrameIdColumn().get(i).intValue();
        setFinishedPercentage(0.1 * (loadedFrames) / numFrames);
        setDescription(
            "Importing " + rawDataFileName + ": Averaging Frame " + frameId + "/" + numFrames);
        SimpleFrame frame = TDFUtils
            .exctractCentroidScanForTimsFrame(newMZmineFile, handle, frameId,
                metaDataTable, frameTable, framePrecursorTable/*, maldiFrameInfoTable*/);
        newMZmineFile.addScan(frame);
        frames.add(frame);
        loadedFrames++;
        if (isCanceled()) {
          TDFUtils.close(handle);
          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    // if (!isMaldi) {
    appendScansFromTimsSegment(handle, frameTable, frames);

//    logger.info("num dp (import): " + TDFUtils.numDP);
//    logger.info("num dp (stored): " + SimpleFrame.numDp);

    // } else {
    // appendScansFromMaldiTimsSegment(newMZmineFile, handle, 1, numFrames, frameTable,
    // metaDataTable, maldiFrameInfoTable);
    // }

    // now assign MS/MS infos
    constructMsMsInfo(newMZmineFile, framePrecursorTable);

    TDFUtils.close(handle);

    if (isCanceled()) {
      return;
    }

    setDescription("Importing " + rawDataFileName + ": Writing raw data file...");
    setFinishedPercentage(1.0);
    logger.info("Imported " + rawDataFileName + ". Loaded " + newMZmineFile.getNumOfScans()
        + " scans and " + newMZmineFile.getNumberOfFrames() + " frames.");
    project.addFile(newMZmineFile);
    // compareMobilities(newMZmineFile);

    setStatus(TaskStatus.FINISHED);

  }

  private void readMetadata() {
    setDescription("Initializing SQL...");

    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      logger.info("Could not load sqlite.JDBC.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    setDescription("Establishing SQL connection to " + tdf.getName());
    Connection connection;
    try {
      connection = DriverManager.getConnection("jdbc:sqlite:" + tdf.getAbsolutePath());

      setDescription("Reading metadata for " + tdf.getName());
      metaDataTable.executeQuery(connection);
      // metaDataTable.print();

      setDescription("Reading frame data for " + tdf.getName());
      frameTable.executeQuery(connection);
      // frameTable.print();

      isMaldi = frameTable.getScanModeColumn()
          .contains(Integer.toUnsignedLong(BrukerScanMode.MALDI.getNum()));

      if (!isMaldi) {
        if (frameTable.getScanModeColumn()
            .contains(Integer.toUnsignedLong(BrukerScanMode.PASEF.getNum()))) {
          setDescription("Reading precursor info for " + tdf.getName());
          precursorTable.executeQuery(connection);
          // precursorTable.print();

          setDescription("Reading PASEF info for " + tdf.getName());
          pasefFrameMsMsInfoTable.executeQuery(connection);
          // pasefFrameMsMsInfoTable.print();

          setDescription("Reading Frame MS/MS info for " + tdf.getName());
          frameMsMsInfoTable.executeQuery(connection);
          // frameMsMsInfoTable.print();

          setDescription("Reading MS/MS-Precursor info for " + tdf.getName());
          framePrecursorTable.executeQuery(connection);
          // framePrecursorTable.print();
        }
      } else {
        setDescription("MALDI info for " + tdf.getName());
        maldiFrameInfoTable.executeQuery(connection);
        /*maldiFrameInfoTable.process();*/
        // maldiFrameInfoTable.print();
      }

      connection.close();
    } catch (Throwable t) {
      t.printStackTrace();
      logger.info("If stack trace contains \"out of memory\" the file was not found.");
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.toString());
    }

    logger.info("Metadata read successfully for " + rawDataFileName);
  }

  private void setDescription(String desc) {
    description = desc;
  }

  /**
   * Adds all scans from the pasef segment to a raw data file. Does not add the frame spectra!
   *
   * @param handle        handle of the tdfbin. {@link TDFUtils#openFile(File)} {@link
   *                      TDFUtils#openFile(File, long)}
   * @param tdfFrameTable {@link TDFFrameTable} of the tdf file
   * @param frames        the frames to load mobility spectra for
   */
  private void appendScansFromTimsSegment(final long handle,
      @Nonnull final TDFFrameTable tdfFrameTable, Set<SimpleFrame> frames) {

    loadedFrames = 0;
    final long numFrames = tdfFrameTable.lastFrameId();

    for (SimpleFrame frame : frames) {
      setDescription("Loading mobility scans of " + rawDataFileName + ": Frame "
          + frame.getFrameId() + "/" + numFrames);
      setFinishedPercentage(0.1 + (0.9 * ((double) loadedFrames / numFrames)));
      final List<BuildingMobilityScan> spectra = TDFUtils
          .loadSpectraForTIMSFrame(newMZmineFile, handle,
              frame.getFrameId(), frame, frameTable);

      frame.setMobilityScans(spectra);

      if (isCanceled()) {
        return;
      }
      loadedFrames++;
    }

  }

  /*private void appendScansFromMaldiTimsSegment(@Nonnull final IMSRawDataFile rawDataFile,
      final long handle, final long firstFrameId, final long lastFrameId,
      @Nonnull final TDFFrameTable tdfFrameTable, @Nonnull final TDFMetaDataTable tdfMetaDataTable,
      @Nonnull final TDFMaldiFrameInfoTable tdfMaldiTable) {

    final long numFrames = tdfFrameTable.lastFrameId();

    for (long frameId = firstFrameId; frameId <= lastFrameId; frameId++) {
      setDescription("Importing " + rawDataFileName + ": Frame " + frameId + "/" + numFrames);
      setFinishedPercentage(0.9 * frameId / numFrames);
      final List<Scan> scans = TDFUtils.loadScansForMaldiTimsFrame(handle, frameId, tdfFrameTable,
          tdfMetaDataTable, tdfMaldiTable);

      try {
        for (Scan scan : scans) {
          rawDataFile.addScan(scan);
        }
      } catch (IOException e) {
        e.printStackTrace();
        TDFUtils.close(handle);
      }
    }
  }*/

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
      if (pathname.getAbsolutePath().endsWith(".tdf")
          || pathname.getAbsolutePath().endsWith(".tdf_bin")) {
        return true;
      }
      return false;
    });

    if (files.length != 2) {
      return null;
    }

    File tdf = Arrays.stream(files).filter(c -> {
      if (c.getAbsolutePath().endsWith(".tdf")) {
        return true;
      }
      return false;
    }).findAny().get();
    File tdf_bin = Arrays.stream(files).filter(c -> {
      if (c.getAbsolutePath().endsWith(".tdf_bin")) {
        return true;
      }
      return false;
    }).findAny().get();

    return new File[]{tdf, tdf_bin};
  }

  private void identifySegments(IMSRawDataFileImpl rawDataFile) {
    rawDataFile.addSegment(Range.closed(1, frameTable.lastFrameId()));
  }

  private void constructMsMsInfo(IMSRawDataFile file, FramePrecursorTable precursorTable) {
    setDescription("Assigning MS/MS precursor info for " + rawDataFileName + ".");

    Date start = new Date();
    int constructed = 0;
    for (Frame frame : file.getFrames()) {
      if (frame.getMSLevel() == 1) {
        continue;
      }

      Set<BuildingPASEFMsMsInfo> buildingInfo =
          precursorTable.getMsMsInfoForFrame(frame.getFrameId());

      for (BuildingPASEFMsMsInfo building : buildingInfo) {
        int parentFrameNumber = building.getParentFrameNumber();

        Optional<Frame> optionalFrame = (Optional<Frame>) file.getFrames().stream()
            .filter(f -> f.getFrameId() == parentFrameNumber).findFirst();
        Frame parentFrame = optionalFrame.orElseGet(() -> null);

        ImsMsMsInfo info = new ImsMsMsInfoImpl(building.getLargestPeakMz(),
            Range.closedOpen(building.getSpectrumNumberRange().lowerEndpoint() - 1,
                building.getSpectrumNumberRange().upperEndpoint() - 1),
            building.getCollisionEnergy(),
            building.getPrecursorCharge(), parentFrame, frame);

        frame.getImsMsMsInfos().add(info);
        constructed++;
      }
    }

    Date end = new Date();
    logger.info(
        "Construced " + constructed + " ImsMsMsInfos for " + file.getFrames().size() + " in " + (
            end.getTime() - start.getTime()) + " ms");
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
