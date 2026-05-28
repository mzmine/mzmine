package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowUtils;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Replay form of {@link SplitCompoundRowModule}. Looks up the source compound and the rows to move
 * by id, then dispatches the mutation to the FX thread.
 */
public class SplitCompoundRowTask extends AbstractFeatureListTask {

  private final @NotNull ModularFeatureList flist;
  private final int sourceCompoundId;
  private final @NotNull String rowIdsToMove;

  protected SplitCompoundRowTask(@NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters) {
    super(null, moduleCallDate, parameters, SplitCompoundRowModule.class);
    this.flist = parameters.getValue(SplitCompoundRowParameters.flist).getMatchingFeatureLists()[0];
    this.sourceCompoundId = parameters.getValue(SplitCompoundRowParameters.sourceCompoundId);
    this.rowIdsToMove = parameters.getValue(SplitCompoundRowParameters.rowIdsToMove);
  }

  @Override
  protected void process() {
    final CompoundList cl = flist.getCompoundList();
    if (cl == null) {
      throw new IllegalStateException(
          "Feature list '%s' has no compound list — run compound grouping first.".formatted(
              flist.getName()));
    }
    final ModularCompoundRow source = cl.findRowByCompoundId(sourceCompoundId);
    if (source == null) {
      throw new IllegalStateException(
          "No compound with id %d in feature list '%s'.".formatted(sourceCompoundId,
              flist.getName()));
    }
    final List<FeatureListRow> resolved = FeatureListUtils.idStringToRows(flist, rowIdsToMove);
    final List<ModularFeatureListRow> rowsToMove = new ArrayList<>(resolved.size());
    for (final FeatureListRow r : resolved) {
      if (r instanceof ModularFeatureListRow mflr) {
        rowsToMove.add(mflr);
      }
    }
    if (rowsToMove.isEmpty()) {
      throw new IllegalStateException(
          "No rows matched ids '%s' in feature list '%s'.".formatted(rowIdsToMove,
              flist.getName()));
    }
    FxThread.runOnFxThreadAndWait(
        () -> CompoundRowUtils.splitIntoNewCompound(cl, source, rowsToMove));
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  public String getTaskDescription() {
    return "Splitting rows %s out of compound id=%d in feature list %s".formatted(rowIdsToMove,
        sourceCompoundId, flist.getName());
  }
}
