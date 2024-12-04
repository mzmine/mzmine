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

package io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction;

import io.github.mzmine.modules.visualization.ims_mobilitymzplot.PlotType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * @author https://github.com/SteffenHeu
 */
public class MobilityMzRegionExtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final RegionsParameter regions = new RegionsParameter("Region",
      "Regions to extract.");

  public static final ComboParameter<PlotType> ccsOrMobility = new ComboParameter<>("Mobility/CCS",
      "Defines if mobility or mz shall be used for extraction.", PlotType.values(),
      PlotType.MOBILITY);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix of newly created feature lists", " extracted");

  public MobilityMzRegionExtractionParameters() {
    super(new Parameter[]{featureLists, regions, ccsOrMobility, suffix},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_mobilitymzregionextraction/filter_mobilitymzregionextraction.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    MobilityMzRegionExtractionSetupDialog dialog = new MobilityMzRegionExtractionSetupDialog(
        valueCheckRequired, this);
    dialog.showAndWait();

    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
