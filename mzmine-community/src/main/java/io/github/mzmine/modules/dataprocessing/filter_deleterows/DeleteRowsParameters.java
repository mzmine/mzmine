/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_deleterows;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.FeatureListUtils;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class DeleteRowsParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flist = new FeatureListsParameter(1, 1);

  public static final StringParameter rowIds = new StringParameter("Row IDs",
      "Row ID from the specific feature list, separated by ',' (comma).");

  public static final StringParameter compoundIds = new StringParameter("Compound IDs",
      "Compound row IDs to remove from the compound list, separated by ',' (comma). "
          + "Only the compound grouping is dropped; the underlying feature list rows are kept.", "",
      false);

  public DeleteRowsParameters() {
    super(flist, rowIds, compoundIds);
  }

  public static @NotNull DeleteRowsParameters of(@NotNull final FeatureList featureList,
      @NotNull final List<? extends FeatureListRow> rows) {
    final DeleteRowsParameters parameterSet = (DeleteRowsParameters) new DeleteRowsParameters().cloneParameterSet();
    parameterSet.setParameter(DeleteRowsParameters.flist,
        new FeatureListsSelection((ModularFeatureList) featureList));
    parameterSet.setParameter(rowIds, FeatureListUtils.rowsToIdString(rows));
    parameterSet.setParameter(compoundIds, "");
    return parameterSet;
  }

  public static @NotNull DeleteRowsParameters of(@NotNull final ModularFeatureList featureList,
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    return of(featureList, List.of(), compounds);
  }

  public static @NotNull DeleteRowsParameters of(@NotNull final ModularFeatureList featureList,
      @NotNull final List<? extends FeatureListRow> rows,
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    final DeleteRowsParameters parameterSet = (DeleteRowsParameters) new DeleteRowsParameters().cloneParameterSet();
    parameterSet.setParameter(DeleteRowsParameters.flist, new FeatureListsSelection(featureList));
    parameterSet.setParameter(rowIds, FeatureListUtils.rowsToIdString(rows));
    parameterSet.setParameter(compoundIds, joinCompoundIds(compounds));
    return parameterSet;
  }

  private static @NotNull String joinCompoundIds(
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    return compounds.stream().map(ModularCompoundRow::getCompoundId).map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
