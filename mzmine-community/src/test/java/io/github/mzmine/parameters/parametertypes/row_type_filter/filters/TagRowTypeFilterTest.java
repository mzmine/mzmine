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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TagRowTypeFilterTest {

  private static final TagDataType TAG_TYPE = DataTypes.get(TagDataType.class);

  private static FeatureListRow rowWithTags(final int... tagIndices) {
    final BitSet tags = new BitSet();
    for (final int index : tagIndices) {
      tags.set(index);
    }
    final FeatureListRow row = Mockito.mock(FeatureListRow.class);
    Mockito.when(row.get(TAG_TYPE)).thenReturn(tags);
    return row;
  }

  @Test
  void exactMatchRequiresTheCompleteTagSet() {
    final RowTypeFilter filter = RowTypeFilter.create(RowTypeFilterOption.TAGS, MatchingMode.EQUAL,
        "0,2");

    Assertions.assertTrue(filter.matches(rowWithTags(0, 2)));
    Assertions.assertFalse(filter.matches(rowWithTags(0)));
    Assertions.assertFalse(filter.matches(rowWithTags(0, 1, 2)));
  }

  @Test
  void anyMatchRequiresAtLeastOneSelectedTag() {
    final RowTypeFilter filter = RowTypeFilter.create(RowTypeFilterOption.TAGS, MatchingMode.ANY,
        "1,3");

    Assertions.assertTrue(filter.matches(rowWithTags(0, 1)));
    Assertions.assertTrue(filter.matches(rowWithTags(3)));
    Assertions.assertFalse(filter.matches(rowWithTags(0, 2)));
    Assertions.assertFalse(filter.matches(rowWithTags()));
  }

  @Test
  void queryUsesStableZeroBasedTagIndices() {
    final BitSet tags = TagRowTypeFilter.parseQuery("0, 2;5");

    Assertions.assertEquals(BitSet.valueOf(new long[]{0b100101}), tags);
    Assertions.assertEquals("0,2,5", TagRowTypeFilter.formatQuery(tags));
    Assertions.assertThrows(QueryFormatException.class, () -> TagRowTypeFilter.parseQuery("-1"));
    Assertions.assertThrows(QueryFormatException.class, () -> TagRowTypeFilter.parseQuery("x"));
  }
}
