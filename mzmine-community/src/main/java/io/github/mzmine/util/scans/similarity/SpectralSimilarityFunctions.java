/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util.scans.similarity;

import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.util.scans.similarity.impl.composite.CompositeCosineSpectralSimilarity;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;

public enum SpectralSimilarityFunctions implements ModuleOptionsEnum<SpectralSimilarityFunction> {
  WEIGHTED_COSINE, NIST_COMPOSITE_COSINE;

  public static SpectralSimilarityFunction createOption(
      final ValueWithParameters<SpectralSimilarityFunctions> simfuncParams) {
    return switch (simfuncParams.value()) {
      case WEIGHTED_COSINE -> new WeightedCosineSpectralSimilarity(simfuncParams.parameters());
      case NIST_COMPOSITE_COSINE ->
          new CompositeCosineSpectralSimilarity(simfuncParams.parameters());
    };
  }

  @Override
  public String toString() {
    return getStableId();
  }

  @Override
  public Class<? extends SpectralSimilarityFunction> getModuleClass() {
    return switch (this) {
      case NIST_COMPOSITE_COSINE -> CompositeCosineSpectralSimilarity.class;
      case WEIGHTED_COSINE -> WeightedCosineSpectralSimilarity.class;
    };
  }

  @Override
  public String getStableId() {
    return switch (this) {
      case NIST_COMPOSITE_COSINE ->
          "Composite cosine identity (e.g., GC-EI-MS; similar to NIST search)";
      case WEIGHTED_COSINE -> "Weighted cosine similarity";
    };
  }
}
