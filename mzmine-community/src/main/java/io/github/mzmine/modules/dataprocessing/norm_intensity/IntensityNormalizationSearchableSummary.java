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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This summary is a faster version that is optimized to search the functions for raw data files.
 * This class is used during intensity normalization and may be used when batch reapplying
 * normalization. The related {@link IntensityNormalizationSummary} is used for saving to parameter
 * and xml to save memory on the map and to use Weak references to raw data files to avoid memory
 * leaks.
 */
public record IntensityNormalizationSearchableSummary(
    @NotNull Map<RawDataFile, RawFileNormalizationFunction> functions,
    @NotNull List<String> messages) {

  /**
   * instance with immutable maps and lists
   */
  public static final @NotNull IntensityNormalizationSearchableSummary EMPTY = new IntensityNormalizationSearchableSummary(
      Map.of(), List.of());

  public IntensityNormalizationSearchableSummary(
      @NotNull Map<RawDataFile, RawFileNormalizationFunction> functions) {
    this(functions, new ArrayList<>());
  }

  public IntensityNormalizationSearchableSummary(int numRawFiles) {
    this(HashMap.newHashMap(numRawFiles));
  }

  public IntensityNormalizationSummary toSimpleSummary() {
    return new IntensityNormalizationSummary(List.copyOf(functions.values()),
        List.copyOf(messages));
  }

  /**
   * Adds a function for a file. If this file already has a function then these two functions are
   * merged.
   *
   * @return the new function either the input or a merged function from the input and previous
   */
  @NotNull
  public RawFileNormalizationFunction addMergeFunction(@NotNull RawDataFile file,
      @NotNull NormalizationFunction function) {
    return functions.compute(file, (_, oldFunc) -> {
      if (oldFunc == null) {
        return new RawFileNormalizationFunction(file, function);
      }

      NormalizationFunction merged = NormalizationFunction.merge(oldFunc.function(), function);
      return new RawFileNormalizationFunction(file, merged);
    });
  }

  @Nullable
  public NormalizationFunction get(RawDataFile file) {
    final RawFileNormalizationFunction fun = functions.get(file);
    return fun != null ? fun.function() : null;
  }

  @Nullable
  public RawFileNormalizationFunction getRawFileFunction(RawDataFile file) {
    return functions.get(file);
  }

  public int size() {
    return functions.size();
  }
}
