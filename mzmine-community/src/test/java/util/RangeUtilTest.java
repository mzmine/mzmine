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

package util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.util.RangeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RangeUtilTest {

  @Test
  public void testDecimalRange() {
    Assertions.assertEquals(Range.closedOpen(5.255, 5.256),
        RangeUtils.getRangeToCeilDecimal("5.255"));
    Assertions.assertEquals(Range.closedOpen(5.25, 5.26), RangeUtils.getRangeToCeilDecimal("5.25"));
    Assertions.assertEquals(Range.closedOpen(5.5, 5.6), RangeUtils.getRangeToCeilDecimal("5.5"));
    Assertions.assertEquals(Range.closedOpen(5d, 6d), RangeUtils.getRangeToCeilDecimal("5."));
    Assertions.assertEquals(Range.closedOpen(5d, 6d), RangeUtils.getRangeToCeilDecimal("5"));
  }

  @Test
  public void testSimpleRange() {
    Assertions.assertNull(SimpleRange.ofInteger(null));
    Assertions.assertNull(SimpleRange.ofDouble(null));

    Assertions.assertEquals(new SimpleDoubleRange(0.123, 123.23),
        SimpleRange.ofDouble(0.123, 123.23));

    Assertions.assertEquals(SimpleRange.ofDouble(-Double.MAX_VALUE, Double.MAX_VALUE),
        SimpleRange.ofDouble(Range.all()));
    Assertions.assertEquals(SimpleRange.ofInteger(Integer.MIN_VALUE, Integer.MAX_VALUE),
        SimpleRange.ofInteger(Range.all()));

    Assertions.assertTrue(SimpleRange.ofDouble(Range.all()).contains(-Double.MAX_VALUE));
    Assertions.assertTrue(SimpleRange.ofDouble(Range.all()).contains(Double.MAX_VALUE));

    // todo: make these work
//    Assertions.assertEquals(Double.MAX_VALUE,
//        RangeUtils.rangeLength(SimpleRange.ofDouble(Range.all()).guava()));

    /*Assertions.assertEquals(Integer.MAX_VALUE, RangeUtils.rangeLength(Range.all()).intValue());
    Assertions.assertEquals(Long.MAX_VALUE, RangeUtils.rangeLength(Range.all()).longValue());
    Assertions.assertEquals(Float.MAX_VALUE, RangeUtils.rangeLength(Range.all()).floatValue());
    Assertions.assertEquals(Double.MAX_VALUE, RangeUtils.rangeLength(Range.all()).doubleValue());*/
  }
}
