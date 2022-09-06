/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction;

import io.github.mzmine.modules.visualization.ims_mobilitymzplot.PlotType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;

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
}
