package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies the compound rows to drop from the
 * {@link io.github.mzmine.datamodel.features.compoundlist.CompoundList} by their compound ids
 * (comma separated). The underlying feature list rows are not touched — only the compound grouping
 * is removed. Compound ids are stable per feature list and survive project save/load.
 */
public class DeleteCompoundRowsParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final StringParameter compoundIds = new StringParameter("Compound IDs",
      "Compound row IDs to remove from the compound list, separated by ',' (comma). "
          + "Only the compound grouping is dropped; the underlying feature list rows are kept.");

  public DeleteCompoundRowsParameters() {
    super(flist, compoundIds);
  }

  public static @NotNull DeleteCompoundRowsParameters of(
      @NotNull final ModularFeatureList featureList,
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    final DeleteCompoundRowsParameters params = (DeleteCompoundRowsParameters) new DeleteCompoundRowsParameters().cloneParameterSet();
    params.setParameter(flist, new FeatureListsSelection(featureList));
    params.setParameter(compoundIds, joinCompoundIds(compounds));
    return params;
  }

  private static @NotNull String joinCompoundIds(
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    return compounds.stream().map(ModularCompoundRow::getCompoundId).map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
