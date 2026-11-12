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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Compares the chromatograms of two feature lists, e.g., of the ADAP and the fast chromatogram
 * builder.
 */
public class ChromatogramComparisonModule extends AbstractRunnableModule {

  public ChromatogramComparisonModule() {
    super("Chromatogram builder comparison", ChromatogramComparisonParameters.class,
        MZmineModuleCategory.VISUALIZATIONFEATURELIST, """
            Compares the chromatograms of two feature lists of the same raw data file side by \
            side, e.g., from two chromatogram builders, and flags chromatograms that only one list \
            has and missing data points.""");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    final FeatureList[] flists = parameters.getValue(ChromatogramComparisonParameters.featureLists)
        .getMatchingFeatureLists();
    if (flists.length != 2) {
      MZmineCore.getDesktop()
          .displayErrorMessage("Select two feature lists to compare their chromatograms.");
      return ExitCode.ERROR;
    }
    FxThread.runLater(() -> {
      final ChromatogramComparisonTab tab = new ChromatogramComparisonTab();
      tab.setFeatureLists(flists[0], flists[1]);
      MZmineCore.getDesktop().addTab(tab);
    });
    return ExitCode.OK;
  }
}
