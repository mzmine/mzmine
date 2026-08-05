package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/// One fragment row paired with the parent rows it shares fragment-precursor evidence with — e.g.
/// MS1 mobility correlation for {@link ImsFragmentationCheck} or an MS2 spectral peak match for
/// {@link InSourceFragmentationCheck}. Parents are stored pre-sorted in display order
/// (ion-identified rows first, then by ascending m/z; see
/// {@link FragmentParentsRendering#PARENT_ORDER}).
record FragmentParents(@NotNull FeatureListRow fragment,
                       @NotNull List<@NotNull FeatureListRow> parents) {

}
