package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/// Reports how many of the compound's member rows have at least one MS2 fragment scan.
public final class Ms2AvailableCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.MS2_AVAILABLE;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    int withMs2 = 0;
    int totalScans = 0;
    final List<FeatureListRow> involved = new ArrayList<>();
    for (final CompoundFeatureMember m : members) {
      final List<Scan> scans = m.row().getAllFragmentScans();
      if (!scans.isEmpty()) {
        withMs2++;
        totalScans += scans.size();
        involved.add(m.row());
      }
    }

    final int totalMembers = members.size();
    final double pct = totalMembers == 0 ? 0d : (100d * withMs2 / totalMembers);

    if (withMs2 == 0) {
      return new DefaultQualityCheckResult(QualityCheckType.MS2_AVAILABLE, QualityCheckStatus.WARN,
          "No MS2 scans across %d member%s".formatted(totalMembers,
              totalMembers == 1 ? "" : "s"), List.of(), List.of());
    }
    final String summary = "%d MS2 scan%s across %d/%d member%s (%.0f%%)".formatted(totalScans,
        totalScans == 1 ? "" : "s", withMs2, totalMembers, totalMembers == 1 ? "" : "s", pct);
    return new DefaultQualityCheckResult(QualityCheckType.MS2_AVAILABLE, QualityCheckStatus.PASS, summary,
        List.of(), involved);
  }
}
