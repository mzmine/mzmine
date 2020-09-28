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

public class TDFMetadataReaderTask extends AbstractTask {

  public static final Logger logger = Logger.getLogger(TDFMetadataReaderTask.class.getName());
  private final File tdf;

  private String description;

  public TDFMetadataReaderTask(final File tdf) {
    this.tdf = tdf;
    description = null;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {

    // load class
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      logger.info("Could not load sqlite.JDBC.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    Connection connection = null;

    try {
      connection = DriverManager.getConnection("jdbc:sqlite:" + tdf.getAbsolutePath());

      TDFMetaDataTable metaDataTable = new TDFMetaDataTable();
      metaDataTable.executeQuery(connection);
      TDFFrameTable frameTable = new TDFFrameTable();
      frameTable.executeQuery(connection);
//      frameTable.print();

      logger.info("done");

//      Statement statement = connection.createStatement();
//      statement.setQueryTimeout(30);

//      ResultSet rsFrameNum = statement.executeQuery("SELECT COUNT(*) FROM frames");
//      if (!rsFrameNum.next()) {
//        logger.info("invalid frame count.");
//      }
//      int numFrames = rsFrameNum.getInt(1);
//      rsFrameNum.close();
//
//      List<Integer> frameNums = new ArrayList<>(numFrames);
//      ResultSet rsFrameNums = statement.executeQuery("SELECT Id FROM Frames");


//        statement.close();
      connection.close();
    } catch (SQLException throwables) {
      throwables.printStackTrace();
      logger.info("If stack trace contains \"out of memory\" the file was not found.");
      setStatus(TaskStatus.ERROR);
      return;
    }
    setStatus(TaskStatus.FINISHED);
  }

  private void setDescription(String desc) {
    description = desc;
  }

}
