package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies a compound row by its compound id and the new representative member by its feature
 * list row id. Both ids are stable within a single feature list, so the operation is replayable
 * after project save/load as long as no upstream step renumbers rows.
 */
public class SetRepresentativeRowParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final IntegerParameter compoundId = new IntegerParameter("Compound ID",
      "ID of the compound row whose representative is being changed.");

  public static final IntegerParameter representativeRowId = new IntegerParameter(
      "Representative feature row ID",
      "ID of the feature list row (within the same feature list) to promote to representative of the compound. "
          + "The row must already be a member of the compound.");

  public SetRepresentativeRowParameters() {
    super(flist, compoundId, representativeRowId);
  }

  public static @NotNull SetRepresentativeRowParameters of(
      @NotNull final ModularFeatureList featureList, @NotNull final ModularCompoundRow compound,
      @NotNull final FeatureListRow newRepresentative) {
    final SetRepresentativeRowParameters params = (SetRepresentativeRowParameters) new SetRepresentativeRowParameters().cloneParameterSet();
    params.setParameter(flist, new FeatureListsSelection(featureList));
    params.setParameter(compoundId, compound.getCompoundId());
    params.setParameter(representativeRowId, newRepresentative.getID());
    return params;
  }
}
