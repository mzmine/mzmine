/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataanalysis.bubbleplots.logratioplot;

import io.github.mzmine.datamodel.features.FeatureList;
import java.awt.Color;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jfree.data.xy.AbstractXYZDataset;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataanalysis.bubbleplots.RTMZAnalyzerWindow;
import io.github.mzmine.modules.dataanalysis.bubbleplots.cvplot.CVParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

public class LogratioPlotModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Logratio analysis";
  private static final String MODULE_DESCRIPTION = "Logratio analysis"; // TODO

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    FeatureList featureLists[] =
        parameters.getParameter(CVParameters.featureLists).getValue().getMatchingFeatureLists();

    for (FeatureList pl : featureLists) {

      // Create dataset & paint scale
      AbstractXYZDataset dataset = new LogratioDataset(pl, parameters);
      InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
      paintScale.add(-1.00, new Color(0, 255, 0));
      paintScale.add(0.00, new Color(0, 0, 0));
      paintScale.add(1.00, new Color(255, 0, 0));

      // Create & show window
      RTMZAnalyzerWindow window = new RTMZAnalyzerWindow(dataset, pl, paintScale);
      window.show();

    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.DATAANALYSIS;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return LogratioParameters.class;
  }

}
