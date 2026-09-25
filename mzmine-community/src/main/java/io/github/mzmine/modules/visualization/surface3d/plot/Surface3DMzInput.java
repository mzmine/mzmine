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

package io.github.mzmine.modules.visualization.surface3d.plot;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Parses m/z overlay input such as {@code 400.1234, 512.3-512.5; 600}. Single values become
 * ranges with the given tolerance.
 */
final class Surface3DMzInput {

  private static final String NUMBER = "(\\d+(?:\\.\\d*)?|\\.\\d+)";
  private static final Pattern RANGE = Pattern.compile(
      "^" + NUMBER + "\\s*(?:-|–|—|\\.\\.)\\s*" + NUMBER + "$");
  private static final Pattern VALUE = Pattern.compile("^" + NUMBER + "$");

  private Surface3DMzInput() {
  }

  /**
   * @param tolerance window around single m/z values
   * @throws IllegalArgumentException with a user readable message
   */
  static @NotNull List<Range<Double>> parse(@NotNull final String text,
      @NotNull final MZTolerance tolerance) {
    final List<Range<Double>> ranges = new ArrayList<>();
    for (final String raw : text.split("[,;\\n]+")) {
      final String token = raw.trim();
      if (token.isEmpty()) {
        continue;
      }
      final Matcher range = RANGE.matcher(token);
      if (range.matches()) {
        final double lower = Double.parseDouble(range.group(1));
        final double upper = Double.parseDouble(range.group(2));
        if (!(upper > lower)) {
          throw new IllegalArgumentException("Invalid m/z range " + token);
        }
        ranges.add(Range.closed(lower, upper));
        continue;
      }
      final Matcher value = VALUE.matcher(token);
      if (!value.matches()) {
        throw new IllegalArgumentException("Cannot read m/z value " + token);
      }
      final double mz = Double.parseDouble(value.group(1));
      if (!(mz > 0)) {
        throw new IllegalArgumentException("m/z values must be positive");
      }
      ranges.add(tolerance.getToleranceRange(mz));
    }
    if (ranges.isEmpty()) {
      throw new IllegalArgumentException("Enter one or more m/z values or ranges");
    }
    return ranges;
  }
}
