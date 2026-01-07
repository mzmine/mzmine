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

package io.github.mzmine.modules.impl;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A module that creates one task per feature list
 */
public abstract class TaskPerFeatureListModule extends AbstractProcessingModule {

  private final boolean requiresMemoryMapping;

  /**
   * @param name              name of the module in the menu and quick access
   * @param parameterSetClass the class of the parameters
   * @param moduleCategory    module category for quick access and batch mode
   * @param requiresMemoryMapping if true and if memory mapping is activated, task will get a memory
   *                              map storage
   * @param description       the description of the task
   */
  public TaskPerFeatureListModule(final @NotNull String name,
      final @Nullable Class<? extends ParameterSet> parameterSetClass,
      final @NotNull MZmineModuleCategory moduleCategory, final boolean requiresMemoryMapping,
      final @NotNull String description) {
    super(name, parameterSetClass, moduleCategory, description);
    this.requiresMemoryMapping = requiresMemoryMapping;
  }

  /**
   * Creates a task for each feature list
   *
   * @param project        the mzmine project
   * @param parameters     the parameter set
   * @param moduleCallDate call date of the module
   * @param storage        memory mapping if active in config
   * @param featureList    the feature list extracted from a {@link FeatureListsParameter}
   * @return a new task instance that will be added to the task controller
   */
  @NotNull
  public abstract Task createTask(final @NotNull MZmineProject project,
      final @NotNull ParameterSet parameters, final @NotNull Instant moduleCallDate,
      @Nullable final MemoryMapStorage storage, @NotNull final FeatureList featureList);

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    try {
      var featureLists = ParameterUtils.getMatchingFeatureListsFromParameter(parameters);

      MemoryMapStorage storage = requiresMemoryMapping ? MemoryMapStorage.forFeatureList() : null;

      // create and start one task for each feature list
      for (final FeatureList featureList : featureLists) {
        Task newTask = createTask(project, parameters, moduleCallDate, storage, featureList);
        // task is added to TaskManager that schedules later
        tasks.add(newTask);
      }

    } catch (IllegalStateException ex) {
      MZmineCore.getDesktop().displayErrorMessage(ex.getMessage());
      return ExitCode.ERROR;
    }

    return ExitCode.OK;
  }


}
