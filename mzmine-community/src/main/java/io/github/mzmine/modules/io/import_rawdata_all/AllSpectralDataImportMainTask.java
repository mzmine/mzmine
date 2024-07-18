package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.threadpools.ThreadPoolTask;
import java.io.File;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class AllSpectralDataImportMainTask extends AbstractTask {

  private final ThreadPoolTask mainImportTask;
  private final File metadataFile;

  public AllSpectralDataImportMainTask(final List<? extends Task> tasks,
      final @NotNull ParameterSet parameters) {
    super(Instant.now(), "Main data import task");
    mainImportTask = ThreadPoolTask.createDefaultTaskManagerPool("Importing data", tasks);
    metadataFile = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        AllSpectralDataImportParameters.metadataFile, null);
  }


  @Override
  public String getTaskDescription() {
    return mainImportTask.getTaskDescription();
  }

  @Override
  public double getFinishedPercentage() {
    return mainImportTask.getFinishedPercentage();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // do data import and library import directly and import metadata after all is completed.
    // this ensures that data files are already loaded
    mainImportTask.run();

    if (mainImportTask.isCanceled()) {
      setStatus(mainImportTask.getStatus());
      setErrorMessage(mainImportTask.getErrorMessage());
    }
    if (metadataFile != null) {
      // load metadata after data files
      Task metaTask = loadMetadata();
      if (metaTask.isCanceled()) {
        setStatus(metaTask.getStatus());
        setErrorMessage(metaTask.getErrorMessage());
        return;
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  private Task loadMetadata() {
    var metaParams = ProjectMetadataImportParameters.create(metadataFile, false);
    var metadataTask = new ProjectMetadataImportTask(metaParams, moduleCallDate);
    metadataTask.run();
    return metadataTask;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }
}
