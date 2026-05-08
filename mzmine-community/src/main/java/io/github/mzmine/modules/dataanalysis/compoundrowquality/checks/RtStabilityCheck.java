package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/// Checks that all member rows fall within an RT tolerance window.
public final class RtStabilityCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.RT_STABILITY;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    final RTTolerance rtTol = context.rtTolerance();

    if (members.size() < 2) {
      return new QualityCheckResult(QualityCheckType.RT_STABILITY, QualityCheckStatus.UNAVAILABLE,
          "Need at least 2 members to assess RT stability", List.of(),
          members.stream().map(CompoundFeatureMember::row).toList());
    }

    float minRt = Float.POSITIVE_INFINITY;
    float maxRt = Float.NEGATIVE_INFINITY;
    final List<FeatureListRow> involved = new ArrayList<>(members.size());
    for (final CompoundFeatureMember m : members) {
      final Float rt = m.row().getAverageRT();
      if (rt == null) {
        continue;
      }
      involved.add(m.row());
      if (rt < minRt) {
        minRt = rt;
      }
      if (rt > maxRt) {
        maxRt = rt;
      }
    }

    if (minRt == Float.POSITIVE_INFINITY) {
      return new QualityCheckResult(QualityCheckType.RT_STABILITY, QualityCheckStatus.UNAVAILABLE,
          "No RT values available", List.of(), involved);
    }

    final float delta = maxRt - minRt;
    final boolean ok = rtTol.checkWithinTolerance(minRt, maxRt);
    final String summary = "ΔRT = %.3f min across %d feature%s".formatted(delta, involved.size(),
        involved.size() == 1 ? "" : "s");
    final List<String> details = List.of("Min RT: %.3f min".formatted(minRt),
        "Max RT: %.3f min".formatted(maxRt), "Tolerance: %s".formatted(rtTol.toString()));
    return new QualityCheckResult(QualityCheckType.RT_STABILITY,
        ok ? QualityCheckStatus.PASS : QualityCheckStatus.WARN, summary, details, involved);
  }
}
