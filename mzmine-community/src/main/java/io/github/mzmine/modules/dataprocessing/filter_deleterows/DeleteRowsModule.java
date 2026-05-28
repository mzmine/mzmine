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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowUtils;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeleteRowsModule extends AbstractProcessingModule {

  public DeleteRowsModule() {
    super("Delete rows", DeleteRowsParameters.class, MZmineModuleCategory.FEATURELISTFILTERING, """
        Deletes rows and/or compound rows from a feature list (by id). Row deletion is specific to \
        the processing — if any other step is changed the IDs will differ and different rows will be \
        deleted. Compound row deletion only removes the compound grouping; the underlying feature \
        list rows are kept.""");
  }

  public static void deleteWithConfirmation(@NotNull final FeatureList flist,
      @Nullable final List<? extends FeatureListRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    final boolean result = DialogLoggerUtil.showDialogYesNo("Deleting rows?",
        "Are you sure you want to delete %d rows?".formatted(rows.size()));
    if (result) {
      MZmineCore.runMZmineModule(DeleteRowsModule.class, DeleteRowsParameters.of(flist, rows));
    }
  }

  public static void deleteRows(@NotNull final FeatureList flist,
      @Nullable final List<? extends FeatureListRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    MZmineCore.runMZmineModule(DeleteRowsModule.class, DeleteRowsParameters.of(flist, rows));
  }

  /**
   * Delete feature list rows and/or compound rows in a single module call. Either list may be
   * empty.
   */
  public static void deleteRows(@NotNull final ModularFeatureList flist,
      @NotNull final List<? extends FeatureListRow> rows,
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    if (rows.isEmpty() && compounds.isEmpty()) {
      return;
    }
    MZmineCore.runMZmineModule(DeleteRowsModule.class,
        DeleteRowsParameters.of(flist, rows, compounds));
  }

  /**
   * Apply compound-row removal directly on the FX thread. Records the change as a
   * {@link SimpleFeatureListAppliedMethod} on the feature list so it is saved with the project.
   * Compound rows that are not present at the top level of the compound list are silently skipped.
   *
   * @return true if at least one compound row was removed
   */
  public static boolean removeCompoundRows(@NotNull final ModularFeatureList featureList,
      @NotNull final Collection<? extends ModularCompoundRow> compounds) {
    if (compounds.isEmpty()) {
      return false;
    }
    final CompoundList cl = featureList.getCompoundList();
    if (cl == null) {
      return false;
    }
    @SuppressWarnings({"unchecked",
        "rawtypes"}) final boolean changed = CompoundRowUtils.removeCompoundRows(cl,
        (Collection<ModularCompoundRow>) (Collection) compounds);
    if (changed) {
      final DeleteRowsParameters params = DeleteRowsParameters.of(featureList, compounds);
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(DeleteRowsModule.class, params, Instant.now()));
    }
    return changed;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    tasks.add(new DeleteRowsTask(null, moduleCallDate, parameters, DeleteRowsModule.class));
    return ExitCode.OK;
  }
}
