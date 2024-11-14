/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.comb_resolver;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.resolver.DetectedByType;
import io.github.mzmine.datamodel.features.types.annotations.resolver.SharpnessType;
import io.github.mzmine.datamodel.features.types.annotations.resolver.VarianceRatioType;
import io.github.mzmine.datamodel.features.types.annotations.resolver.ZigZagIndexType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import org.jetbrains.annotations.NotNull;

public record CombinedResolverResult(double zigZagIndex, double varianceThreshold, double score,
                                     double sharpness, DetectedBy detectedBy) implements
    ModularDataRecord {

  @Override
  public Object getValue(@NotNull DataType<?> sub) {
    return switch (sub) {
      case DetectedByType _ -> detectedBy();
      case SharpnessType _ -> sharpness();
      case ZigZagIndexType _ -> zigZagIndex();
      case VarianceRatioType _ -> varianceThreshold();
      default -> null;
    };
  }

  enum DetectedBy {
    FIRST, SECOND, BOTH;
  }
}
