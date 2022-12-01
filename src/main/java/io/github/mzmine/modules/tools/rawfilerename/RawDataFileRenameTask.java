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

package io.github.mzmine.modules.tools.rawfilerename;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawDataFileRenameTask extends AbstractTask {
  private final ParameterSet parameters;

  public RawDataFileRenameTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ParameterSet parameters) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Renaming raw data file.";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final RawDataFile[] matchingRawDataFiles = parameters.getParameter(
        RawDataFileRenameParameters.files).getValue().getMatchingRawDataFiles();
    final String newName = parameters.getParameter(RawDataFileRenameParameters.newName).getValue();

    if (matchingRawDataFiles.length == 0) {
      setStatus(TaskStatus.FINISHED);
    }

    RawDataFile file = matchingRawDataFiles[0];

    // set name is now threadsafe and will return the set name after checking for duplicates etc
    final String realName = file.setName(newName);
    parameters.setParameter(RawDataFileRenameParameters.newName, realName);

    file.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(RawDataFileRenameModule.class, parameters,
            getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);
  }
}
