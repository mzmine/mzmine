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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms;

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

/**
 * Subtract blank chromatograms from samples chromatograms. Only runs before feature resolver, which
 * is checked in preconditions in the task. Uses the maximum intensity over all blanks for a
 * specific mz and retention time to subtract.
 */
public class ChromatogramBlankSubtractionModule extends SingleTaskFeatureListsModule {

  public ChromatogramBlankSubtractionModule() {
    super("Chromatogram blank subtraction", ChromatogramBlankSubtractionParameters.class,
        MZmineModuleCategory.EIC_DETECTION, """
            Subtracts blank chromatograms from samples. Uses the maximum intensity of m/z chromatogram across all blanks.
            This results in blank subtracted extracted ion chromatograms as input to any feature resolver.
            Feature resolving then describes the minimum height and other feature constraints.""");
  }

  @Override
  public @NotNull Task createTask(final @NotNull MZmineProject project,
      final @NotNull ParameterSet parameters, final @NotNull Instant moduleCallDate,
      final @Nullable MemoryMapStorage storage, final @NotNull FeatureList[] featureList) {
    return new ChromatogramBlankSubtractionTask(storage, moduleCallDate, parameters, getClass(),
        project, featureList);
  }
}
