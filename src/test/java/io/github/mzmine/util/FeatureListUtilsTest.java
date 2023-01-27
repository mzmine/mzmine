/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DetectionType;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class FeatureListUtilsTest {

  @Mock
  RawDataFile raw;

  ModularFeatureList flist;

  List<FeatureListRow> rows;

  @BeforeEach
  void setUp() {
    flist = new ModularFeatureList("List", null, raw);
    rows = new ArrayList<>();
    rows.add(getRow(9f));
    rows.add(getRow(3f));
    rows.add(getRow(7f));
    rows.add(getRow(5f));
    rows.add(getRow(10f));
    rows.add(getRow(2f));
    rows.add(getRow(4f));
    rows.add(getRow(8f));
    rows.add(getRow(6f));
    rows.add(getRow(1f));
  }

  private FeatureListRow getRow(Float values) {
    ModularFeature f = new ModularFeature(flist);
    f.set(RawFileType.class, raw);
    f.set(MZType.class, values.doubleValue());
    f.set(RTType.class, values);
    f.set(MobilityType.class, values);
    f.set(DetectionType.class, FeatureStatus.DETECTED);
    ModularFeatureListRow row = new ModularFeatureListRow(flist, 1, f);
    return row;
  }

  @Test
  void getCandidatesWithinRanges() {
    assert FeatureListUtils.getCandidatesWithinRanges(Range.closed(1d, 8d), Range.closed(4f, 11f),
        Range.closed(7f, 10f), rows, false).size() == 2;
    assert FeatureListUtils.getCandidatesWithinRanges(Range.closed(1d, 7d), Range.closed(4f, 11f),
        Range.closed(7f, 10f), rows, false).size() == 1;
    assert FeatureListUtils.getCandidatesWithinRanges(Range.closed(1d, 10d), Range.all(),
        Range.closed(7f, 10f), rows, false).size() == 4;
  }

  @Test
  void binarySearch() {
    rows.sort(FeatureListRowSorter.MZ_ASCENDING);
    assertEquals(5f, rows.get(FeatureListUtils.binarySearch(rows, 5.4)).getAverageRT());
    assertEquals(5f, rows.get(FeatureListUtils.binarySearch(rows, 4.55)).getAverageRT());
    assertEquals(10f, rows.get(FeatureListUtils.binarySearch(rows, 10)).getAverageRT());
  }

}
