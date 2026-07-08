/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.io.ProjectMetadataExportParameters.MetadataFileFormat;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import java.io.File;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataExportTask extends AbstractSimpleToolTask {

  private final File file;
  private final MetadataFileFormat format;
  private final List<ProjectMetadataColumnMapping> columnMappings;
  private final @Nullable List<? extends RawDataFile> rawDataFiles;

  /**
   * @param moduleCallDate the call date of module to order execution order
   */
  public ProjectMetadataExportTask(final @NotNull Instant moduleCallDate,
      @NotNull final ParameterSet parameters) {
    this(moduleCallDate, parameters, null);
  }

  /**
   * @param moduleCallDate the call date of module to order execution order
   * @param rawDataFiles   raw data files to include in the export
   */
  public ProjectMetadataExportTask(final @NotNull Instant moduleCallDate,
      @NotNull final ParameterSet parameters,
      @Nullable final List<? extends RawDataFile> rawDataFiles) {
    super(moduleCallDate, parameters);
    file = parameters.getValue(ProjectMetadataExportParameters.fileName);
    format = parameters.getValue(ProjectMetadataExportParameters.format);

    columnMappings = parameters.getEmbeddedParameterValueIfSelectedOrElseGet(
        ProjectMetadataExportParameters.columnMappings, List::of);
    this.rawDataFiles = rawDataFiles == null ? null : List.copyOf(rawDataFiles);
  }

  @Override
  protected void process() {
    final MetadataTable metadata = ProjectService.getProject().getProjectMetadata();
    final ProjectMetadataWriter writer = new ProjectMetadataWriter(metadata, format,
        columnMappings);
    final List<? extends RawDataFile> exportRawDataFiles =
        rawDataFiles == null ? List.of(ProjectService.getProject().getDataFiles()) : rawDataFiles;
    if (!writer.exportTo(file, exportRawDataFiles)) {
      error("Error during metadata file export");
      return;
    }
  }

  @Override
  public String getTaskDescription() {
    return "Exporting project metadata to file";
  }
}
