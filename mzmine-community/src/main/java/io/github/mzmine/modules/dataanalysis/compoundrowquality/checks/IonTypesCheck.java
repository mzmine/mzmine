package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
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
      return new DefaultQualityCheckResult(QualityCheckType.ION_TYPES, QualityCheckStatus.UNAVAILABLE,
          "No ion types annotated", List.of(), involved);
    }

    final String summary = distinct.size() + " adduct" + (distinct.size() == 1 ? "" : "s") + ": "
        + String.join(", ", distinct.keySet());
    final List<String> details = distinct.entrySet().stream().map(e -> {
      final Double mz = e.getValue().getAverageMZ();
      return "%s — m/z %.4f".formatted(e.getKey(), mz == null ? Double.NaN : mz);
    }).toList();
    return new DefaultQualityCheckResult(QualityCheckType.ION_TYPES, QualityCheckStatus.PASS, summary,
        details, involved);
  }
}
