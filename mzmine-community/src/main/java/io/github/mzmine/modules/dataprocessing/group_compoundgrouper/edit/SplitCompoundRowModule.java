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
    tasks.add(new SplitCompoundRowTask(moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
