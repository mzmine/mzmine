package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
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
 * Removes compound rows from a feature list's {@link CompoundList} so that the affected member rows
 * are no longer grouped. The underlying feature list rows are kept intact — use
 * {@link io.github.mzmine.modules.dataprocessing.filter_deleterows.DeleteRowsModule} to delete
 * feature rows. Run as an {@link AbstractProcessingModule} so the manual deletion is recorded as an
 * applied method and survives project save/load.
 */
public class DeleteCompoundRowsModule extends AbstractProcessingModule {

  public DeleteCompoundRowsModule() {
    super("Delete compound rows", DeleteCompoundRowsParameters.class,
        MZmineModuleCategory.FEATURE_GROUPING, """
            Removes compound rows (by id) from the compound list of a feature list. \
            Only the compound grouping is dropped — the underlying feature list rows are kept. \
            Use 'Delete rows' to delete actual feature list rows.""");
  }

  /**
   * Apply directly on the FX thread. Records the change as a {@link SimpleFeatureListAppliedMethod}
   * on the feature list so it is saved with the project. Compound rows that are not present at the
   * top level of the compound list are silently skipped.
   *
   * @return true if at least one compound row was removed
   */
  public static boolean apply(@NotNull final ModularFeatureList featureList,
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    if (compounds.isEmpty()) {
      return false;
    }
    final CompoundList cl = featureList.getCompoundList();
    if (cl == null) {
      return false;
    }
    @SuppressWarnings({"unchecked",
        "rawtypes"}) final boolean changed = CompoundRowUtils.removeCompoundRows(cl,
        (Collection<ModularCompoundRow>) (Collection) compounds);
    if (changed) {
      final DeleteCompoundRowsParameters params = DeleteCompoundRowsParameters.of(featureList,
          compounds);
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(DeleteCompoundRowsModule.class, params,
              Instant.now()));
    }
    return changed;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    tasks.add(new DeleteCompoundRowsTask(moduleCallDate, parameters));
    return ExitCode.OK;
  }
}
