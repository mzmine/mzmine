package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.FeatureListUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies the compound row to split by its compound id and the rows to move off into a new
 * compound by their feature list row ids (comma separated). Ids are stable per feature list and
 * survive project save/load.
 */
public class SplitCompoundRowParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final IntegerParameter sourceCompoundId = new IntegerParameter("Source compound ID",
      "ID of the compound row to split. Its selected members are moved into a new compound row.");

  public static final StringParameter rowIdsToMove = new StringParameter("Member row IDs to move",
      "Feature list row IDs to extract from the source compound and place into a new compound row, "
          + "separated by ',' (comma). Every row must currently be a member of the source compound.");

  public SplitCompoundRowParameters() {
    super(flist, sourceCompoundId, rowIdsToMove);
  }

  public static @NotNull SplitCompoundRowParameters of(
      @NotNull final ModularFeatureList featureList, @NotNull final ModularCompoundRow source,
      @NotNull final List<? extends FeatureListRow> rowsToMove) {
    final SplitCompoundRowParameters params = (SplitCompoundRowParameters) new SplitCompoundRowParameters().cloneParameterSet();
    params.setParameter(flist, new FeatureListsSelection(featureList));
    params.setParameter(sourceCompoundId, source.getCompoundId());
    params.setParameter(rowIdsToMove, FeatureListUtils.rowsToIdString(rowsToMove));
    return params;
  }
}
