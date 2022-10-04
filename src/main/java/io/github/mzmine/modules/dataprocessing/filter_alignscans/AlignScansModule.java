/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_alignscans;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class AlignScansModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Align scans (LC/MS or MS level 1)";
  private static final String MODULE_DESCRIPTION =
      "This module align scans for small fluctuations correlating consecutive scans.";

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  @Override
  public @NotNull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull
  String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    RawDataFile[] dataFiles =
        parameters.getParameter(new RawDataFilesParameter()).getValue().getMatchingRawDataFiles();

    // one storage for all files in the same module call
    MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    for (int i = 0; i < dataFiles.length; i++) {
      Task newTask = new AlignScansTask(project, dataFiles[i], parameters.cloneParameterSet(),
          storage, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATAFILTERING;
  }

  @Override
  public @NotNull
  Class<? extends ParameterSet> getParameterSetClass() {
    return AlignScansParameters.class;
  }

}
