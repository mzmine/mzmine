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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.RegionsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfig;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfigParameter;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfigSubParameters;
import io.github.mzmine.parameters.parametertypes.statistics.TTestConfigurationParameter;
import io.github.mzmine.parameters.parametertypes.statistics.UnivariateRowSignificanceTestConfig;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VolcanoPlotRegionExtractionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter(1, 1);
  public static final TTestConfigurationParameter config = new TTestConfigurationParameter(
      "TTest configuration", "Configure the t-test.");

  public static final RegionsParameter regions = new RegionsParameter();

  public static final AbundanceDataTablePreparationConfigParameter dataPreparationConfig = new AbundanceDataTablePreparationConfigParameter();

  private final AbundanceMeasureParameter LEGACY_ABUNDANCE_PARAMETER = new AbundanceMeasureParameter(
      "Abundance measure", "Select which metric is used to calculate the p Values.",
      AbundanceMeasure.values());

  public VolcanoPlotRegionExtractionParameters() {
    super(flists, regions, dataPreparationConfig, config);
  }

  public static VolcanoPlotRegionExtractionParameters create(VolcanoPlotModel model,
      List<List<Point2D>> regions) {
    final VolcanoPlotRegionExtractionParameters param = (VolcanoPlotRegionExtractionParameters) new VolcanoPlotRegionExtractionParameters().cloneParameterSet();
    param.setParameter(VolcanoPlotRegionExtractionParameters.flists,
        new FeatureListsSelection((ModularFeatureList) model.getFlists().getFirst()));
    param.setParameter(VolcanoPlotRegionExtractionParameters.regions, regions);

    final UnivariateRowSignificanceTestConfig test = model.getTest();
    param.setParameter(VolcanoPlotRegionExtractionParameters.config, test);

    final AbundanceDataTablePreparationConfigSubParameters dataPrepParams = param.getParameter(
        VolcanoPlotRegionExtractionParameters.dataPreparationConfig).getEmbeddedParameters();

    dataPrepParams.setParameter(
        AbundanceDataTablePreparationConfigSubParameters.missingValueImputation,
        model.getMissingValueImputation());
    dataPrepParams.setParameter(AbundanceDataTablePreparationConfigSubParameters.abundanceMeasure,
        model.getAbundanceMeasure());

    return param;
  }

  public VolcanoPlotModel toModel(FeaturesDataTable dataTable) {
    VolcanoPlotModel model = new VolcanoPlotModel();
    model.setFlists(List.of(getValue(flists).getMatchingFeatureLists()));
    model.setTest(getValue(config));
    final AbundanceDataTablePreparationConfig preparationConfig = getParameter(
        dataPreparationConfig).createConfig();
    model.setAbundanceMeasure(preparationConfig.measure());
    model.setMissingValueImputation(preparationConfig.missingValueImputation());
    model.setFeatureDataTable(dataTable);
    return model;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      // this changes behavior a bit but should be fine. There will be very few batches that actually rely on this module.
      // For a while people can just use the same old version to reproduce old results
      case 2 ->
          "Added support for missing value imputation. This introduced missing value imputation as default global LOD (limit of detection).";
      default -> null;
    };
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();

    // the AbundanceMeasure parameter was moved into another parameter
    final AbundanceDataTablePreparationConfigSubParameters embeddedParameters = getParameter(
        dataPreparationConfig).getEmbeddedParameters();
    final AbundanceMeasureParameter newAbundanceParameter = embeddedParameters.getParameter(
        AbundanceDataTablePreparationConfigSubParameters.abundanceMeasure);
    map.put(LEGACY_ABUNDANCE_PARAMETER.getName(), newAbundanceParameter);
    return map;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int version) {
    super.handleLoadedParameters(loadedParams, version);
    if (!loadedParams.containsKey(dataPreparationConfig.getName())) {
      // old parameterset only used abundance measure directly
      // set default to zero value imputation
      getParameter(dataPreparationConfig).getEmbeddedParameters()
          .setParameter(AbundanceDataTablePreparationConfigSubParameters.missingValueImputation,
              ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION);
    }
  }
}
