package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Single quality-check outcome for a CompoundRow.
 *
 * @param type          which check this is
 * @param status        pass / warn / fail / unavailable
 * @param summary       short text shown in collapsed view (e.g. "4 adducts: M+H, M+Na, M+NH4")
 * @param detailLines   longer per-bullet detail lines shown when expanded
 * @param involvedRows  member rows that drive this result (for future click-to-navigate)
 */
public record QualityCheckResult(
    @NotNull QualityCheckType type,
    @NotNull QualityCheckStatus status,
    @NotNull String summary,
    @NotNull List<@NotNull String> detailLines,
    @NotNull List<@NotNull FeatureListRow> involvedRows) {

  public QualityCheckResult {
    detailLines = List.copyOf(detailLines);
    involvedRows = List.copyOf(involvedRows);
  }

  public static @NotNull QualityCheckResult of(@NotNull QualityCheckType type,
      @NotNull QualityCheckStatus status, @NotNull String summary) {
    return new QualityCheckResult(type, status, summary, List.of(), List.of());
  }
}
