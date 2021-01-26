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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_bruker_tdf.datamodel.BrukerScanMode;
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
   * @param newMZmineFile needs to be created as {@link IMSRawDataFileImpl} via
   *        {@link MZmineCore#createNewIMSFile(String)}.
   */
  public TDFImportTask(MZmineProject project, File file, IMSRawDataFile newMZmineFile) {
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

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    readMetadata();

    long handle = TDFUtils.openFile(tdfBin);

    File[] files = new File[2];
    if (fileNameToOpen.isDirectory()) {
      files = getDataFilesFromDir(fileNameToOpen);
    } else {
      files = getDataFilesFromDir(fileNameToOpen.getParentFile());
    }

    this.tdf = files[0];
    this.tdfBin = files[0];

    if (tdf == null || tdfBin == null || !tdf.exists() || !tdf.canRead() || !tdfBin.exists()
        || !tdfBin.canRead()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Cannot open sql or bin files: " + tdf.getName() + "; " + tdfBin.getName());
    }

    metaDataTable = new TDFMetaDataTable();
    frameTable = new TDFFrameTable();
    precursorTable = new TDFPrecursorTable();
    pasefFrameMsMsInfoTable = new TDFPasefFrameMsMsInfoTable();
    frameMsMsInfoTable = new TDFFrameMsMsInfoTable();
    framePrecursorTable = new FramePrecursorTable(frameTable);
    maldiFrameInfoTable = new TDFMaldiFrameInfoTable();
    isMaldi = false;

    rawDataFileName = tdfBin.getParentFile().getName();

    if (!(newMZmineFile instanceof IMSRawDataFileImpl)) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Raw data file was not recognised as IMSRawDataFile.");
      return;
    }

    newMZmineFile.setName(rawDataFileName);

    loadedFrames = 0;

    if (handle == 0l) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Failed to open the file " + tdfBin + " using the Bruker TDF library");
      return;
    }

    int numFrames = frameTable.getNumberOfFrames();

    Date start = new Date();

    identifySegments((IMSRawDataFileImpl) newMZmineFile);

    loadedFrames = 0;
    // collect average spectra for each frame
    int frameId = frameTable.getFrameIdColumn().get(0).intValue();
    Set<Frame> frames = new LinkedHashSet<>();
    try {
      for (int scanNum = frameId; scanNum < frameTable.getNumberOfFrames(); scanNum++) {
        finishedPercentage = 0.1 * (loadedFrames) / numFrames;
        setDescription(
            "Importing " + rawDataFileName + ": Averaging Frame " + frameId + "/" + numFrames);
        Frame frame = TDFUtils.exctractCentroidScanForTimsFrame(newMZmineFile, handle, frameId,
            metaDataTable, frameTable, framePrecursorTable);
        newMZmineFile.addScan(frame);
        frames.add(frame);
        frameId++;
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

    // } else {
    // appendScansFromMaldiTimsSegment(newMZmineFile, handle, 1, numFrames, frameTable,
    // metaDataTable, maldiFrameInfoTable);
    // }

    TDFUtils.close(handle);

    if (isCanceled()) {
      return;
    }

    setDescription("Importing " + rawDataFileName + ": Writing raw data file...");
    finishedPercentage = 1.0;
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

  private void setDescription(String desc) {
    description = desc;
  }

  /**
   * Adds all scans from the pasef segment to a raw data file. Does not add the frame spectra!
   *
   * @param handle handle of the tdfbin. {@link TDFUtils#openFile(File)}
   *        {@link TDFUtils#openFile(File, long)}
   * @param tdfFrameTable {@link TDFFrameTable} of the tdf file
   * @param frames the frames to load mobility spectra for
   */
  private void appendScansFromTimsSegment(final long handle,
      @Nonnull final TDFFrameTable tdfFrameTable, Set<Frame> frames) {

    loadedFrames = 0;
    final long numFrames = tdfFrameTable.getNumberOfFrames();

    for (Frame frame : frames) {
      setDescription("Loading mobility scans of " + rawDataFileName + ": Frame "
          + frame.getFrameId() + "/" + numFrames);
      finishedPercentage = 0.1 + (0.9 * ((double) loadedFrames / numFrames));
      final Set<MobilityScan> spectra = TDFUtils.loadSpectraForTIMSFrame(newMZmineFile, handle,
          frame.getFrameId(), frame, frameTable);

      for (MobilityScan spectrum : spectra) {
        frame.addMobilityScan(spectrum);
      }

      if (isCanceled()) {
        return;
      }
      loadedFrames++;
    }

  }

  private void appendScansFromMaldiTimsSegment(@Nonnull final IMSRawDataFile rawDataFile,
      final long handle, final long firstFrameId, final long lastFrameId,
      @Nonnull final TDFFrameTable tdfFrameTable, @Nonnull final TDFMetaDataTable tdfMetaDataTable,
      @Nonnull final TDFMaldiFrameInfoTable tdfMaldiTable) {

    final long numFrames = tdfFrameTable.getNumberOfFrames();

    for (long frameId = firstFrameId; frameId <= lastFrameId; frameId++) {
      setDescription("Importing " + rawDataFileName + ": Frame " + frameId + "/" + numFrames);
      finishedPercentage = 0.9 * frameId / numFrames;
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
  }

  private File[] getDataFilesFromDir(File dir) {

    if (!dir.exists() || !dir.isDirectory()) {
      throw new IllegalArgumentException("Invalid directory.");
    }

    if (!dir.getAbsolutePath().endsWith(".d")) {
      throw new IllegalArgumentException("Invalid directory ending..");
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

    return new File[] {tdf, tdf_bin};
  }

  private void identifySegments(IMSRawDataFileImpl rawDataFile) {
    rawDataFile.addSegment(Range.closed(1, frameTable.getNumberOfFrames()));
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
