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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Replay form of {@link MergeCompoundRowsModule}. Resolves target compound, other compounds and
 * extra rows by id and dispatches the mutation to the FX thread.
 */
public class MergeCompoundRowsTask extends AbstractFeatureListTask {

  private final @NotNull ModularFeatureList flist;
  private final int targetCompoundId;
  private final @NotNull String otherCompoundIds;
  private final @NotNull String extraRowIds;

  protected MergeCompoundRowsTask(@NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters) {
    super(null, moduleCallDate, parameters, MergeCompoundRowsModule.class);
    this.flist = parameters.getValue(MergeCompoundRowsParameters.flist)
        .getMatchingFeatureLists()[0];
    this.targetCompoundId = parameters.getValue(MergeCompoundRowsParameters.targetCompoundId);
    this.otherCompoundIds = parameters.getValue(MergeCompoundRowsParameters.otherCompoundIds);
    this.extraRowIds = parameters.getValue(MergeCompoundRowsParameters.extraRowIds);
  }

  @Override
  protected void process() {
    final CompoundList cl = flist.getCompoundList();
    if (cl == null) {
      throw new IllegalStateException(
          "Feature list '%s' has no compound list — run compound grouping first.".formatted(
              flist.getName()));
    }
    final ModularCompoundRow target = cl.findRowByCompoundId(targetCompoundId);
    if (target == null) {
      throw new IllegalStateException(
          "No compound with id %d in feature list '%s'.".formatted(targetCompoundId,
              flist.getName()));
    }

    final List<ModularCompoundRow> otherCompounds = resolveCompoundIds(cl, otherCompoundIds);
    final List<ModularFeatureListRow> extraRows = resolveExtraRows(extraRowIds);

    if (otherCompounds.isEmpty() && extraRows.isEmpty()) {
      throw new IllegalStateException(
          "Both 'Other compound IDs' and 'Extra row IDs' are empty — nothing to merge.");
    }

    FxThread.runOnFxThreadAndWait(
        () -> CompoundRowUtils.mergeCompoundRows(cl, target, otherCompounds, extraRows));
  }

  private @NotNull List<ModularCompoundRow> resolveCompoundIds(@NotNull final CompoundList cl,
      @NotNull final String idsString) {
    if (idsString.isBlank()) {
      return List.of();
    }
    final List<ModularCompoundRow> resolved = new ArrayList<>();
    for (final String token : Arrays.stream(idsString.split(",")).map(String::strip)
        .filter(s -> !s.isEmpty()).toList()) {
      final int id;
      try {
        id = Integer.parseInt(token);
      } catch (NumberFormatException e) {
        throw new IllegalStateException(
            "Invalid compound id '%s' in 'Other compound IDs'.".formatted(token));
      }
      final ModularCompoundRow cr = cl.findRowByCompoundId(id);
      if (cr == null) {
        throw new IllegalStateException(
            "No compound with id %d in feature list '%s'.".formatted(id, flist.getName()));
      }
      resolved.add(cr);
    }
    return resolved;
  }

  private @NotNull List<ModularFeatureListRow> resolveExtraRows(@NotNull final String idsString) {
    if (idsString.isBlank()) {
      return List.of();
    }
    final List<FeatureListRow> rows = FeatureListUtils.idStringToRows(flist, idsString);
    return rows.stream().map(r -> r instanceof ModularFeatureListRow m ? m : null)
        .filter(Objects::nonNull).toList();
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  public String getTaskDescription() {
    return "Merging compounds [%s] and rows [%s] into compound id=%d in feature list %s".formatted(
        otherCompoundIds, extraRowIds, targetCompoundId, flist.getName());
  }
}
