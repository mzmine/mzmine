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

package io.github.mzmine.modules.impl;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A module that creates one task per RawDataFile
 */
public abstract class TaskPerRawDataFileModule extends AbstractProcessingModule {

  public TaskPerRawDataFileModule(@NotNull final String name, @NotNull final String description,
      @NotNull final MZmineModuleCategory moduleCategory, final boolean useMemoryMapping,
      @Nullable final Class<? extends ParameterSet> parameterSetClass) {
    super(name, parameterSetClass, description, moduleCategory, useMemoryMapping);
  }

  /**
   * Creates a task for each RawDataFile
   *
   * @param project        the mzmine project
   * @param parameters     the parameter set
   * @param moduleCallDate call date of the module
   * @param storage        memory mapping if active by {@link #useMemoryMapping}
   * @param raw            the raw data file extracted from a {@link RawDataFilesParameter}
   * @return a new task instance that will be added to the task controller
   */
  @NotNull
  public abstract Task createTask(final @NotNull MZmineProject project,
      final @NotNull ParameterSet parameters, final @NotNull Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage, @NotNull final RawDataFile raw);

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {

    // get parameters here only needed to run one task per RawDataFile
    var rawFilesParameter = parameters.streamForClass(RawDataFilesParameter.class).toList();
    int nRawFiles = rawFilesParameter.size();
    if (rawFilesParameter.isEmpty()) {
      MZmineCore.getDesktop().displayErrorMessage(
          "There is no RawDataFilesParameter (needs 1) in class " + getClass().getName());
      return ExitCode.ERROR;
    }
    if (nRawFiles > 1) {
      MZmineCore.getDesktop().displayErrorMessage(
          STR."There are too many (\{nRawFiles}) RawDataFilesParameter in class \{getClass().getName()}. Can only have 1. Coding error.");
      return ExitCode.ERROR;
    }
    // exactly one parameter for RawDataFiles found
    var rawFiles = parameters.getValue(rawFilesParameter.getFirst()).getMatchingRawDataFiles();

    // raw data processing modules memory map to mass list storage
    MemoryMapStorage storage = useMemoryMapping ? MemoryMapStorage.forMassList() : null;

    // create and start one task for each RawDataFile
    for (final var raw : rawFiles) {
      Task newTask = createTask(project, parameters, moduleCallDate, storage, raw);
      // task is added to TaskManager that schedules later
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }


}
