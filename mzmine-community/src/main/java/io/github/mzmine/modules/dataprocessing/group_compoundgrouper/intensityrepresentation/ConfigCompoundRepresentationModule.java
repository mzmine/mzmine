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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRecommendation;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRule;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.CompoundGrouperModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ConfigCompoundRepresentationModule implements MZmineProcessingModule {

  public static final String NAME = "Configure compound representations";

  private static final String DESCRIPTION =
      "Configures how compound rows expose their per-raw-file intensity (area, height, "
          + "normalized variants). Either falls back to the representative row's feature, or "
          + "synthesizes new compound features by summing over all members or only members that "
          + "carry an Ion Identity. Run after Compound Grouping. Re-run any time to switch modes.";

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @NotNull List<@NotNull ModuleOrderRecommendation> getModuleOrderRecommendations() {
    return List.of(new ModuleOrderRecommendation(
        "Compound representations can only be configured after a compound list was created",
        ModuleOrderRule.mustRunAfter(CompoundGrouperModule.class)));
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURE_GROUPING;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ConfigCompoundRepresentationParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    final ModularFeatureList[] featureLists = parameters.getValue(
        ConfigCompoundRepresentationParameters.FEATURE_LISTS).getMatchingFeatureLists();
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (final ModularFeatureList flist : featureLists) {
      tasks.add(new ConfigCompoundRepresentationTask(flist, parameters, storage, moduleCallDate));
    }
    return ExitCode.OK;
  }
}
