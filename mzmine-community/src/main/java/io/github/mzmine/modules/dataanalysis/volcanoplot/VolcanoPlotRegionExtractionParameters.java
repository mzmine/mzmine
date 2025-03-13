/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestModules;
import io.github.mzmine.modules.dataanalysis.significance.ttest.StudentTTest;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.statistics.TTestConfigurationParameter;
import java.awt.geom.Point2D;
import java.util.List;

public class VolcanoPlotRegionExtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter(1, 1);
  public static final AbundanceMeasureParameter abundance = new AbundanceMeasureParameter(
      "Abundance measure", "Select which metric is used to calculate the p Values.",
      AbundanceMeasure.values());
  public static final ComboParameter<RowSignificanceTestModules> test = new ComboParameter(
      "Significance test", "Select the significance test",
      RowSignificanceTestModules.TWO_GROUP_TESTS);
  public static final TTestConfigurationParameter config = new TTestConfigurationParameter(
      "TTest configuration", "Configure the T-Test.");

  public static final RegionsParameter regions = new RegionsParameter();

  public VolcanoPlotRegionExtractionParameters() {
    super(flists, abundance, test, regions, config);
  }

  public static VolcanoPlotRegionExtractionParameters create(VolcanoPlotModel model,
      List<List<Point2D>> regions) {
    final VolcanoPlotRegionExtractionParameters param = (VolcanoPlotRegionExtractionParameters) new VolcanoPlotRegionExtractionParameters().cloneParameterSet();
    param.setParameter(VolcanoPlotRegionExtractionParameters.flists,
        new FeatureListsSelection((ModularFeatureList) model.getFlists().getFirst()));
    param.setParameter(VolcanoPlotRegionExtractionParameters.abundance,
        model.getAbundanceMeasure());
    param.setParameter(VolcanoPlotRegionExtractionParameters.regions, regions);
    param.setParameter(VolcanoPlotRegionExtractionParameters.test,
        RowSignificanceTestModules.TTEST);
    param.setParameter(VolcanoPlotRegionExtractionParameters.config,
        ((StudentTTest<?>) model.getTest()).toConfiguration());

    return param;
  }

  public VolcanoPlotModel toModel() {
    VolcanoPlotModel model = new VolcanoPlotModel();
    model.setFlists(List.of(getValue(flists).getMatchingFeatureLists()));
    model.setTest(getValue(config).toValidConfig());
    model.setAbundanceMeasure(getValue(abundance));
    return model;
  }
}
