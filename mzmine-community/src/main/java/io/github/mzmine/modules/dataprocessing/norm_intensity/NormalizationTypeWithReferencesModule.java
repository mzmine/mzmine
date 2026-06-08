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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Creates one normalization function per reference file. And then interpolated between them.
 */
public abstract class NormalizationTypeWithReferencesModule implements NormalizationTypeModule {


  /**
   * Selects references, creates reference functions, and calls interpolation on the functions. All
   * functions are merged into the summary.
   */
  @Override
  public void createAllNormalizationFunctionsToSummary(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull MetadataTable metadata, @NotNull ParameterSet mainParameters,
      @NotNull ParameterSet moduleSpecificParameters) {

    final List<RawDataFile> referenceSamples = getReferenceSamples(featureList, samplesBatch,
        moduleSpecificParameters);
    // important to use the actual function from this normalization and not the composite/merged function
    // because interpolation should only happen between this normalization step
    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> refFunctions = createReferenceFunctions(
        summary, referenceSamples, featureList, samplesBatch, metadata, mainParameters,
        moduleSpecificParameters);

    interpolateAllFunctionsToSummary(summary, featureList, samplesBatch, metadata, refFunctions,
        mainParameters, moduleSpecificParameters);
  }


  /**
   * Interpolate all missing sample functions from the reference functions. Default implementation
   * uses linear binary interpolation. Overwrite for other interpolation.
   *
   * @param summary      results be merged into the summary
   * @param samplesBatch the samples batch to process
   * @param refFunctions the reference functions of this normalization step. This may not be the
   *                     function in summary as summary merges with the previous normalization
   *                     steps.
   */
  protected void interpolateAllFunctionsToSummary(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull MetadataTable metadata,
      Map<@NotNull RawDataFile, @NotNull NormalizationFunction> refFunctions,
      @NotNull ParameterSet mainParameters, @NotNull ParameterSet moduleSpecificParameters) {
    // will check if interpolation is needed to then interpolate functions and save them to summary
    NormalizationFunctionUtils.interpolateLinearBinary(summary, samplesBatch, refFunctions,
        metadata);
  }

  /**
   * Should be protected. No need to be called. Call
   * {@link #createAllNormalizationFunctionsToSummary(IntensityNormalizationSearchableSummary,
   * ModularFeatureList, SamplesBatch, MetadataTable, ParameterSet, ParameterSet)}
   *
   * @return Creates reference functions for the reference samples. the returned functions should be
   * only the ones from this normalization step. Not the merged functions from the summary
   */
  protected abstract @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull List<@NotNull RawDataFile> referenceFiles, @NotNull ModularFeatureList featureList,
      @NotNull SamplesBatch samplesBatch, @NotNull MetadataTable metadata,
      @NotNull ParameterSet mainParameters, @NotNull ParameterSet moduleSpecificParameters);

  public abstract @NotNull List<RawDataFile> getReferenceSamples(@NotNull final FeatureList flist,
      @NotNull SamplesBatch samplesBatch,
      @NotNull final ParameterSet normalizationModuleParameters);
}
