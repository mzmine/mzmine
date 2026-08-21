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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The parsed contents of an MSPepSearch {@code /OUTTAB} table.
 *
 * @param hits     every hit row, in file order.
 * @param warnings anything that could not be interpreted. Reported to the user but never fatal.
 */
public record NistPepSearchResult(@NotNull List<NistHit> hits, @NotNull List<String> warnings) {

  /**
   * Groups the hits by the search spectrum they belong to, keyed on the 1-based
   * {@code /OutSpecNum} ordinal. Hits without an ordinal are dropped - use {@link #byUnknownName()}
   * for those.
   */
  public @NotNull Map<Integer, List<NistHit>> bySpecNum() {

    final Map<Integer, List<NistHit>> grouped = new LinkedHashMap<>();
    for (final NistHit hit : hits) {
      if (hit.specNum() > 0) {
        grouped.computeIfAbsent(hit.specNum(), _ -> new ArrayList<>()).add(hit);
      }
    }
    return grouped;
  }

  /**
   * Groups the hits by the {@code Name:} of the submitted MSP entry. The fallback mapping for when
   * the {@code Num} column is absent.
   */
  public @NotNull Map<String, List<NistHit>> byUnknownName() {

    final Map<String, List<NistHit>> grouped = new LinkedHashMap<>();
    for (final NistHit hit : hits) {
      if (hit.unknownName() != null) {
        grouped.computeIfAbsent(hit.unknownName(), _ -> new ArrayList<>()).add(hit);
      }
    }
    return grouped;
  }
}
