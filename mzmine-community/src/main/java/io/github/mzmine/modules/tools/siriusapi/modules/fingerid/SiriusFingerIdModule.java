/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.siriusapi.modules.fingerid;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.modules.tools.siriusapi.JobWaiterTask;
import io.github.mzmine.modules.tools.siriusapi.Sirius;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureUtils;
import io.sirius.ms.sdk.model.Job;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class SiriusFingerIdModule extends AbstractProcessingModule {

  public SiriusFingerIdModule() {
    super("Sirius CSI:FingerID", SiriusFingerIdParameters.class, MZmineModuleCategory.ANNOTATION,
        "Executes CSI:FingerID from an mzmine feature list.");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    final String idStr = parameters.getValue(SiriusFingerIdParameters.rowIds);
    final ModularFeatureList flist = parameters.getValue(SiriusFingerIdParameters.flist)
        .getMatchingFeatureLists()[0];

    final List<FeatureListRow> rows = FeatureUtils.idStringToRows(flist, idStr);
    final JobWaiterTask jobWaiterTask;
    try (Sirius sirius = new Sirius()) {
      final Job job = sirius.runFingerId(rows);

      jobWaiterTask = new JobWaiterTask(this.getClass(), moduleCallDate, parameters,
          () -> sirius.api().jobs()
              .getJob(sirius.getProject().getProjectId(), job.getId(), List.of()),
          () -> sirius.importResultsForRows(rows));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    tasks.add(jobWaiterTask);
    return ExitCode.OK;
  }

  public void run(List<? extends FeatureListRow> rows) {
    final ArrayList<Task> tasks = new ArrayList<>();
    final SiriusFingerIdParameters parameters = SiriusFingerIdParameters.of(rows);
    runModule(ProjectService.getProject(), parameters, tasks, Instant.now());
    TaskService.getController().addTasks(tasks.toArray(Task[]::new));
  }
}
