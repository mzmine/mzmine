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
import org.jetbrains.annotations.NotNull;

/**
 * Promotes a member row of a compound row to representative and demotes the previous representative
 * to correlated. Run as an {@link AbstractProcessingModule} so the manual choice is recorded as an
 * applied method on the feature list and survives project save/load.
 */
public class SetRepresentativeRowModule extends AbstractProcessingModule {

  public SetRepresentativeRowModule() {
    super("Set compound representative row", SetRepresentativeRowParameters.class,
        MZmineModuleCategory.FEATURE_GROUPING, """
            Promotes a member row of a compound row to be its representative. \
            The previous representative is demoted to a correlated member. \
            Used to manually adjust a compound row's preferred row after compound grouping.""");
  }

  /**
   * Apply directly on the FX thread. Records the change as a {@link SimpleFeatureListAppliedMethod}
   * on the feature list so it is saved with the project. Intended to be called from UI actions
   * (e.g. the feature table context menu) where the operation must complete synchronously so the UI
   * can refresh immediately.
   *
   * @return true if the representative actually changed
   */
  public static boolean apply(@NotNull final ModularFeatureList featureList,
      @NotNull final ModularCompoundRow compound,
      @NotNull final ModularFeatureListRow newRepresentative) {
    final boolean changed = CompoundRowUtils.setRepresentative(compound, newRepresentative);
    if (changed) {
      final SetRepresentativeRowParameters params = SetRepresentativeRowParameters.of(featureList,
          compound, newRepresentative);
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(SetRepresentativeRowModule.class, params,
              Instant.now()));
    }
    return changed;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    tasks.add(new SetRepresentativeRowTask(project, moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
