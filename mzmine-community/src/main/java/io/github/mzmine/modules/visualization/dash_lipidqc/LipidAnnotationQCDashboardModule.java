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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * MZmine module that opens the lipid annotation quality control dashboard for inspecting and
 * reviewing lipid annotations in a selected feature list.
 */
public class LipidAnnotationQCDashboardModule extends AbstractRunnableModule {

  public LipidAnnotationQCDashboardModule() {
    super("Lipid dashboard", LipidAnnotationQCDashboardParameters.class,
        MZmineModuleCategory.VISUALIZATIONFEATURELIST,
        "Inspect lipid annotation quality for selected feature list rows.");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    final SimpleRunnableTask task = new SimpleRunnableTask(() -> {
      final LipidAnnotationQCDashboardTab tab = new LipidAnnotationQCDashboardTab();
      tab.onFeatureListSelectionChanged(Arrays.asList(
          parameters.getValue(LipidAnnotationQCDashboardParameters.flists)
              .getMatchingFeatureLists()));
      ((MZmineGUI) DesktopService.getDesktop()).addTab(tab);
    });
    tasks.add(task);
    return ExitCode.OK;
  }
}

