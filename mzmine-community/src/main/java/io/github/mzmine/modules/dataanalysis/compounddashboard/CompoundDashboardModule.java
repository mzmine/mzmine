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

package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.CompoundGrouperModule;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class CompoundDashboardModule extends AbstractRunnableModule {

  public static final String NAME = "Compound dashboard";

  public CompoundDashboardModule() {
    super(NAME, CompoundDashboardParameters.class, MZmineModuleCategory.VISUALIZATIONFEATURELIST,
        "Visualizes compound lists and interactively links compound evidence.");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    final SimpleRunnableTask task = new SimpleRunnableTask(() -> {
      final @NotNull ModularFeatureList[] flists = parameters.getValue(
          CompoundDashboardParameters.flists).getMatchingFeatureLists();
      if (flists.length == 0) {
        return;
      }

      final ModularFeatureList fl = flists[0];
      if (fl.getCompoundList() != null) {
        MZmineCore.getDesktop().addTab(new CompoundDashboardTab(fl));
      } else {
        DialogLoggerUtil.showWarningNotification(
            "Run %s module.".formatted(CompoundGrouperModule.NAME), """
                No compound list found in selected feature list %s
                Run %s module first.""".formatted(fl.getName(), CompoundGrouperModule.NAME));
      }
    });
    tasks.add(task);
    return ExitCode.OK;
  }
}
