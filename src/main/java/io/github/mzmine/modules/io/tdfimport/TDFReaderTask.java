package io.github.mzmine.modules.io.tdfimport;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.tdfimport.datamodel.TDFLibrary;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFPrecursorTable;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class TDFReaderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(TDFReaderTask.class.getName());

  private String description;

  private final TDFLibrary tdfLibrary;
  private final File tdf;
  private final File tdfBin;

  private final TDFMetaDataTable metaDataTable;
  private final TDFFrameTable frameTable;
  private final TDFPrecursorTable precursorTable;

  private double finishedPercentage;

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
   * @param tdfLibrary
   * @param tdf
   * @param tdfBin
   */
  public TDFReaderTask(TDFLibrary tdfLibrary, File tdf, File tdfBin) {
    this.tdfLibrary = tdfLibrary;
    this.tdf = tdf;
    this.tdfBin = tdfBin;
    metaDataTable = new TDFMetaDataTable();
    frameTable = new TDFFrameTable();
    precursorTable = new TDFPrecursorTable();

    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void run() {
    if (tdfLibrary == null || tdf == null || tdfBin == null || !tdf.exists() || !tdf.canRead()
        || !tdfBin.exists() || !tdfBin.canRead()) {
      logger.info("Cannot open sql or bin files: " + tdf.getName() + "; " + tdfBin.getName());
      return;
    }

    setStatus(TaskStatus.PROCESSING);
    readMetadata();

    long handle = tdfLibrary.tims_open(tdfBin.getAbsolutePath(), 0);
    if (handle == 0) {
      logger.info("Could not open file " + tdfBin.getAbsolutePath());
    }

    byte[] buffer = new byte[200000];
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

      setDescription("Reading frame data for " + tdf.getName());
      frameTable.executeQuery(connection);

      setDescription("Reading precursor info for " + tdf.getName());


      connection.close();
    } catch (SQLException throwable) {
      throwable.printStackTrace();
      logger.info("If stack trace contains \"out of memory\" the file was not found.");
      setStatus(TaskStatus.ERROR);
      return;
    }
  }

  private void setDescription(String desc) {
    description = desc;
  }
}
