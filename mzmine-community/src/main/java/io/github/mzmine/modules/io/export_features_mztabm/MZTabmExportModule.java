/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineDesktop;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Currently still in redevelopment
 */
public class MZTabmExportModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Export to mzTab-m file.";
  //TODO modify desc
  private static final String MODULE_DESCRIPTION = //
      "This method exports the feature list contents into a mzTab-m file.";

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }


  /**
   * Schedules an export with a snapshot of the MS data files metadata grouping selection.
   *
   * @param project project owning the selected feature lists
   * @param parameters export settings
   * @param tasks destination for the export task
   * @param moduleCallDate module invocation time
   * @return successful task scheduling status
   */
  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    // decision: Capture the UI selection before the asynchronous export starts.
    final MetadataColumn<?> groupingColumn = DesktopService.getDesktop() instanceof MZmineDesktop desktop
        ? desktop.getRawDataGroupingColumn() : null;
    final MZTabmExportTask task = new MZTabmExportTask(project, parameters, moduleCallDate,
        groupingColumn);
    tasks.add(task);
    return ExitCode.OK;
  }

  //TODO update on addition of SMF and SME support
  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTEXPORT;
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MZTabmExportParameters.class;
  }
}
