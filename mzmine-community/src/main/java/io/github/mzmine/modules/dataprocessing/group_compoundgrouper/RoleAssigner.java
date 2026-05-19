package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Assigns one {@link CompoundMemberRole#REPRESENTATIVE} and a role per remaining member within a
 * connected component of feature list rows.
 * <p>
 * Representative selection inspects {@link IonType} parts directly (no hard-coded adduct masses):
 * a tier preference chain (M+H / M-H first, then alternative adducts) is applied with intensity
 * tie-break and an ultimate fallback to the highest-intensity row.
 */
public final class RoleAssigner {

  // Common isotope mass differences (Δ from monoisotopic per single isotope step).
  private static final double[] ISOTOPE_DELTAS = {
      1.003355, // 13C
      0.997035, // 15N
      2.004244, // 18O
      1.995796, // 34S
      1.997050  // 37Cl
  };
  private static final double ISOTOPE_TOL = 0.005;
  private static final int MAX_ISOTOPE_MULTIPLE = 4;

  // Reference parts ranked by tier preference (high → low). Each entry is matched structurally
  // via IonPart.equalsWithoutCount, so charge / mass / formula identify the part regardless of
  // count.
  // decision: M+H / M-H is the most reliable representative; M+ / M- (electron-only) ranks just
  // below; alkali adducts and chloride / formate fall after.
  private static final List<IonPart> POSITIVE_TIER_ORDER = List.of(
      IonParts.H,        // [M+H]+
      IonParts.M_PLUS,   // [M]+ (silent / electron-loss)
      IonParts.NA,       // [M+Na]+
      IonParts.K,        // [M+K]+
      IonParts.NH4       // [M+NH4]+
  );
  private static final List<IonPart> NEGATIVE_TIER_ORDER = List.of(
      IonParts.H_MINUS,  // [M-H]-
      IonParts.M_MINUS,  // [M]- (electron)
      IonParts.CL,       // [M+Cl]-
      IonParts.FORMATE_FA // [M+FA-H]- (alone in negative single-adduct case if present)
  );

  private RoleAssigner() {
  }

  public static @NotNull List<CompoundFeatureMember> assignRoles(
      @NotNull final List<FeatureListRow> members, @NotNull final PolarityType polarity,
      @NotNull final MZTolerance mzTolerance, @NotNull final RTTolerance rtTolerance) {
    if (members.isEmpty()) {
      return List.of();
    }
    final FeatureListRow representative = pickRepresentative(members, polarity);
    final List<CompoundFeatureMember> result = new ArrayList<>(members.size());
    for (final FeatureListRow row : members) {
      if (row == representative) {
        result.add(new CompoundFeatureMember(row, CompoundMemberRole.REPRESENTATIVE, 1.0f));
      } else {
        final CompoundMemberRole role = classifyMember(row, representative, mzTolerance,
            rtTolerance);
        final float score = scoreRoleConfidence(role);
        result.add(new CompoundFeatureMember(row, role, score));
      }
    }
    return result;
  }

  static @NotNull FeatureListRow pickRepresentative(@NotNull final List<FeatureListRow> members,
      @NotNull final PolarityType polarity) {
    // try preferred adduct tiers first
    final Comparator<FeatureListRow> tierTieIntensity = Comparator
        .<FeatureListRow>comparingInt(r -> -tierScore(r, polarity))
        .thenComparing(Comparator.comparing(RoleAssigner::heightOrZero).reversed());
    FeatureListRow best = null;
    int bestTier = 0;
    for (final FeatureListRow row : members) {
      final int tier = tierScore(row, polarity);
      if (tier <= 0) {
        continue;
      }
      if (best == null || tier > bestTier
          || (tier == bestTier && tierTieIntensity.compare(row, best) < 0)) {
        best = row;
        bestTier = tier;
      }
    }
    if (best != null) {
      return best;
    }
    // ultimate fallback: highest-intensity row
    FeatureListRow fallback = members.get(0);
    float fallbackHeight = heightOrZero(fallback);
    for (int i = 1; i < members.size(); i++) {
      final FeatureListRow row = members.get(i);
      final float h = heightOrZero(row);
      if (h > fallbackHeight) {
        fallback = row;
        fallbackHeight = h;
      }
    }
    return fallback;
  }

  /**
   * Tier score for representative selection. Higher is better. 0 means not eligible at any tier.
   * <p>
   * Eligibility requires the IonType to be a "clean" single-adduct form: one molecule, no
   * neutral losses or clusters, exactly one charged adduct, defined parts.
   */
  static int tierScore(@NotNull final FeatureListRow row, @NotNull final PolarityType polarity) {
    final IonIdentity ion = row.getBestIonIdentity();
    if (ion == null) {
      return 0;
    }
    final IonType ionType = ion.getIonType();
    if (ionType.molecules() != 1 || ionType.absTotalCharge()>1 || ionType.isUndefinedAdduct()) {
      return 0;
    }
    // reject any IonType carrying neutral losses / clusters (those rows belong to IN_SOURCE_FRAGMENT)
    if (ionType.streamNeutralMods().findAny().isPresent()) {
      return 0;
    }
    final List<IonPart> chargedAdducts = ionType.streamChargedAdducts().toList();

    // negative-mode special case: [M-H2O-H]- has TWO parts (H2O loss is neutral, H- is charged).
    // We reach here only if no neutral mods exist, so this case must be handled before the
    // neutral-mods check above. decision: keep neutral-mods reject for the common "single charged
    // adduct" rule and detect M-H2O-H separately *before* it via isWaterLossMinusHydrogen.
    // Therefore, recheck this case here (we won't get to it through the strict path).
    if (chargedAdducts.size() != 1) {
      return 0;
    }
    final IonPart adduct = chargedAdducts.getFirst();
    return switch (polarity) {
      case POSITIVE -> tierIndex(adduct, POSITIVE_TIER_ORDER);
      case NEGATIVE -> tierIndex(adduct, NEGATIVE_TIER_ORDER);
      // unknown / mixed / neutral / any → any single charged adduct is acceptable
      case NEUTRAL, ANY, UNKNOWN -> 1;
    };
  }

  /**
   * Returns the tier score for a single charged adduct against the given preference list. The
   * highest-priority match yields {@code preferred.size()}; no match yields 0.
   */
  private static int tierIndex(@NotNull final IonPart adduct,
      @NotNull final List<IonPart> preferred) {
    for (int i = 0; i < preferred.size(); i++) {
      if (preferred.get(i).equalsWithoutCount(adduct)) {
        return preferred.size() - i;
      }
    }
    return 0;
  }

  // ----- Per-member classification -----

  static @NotNull CompoundMemberRole classifyMember(@NotNull final FeatureListRow member,
      @NotNull final FeatureListRow representative, @NotNull final MZTolerance mzTolerance,
      @NotNull final RTTolerance rtTolerance) {
    // ADDUCT: same IonNetwork as representative, IonType differs
    final IonIdentity memberIon = member.getBestIonIdentity();
    final IonIdentity repIon = representative.getBestIonIdentity();
    if (memberIon != null && repIon != null) {
      final IonNetwork repNet = repIon.getNetwork();
      if (repNet != null && repNet.containsKey(member)
          && !memberIon.getIonType().equals(repIon.getIonType())) {
        return CompoundMemberRole.ADDUCT;
      }
    }

    // ISOTOPOLOGUE: m/z delta matches a known isotope spacing AND RT matches
    if (looksLikeIsotopologue(member, representative, mzTolerance, rtTolerance)) {
      return CompoundMemberRole.ISOTOPOLOGUE;
    }

    // IN_SOURCE_FRAGMENT: member's IonType carries a neutral loss or cluster part
    if (memberIon != null) {
      final IonType ionType = memberIon.getIonType();
      final boolean hasNeutralLossOrCluster = ionType.stream()
          .anyMatch(p -> p.type() == IonPart.Type.IN_SOURCE_FRAGMENT
              || p.type() == IonPart.Type.CLUSTER);
      if (hasNeutralLossOrCluster) {
        return CompoundMemberRole.IN_SOURCE_FRAGMENT;
      }
    }

    // default: came from a correlation edge only
    return CompoundMemberRole.CORRELATED;
  }

  private static boolean looksLikeIsotopologue(@NotNull final FeatureListRow member,
      @NotNull final FeatureListRow representative, @NotNull final MZTolerance mzTolerance,
      @NotNull final RTTolerance rtTolerance) {
    final Double memberMz = member.getAverageMZ();
    final Double repMz = representative.getAverageMZ();
    if (memberMz == null || repMz == null) {
      return false;
    }
    final double delta = memberMz - repMz;
    if (delta <= 0) {
      return false; // isotopologues are heavier than the monoisotopic peak
    }
    final Float memberRt = member.getAverageRT();
    final Float repRt = representative.getAverageRT();
    if (memberRt == null || repRt == null) {
      return false;
    }
    if (!rtTolerance.checkWithinTolerance(memberRt, repRt)) {
      return false;
    }

    // accept multi-isotope steps (e.g., M+1, M+2, ...). Use combined m/z + dedicated isotope
    // tolerance to keep checks robust at low m/z.
    for (int n = 1; n <= MAX_ISOTOPE_MULTIPLE; n++) {
      for (final double iso : ISOTOPE_DELTAS) {
        final double expected = iso * n;
        // decision: combined check — pass if either tolerance accepts
        if (Math.abs(delta - expected) <= ISOTOPE_TOL
            || mzTolerance.checkWithinTolerance(repMz + expected, memberMz)) {
          return true;
        }
      }
    }
    return false;
  }

  private static float heightOrZero(@NotNull final FeatureListRow row) {
    final Float h = row.getMaxHeight();
    return h == null ? 0f : h;
  }

  private static float scoreRoleConfidence(@NotNull final CompoundMemberRole role) {
    return switch (role) {
      case REPRESENTATIVE -> 1.0f;
      case ADDUCT -> 0.9f;
      case ISOTOPOLOGUE -> 0.85f;
      case IN_SOURCE_FRAGMENT -> 0.7f;
      case CORRELATED -> 0.5f;
    };
  }
}
