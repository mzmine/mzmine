package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import static io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector.highestMatching;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Picks the representative row by annotation richness, then by intensity:
 * <ol>
 *   <li>highest-intensity row with {@link FeatureListRow#isIdentified()} == true (compound /
 *       spectral library / lipid / manual / formula annotation present)</li>
 *   <li>highest-intensity row carrying any {@link io.github.mzmine.datamodel.identities.iontype.IonIdentity}</li>
 *   <li>highest-intensity row overall</li>
 * </ol>
 * Polarity is ignored.
 */
public final class AnnotatedFirstRepresentativeSelector implements CompoundRepresentativeSelector {

  @Override
  public @NotNull FeatureListRow pickRepresentative(@NotNull final List<FeatureListRow> members) {
    final FeatureListRow identified = highestMatching(members, FeatureListRow::isIdentified);
    if (identified != null) {
      return identified;
    }
    final FeatureListRow withIon = highestMatching(members, r -> r.getBestIonIdentity() != null);
    if (withIon != null) {
      return withIon;
    }
    return CompoundRepresentativeSelector.pickHighestIntensity(members);
  }

}
