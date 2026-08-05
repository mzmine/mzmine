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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Result of {@link ElementAutoDetector}: the heavy elements inferred to be present in an isotope
 * pattern, together with a best-effort rough atom count and a confidence per element.
 *
 * @param elements   detected heavy-element symbols (e.g. {@code {"Cl", "Br"}})
 * @param counts     symbol to a rough {@code {min, max}} atom count; may be empty when no estimate
 *                   could be made. The benchmark element metric ignores this and reads only
 *                   {@link #elements()}.
 * @param confidence symbol to a 0..1 confidence that the element is present
 */
public record DetectedComposition(@NotNull Set<String> elements, @NotNull Map<String, int[]> counts,
                                  @NotNull Map<String, Double> confidence) {

  /**
   * @return an empty composition (nothing detected).
   */
  @NotNull
  public static DetectedComposition empty() {
    return new DetectedComposition(Set.of(), Map.of(), Map.of());
  }
}
