/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class IntensityNormalizerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "norm");

  public static final ComboParameter<AbundanceMeasure> featureMeasurementType = new ComboParameter<AbundanceMeasure>(
      "Feature measurement type", "Measure features using",
      List.of(AbundanceMeasure.Area, AbundanceMeasure.Height), AbundanceMeasure.Height);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      "Original feature list",
      "Defines the processing.\nKEEP is to keep the original feature list and create a new"
          + "processed list.\nREMOVE saves memory.", false);

  public static final ModuleOptionsEnumComboParameter<NormalizationType> normalizationType = new ModuleOptionsEnumComboParameter<>(
      "Normalization type", "Normalize intensities by...",
      NormalizationType.MedianFeatureIntensity);

  /**
   * Holds the result of the normalization in a
   * {@link io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod} so it can
   * later be applied to newly added features by gap filling and manual integration. Use
   * {@link IntensityNormalizerModule#getNormalizationFunctionOfLatestCallForFile(FeatureList,
   * RawDataFile)} and
   * {@link IntensityNormalizerModule#getNormalizationFunctionsOfLatestCall(FeatureList)} to extract
   * these {@link NormalizationFunction}s.
   */
  public static final HiddenParameter<List<NormalizationFunction>> normalizationFunctions = new HiddenParameter<>(
      new NormalizationFunctionsParameter());

  public IntensityNormalizerParameters() {
    super(new Parameter[]{featureLists, suffix, normalizationType, featureMeasurementType,
            handleOriginal, normalizationFunctions},
        "https://mzmine.github.io/mzmine_documentation/module_docs/norm_intensity/norm_intensity.html");
  }

  public static @NotNull IntensityNormalizerParameters create(
      final @NotNull FeatureListsSelection selectedFeatureLists,
      final @NotNull String selectedSuffix,
      final @NotNull NormalizationType selectedNormalizationType,
      final @NotNull ParameterSet selectedNormalizationTypeParameters,
      final @NotNull AbundanceMeasure selectedFeatureMeasurementType,
      final @NotNull OriginalFeatureListOption selectedOriginalFeatureListHandling,
      final @NotNull List<NormalizationFunction> selectedNormalizationFunctions) {
    final IntensityNormalizerParameters parameters = (IntensityNormalizerParameters) new IntensityNormalizerParameters().cloneParameterSet();
    parameters.setParameter(IntensityNormalizerParameters.featureLists, selectedFeatureLists);
    parameters.setParameter(IntensityNormalizerParameters.suffix, selectedSuffix);
    parameters.getParameter(IntensityNormalizerParameters.normalizationType)
        .setValue(selectedNormalizationType,
            selectedNormalizationTypeParameters.cloneParameterSet());
    parameters.setParameter(IntensityNormalizerParameters.featureMeasurementType,
        selectedFeatureMeasurementType);
    parameters.setParameter(IntensityNormalizerParameters.handleOriginal,
        selectedOriginalFeatureListHandling);
    parameters.setParameter(IntensityNormalizerParameters.normalizationFunctions,
        selectedNormalizationFunctions);
    return parameters;
  }
}
