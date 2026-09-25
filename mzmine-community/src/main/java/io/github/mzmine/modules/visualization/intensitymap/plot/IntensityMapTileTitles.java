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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Short tile titles: parts shared by all tiles, such as one raw file with several m/z overlays,
 * are left out.
 */
final class IntensityMapTileTitles {

  private IntensityMapTileTitles() {
  }

  static @NotNull List<String> of(@NotNull final List<IntensityMapSeries> series) {
    final boolean sameName = series.stream().map(IntensityMapSeries::name).distinct().count() == 1;
    final boolean sameDescription =
        series.stream().map(IntensityMapSeries::description).distinct().count() == 1;
    if (series.size() > 1 && sameName && series.stream()
        .noneMatch(value -> value.description().isBlank())) {
      // descriptions such as m/z ranges read best in full
      return series.stream().map(IntensityMapSeries::description).toList();
    }
    if (series.size() > 1 && sameDescription) {
      return distinctParts(series.stream().map(IntensityMapSeries::name).toList());
    }
    return distinctParts(series.stream().map(IntensityMapSeries::fullName).toList());
  }

  /**
   * Removes the prefix and suffix shared by all titles, e.g. the date and project of file names
   * or the file extension, at separator boundaries.
   */
  static @NotNull List<String> distinctParts(@NotNull final List<String> titles) {
    if (titles.size() < 2 || titles.stream().distinct().count() < titles.size()) {
      return titles;
    }
    final String first = titles.getFirst();
    int prefix = first.length();
    int suffix = first.length();
    for (final String title : titles) {
      prefix = Math.min(prefix, commonPrefix(first, title));
      suffix = Math.min(suffix, commonSuffix(first, title));
    }
    // cut only after a separator, so that numbers and names are not split
    while (prefix > 0 && !isSeparator(first.charAt(prefix - 1))) {
      prefix--;
    }
    while (suffix > 0 && !isSeparator(first.charAt(first.length() - suffix))) {
      suffix--;
    }
    final int cutPrefix = prefix;
    final int cutSuffix = suffix;
    final List<String> parts = titles.stream().map(title -> {
      final int end = Math.max(cutPrefix, title.length() - cutSuffix);
      return title.substring(cutPrefix, end).strip();
    }).toList();
    return parts.stream().anyMatch(String::isEmpty) ? titles : parts;
  }

  private static int commonPrefix(@NotNull final String a, @NotNull final String b) {
    int i = 0;
    while (i < a.length() && i < b.length() && a.charAt(i) == b.charAt(i)) {
      i++;
    }
    return i;
  }

  private static int commonSuffix(@NotNull final String a, @NotNull final String b) {
    int i = 0;
    while (i < a.length() && i < b.length()
        && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
      i++;
    }
    return i;
  }

  private static boolean isSeparator(final char c) {
    return c == '_' || c == '-' || c == ' ' || c == '.' || c == '·';
  }
}
