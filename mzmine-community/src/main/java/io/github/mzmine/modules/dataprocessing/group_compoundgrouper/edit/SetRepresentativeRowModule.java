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
    tasks.add(new SetRepresentativeRowTask(moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
