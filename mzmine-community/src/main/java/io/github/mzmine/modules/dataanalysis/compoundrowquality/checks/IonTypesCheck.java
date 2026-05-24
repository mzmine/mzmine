package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/// Lists the distinct ion types observed across the compound's non-isotope members.
public final class IonTypesCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.ION_TYPES;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    // First-seen representative row per distinct ion-type label; insertion order preserved so the
    // chips render in the same order ions first appeared on the compound.
    final Map<String, FeatureListRow> distinct = new LinkedHashMap<>();
    final List<FeatureListRow> involved = new ArrayList<>();

    for (final CompoundFeatureMember member : members) {
      if (member.role() == CompoundMemberRole.ISOTOPOLOGUE) {
        continue;
      }
      final IonIdentity ion = member.row().getBestIonIdentity();
      if (ion == null) {
        continue;
      }
      final String key = ion.getIonType().toString();
      distinct.putIfAbsent(key, member.row());
      involved.add(member.row());
    }

    if (distinct.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.ION_TYPES,
          QualityCheckStatus.UNAVAILABLE, "No ion types annotated", List.of(), involved);
    }

    final String summary =
        distinct.size() + " adduct" + (distinct.size() == 1 ? "" : "s") + ": " + String.join(", ",
            distinct.keySet());
    final List<FeatureListRow> distinctRows = List.copyOf(distinct.values());

    // When the host (e.g. CompoundDashboardController) supplied a color assignment, render the
    // ion list as colored, clickable chips that mirror the dashboard coloring. Without an
    // assignment fall back to the plain-text default so the pane keeps working standalone.
    final ColorAssignment coloring = context.colorAssignment();
    if (coloring != null) {
      return new IonTypesQualityResult(QualityCheckStatus.PASS, summary, distinctRows, involved,
          coloring, context.onRowClick());
    }
    // Plain-text fallback: one detail line per ion, "ion m/z value" (no em-dash separator).
    final List<String> details = distinct.entrySet().stream().map(e -> {
      final Double mz = e.getValue().getAverageMZ();
      return "%s m/z %.4f".formatted(e.getKey(), mz == null ? Double.NaN : mz);
    }).toList();
    return new DefaultQualityCheckResult(QualityCheckType.ION_TYPES, QualityCheckStatus.PASS,
        summary, details, involved);
  }
}
