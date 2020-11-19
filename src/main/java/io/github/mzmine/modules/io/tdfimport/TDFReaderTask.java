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

package io.github.mzmine.modules.io.tdfimport;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.tdfimport.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.FramePrecursorTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFFrameMsMsInfoTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFPasefFrameMsMsInfoTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFPrecursorTable;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class TDFReaderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(TDFReaderTask.class.getName());

  private String description;

  private final File tdf;
  private final File tdfBin;
  private final String rawDataFileName;

  private final TDFMetaDataTable metaDataTable;
  private final TDFFrameTable frameTable;
  private final TDFPrecursorTable precursorTable;
  private final TDFPasefFrameMsMsInfoTable pasefFrameMsMsInfoTable;
  private final TDFFrameMsMsInfoTable frameMsMsInfoTable;
  private final FramePrecursorTable framePrecursorTable;
  private final TDFMaldiFrameInfoTable maldiFrameInfoTable;

  private double finishedPercentage;
  private boolean isMaldi;

  /**
   * Bruker tims format:
   * - Folder
   *  - contains multiple files
   *  - one folder per analysis
   *  - .d extension
   *  - *.tdf - SQLite database; contains metadata
   *  - *.tdf_bin - contains peak data
   *
   *  - *.tdf_bin
   *   - list of frames
   *   - frame:
   *    - set of spectra at one specific time
   *    - single spectrum for "each" mobility
   *    - spectrum:
   *     - intensity vs m/z
   */

  /**
   * @param tdf
   * @param tdfBin
   */
  public TDFReaderTask(File tdf, File tdfBin) {
    this.tdf = tdf;
    this.tdfBin = tdfBin;
    metaDataTable = new TDFMetaDataTable();
    frameTable = new TDFFrameTable();
    precursorTable = new TDFPrecursorTable();
    pasefFrameMsMsInfoTable = new TDFPasefFrameMsMsInfoTable();
    frameMsMsInfoTable = new TDFFrameMsMsInfoTable();
    framePrecursorTable = new FramePrecursorTable();
    maldiFrameInfoTable = new TDFMaldiFrameInfoTable();
    isMaldi = false;

    if (tdf == null || tdfBin == null || !tdf.exists() || !tdf.canRead()
        || !tdfBin.exists() || !tdfBin.canRead()) {
      throw new IllegalArgumentException(
          "\"Cannot open sql or bin files: \" + tdf.getName() + \"; \" + tdfBin.getName()");
    }

    rawDataFileName = tdfBin.getParentFile().getName();

    setStatus(TaskStatus.WAITING);
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

    RawDataFileWriter newMZmineFile;
    try {
      newMZmineFile = MZmineCore.createNewIMSFile(rawDataFileName);
    } catch (IOException e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      return;
    }

    long handle = TDFUtils.openFile(tdfBin);
    int numFrames = frameTable.getNumberOfFrames();

    if (!isMaldi) {
      appendScansFromTimsSegment(newMZmineFile, handle, 1, numFrames, frameTable, metaDataTable,
          pasefFrameMsMsInfoTable, framePrecursorTable);
    } else {
      appendScansFromMaldiTimsSegment(newMZmineFile, handle, 1, numFrames, frameTable,
          metaDataTable, maldiFrameInfoTable);
    }

    final long lastFrameId = frameTable.getFrameIdColumn().get(frameTable.getNumberOfFrames() - 1);
    final int lastScanNum = Math.toIntExact(
        frameTable.getFirstScanNumForFrame(lastFrameId) +
            frameTable.getNumScansColumn().get(frameTable.getFrameIdColumn().indexOf(lastFrameId)));

    int frameId = 1;
    try {
      for (int scanNum = lastScanNum; scanNum < lastScanNum + numFrames; scanNum++) {
        finishedPercentage = 0.9 + 0.1 * frameId/numFrames;
        setDescription(
            "Importing " + rawDataFileName + ": Averaging Frame " + frameId + "/" + numFrames);
        Scan frame = TDFUtils
            .exctractCentroidScanForTimsFrame(handle, frameId, scanNum, metaDataTable, frameTable);
        newMZmineFile.addScan(frame);
        frameId++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    TDFUtils.close(handle);

    try {
      setDescription("Importing " + rawDataFileName + ": Writing raw data file...");
      RawDataFile file = newMZmineFile.finishWriting();
      finishedPercentage = 1.0;
      logger.info("Imported " + rawDataFileName + ". Loaded " + file.getNumOfScans() + " scans and "
          + ((IMSRawDataFile) file).getNumberOfFrames() + " frames.");
      MZmineCore.getProjectManager().getCurrentProject().addFile(file);
    } catch (IOException e) {
      e.printStackTrace();
    }

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
      metaDataTable.print();

      setDescription("Reading frame data for " + tdf.getName());
      frameTable.executeQuery(connection);
      frameTable.print();

      isMaldi = frameTable.getScanModeColumn()
          .contains(Integer.toUnsignedLong(BrukerScanMode.MALDI.getNum()));

      if (!isMaldi) {
        setDescription("Reading precursor info for " + tdf.getName());
        precursorTable.executeQuery(connection);
        precursorTable.print();

        setDescription("Reading PASEF info for " + tdf.getName());
        pasefFrameMsMsInfoTable.executeQuery(connection);
        pasefFrameMsMsInfoTable.print();

        setDescription("Reading Frame MS/MS info for " + tdf.getName());
        frameMsMsInfoTable.executeQuery(connection);
        frameMsMsInfoTable.print();

        setDescription("Reading MS/MS-Precursor info for " + tdf.getName());
        framePrecursorTable.executeQuery(connection);
        framePrecursorTable.print();
      } else {
        setDescription("MALDI info for " + tdf.getName());
        maldiFrameInfoTable.executeQuery(connection);
        maldiFrameInfoTable.print();
      }

      connection.close();
    } catch (SQLException throwable) {
      throwable.printStackTrace();
      logger.info("If stack trace contains \"out of memory\" the file was not found.");
      setStatus(TaskStatus.ERROR);
    }
  }

  private void setDescription(String desc) {
    description = desc;
  }

  /**
   * Adds all scans from the pasef segment to a raw data file. Does not add the frame spectra!
   *
   * @param rawDataFile                The raw file the scans will be added to
   * @param handle                     handle of the tdfbin. {@link TDFUtils#openFile(File)} {@link
   *                                   TDFUtils#openFile(File, long)}
   * @param firstFrameId               id of the first frame in the segment
   * @param lastFrameId                id of the last frame in the segment
   * @param tdfFrameTable              {@link TDFFrameTable} of the tdf file
   * @param tdfMetaDataTable           {@link TDFMetaDataTable} of the tdf file
   * @param tdfPasefFrameMsMsInfoTable {@link TDFPasefFrameMsMsInfoTable} of the tdf file
   * @param framePrecursorTable        {@link FramePrecursorTable} of the tdf file
   */
  private void appendScansFromTimsSegment(@Nonnull final RawDataFileWriter rawDataFile,
      final long handle,
      final long firstFrameId, final long lastFrameId,
      @Nonnull final TDFFrameTable tdfFrameTable,
      @Nonnull final TDFMetaDataTable tdfMetaDataTable,
      @Nonnull final TDFPasefFrameMsMsInfoTable tdfPasefFrameMsMsInfoTable,
      @Nonnull final FramePrecursorTable framePrecursorTable) {
    final long numFrames = tdfFrameTable.getNumberOfFrames();

    for (long frameId = firstFrameId; frameId <= lastFrameId; frameId++) {
      setDescription("Importing " + rawDataFileName + ": Frame " + frameId + "/" + numFrames);
      finishedPercentage = 0.9 * frameId / numFrames;
      final List<Scan> scans = TDFUtils.loadScansForTIMSFrame(
          handle, frameId, tdfFrameTable, tdfMetaDataTable,
          framePrecursorTable);

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

  private void appendScansFromMaldiTimsSegment(@Nonnull final RawDataFileWriter rawDataFile,
      final long handle, final long firstFrameId, final long lastFrameId,
      @Nonnull final TDFFrameTable tdfFrameTable,
      @Nonnull final TDFMetaDataTable tdfMetaDataTable,
      @Nonnull final TDFMaldiFrameInfoTable tdfMaldiTable) {

    final long numFrames = tdfFrameTable.getNumberOfFrames();

    for (long frameId = firstFrameId; frameId <= lastFrameId; frameId++) {
      setDescription(
          "Importing " + rawDataFileName + ": Frame " + frameId + "/" + numFrames);
      finishedPercentage = 0.9 * frameId / numFrames;
      final List<Scan> scans = TDFUtils.loadScansForMaldiTimsFrame(
          handle, frameId, tdfFrameTable, tdfMetaDataTable, tdfMaldiTable);

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

}
