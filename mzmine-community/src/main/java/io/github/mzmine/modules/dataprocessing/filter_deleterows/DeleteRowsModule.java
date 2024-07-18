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
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeleteRowsModule extends AbstractProcessingModule {

  public DeleteRowsModule() {
    super("Delete rows", DeleteRowsParameters.class, MZmineModuleCategory.FEATURELISTFILTERING, """
        Deletes rows from a feature list (by id). The deletion is specific to the processing. \
        If any other step is changed, the IDs will be different and different rows will be deleted.""");
  }

  public static void deleteWithConfirmation(FeatureList flist,
      @Nullable List<? extends FeatureListRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    final ButtonType btn = DesktopService.getDesktop()
        .displayConfirmation("Are you sure you want to delete %d rows?".formatted(rows.size()),
            ButtonType.YES, ButtonType.NO);
    if (btn == ButtonType.YES) {
      MZmineCore.runMZmineModule(DeleteRowsModule.class, DeleteRowsParameters.of(flist, rows));
    }
  }

  public static void deleteRows(FeatureList flist, @Nullable List<? extends FeatureListRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return;
    }
    MZmineCore.runMZmineModule(DeleteRowsModule.class, DeleteRowsParameters.of(flist, rows));
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    tasks.add(new DeleteRowsTask(null, moduleCallDate, parameters, DeleteRowsModule.class));

    return ExitCode.OK;
  }
}
