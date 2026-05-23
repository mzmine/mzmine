package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowUtils;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Replay form of {@link DeleteCompoundRowsModule}. Resolves the compound rows by id and dispatches
 * the removal to the FX thread.
 */
public class DeleteCompoundRowsTask extends AbstractFeatureListTask {

  private final @NotNull ModularFeatureList flist;
  private final @NotNull String compoundIds;

  protected DeleteCompoundRowsTask(@NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters) {
    super(null, moduleCallDate, parameters, DeleteCompoundRowsModule.class);
    this.flist = parameters.getValue(DeleteCompoundRowsParameters.flist)
        .getMatchingFeatureLists()[0];
    this.compoundIds = parameters.getValue(DeleteCompoundRowsParameters.compoundIds);
  }

  @Override
  protected void process() {
    final CompoundList cl = flist.getCompoundList();
    if (cl == null) {
      throw new IllegalStateException(
          "Feature list '%s' has no compound list — nothing to delete.".formatted(flist.getName()));
    }

    final List<ModularCompoundRow> resolved = new ArrayList<>();
    for (final String token : Arrays.stream(compoundIds.split(",")).map(String::strip)
        .filter(s -> !s.isEmpty()).toList()) {
      final int id;
      try {
        id = Integer.parseInt(token);
      } catch (NumberFormatException e) {
        throw new IllegalStateException(
            "Invalid compound id '%s' in 'Compound IDs'.".formatted(token));
      }
      final ModularCompoundRow cr = cl.findRowByCompoundId(id);
      if (cr == null) {
        throw new IllegalStateException(
            "No compound with id %d in feature list '%s'.".formatted(id, flist.getName()));
      }
      resolved.add(cr);
    }
    if (resolved.isEmpty()) {
      throw new IllegalStateException("No compound ids supplied.");
    }
    FxThread.runOnFxThreadAndWait(() -> CompoundRowUtils.removeCompoundRows(cl, resolved));
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  public String getTaskDescription() {
    return "Deleting compound rows %s from feature list %s.".formatted(compoundIds,
        flist.getName());
  }
}
