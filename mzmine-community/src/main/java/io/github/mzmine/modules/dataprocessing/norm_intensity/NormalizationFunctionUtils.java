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
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.InterpolationWeights;
import io.github.mzmine.modules.visualization.projectmetadata.table.InterpolationWeights.BinaryInterpolationWeights;
import io.github.mzmine.modules.visualization.projectmetadata.table.InterpolationWeights.MultiInterpolationWeights;
import io.github.mzmine.modules.visualization.projectmetadata.table.InterpolationWeights.SingleInterpolationWeight;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import io.github.mzmine.util.StringUtils;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public class NormalizationFunctionUtils {


  /**
   * Get reference files from sample batch or throw {@link IllegalStateException} if no empty
   * reference files (and not allowed)
   *
   * @param allowEmpty if false and empty result, throw exception
   */
  @NotNull
  public static List<RawDataFile> getReferenceSamplesOrThrow(boolean allowEmpty,
      @NotNull SamplesBatch samplesBatch, List<SampleType> includedTypes) {
    final var sampleTypeFilter = new SampleTypeFilter(includedTypes);
    final List<RawDataFile> filtered = sampleTypeFilter.filterFiles(samplesBatch.getRaws());
    if (filtered.isEmpty() && !allowEmpty) {
      throw new IllegalStateException(
          "No reference files found for batch with ID %s for sample types: %s".formatted(
              samplesBatch.getGroupMetadataValueStr(),
              StringUtils.join(includedTypes, ", ", SampleType::toString)));
    }
    return filtered;
  }

  /**
   * Interpolate all files in samplesBatch that are covered by refFunctions. Does nothing if all
   * functions are already computed. Otherwise computes interpolated functions for standards, e.g.,
   * based on the next two neighboring pooled QCs (reference samples)
   *
   * @param summary      results are merged into summary
   * @param samplesBatch the batch currently processed
   * @param refFunctions the reference file functions of this normalization step. This is not the
   *                     final merged functions but really the step looked at.
   * @param metadata     sample metadata
   */
  public static void interpolateLinearBinary(
      @NotNull IntensityNormalizationSearchableSummary summary, @NotNull SamplesBatch samplesBatch,
      @UnknownNullability Map<@NotNull RawDataFile, @NotNull NormalizationFunction> refFunctions,
      @NotNull MetadataTable metadata) {
    if (refFunctions.size() == samplesBatch.size()) {
      return;
    } else if (refFunctions.size() > samplesBatch.size()) {
      throw new IllegalStateException("Cannot have more reference functions than in sample batch");
    }
    final List<RawDataFile> referenceFiles = List.copyOf(refFunctions.keySet());

    // Interpolate any files not yet covered (batch-aware path already covers all files).
    for (final RawDataFile fileToInterpolate : samplesBatch.getRaws()) {
      if (refFunctions.containsKey(fileToInterpolate)) {
        continue;
      }

      // either binary or single reference sample found
      final InterpolationWeights weights = MetadataTableUtils.extractAcquisitionDateInterpolationWeights(
          fileToInterpolate, referenceFiles, metadata);

      final NormalizationFunction resultFunction = switch (weights) {
        // use a copy of the single function that was found
        case SingleInterpolationWeight w -> // only one reference found
            refFunctions.get(w.closestRun());

        // interpolated between two functions
        case BinaryInterpolationWeights w -> {
          // both left and right functions are not null so interpolate between them
          final NormalizationFunction previousFunction = refFunctions.get(
              w.previousRun().file());
          final NormalizationFunction nextFunction = refFunctions.get(w.nextRun().file());

          if (previousFunction == null || nextFunction == null) {
            throw new IllegalStateException(
                "No reference normalization functions available for file: %s in samples batch %s".formatted(
                    fileToInterpolate.getName(), samplesBatch.getGroupMetadataValueStr()));
          }

          if (previousFunction instanceof FactorNormalizationFunction prev
              && nextFunction instanceof FactorNormalizationFunction next) {
            // special case where simple factor functions can be turned into a new simple function
            final double factor =
                next.factor() * w.nextRun().weight() + prev.factor() * w.previousRun().weight();
            yield new FactorNormalizationFunction(factor);
          } else {
            // saves both functions to interpolate, works for all cases
            yield new InterpolatedNormalizationFunction(previousFunction,
                w.previousRun().weight(), nextFunction, w.nextRun().weight());
          }
        }
        case MultiInterpolationWeights w -> throw new UnsupportedOperationException(
            "Currently not supported to apply multi point >2 interpolation on qcs");
      };

      // finally add to summary and merge with previously applied functions
      summary.addMergeFunction(fileToInterpolate, resultFunction);
    }
  }
}
