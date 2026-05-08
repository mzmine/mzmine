package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.AnnotationAgreementCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.InSourceFragmentationCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.IonTypesCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.MainAdductCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.Ms2AvailableCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.RtStabilityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.SpectralLibraryMatchCheck;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Pure compute logic: takes a {@link CompoundRow} plus thresholds and returns the list of
 * {@link QualityCheckResult}s. Stateless and safe to call off the FX thread. Each check is
 * implemented as a standalone {@link QualityCheck}; this class just orchestrates them in display
 * order.
 */
public class CompoundRowQualityInteractor {

  /// Display order matches {@link QualityCheckType} ordering.
  private final @NotNull List<@NotNull QualityCheck> checks = List.of(new IonTypesCheck(),
      new RtStabilityCheck(), new AnnotationAgreementCheck(), new MainAdductCheck(),
      new Ms2AvailableCheck(), new SpectralLibraryMatchCheck(), new InSourceFragmentationCheck());

  public @NotNull List<QualityCheckResult> compute(@NotNull CompoundRow row,
      @NotNull RTTolerance rtTol, @NotNull MZTolerance mzTol, @NotNull MZTolerance ms2Tol) {
    final QualityCheckContext context = new QualityCheckContext(rtTol, mzTol, ms2Tol);
    final List<QualityCheckResult> out = new ArrayList<>(checks.size());
    for (final QualityCheck check : checks) {
      out.add(check.evaluate(row, context));
    }
    return out;
  }
}
