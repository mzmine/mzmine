/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper.edit;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowUtils;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Replay form of {@link SetRepresentativeRowModule}. Resolves compound row and member row by id
 * from the target feature list and dispatches the mutation to the FX thread.
 */
public class SetRepresentativeRowTask extends AbstractFeatureListTask {

  private final @NotNull MZmineProject project;
  private final @NotNull ModularFeatureList flist;
  private final int compoundId;
  private final int representativeRowId;

  protected SetRepresentativeRowTask(@NotNull final MZmineProject project,
      @NotNull final Instant moduleCallDate,
      @NotNull final ParameterSet parameters) {
    super(null, moduleCallDate, parameters, SetRepresentativeRowModule.class);
    this.project = project;
    this.flist = parameters.getValue(SetRepresentativeRowParameters.flist)
        .getMatchingFeatureLists(project)[0];
    this.compoundId = parameters.getValue(SetRepresentativeRowParameters.compoundId);
    this.representativeRowId = parameters.getValue(
        SetRepresentativeRowParameters.representativeRowId);
  }

  @Override
  protected void process() {
    final CompoundList cl = flist.getCompoundList();
    if (cl == null) {
      throw new IllegalStateException(
          "Feature list '%s' has no compound list — run compound grouping first.".formatted(
              flist.getName()));
    }
    final ModularCompoundRow compound = cl.findRowByCompoundId(compoundId);
    if (compound == null) {
      throw new IllegalStateException(
          "No compound with id %d in feature list '%s'.".formatted(compoundId, flist.getName()));
    }
    final FeatureListRow row = flist.findRowByID(representativeRowId);
    if (!(row instanceof ModularFeatureListRow newRep)) {
      throw new IllegalStateException(
          "No row with id %d in feature list '%s'.".formatted(representativeRowId,
              flist.getName()));
    }
    FxThread.runOnFxThreadAndWait(() -> CompoundRowUtils.setRepresentative(compound, newRep));
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  public String getTaskDescription() {
    return "Setting row id=%d as representative of compound id=%d in feature list %s".formatted(
        representativeRowId, compoundId, flist.getName());
  }
}
