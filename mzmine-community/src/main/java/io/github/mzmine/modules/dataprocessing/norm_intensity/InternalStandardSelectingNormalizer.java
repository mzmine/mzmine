/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.norm_intensity.StandardCompoundNormalizationTypeModule.StandardCompoundSelection;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Internal standards should be selected for the whole dataset so that all batches use the same rows
 * as internal standards.
 */
public sealed interface InternalStandardSelectingNormalizer extends NormalizationTypeModule permits
    StandardCompoundNormalizationTypeModule {

  /**
   * Resolves the standard compounds file to feature list rows and determines the reference level of
   * each standard. Call this once for the whole feature list, before the samples are split into
   * batches, so that all batches use the same rows and the same reference levels.
   *
   * @param referenceFiles all reference samples of the whole feature list, not of a single batch
   */
  @NotNull StandardCompoundSelection selectStandards(
      @NotNull final IntensityNormalizationSearchableSummary summary,
      @NotNull final ModularFeatureList featureList,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleSpecificParameters);


  /**
   * Same as
   * {@link
   * NormalizationTypeModule#createAllNormalizationFunctionsToSummary(IntensityNormalizationSearchableSummary,
   * ModularFeatureList, SamplesBatch, MetadataTable, ParameterSet, ParameterSet)} but with a
   * selection of standard rows that was already resolved for the whole feature list. All batches
   * have to use the same rows and the same reference levels, otherwise the same standard could be
   * represented by a different row in each batch.
   */
  void createAllNormalizationFunctionsToSummary(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull MetadataTable metadata, @NotNull ParameterSet mainParameters,
      @NotNull ParameterSet moduleSpecificParameters,
      @NotNull StandardCompoundSelection selection);
}
