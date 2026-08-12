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

import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * This is a storable version that uses weak references in {@link RawDataFilePlaceholder}. The
 * searchable version is used during normalization building
 * {@link IntensityNormalizationSearchableSummary}
 */
public record IntensityNormalizationSummary(@NotNull List<RawFileNormalizationFunction> functions,
                                            @NotNull List<String> messages) {

  public static final @NotNull IntensityNormalizationSummary EMPTY = new IntensityNormalizationSummary(
      List.of(), List.of());

  @NotNull
  public IntensityNormalizationSummary copy() {
    return new IntensityNormalizationSummary(List.copyOf(functions), List.copyOf(messages));
  }

  public IntensityNormalizationSummary(
      @NotNull List<RawFileNormalizationFunction> functions) {
    this(functions, new ArrayList<>());
  }

  public int size() {
    return functions.size();
  }

  public boolean isEmpty() {
    return functions.isEmpty();
  }

  @NotNull
  public NormalizationFunction get(int index) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }
    return functions.get(index).function();
  }
  @NotNull
  public RawFileNormalizationFunction getRawFileFunction(int index) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }
    return functions.get(index);
  }
}
