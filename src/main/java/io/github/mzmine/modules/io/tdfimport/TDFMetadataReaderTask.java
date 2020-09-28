package io.github.mzmine.modules.io.tdfimport;

import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFFrameTable;
import io.github.mzmine.modules.io.tdfimport.datamodel.sql.TDFMetaDataTable;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class TDFMetadataReaderTask extends AbstractTask {

  public static final Logger logger = Logger.getLogger(TDFMetadataReaderTask.class.getName());
  private final File tdf;

  private String description;
  private double finishedPercentage;
  private TDFMetaDataTable metaDataTable;
  private TDFFrameTable frameTable;

  public TDFMetadataReaderTask(final File tdf) {
    this.tdf = tdf;
    description = null;
    finishedPercentage = 0;
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
      metaDataTable = new TDFMetaDataTable();
      metaDataTable.executeQuery(connection);

      setDescription("Reading frame data for " + tdf.getName());
      frameTable = new TDFFrameTable();
      frameTable.executeQuery(connection);

      connection.close();
    } catch (SQLException throwable) {
      throwable.printStackTrace();
      logger.info("If stack trace contains \"out of memory\" the file was not found.");
      setStatus(TaskStatus.ERROR);
      return;
    }
    setStatus(TaskStatus.FINISHED);
  }

  @Nullable
  public TDFMetaDataTable getMetadataTable() {
    return metaDataTable;
  }

  @Nullable
  public TDFFrameTable getFrameTable() {
    return frameTable;
  }

  private void setDescription(String desc) {
    description = desc;
  }

}
