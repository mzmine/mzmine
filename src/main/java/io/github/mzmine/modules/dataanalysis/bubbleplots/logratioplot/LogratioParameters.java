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
