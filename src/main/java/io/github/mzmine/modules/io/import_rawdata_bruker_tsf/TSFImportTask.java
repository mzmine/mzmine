/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_tsf;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.BrukerScanMode;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFFrameMsMsInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMaldiFrameLaserInfoTable;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TSFImportTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(TSFImportTask.class.getName());

  private final TDFMetaDataTable metaDataTable;
  private final TDFMaldiFrameInfoTable maldiFrameInfoTable;
  private final TDFFrameMsMsInfoTable frameMsMsInfoTable;
  private final TDFMaldiFrameLaserInfoTable maldiFrameLaserInfoTable;
  private final TSFFrameTable frameTable;
  private final MZmineProject project;
  private final File dirPath;
  private final String rawDataFileName;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private boolean isMaldi = false;
  private String description;
  private File tsf;
  private File tsf_bin;
  private int totalScans = 1;
  private int processedScans = 0;

  public TSFImportTask(MZmineProject project, File fileName, @Nullable MemoryMapStorage storage,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dirPath = fileName;
    rawDataFileName = fileName.getName();
    this.parameters = parameters;
    this.module = module;

    metaDataTable = new TDFMetaDataTable();
    maldiFrameInfoTable = new TDFMaldiFrameInfoTable();
    frameTable = new TSFFrameTable();
    maldiFrameLaserInfoTable = new TDFMaldiFrameLaserInfoTable();
    frameMsMsInfoTable = new TDFFrameMsMsInfoTable();

    setDescription("Importing " + rawDataFileName + ": Waiting.");
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return processedScans / (double) totalScans;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    setDescription("Importing " + rawDataFileName + ": opening files.");
    File[] files = getDataFilesFromDir(dirPath);
    if (files == null || files.length != 2 || files[0] == null || files[1] == null) {
      setErrorMessage("Could not find tsf files.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    tsf = files[0];
    tsf_bin = files[1];

    setDescription("Importing " + rawDataFileName + ": Initialising tsf reader.");
    final TSFUtils tsfUtils;
    try {
      tsfUtils = new TSFUtils();
    } catch (IOException e) {
      e.printStackTrace();
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    setDescription("Importing " + rawDataFileName + ": Opening " + tsf.getAbsolutePath());
    final long handle = tsfUtils.openFile(tsf_bin);
    if (handle == 0L) {
      setErrorMessage("Could not open " + tsf_bin.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      return;
    }

    setDescription("Importing " + rawDataFileName + ": Reading metadata");
    readMetadata();

    final RawDataFile newMZmineFile;
    try {
      if (isMaldi) {
        newMZmineFile = MZmineCore.createNewImagingFile(rawDataFileName, dirPath.getAbsolutePath(),
            getMemoryMapStorage());
      } else {
        newMZmineFile = MZmineCore.createNewFile(rawDataFileName, dirPath.getAbsolutePath(),
            getMemoryMapStorage());
      }
    } catch (IOException e) {
      setErrorMessage("Error creating raw data file.");
      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return;
    }

    newMZmineFile.setStartTimeStamp(metaDataTable.getAcquisitionDateTime());

    final int numScans = frameTable.getFrameIdColumn().size();
    totalScans = numScans;
    final boolean tryProfile = MZmineCore.getInstance().isTsfProfile();
    final MassSpectrumType importSpectrumType =
        tryProfile && metaDataTable.hasProfileSpectra() ? MassSpectrumType.PROFILE
            : MassSpectrumType.CENTROIDED;

    if (!importTSF(tsfUtils, handle, numScans, newMZmineFile, importSpectrumType)) {
      return;
    }
    addMsMsInfo(newMZmineFile);

    newMZmineFile.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(module, parameters, getModuleCallDate()));

    project.addFile(newMZmineFile);
    setStatus(TaskStatus.FINISHED);
  }

  private boolean importTSF(TSFUtils tsfUtils, long handle, int numScans, RawDataFile newMZmineFile,
      MassSpectrumType importSpectrumType) {
    for (int i = 0; i < numScans; i++) {
      final long frameId = frameTable.getFrameIdColumn().get(i);

      setDescription("Importing " + rawDataFileName + ": Scan " + frameId + "/" + numScans);

      final Scan scan = tsfUtils.loadScan(newMZmineFile, handle, frameId, metaDataTable, frameTable,
          frameMsMsInfoTable, maldiFrameInfoTable, importSpectrumType);

      try {
        newMZmineFile.addScan(scan);
      } catch (IOException e) {
        e.printStackTrace();
        setErrorMessage("Could not add scan " + frameId + " to raw data file.");
        setStatus(TaskStatus.ERROR);
        return true;
      }

      if (isCanceled()) {
        return false;
      }
      processedScans++;
    }

    if (newMZmineFile instanceof ImagingRawDataFile imgFile) {
      imgFile.setImagingParam(
          new ImagingParameters(metaDataTable, maldiFrameInfoTable, maldiFrameLaserInfoTable));
    }
    return true;
  }

  private void readMetadata() {
    setDescription("Initializing SQL...");

    // initialize jdbc driver:
    // https://stackoverflow.com/questions/6740601/what-does-class-fornameorg-sqlite-jdbc-do/6740632
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      logger.info("Could not load sqlite.JDBC.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    synchronized (org.sqlite.JDBC.class) {
      setDescription("Establishing SQL connection to " + tsf.getName());
      Connection connection;
      try {
        connection = DriverManager.getConnection("jdbc:sqlite:" + tsf.getAbsolutePath());

        setDescription("Reading metadata for " + tsf.getName());
        metaDataTable.executeQuery(connection);
        // metaDataTable.print();

        setDescription("Reading frame data for " + tsf.getName());
        frameTable.executeQuery(connection);
        // frameTable.print();

        isMaldi = frameTable.getScanModeColumn()
            .contains(Integer.toUnsignedLong(BrukerScanMode.MALDI.getNum()));

        setDescription("Reading frame msms data for " + tsf.getName());
        frameMsMsInfoTable.executeQuery(connection);

        if (isMaldi) {
          setDescription("MALDI info for " + tsf.getName());
          maldiFrameInfoTable.executeQuery(connection);
          maldiFrameInfoTable.process();
          maldiFrameLaserInfoTable.executeQuery(connection);
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

  public void setDescription(String description) {
    this.description = description;
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
      if (pathname.getAbsolutePath().endsWith(".tsf") || pathname.getAbsolutePath()
          .endsWith(".tsf_bin")) {
        return true;
      }
      return false;
    });

    if (files.length != 2) {
      return null;
    }

    File tsf = Arrays.stream(files).filter(c -> {
      if (c.getAbsolutePath().endsWith(".tsf")) {
        return true;
      }
      return false;
    }).findAny().orElse(null);
    File tsf_bin = Arrays.stream(files).filter(c -> {
      if (c.getAbsolutePath().endsWith(".tsf_bin")) {
        return true;
      }
      return false;
    }).findAny().orElse(null);

    return new File[]{tsf, tsf_bin};
  }

  private void addMsMsInfo(RawDataFile file) {
    setDescription("Assigning MS/MS precursor info for " + rawDataFileName + ".");

    Date start = new Date();
    int constructed = 0;
    for (Scan scan : file.getScans()) {
      if (scan.getMSLevel() == 1) {
        continue;
      }

      final int scanId = scan.getScanNumber();
      final int scanInfo = frameMsMsInfoTable.getColumn(TDFFrameMsMsInfoTable.FRAME_ID)
          .indexOf((long) scanId);

      if (scanInfo == -1) {
        continue;
      }

      ((SimpleScan) scan).setMsMsInfo(
          frameMsMsInfoTable.getDDAMsMsInfo(scanInfo, scan.getMSLevel(), scan, null));
    }

    Date end = new Date();
    logger.info(
        "Construced " + constructed + " DDAMsMsInfos for " + file.getScans().size() + " in " + (
            end.getTime() - start.getTime()) + " ms");
  }
}
