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

package io.github.mzmine.modules.dataprocessing.featdet_maldispotfeaturedetection;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MaldiSpotFeatureDetectionModule implements MZmineProcessingModule {

  @Override
  public @NotNull String getName() {
    return "MALDI spot feature detection";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return MaldiSpotFeatureDetectionParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Detects features in MALDI spots. Only works for native timsTOF fleX data.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    final RawDataFile[] files = parameters.getParameter(MaldiSpotFeatureDetectionParameters.files)
        .getValue().getMatchingRawDataFiles();
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();
    for (RawDataFile file : files) {
      if (file instanceof ImagingRawDataFile imsImagingRawDataFile) {
        tasks.add(new MaldiSpotFeatureDetectionTask(storage, moduleCallDate, parameters, project,
            imsImagingRawDataFile));
      }
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }
}
