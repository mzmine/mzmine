package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonType;
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

/// Checks that at least one of the compound's annotated adducts matches one of the most common
/// ions of the row's polarity (taken from {@link IonLibraries#MZMINE_DEFAULT_DUAL_POLARITY_SMALLEST}).
public final class MainAdductCheck implements QualityCheck {

  /// Library of the most common ions across both polarities. Filtered by polarity at evaluation time.
  private static final @NotNull IonLibrary MAIN_LIBRARY = IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_SMALLEST;

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.MAIN_ADDUCT_PRESENT;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    PolarityType polarity = PolarityType.NEUTRAL;
    final Map<IonType, FeatureListRow> ionsByType = new LinkedHashMap<>();
    final List<FeatureListRow> involved = new ArrayList<>();

    for (final CompoundFeatureMember m : row.getCompoundMembers()) {
      if (m.role() == CompoundMemberRole.ISOTOPOLOGUE) {
        continue;
      }
      final IonIdentity ion = m.row().getBestIonIdentity();
      if (ion == null) {
        continue;
      }
      final IonType ionType = ion.getIonType();
      ionsByType.putIfAbsent(ionType, m.row());
      involved.add(m.row());
      if (polarity == PolarityType.NEUTRAL) {
        polarity = ionType.getPolarity();
      }
    }

    if (ionsByType.isEmpty()) {
      return new QualityCheckResult(QualityCheckType.MAIN_ADDUCT_PRESENT,
          QualityCheckStatus.UNAVAILABLE, "No ion types annotated", List.of(), involved);
    }

    if (!PolarityType.isDefined(polarity)) {
      return new QualityCheckResult(QualityCheckType.MAIN_ADDUCT_PRESENT,
          QualityCheckStatus.UNAVAILABLE, "Unknown polarity — cannot determine main adduct",
          List.of(), involved);
    }

    final List<IonType> targetIons = MAIN_LIBRARY.filterPolarity(polarity).ions();

    IonType hit = null;
    for (final IonType candidate : ionsByType.keySet()) {
      for (final IonType target : targetIons) {
        if (candidate.equals(target)) {
          hit = candidate;
          break;
        }
      }
      if (hit != null) {
        break;
      }
    }

    if (hit != null) {
      return new QualityCheckResult(QualityCheckType.MAIN_ADDUCT_PRESENT, QualityCheckStatus.PASS,
          "%s detected".formatted(hit), List.of("Polarity: %s".formatted(polarity)), involved);
    }

    final String expected = String.join(", ",
        targetIons.stream().map(IonType::toString).toList());
    final String detected = String.join(", ",
        ionsByType.keySet().stream().map(IonType::toString).toList());
    return new QualityCheckResult(QualityCheckType.MAIN_ADDUCT_PRESENT, QualityCheckStatus.FAIL,
        "Missing main %s adduct".formatted(polarity.toString().toLowerCase()),
        List.of("Expected one of: " + expected, "Detected: " + detected), involved);
  }
}
