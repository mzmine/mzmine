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

package io.github.mzmine.modules.visualization.intensitymap;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Receives the results of an {@link IntensityMapSamplingTask}, on the task thread.
 */
interface IntensityMapSamplingListener {

  /**
   * @param progress share of the sampled files in [0, 1], throttled
   */
  void progress(@NotNull IntensityMapSamplingTask task, double progress);

  /**
   * @param results sampled data by layer id: the base for base reads, the base merged with the
   *                window otherwise
   * @param empty   ids of layers without data
   * @param skipped error message by name of a file that could not be sampled
   */
  void finished(@NotNull IntensityMapSamplingTask task,
      @NotNull Map<String, IntensityMapGrid> results, @NotNull Set<String> empty,
      @NotNull Map<String, String> skipped);

  /**
   * The task was canceled.
   */
  void released(@NotNull IntensityMapSamplingTask task);

  void failed(@NotNull IntensityMapSamplingTask task, @NotNull String message);
}
