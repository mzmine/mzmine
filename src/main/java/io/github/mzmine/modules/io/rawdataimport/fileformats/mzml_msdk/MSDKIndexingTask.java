package io.github.mzmine.modules.io.rawdataimport.fileformats.mzml_msdk;

import io.github.msdk.MSDKException;
import io.github.mzmine.modules.io.rawdataimport.fileformats.mzml_msdk.msdk.MzMLFileImportMethod;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;

public class MSDKIndexingTask extends AbstractTask {

  private final MzMLFileImportMethod importMethod;

  public MSDKIndexingTask(File file) {
    importMethod = new MzMLFileImportMethod(file);
  }


  @Override
  public String getTaskDescription() {
    return "Indexing file " + importMethod.getMzMLFile().getName();
  }

  @Override
  public double getFinishedPercentage() {
    return importMethod.getFinishedPercentage().doubleValue();
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      importMethod.execute();
      setStatus(TaskStatus.FINISHED);
    } catch (MSDKException e) {
      e.printStackTrace();
    }
  }

  public MzMLFileImportMethod getImportMethod() {
    return importMethod;
  }

  @Override
  public void cancel() {
    super.cancel();
    importMethod.cancel();
  }
}
