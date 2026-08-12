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
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntensityNormalizerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix appended to the output feature list name (e.g., 'featurelist norm')", "norm");

  public static final AbundanceMeasureParameter featureMeasurementType = new AbundanceMeasureParameter(
      AbundanceMeasure.rawValues(), AbundanceMeasure.Height);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true, OriginalFeatureListOption.PROCESS_IN_PLACE);

  // ── Pre-normalization step 1: metadata-based (dilution factor, sample weight, injection volume)
  /**
   * Applied first, before IS and QC-based corrections. Each sample is divided by its metadata value
   * (e.g. dilution factor, injection volume, sample weight). either divide or multiply
   */
  public static final OptionalParameter<MetadataNormalizationConfigParameter> metadataNormFactorCol = new OptionalParameter<>(
      new MetadataNormalizationConfigParameter(NormalizationType.MetadataColumn.toString(), """
          Step 1 of 3: Pre-normalization by a numeric metadata column applied before internal standard and QC drift corrections.
          Each raw file is multiplied or divided by its metadata value (e.g., divide by sample weight or injection volume, multiply by dilution factor).
          Every file must have a numeric value in the selected column; set a value to 0 to skip normalization for that file.""",
          MetadataNormalizationConfig.getDefault()), false);

  // ── Pre-normalization step 2: internal standard compounds
  /**
   * Applied after metadata normalization. Each feature is corrected by the nearest/weighted
   * internal standard compound(s) in m/z and RT space, correcting for extraction efficiency and
   * matrix effects.
   */
  public static final ModuleOptionsEnumComboParameter<NormalizationType> internalStandardization = new ModuleOptionsEnumComboParameter<>(
      "Intra-sample correction",
      "Step 2 of 3: Feature-specific correction by internal standard compounds, applied after metadata normalization (step 1). "
          + "Each feature is corrected by its nearest or distance-weighted IS compound(s) in m/z–RT space, "
          + "accounting for extraction efficiency and matrix effects.",
      NormalizationType.intraSampleNormalizers(), false);

  // ── Main normalization: QC-based signal drift correction
  public static final ModuleOptionsEnumComboParameter<NormalizationType> normalizationType = new ModuleOptionsEnumComboParameter<>(
      "Inter-sample correction", """
      Step 3 of 3: QC-based signal drift correction, applied after metadata normalization (step 1) and %s (step 2).
      Corrects intra-batch drift using reference samples (e.g., pooled QCs) and then aligns batch medians to the global reference median for inter-batch correction.
      Non-reference samples receive interpolated correction factors based on acquisition date entered in the project or data file metadata.""".formatted(
          internalStandardization.getName()),
      NormalizationType.intraBatchDriftNormalizers());

  /**
   * When set, QC drift correction is computed independently per batch (intra-batch correction),
   * then batches are aligned to the global QC median (inter-batch correction / pooled QC
   * normalization). Without a batch ID, all QC samples form one continuous reference sequence.
   */
  public static final OptionalParameter<MetadataGroupingParameter> batchIdColumn = new OptionalParameter<>(
      new MetadataGroupingParameter("Batch correction metadata column", """
          Metadata column identifying the analytical batch (samples measured under comparable conditions).
          When set, samples are split into batches before applying steps 2 (%s) and 3 (%s), \
          so that reference samples (e.g., pooled QCs) and interpolation are batch-specific.
          After intra-batch correction, a final inter-batch normalization aligns the median \
          reference-sample signal of each batch to the global reference median.""".formatted(internalStandardization.getName(), normalizationType.getName())), false);

  /**
   * Holds the result of the normalization in a
   * {@link io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod} so it can
   * later be applied to newly added features by gap filling and manual integration. Use
   * {@link IntensityNormalizerModule#getNormalizationFunctionsOfLatestCallForFile(FeatureList,
   * RawDataFile)} and
   * {@link IntensityNormalizerModule#getNormalizationFunctionsOfLatestCall(FeatureList)} to extract
   * these {@link NormalizationFunction}s.
   */
  public static final HiddenParameter<IntensityNormalizationSummary> hiddenNormalizationSummary = new HiddenParameter<>(
      new NormalizationFunctionsParameter());

  public IntensityNormalizerParameters() {
    super(new Parameter[]{featureLists, suffix, handleOriginal, featureMeasurementType,
            batchIdColumn, metadataNormFactorCol, internalStandardization, normalizationType,
            hiddenNormalizationSummary},
        "https://mzmine.github.io/mzmine_documentation/module_docs/norm_intensity/norm_intensity.html");
  }

  public static @NotNull IntensityNormalizerParameters create(
      final @NotNull FeatureListsSelection selectedFeatureLists,
      final @NotNull String selectedSuffix, final @Nullable MetadataNormalizationConfig selectedMetadataNorm,
      final @NotNull NormalizationType selectedInternalNorm,
      final @Nullable ParameterSet selectedInternalNormParam,
      final @NotNull NormalizationType selectedNormalizationType,
      final @NotNull ParameterSet selectedNormalizationTypeParameters,
      final @Nullable String selectedBatchIdColumn,
      final @NotNull AbundanceMeasure selectedFeatureMeasurementType,
      final @NotNull OriginalFeatureListOption selectedOriginalFeatureListHandling,
      final @Nullable IntensityNormalizationSummary normalizationSummary) {
    final IntensityNormalizerParameters parameters = (IntensityNormalizerParameters) new IntensityNormalizerParameters().cloneParameterSet();
    parameters.setParameter(IntensityNormalizerParameters.featureLists, selectedFeatureLists);
    parameters.setParameter(IntensityNormalizerParameters.suffix, selectedSuffix);
    parameters.setParameter(IntensityNormalizerParameters.metadataNormFactorCol,
        selectedMetadataNorm != null, selectedMetadataNorm);

    // internal standards
    final ModuleOptionsEnumComboParameter<NormalizationType> internalNormParent = parameters.getParameter(
        IntensityNormalizerParameters.internalStandardization);
    if (selectedInternalNormParam != null) {
      internalNormParent.setValue(selectedInternalNorm, selectedInternalNormParam);
    } else {
      internalNormParent.setValue(selectedInternalNorm);
    }

    // intra and inter batch correction
    parameters.getParameter(IntensityNormalizerParameters.normalizationType)
        .setValue(selectedNormalizationType,
            selectedNormalizationTypeParameters.cloneParameterSet());
    parameters.setParameter(IntensityNormalizerParameters.batchIdColumn,
        selectedBatchIdColumn != null, selectedBatchIdColumn);
    parameters.setParameter(IntensityNormalizerParameters.featureMeasurementType,
        selectedFeatureMeasurementType);
    parameters.setParameter(IntensityNormalizerParameters.handleOriginal,
        selectedOriginalFeatureListHandling);
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        normalizationSummary);
    return parameters;
  }
}
