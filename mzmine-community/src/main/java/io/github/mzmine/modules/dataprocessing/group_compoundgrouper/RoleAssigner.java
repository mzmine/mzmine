package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonModificationType;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Assigns one {@link CompoundMemberRole#REPRESENTATIVE} and a role per remaining member within a
 * connected component of feature list rows.
 * <p>
 * Representative selection uses a polarity-aware preference chain (M+H / M-H first, then other
 * adducts) with intensity tie-break and an ultimate fallback to the highest-intensity row.
 */
public final class RoleAssigner {

  // Common adduct mass differences used for representative tier scoring
  private static final double MASS_PROTON = 1.007276;
  private static final double MASS_SODIUM = 22.989218;
  private static final double MASS_POTASSIUM = 38.963158;
  private static final double MASS_AMMONIUM = 18.034164;
  private static final double MASS_WATER_LOSS = -18.010565;
  private static final double MASS_CHLORIDE = 34.969402;
  private static final double MASS_FORMATE_LOSS_H = 44.998201;
  private static final double TIER_MASS_TOL = 0.005;

  // Common isotope mass differences (Δ from monoisotopic per single isotope step)
  private static final double[] ISOTOPE_DELTAS = {
      1.003355, // 13C
      0.997035, // 15N
      2.004244, // 18O
      1.995796, // 34S
      1.997050  // 37Cl
  };
  private static final double ISOTOPE_TOL = 0.005;
  private static final int MAX_ISOTOPE_MULTIPLE = 4;

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

  // ----- Representative selection -----

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
   * Tier score for representative selection. Higher is better. 0 means not eligible at tier.
   */
  static int tierScore(@NotNull final FeatureListRow row, @NotNull final PolarityType polarity) {
    final IonIdentity ion = row.getBestIonIdentity();
    if (ion == null) {
      return 0;
    }
    final IonType ionType = ion.getIonType();
    if (ionType == null || ionType.getMolecules() != 1 || ionType.isUndefinedAdduct()) {
      return 0;
    }
    if (ionType.getAdduct() == null
        || ionType.getAdduct().getType() != IonModificationType.ADDUCT) {
      return 0;
    }
    final double diff = ionType.getMassDifference();
    return switch (polarity) {
      case POSITIVE -> positiveTier(diff);
      case NEGATIVE -> negativeTier(diff);
      // unknown / mixed / neutral / any → any ADDUCT is acceptable
      case NEUTRAL, ANY, UNKNOWN -> 1;
    };
  }

  private static int positiveTier(final double massDiff) {
    if (Math.abs(massDiff - MASS_PROTON) <= TIER_MASS_TOL) {
      return 5; // M+H
    }
    if (Math.abs(massDiff) <= TIER_MASS_TOL) {
      return 4; // M+
    }
    if (Math.abs(massDiff - MASS_SODIUM) <= TIER_MASS_TOL) {
      return 3; // M+Na
    }
    if (Math.abs(massDiff - MASS_POTASSIUM) <= TIER_MASS_TOL) {
      return 2; // M+K
    }
    if (Math.abs(massDiff - MASS_AMMONIUM) <= TIER_MASS_TOL) {
      return 1; // M+NH4
    }
    return 0;
  }

  private static int negativeTier(final double massDiff) {
    if (Math.abs(massDiff + MASS_PROTON) <= TIER_MASS_TOL) {
      return 5; // M-H
    }
    if (Math.abs(massDiff) <= TIER_MASS_TOL) {
      return 4; // M-
    }
    // M-H2O-H ≈ -19.017841
    if (Math.abs(massDiff - (MASS_WATER_LOSS - MASS_PROTON)) <= TIER_MASS_TOL) {
      return 3;
    }
    if (Math.abs(massDiff - MASS_CHLORIDE) <= TIER_MASS_TOL) {
      return 2; // M+Cl
    }
    if (Math.abs(massDiff - MASS_FORMATE_LOSS_H) <= TIER_MASS_TOL) {
      return 1; // M+FA-H
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
          && !ionTypeEquals(memberIon.getIonType(), repIon.getIonType())) {
        return CompoundMemberRole.ADDUCT;
      }
    }

    // ISOTOPOLOGUE: m/z delta matches a known isotope spacing AND RT matches
    if (looksLikeIsotopologue(member, representative, mzTolerance, rtTolerance)) {
      return CompoundMemberRole.ISOTOPOLOGUE;
    }

    // IN_SOURCE_FRAGMENT: member's IonType is a CLUSTER or NEUTRAL_LOSS modification
    if (memberIon != null) {
      final IonType ionType = memberIon.getIonType();
      if (ionType != null && ionType.getAdduct() != null) {
        final IonModificationType type = ionType.getAdduct().getType();
        if (type == IonModificationType.CLUSTER || type == IonModificationType.NEUTRAL_LOSS) {
          return CompoundMemberRole.IN_SOURCE_FRAGMENT;
        }
      }
      // also check the modification side
      if (ionType != null && ionType.getModification() != null) {
        final IonModificationType modType = ionType.getModification().getType();
        if (modType == IonModificationType.CLUSTER || modType == IonModificationType.NEUTRAL_LOSS) {
          return CompoundMemberRole.IN_SOURCE_FRAGMENT;
        }
      }
    }

    // default: came from a RowGroup correlation only
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

  private static boolean ionTypeEquals(@Nullable final IonType a, @Nullable final IonType b) {
    if (a == null || b == null) {
      return a == b;
    }
    return Math.abs(a.getMassDifference() - b.getMassDifference()) <= TIER_MASS_TOL
        && a.getMolecules() == b.getMolecules() && a.getCharge() == b.getCharge();
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
