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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies the merge target by compound id, the other compounds to consume by their compound ids
 * (comma separated), and any extra rows that should be pulled in by their feature list row ids
 * (comma separated). Either {@link #otherCompoundIds} or {@link #extraRowIds} may be empty, but not
 * both.
 */
public class MergeCompoundRowsParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final IntegerParameter targetCompoundId = new IntegerParameter("Target compound ID",
      "ID of the compound row that receives all merged members. Its representative is preserved.");

  public static final StringParameter otherCompoundIds = new StringParameter("Other compound IDs",
      "Compound IDs to merge into the target, separated by ',' (comma). Each listed compound is "
          + "removed from the compound list and its members are appended to the target. May be empty.",
      "", false);

  public static final StringParameter extraRowIds = new StringParameter("Extra feature row IDs",
      "Feature list row IDs (not whole compounds) to additionally pull into the target, separated "
          + "by ',' (comma). Any other compound that currently holds one of these rows loses it. "
          + "May be empty.", "", false);

  public MergeCompoundRowsParameters() {
    super(flist, targetCompoundId, otherCompoundIds, extraRowIds);
  }

  public static @NotNull MergeCompoundRowsParameters of(
      @NotNull final ModularFeatureList featureList, @NotNull final ModularCompoundRow target,
      @NotNull final Collection<ModularCompoundRow> otherCompounds,
      @NotNull final List<? extends FeatureListRow> extraRows) {
    final MergeCompoundRowsParameters params = (MergeCompoundRowsParameters) new MergeCompoundRowsParameters().cloneParameterSet();
    params.setParameter(flist, new FeatureListsSelection(featureList));
    params.setParameter(targetCompoundId, target.getCompoundId());
    params.setParameter(otherCompoundIds, joinCompoundIds(otherCompounds));
    params.setParameter(extraRowIds, FeatureListUtils.rowsToIdString(extraRows));
    return params;
  }

  private static @NotNull String joinCompoundIds(
      @NotNull final Collection<ModularCompoundRow> compounds) {
    return compounds.stream().map(ModularCompoundRow::getCompoundId).map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
