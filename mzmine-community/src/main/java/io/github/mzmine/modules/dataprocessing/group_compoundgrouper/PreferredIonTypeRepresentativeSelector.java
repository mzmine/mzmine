package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Picks the representative row by inspecting {@link IonType} parts directly: a tier preference
 * chain (M+H / M-H first, then alternative adducts) is applied with intensity tie-break and an
 * ultimate fallback to the highest-intensity row.
 */
public final class PreferredIonTypeRepresentativeSelector implements
    CompoundRepresentativeSelector {

  // Reference parts ranked by tier preference (high → low). Each entry is matched structurally
  // via IonPart.equalsWithoutCount, so charge / mass / formula identify the part regardless of
  // count.
  // decision: M+H / M-H is the most reliable representative; M+ / M- (electron-only) ranks just
  // below; alkali adducts and chloride / formate fall after.
  public static final List<IonPart> TIER_ORDER = List.of( //
      IonParts.H,        // [M+H]+
      IonParts.H_MINUS,  // [M-H]-
      IonParts.M_PLUS,   // [M]+ (silent / electron-loss)
      IonParts.M_MINUS,  // [M]- (electron)
      IonParts.NA,       // [M+Na]+
      IonParts.CL,       // [M+Cl]-
      IonParts.K,        // [M+K]+
      IonParts.NH4,       // [M+NH4]+
      IonParts.FORMATE_FA // [M+FA-H]- (alone in negative single-adduct case if present)
  );

  @Override
  public @NotNull FeatureListRow pickRepresentative(@NotNull final List<FeatureListRow> members) {
    final Comparator<FeatureListRow> tierTieIntensity = Comparator.<FeatureListRow>comparingInt(
        r -> -tierScore(r)).thenComparing(
        Comparator.comparing(CompoundRepresentativeSelector::heightOrZero).reversed());
    FeatureListRow best = null;
    int bestTier = 0;
    for (final FeatureListRow row : members) {
      final int tier = tierScore(row);
      if (tier <= 0) {
        continue;
      }
      if (best == null || tier > bestTier || (tier == bestTier
          && tierTieIntensity.compare(row, best) < 0)) {
        best = row;
        bestTier = tier;
      }
    }
    if (best != null) {
      return best;
    }
    return CompoundRepresentativeSelector.pickHighestIntensity(members);
  }

  /**
   * Tier score for representative selection. Higher is better. 0 means not eligible at any tier.
   * <p>
   * Eligibility requires the IonType to be a "clean" single-adduct form: one molecule, no neutral
   * losses or clusters, exactly one charged adduct, defined parts.
   */
  static int tierScore(@NotNull final FeatureListRow row) {
    final IonIdentity ion = row.getBestIonIdentity();
    if (ion == null) {
      return 0;
    }
    final IonType ionType = ion.getIonType();
    if (ionType.molecules() != 1 || ionType.absTotalCharge() > 1 || ionType.isUndefinedAdduct()) {
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
    return tierIndex(adduct, TIER_ORDER);
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
}
