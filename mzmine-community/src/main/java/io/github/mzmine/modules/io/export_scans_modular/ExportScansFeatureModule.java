/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.SingleTaskFeatureListsModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExportScansFeatureModule extends SingleTaskFeatureListsModule {

  public static final String MODULE_NAME = "Export scans (feature list)";

  public ExportScansFeatureModule() {
    super(MODULE_NAME, ExportScansFeatureMainParameters.class,
        MZmineModuleCategory.FEATURELISTEXPORT, """
            Export scans like MS1 or fragmentation scans for feature lists. This module supports various \
            output formats and options to control merging and scan selection. This can be useful to export \
            libraries of all spectra detected in blank samples.""");
  }

  @Override
  public @NotNull Task createTask(final @NotNull MZmineProject project,
      final @NotNull ParameterSet parameters, final @NotNull Instant moduleCallDate,
      final @Nullable MemoryMapStorage storage, final @NotNull FeatureList[] featureList) {
    return new ExportScansFeatureTask(storage, moduleCallDate, parameters, this.getClass(),
        featureList);
  }
}
