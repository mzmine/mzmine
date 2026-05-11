package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
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
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/// Flags member rows whose precursor m/z appears as a fragment in the MS2 of a higher-m/z member,
/// suggesting that the member is an in-source fragment of the larger molecule.
public final class InSourceFragmentationCheck implements QualityCheck {

  /// Minimum m/z difference between the parent (MS2 precursor) and the candidate fragment row.
  /// Anything within this window is considered too close in mass to confidently call an in-source
  /// fragmentation event.
  private static final double MIN_PARENT_MZ_OFFSET = 10d;

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.IN_SOURCE_FRAGMENTATION;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    final MZTolerance ms2Tol = context.ms2Tolerance();

    // members already explicitly tagged as in-source — count those unconditionally
    final List<FeatureListRow> tagged = new ArrayList<>();
    for (final CompoundFeatureMember m : members) {
      if (m.role() == CompoundMemberRole.IN_SOURCE_FRAGMENT) {
        tagged.add(m.row());
      }
    }

    // For each candidate member: check if any other member with greater m/z (by at least
    // MIN_PARENT_MZ_OFFSET) has an MS2 spectrum that contains the candidate's precursor m/z.
    final List<String> matched = new ArrayList<>();
    final Set<FeatureListRow> involved = new LinkedHashSet<>(tagged);

    for (final CompoundFeatureMember candidate : members) {
      if (candidate.role() == CompoundMemberRole.REPRESENTATIVE
          || candidate.role() == CompoundMemberRole.ISOTOPOLOGUE
          || candidate.role() == CompoundMemberRole.IN_SOURCE_FRAGMENT) {
        continue;
      }
      final FeatureListRow candidateRow = candidate.row();
      final Double candidateMz = candidateRow.getAverageMZ();
      if (candidateMz == null) {
        continue;
      }

      if (anyHigherMzMs2HitForMz(members, candidateRow, candidateMz, ms2Tol)) {
        final IonIdentity ion = candidateRow.getBestIonIdentity();
        final String label =
            ion != null ? ion.getIonType().toString() : "m/z %.4f".formatted(candidateMz);
        matched.add(label);
        involved.add(candidateRow);
      }
    }

    if (tagged.isEmpty() && matched.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.IN_SOURCE_FRAGMENTATION,
          QualityCheckStatus.PASS, "No in-source fragments detected", List.of(),
          List.copyOf(involved));
    }

    final List<String> details = new ArrayList<>();
    if (!tagged.isEmpty()) {
      details.add("%d member%s already tagged as in-source".formatted(tagged.size(),
          tagged.size() == 1 ? "" : "s"));
    }
    if (!matched.isEmpty()) {
      details.add("MS2 hits: " + String.join(", ", matched));
    }
    final int total = tagged.size() + matched.size();
    return new DefaultQualityCheckResult(QualityCheckType.IN_SOURCE_FRAGMENTATION,
        QualityCheckStatus.WARN,
        "%d possible in-source fragment%s".formatted(total, total == 1 ? "" : "s"), details,
        List.copyOf(involved));
  }

  private static boolean anyHigherMzMs2HitForMz(@NotNull List<CompoundFeatureMember> members,
      @NotNull FeatureListRow candidate, double candidateMz, @NotNull MZTolerance ms2Tol) {
    for (final CompoundFeatureMember other : members) {
      if (other.row() == candidate) {
        continue;
      }
      final Double otherMz = other.row().getAverageMZ();
      if (otherMz == null) {
        continue;
      }
      // require the other row to be at least MIN_PARENT_MZ_OFFSET above the candidate
      if (otherMz < candidateMz + MIN_PARENT_MZ_OFFSET) {
        continue;
      }
      for (final Scan scan : other.row().getAllFragmentScans()) {
        final MassList ml = scan.getMassList();
        if (ml == null) {
          continue;
        }
        if (massListContains(ml, candidateMz, ms2Tol)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean massListContains(@NotNull MassList ml, double targetMz,
      @NotNull MZTolerance tol) {
    final int n = ml.getNumberOfDataPoints();
    if (n == 0) {
      return false;
    }
    final int closestIdx = ml.binarySearch(targetMz, DefaultTo.CLOSEST_VALUE);
    if (closestIdx < 0) {
      return false;
    }
    return tol.checkWithinTolerance(ml.getMzValue(closestIdx), targetMz);
  }
}
