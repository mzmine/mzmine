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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowUtils;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Merges one or more compound rows and/or extra member rows into a target compound row. Run as an
 * {@link AbstractProcessingModule} so the manual merge is saved with the project.
 */
public class MergeCompoundRowsModule extends AbstractProcessingModule {

  public MergeCompoundRowsModule() {
    super("Merge compound rows", MergeCompoundRowsParameters.class,
        MZmineModuleCategory.FEATURE_GROUPING, """
            Merges other compound rows and/or extra member rows into a single target compound row. \
            The target's representative is preserved; secondary representatives are demoted to \
            correlated. Compound rows that are fully consumed are removed from the compound list.""");
  }

  /**
   * Apply directly on the FX thread. Records the change as an applied method on the feature list.
   *
   * @return true if anything actually changed
   */
  public static boolean apply(@NotNull final ModularFeatureList featureList,
      @NotNull final ModularCompoundRow target,
      @NotNull final Collection<ModularCompoundRow> otherCompounds,
      @NotNull final List<? extends ModularFeatureListRow> extraRows) {
    if (otherCompounds.isEmpty() && extraRows.isEmpty()) {
      return false;
    }
    final boolean changed = CompoundRowUtils.mergeCompoundRows(target.getCompoundList(), target,
        otherCompounds, extraRows);
    if (changed) {
      final MergeCompoundRowsParameters params = MergeCompoundRowsParameters.of(featureList, target,
          otherCompounds, extraRows);
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(MergeCompoundRowsModule.class, params, Instant.now()));
    }
    return changed;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    tasks.add(new MergeCompoundRowsTask(project, moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
