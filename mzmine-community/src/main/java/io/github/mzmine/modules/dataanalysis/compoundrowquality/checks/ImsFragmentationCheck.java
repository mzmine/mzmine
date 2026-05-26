package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
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
    // Skip the check entirely for non-IMS feature lists. The interactor filters DOES_NOT_APPLY
    // results out so this check produces no card at all on non-mobility datasets.
    if (!featureList.hasRowType(FeatureShapeMobilogramType.class)) {
      return new DefaultQualityCheckResult(QualityCheckType.IMS_FRAGMENTATION,
          QualityCheckStatus.DOES_NOT_APPLY, "IMS check does not apply to non-IMS data", List.of(),
          List.of());
    }
    final Optional<R2RMap<RowsRelationship>> mapOpt = featureList.getRowMap(
        Type.MS1_MOBILITY_FEATURE_CORR);
    if (mapOpt.isEmpty() || mapOpt.get().isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.IMS_FRAGMENTATION,
          QualityCheckStatus.UNAVAILABLE, "No IMS mobility correlation map available", List.of(),
          List.of());
    }
    final R2RMap<RowsRelationship> mobilityCorrMap = mapOpt.get();

    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    final List<FragmentParents> fragmentEntries = new ArrayList<>();
    final Set<FeatureListRow> involvedFragments = new LinkedHashSet<>();
    final List<String> matchedLabels = new ArrayList<>();

    for (final CompoundFeatureMember candidate : members) {
      final FeatureListRow candidateRow = candidate.row();
      final Double candidateMz = candidateRow.getAverageMZ();
      if (candidateMz == null) {
        continue;
      }
      final List<FeatureListRow> parents = collectHigherMzMobilityCorrelatedParents(members,
          candidateRow, candidateMz, mobilityCorrMap);
      if (parents.isEmpty()) {
        continue;
      }
      parents.sort(FragmentParentsRendering.PARENT_ORDER);
      fragmentEntries.add(new FragmentParents(candidateRow, List.copyOf(parents)));
      involvedFragments.add(candidateRow);
      final IonIdentity ion = candidateRow.getBestIonIdentity();
      matchedLabels.add(ion != null ? ion.getIonType().toString() : "%.4f".formatted(candidateMz));
    }

    if (fragmentEntries.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.IMS_FRAGMENTATION,
          QualityCheckStatus.PASS, "No IMS fragments detected", List.of(), List.of());
    }

    final String summary = "%d possible IMS fragment%s after ion mobility separation".formatted(
        involvedFragments.size(), involvedFragments.size() == 1 ? "" : "s");

    // When the host (e.g. CompoundDashboardController) supplied a color assignment, render each
    // fragment with its parents as colored, clickable chips that mirror the dashboard coloring.
    // Without an assignment fall back to the plain text default so the pane keeps working
    // standalone.
    final ColorAssignment coloring = context.colorAssignment();
    if (coloring != null) {
      return new ImsFragmentationQualityResult(QualityCheckStatus.WARN, summary, fragmentEntries,
          coloring, context.onRowClick());
    }
    final List<String> details = List.of(
        "Mobility-correlated with higher-m/z member: " + String.join(", ", matchedLabels));
    return new DefaultQualityCheckResult(QualityCheckType.IMS_FRAGMENTATION,
        QualityCheckStatus.WARN, summary, details, List.copyOf(involvedFragments));
  }

  private static @NotNull List<@NotNull FeatureListRow> collectHigherMzMobilityCorrelatedParents(
      @NotNull List<CompoundFeatureMember> members, @NotNull FeatureListRow candidate,
      double candidateMz, @NotNull R2RMap<RowsRelationship> mobilityCorrMap) {
    final List<FeatureListRow> parents = new ArrayList<>();
    for (final CompoundFeatureMember other : members) {
      final FeatureListRow otherRow = other.row();
      if (otherRow == candidate) {
        continue;
      }
      final Double otherMz = otherRow.getAverageMZ();
      // other mz needs to be higher than +6 so that candidate is an actual fragment
      if (otherMz == null || otherMz <= candidateMz + 6) {
        continue;
      }
      if (mobilityCorrMap.contains(candidate, otherRow)) {
        parents.add(otherRow);
      }
    }
    return parents;
  }
}
