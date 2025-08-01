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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.rawfilemethod;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.AbstractRtCorrectionFunction;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.ScanRtCorrectionModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplyRtCorrectionToRawFileModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getDescription() {
    return """
        This module applies retention time corrections calculated by the %s module to raw data files.
        This module ensures reproducibility in project files.""".formatted(
        ScanRtCorrectionModule.MODULE_NAME);
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new ApplyRtCorrectionToRawFileTask(null, moduleCallDate, parameters,
        ApplyRtCorrectionToRawFileModule.class));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.NORMALIZATION;
  }

  @Override
  public @NotNull String getName() {
    return "RTCorr (for project load only)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ApplyRtCorrectionToRawFileParameters.class;
  }

  public static void applyOnThisThread(List<AbstractRtCorrectionFunction> calis) {
    final ApplyRtCorrectionToRawFileParameters param = ApplyRtCorrectionToRawFileParameters.create(
        calis);

    final ApplyRtCorrectionToRawFileTask task = new ApplyRtCorrectionToRawFileTask(null,
        Instant.now(), param, ApplyRtCorrectionToRawFileModule.class);
    task.run();
  }
}
