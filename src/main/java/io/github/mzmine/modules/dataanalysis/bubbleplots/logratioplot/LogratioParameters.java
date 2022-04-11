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

package io.github.mzmine.modules.dataanalysis.bubbleplots.logratioplot;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureMeasurementType;

public class LogratioParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1, 1);

  public static final MultiChoiceParameter<RawDataFile> groupOneFiles =
      new MultiChoiceParameter<RawDataFile>("Group one", "Samples in group one", new RawDataFile[0],
          null, 1);

  public static final MultiChoiceParameter<RawDataFile> groupTwoFiles =
      new MultiChoiceParameter<RawDataFile>("Group two", "Samples in group two", new RawDataFile[0],
          null, 1);

  public static final ComboParameter<FeatureMeasurementType> measurementType =
      new ComboParameter<FeatureMeasurementType>("Peak measurement type",
          "Determines whether peak's area or height is used in computations.",
          FeatureMeasurementType.values());

  public LogratioParameters() {
    super(new Parameter[] {featureLists, groupOneFiles, groupTwoFiles, measurementType});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    FeatureListsSelection featureListSel = getParameter(featureLists).getValue();
    FeatureList selectedFeatureLists[] = featureListSel.getMatchingFeatureLists();
    RawDataFile plDataFiles[] = selectedFeatureLists[0].getRawDataFiles().toArray(RawDataFile[]::new);

    getParameter(groupOneFiles).setChoices(plDataFiles);
    getParameter(groupTwoFiles).setChoices(plDataFiles);

    return super.showSetupDialog(valueCheckRequired);
  }

}
