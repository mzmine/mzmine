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
import org.jetbrains.annotations.Nullable;

/**
 * Splits selected member rows out of an existing compound row into a brand new compound row. Run as
 * an {@link AbstractProcessingModule} so the manual split is saved with the project.
 */
public class SplitCompoundRowModule extends AbstractProcessingModule {

  public SplitCompoundRowModule() {
    super("Split compound row", SplitCompoundRowParameters.class,
        MZmineModuleCategory.FEATURE_GROUPING, """
            Removes selected member rows from a compound row and places them into a new compound \
            row. The new compound inherits the source compound's confidence and neutral mass. If \
            the source compound ends up empty it is dropped from the compound list.""");
  }

  /**
   * Apply directly on the FX thread. Records the change as an applied method on the feature list.
   *
   * @return the newly created compound row, or null if no rows were actually moved
   */
  public static @Nullable ModularCompoundRow apply(@NotNull final ModularFeatureList featureList,
      @NotNull final ModularCompoundRow source,
      @NotNull final List<? extends ModularFeatureListRow> rowsToMove) {
    if (rowsToMove.isEmpty()) {
      return null;
    }
    final ModularCompoundRow created = CompoundRowUtils.splitIntoNewCompound(
        source.getCompoundList(), source, rowsToMove);
    if (created != null) {
      final SplitCompoundRowParameters params = SplitCompoundRowParameters.of(featureList, source,
          rowsToMove);
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(SplitCompoundRowModule.class, params, Instant.now()));
    }
    return created;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    tasks.add(new SplitCompoundRowTask(project, moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
