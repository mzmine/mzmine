/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.significance;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface SignificanceTest {

  /**
   * Applies prechecks. Use {@link #test(List)} to skip tests for multiple tests
   *
   * @return The tests p value
   */
  default double checkAndTest(List<double @NotNull []> data) {
    applyPreChecks(data);
    return test(data);
  }

  /**
   * Consider applying {@link #applyPreChecks(List)} once or on every call via
   * {@link #checkAndTest(List)}
   *
   * @return The tests p value
   */
  double test(List<double @NotNull []> data);


  /**
   * Applies prechecks and throws IllegalArgumentException if mismatch
   *
   * @param data the list of groups data
   */
  void applyPreChecks(List<double @NotNull []> data);
}
