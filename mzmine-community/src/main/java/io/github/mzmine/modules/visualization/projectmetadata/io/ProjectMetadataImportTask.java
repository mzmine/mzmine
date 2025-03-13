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

package io.github.mzmine.modules.visualization.projectmetadata.io;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ProjectMetadataImportTask extends AbstractSimpleToolTask {

  private static final Logger logger = Logger.getLogger(ProjectMetadataImportTask.class.getName());
  private final File[] files;
  private final int totalFiles;
  private final Boolean skipColOnError;
  private final Boolean removeAttributesPrefix;
  private int doneFiles = 0;

  public ProjectMetadataImportTask(@NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(moduleCallDate, parameters);
    File file = parameters.getValue(ProjectMetadataImportParameters.fileName);
    this.files = new File[]{file};
    this.totalFiles = files.length;
    skipColOnError = parameters.getValue(ProjectMetadataImportParameters.skipErrorColumns);
    removeAttributesPrefix = parameters.getValue(
        ProjectMetadataImportParameters.removeAttributePrefix);
  }

  @Override
  public String getTaskDescription() {
    return "Importing metadata parameters from the selected file";
  }

  @Override
  protected void process() {
    ProjectMetadataReader reader = new ProjectMetadataReader(skipColOnError,
        removeAttributesPrefix);

    MetadataTable mergedMetadata = null;
    for (File file : files) {
      if (!(file.exists() && file.canRead())) {
        setStatus(TaskStatus.ERROR);
        final String msg = "Cannot read metadata file " + file + ". The file/path may not exist.";
        error(msg);
        DesktopService.getDesktop().displayErrorMessage(msg);
        return;
      }

      MetadataTable newMetadata = reader.readFile(file);
      var errors = reader.getErrors();
      if (!errors.isEmpty()) {
        var message = """
            Errors during metadata import of file: %s
            %s""".formatted(file.getAbsolutePath(), String.join("\n", errors));
        logger.warning(message);
        DesktopService.getDesktop().displayErrorMessage(message);
        // this is only a failure if newMetadata is null
        if (newMetadata == null) {
          error(message);
          return;
        }
      }
      if (newMetadata == null) {
        error("Undefined error during metadata import");
        return;
      }

      if (mergedMetadata == null) {
        mergedMetadata = newMetadata;
      } else {
        mergedMetadata = mergedMetadata.merge(newMetadata);
      }
    }
    if (mergedMetadata == null) {
      return;
    }
    
    MetadataTable projectMetadata = MZmineCore.getProjectMetadata();
    projectMetadata.merge(mergedMetadata);
  }
}
