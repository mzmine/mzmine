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

package io.github.mzmine.parameters.parametertypes.other_detectors;

import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public enum OtherRawOrProcessed implements UniqueIdSupplier {
  RAW, PREPROCESSED, FEATURES;

  public Stream<OtherFeature> streamMatching(OtherTimeSeriesData data) {
    return switch (this) {
      case RAW -> data.getRawTraces().stream();
      case PREPROCESSED -> data.getPreprocessedTraces().stream();
      case FEATURES -> data.getProcessedFeatures().stream();
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case RAW -> "raw";
      case PREPROCESSED -> "pre-processed";
      case FEATURES -> "features";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case RAW -> "RAW";
      case PREPROCESSED -> "PREPROCESSED";
      case FEATURES -> "FEATURES";
    };
  }
}
