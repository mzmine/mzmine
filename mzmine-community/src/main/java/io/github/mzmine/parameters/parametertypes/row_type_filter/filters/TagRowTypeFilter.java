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

package io.github.mzmine.parameters.parametertypes.row_type_filter.filters;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.TagDataType;
import io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption;
import java.util.BitSet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Matches feature-list rows by their selected tags.
 */
public final class TagRowTypeFilter extends AbstractRowTypeFilter {

  private static final TagDataType TAG_TYPE = DataTypes.get(TagDataType.class);

  private final @NotNull BitSet selectedTags;

  public TagRowTypeFilter(@NotNull final MatchingMode matchingMode, @NotNull final String query) {
    super(RowTypeFilterOption.TAGS, matchingMode, query);
    if (matchingMode != MatchingMode.EQUAL && matchingMode != MatchingMode.ANY) {
      throw new QueryFormatException("Tags only support exact or any-of matching.");
    }
    selectedTags = parseQuery(query);
    if (selectedTags.isEmpty()) {
      throw new QueryFormatException("Select at least one tag.");
    }
  }

  public static @NotNull BitSet parseQuery(@NotNull final String query) {
    final BitSet tags = new BitSet();
    if (query.isBlank()) {
      return tags;
    }

    for (final String token : query.trim().split("[\\s,;]+")) {
      try {
        final int tagIndex = Integer.parseInt(token);
        if (tagIndex < 0) {
          throw new QueryFormatException("Tag indices must not be negative: " + token);
        }
        tags.set(tagIndex);
      } catch (NumberFormatException e) {
        throw new QueryFormatException("Invalid tag index: " + token);
      }
    }
    return tags;
  }

  public static @NotNull String formatQuery(@NotNull final BitSet tags) {
    return tags.stream().mapToObj(Integer::toString).collect(Collectors.joining(","));
  }

  @Override
  public boolean matches(@NotNull final FeatureListRow row) {
    final BitSet rowTags = row.get(TAG_TYPE);
    if (matchingMode == MatchingMode.EQUAL) {
      return selectedTags.equals(rowTags == null ? new BitSet() : rowTags);
    }

    if (rowTags == null || rowTags.isEmpty()) {
      return false;
    }
    final BitSet overlap = (BitSet) selectedTags.clone();
    overlap.and(rowTags);
    return !overlap.isEmpty();
  }
}
