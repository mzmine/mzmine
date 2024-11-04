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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataParameters;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataTask;
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
      return;
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

    // recolor by default without any metadata
    ColorByMetadataTask colorTask = recolorBlanksAndQcs();
    if (colorTask.isCanceled()) {
      setStatus(colorTask.getStatus());
      setErrorMessage(colorTask.getErrorMessage());
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private ColorByMetadataTask recolorBlanksAndQcs() {
    ColorByMetadataTask task = new ColorByMetadataTask(moduleCallDate,
        ColorByMetadataParameters.createDefault(), AllSpectralDataImportModule.class);
    task.run();
    return task;
  }

  private Task loadMetadata() {
    var metaParams = ProjectMetadataImportParameters.create(metadataFile, false, false);
    var metadataTask = new ProjectMetadataImportTask(metaParams, moduleCallDate);
    metadataTask.run();
    return metadataTask;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }
}
