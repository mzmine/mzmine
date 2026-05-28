package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Assigns one {@link CompoundMemberRole#REPRESENTATIVE} and a role per remaining member within a
 * connected component of feature list rows. Representative selection is delegated to a
 * pluggable {@link CompoundRepresentativeSelector}; per-member role classification (adduct,
 * isotopologue, in-source fragment, correlated) is handled here.
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

  private RoleAssigner() {
  }

  public static @NotNull List<CompoundFeatureMember> assignRoles(
      @NotNull final List<FeatureListRow> members, @NotNull final PolarityType polarity,
      @NotNull final MZTolerance mzTolerance, @NotNull final RTTolerance rtTolerance,
      @NotNull final CompoundRepresentativeSelector representativeSelector) {
    if (members.isEmpty()) {
      return List.of();
    }
    final FeatureListRow representative = representativeSelector.pickRepresentative(members);
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

  static boolean looksLikeIsotopologue(@NotNull final FeatureListRow member,
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
