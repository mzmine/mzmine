package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.AnnotationAgreementCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.CompoundAnnotationMatchCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.ImsFragmentationCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.InSourceFragmentationCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.IonTypesCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.MainAdductCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.Ms2AvailableCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.RtStabilityCheck;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure compute logic: takes a {@link CompoundRow} plus thresholds and returns the list of
 * {@link QualityCheckResult}s. Stateless and safe to call off the FX thread. Each check is
 * implemented as a standalone {@link QualityCheck}; this class just orchestrates them in display
 * order.
 */
public class CompoundRowQualityInteractor {

  /// Display order matches {@link QualityCheckType} ordering.
  private final @NotNull List<@NotNull QualityCheck> checks = List.of(
      new CompoundAnnotationMatchCheck(), new AnnotationAgreementCheck(), new IonTypesCheck(),
      new MainAdductCheck(), new RtStabilityCheck(), new Ms2AvailableCheck(),
      new InSourceFragmentationCheck(),
      new ImsFragmentationCheck());

  public @NotNull List<QualityCheckResult> compute(@NotNull CompoundRow row,
      @NotNull RTTolerance rtTol, @NotNull MZTolerance mzTol, @NotNull MZTolerance ms2Tol,
      @Nullable SimpleColorPalette palette, @Nullable Consumer<@NotNull FeatureListRow> onRowClick,
      @Nullable Consumer<@NotNull QualityCheckEvent> onEvent) {
    // Clone with reset cycling counter so the assignment matches the dashboard's plot coloring
    // (which also starts from a fresh clone).
    final ColorAssignment colorAssignment =
        palette == null ? null : CompoundDashboardColoring.assign(row, palette.clone(true));
    final QualityCheckContext context = new QualityCheckContext(rtTol, mzTol, ms2Tol,
        colorAssignment, onRowClick, onEvent);
    final List<QualityCheckResult> out = new ArrayList<>(checks.size());
    for (final QualityCheck check : checks) {
      final QualityCheckResult result = check.evaluate(row, context);
      // Drop checks that don't apply to this dataset (e.g. IMS on a non-IMS feature list) so the
      // pane never renders a card for them.
      if (result.status() == QualityCheckStatus.DOES_NOT_APPLY) {
        continue;
      }
      out.add(result);
    }
    return out;
  }
}
