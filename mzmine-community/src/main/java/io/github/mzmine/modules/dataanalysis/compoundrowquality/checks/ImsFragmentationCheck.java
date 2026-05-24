package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/// Flags member rows that share ion-mobility shape correlation
/// ({@link Type#MS1_MOBILITY_FEATURE_CORR}) with a higher-m/z member of the same compound,
/// suggesting the lower-m/z member is a fragment that survived ion-mobility separation with the
/// same mobility as the precursor.
public final class ImsFragmentationCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.IMS_FRAGMENTATION;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final FeatureList featureList = row.getCompoundList().getFeatureList();
    final Optional<R2RMap<RowsRelationship>> mapOpt = featureList.getRowMap(
        Type.MS1_MOBILITY_FEATURE_CORR);
    if (mapOpt.isEmpty() || mapOpt.get().isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.IMS_FRAGMENTATION,
          QualityCheckStatus.UNAVAILABLE, "No IMS mobility correlation map available", List.of(),
          List.of());
    }
    final R2RMap<RowsRelationship> mobilityCorrMap = mapOpt.get();

    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    final List<String> matched = new ArrayList<>();
    final Set<FeatureListRow> involved = new LinkedHashSet<>();

    for (final CompoundFeatureMember candidate : members) {
      final FeatureListRow candidateRow = candidate.row();
      final Double candidateMz = candidateRow.getAverageMZ();
      if (candidateMz == null) {
        continue;
      }
      if (anyHigherMzMobilityCorrelated(members, candidateRow, candidateMz, mobilityCorrMap)) {
        final IonIdentity ion = candidateRow.getBestIonIdentity();
        final String label =
            ion != null ? ion.getIonType().toString() : "m/z %.4f".formatted(candidateMz);
        matched.add(label);
        involved.add(candidateRow);
      }
    }

    if (matched.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.IMS_FRAGMENTATION,
          QualityCheckStatus.PASS, "No IMS fragments detected", List.of(), List.of());
    }

    final List<String> details = List.of("Mobility-correlated with higher-m/z member: " //
        + String.join(", ", matched));
    return new DefaultQualityCheckResult(QualityCheckType.IMS_FRAGMENTATION,
        QualityCheckStatus.WARN, "%d possible IMS fragment%s after ion mobility separation" //
        .formatted(matched.size(), matched.size() == 1 ? "" : "s"), details, List.copyOf(involved));
  }

  private static boolean anyHigherMzMobilityCorrelated(@NotNull List<CompoundFeatureMember> members,
      @NotNull FeatureListRow candidate, double candidateMz,
      @NotNull R2RMap<RowsRelationship> mobilityCorrMap) {
    for (final CompoundFeatureMember other : members) {
      final FeatureListRow otherRow = other.row();
      if (otherRow == candidate) {
        continue;
      }
      final Double otherMz = otherRow.getAverageMZ();
      if (otherMz == null || otherMz <= candidateMz) {
        continue;
      }
      if (mobilityCorrMap.contains(candidate, otherRow)) {
        return true;
      }
    }
    return false;
  }
}
