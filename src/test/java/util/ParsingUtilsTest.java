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
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParsingUtilsTest {

  @Test
  void testSublistIndicesGeneration() {
    final List<Integer> all = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29);
    final List<Integer> sublist = List.of(5, 6, 7, 8, 9, 15, 25, 28, 29);

    final int[] indicesOfSubListElements = ParsingUtils.getIndicesOfSubListElements(sublist, all);

    final List<Integer> sublistFromIndices = ParsingUtils.getSublistFromIndices(all,
        indicesOfSubListElements);

    Assertions.assertEquals(sublist, sublistFromIndices);

    final List<Integer> failingSublist = List.of(5, 6, 7, 8, 9, 25, 15, 28, 29);

    Assertions.assertThrows(IllegalStateException.class,
        () -> ParsingUtils.getIndicesOfSubListElements(failingSublist, all));
  }

  @Test
  void testStringToDoubleRange() {
    final Range<Double> range1 = Range.closed(5.345, 17.32E10);
    final Range<Double> range2 = Range.closed(-0.3347E-10, 0.3348);

    final String string1 = ParsingUtils.rangeToString((Range) range1);
    final String string2 = ParsingUtils.rangeToString((Range) range2);

    Assertions.assertEquals("[5.345;1.732E11]", string1);
    Assertions.assertEquals("[-3.347E-11;0.3348]", string2);

    Assertions.assertEquals(range1, ParsingUtils.stringToDoubleRange(string1));
    Assertions.assertEquals(range2, ParsingUtils.stringToDoubleRange(string2));

    Assertions.assertThrows(IllegalStateException.class,
        () -> ParsingUtils.stringToDoubleRange(":"));
    Assertions.assertThrows(NumberFormatException.class,
        () -> ParsingUtils.stringToDoubleRange("2A3;2E5"));
  }

  @Test
  void testStringToFloatRange() {
    final Range<Float> range1 = Range.closed(5.345f, 17.32E10f);
    final Range<Float> range2 = Range.closed(-0.3347E-10f, 0.3348f);

    final String string1 = ParsingUtils.rangeToString((Range) range1);
    final String string2 = ParsingUtils.rangeToString((Range) range2);

    Assertions.assertEquals("[5.345;1.732E11]", string1);
    Assertions.assertEquals("[-3.347E-11;0.3348]", string2);

    Assertions.assertEquals(range1, ParsingUtils.stringToFloatRange(string1));
    Assertions.assertEquals(range2, ParsingUtils.stringToFloatRange(string2));

    Assertions.assertThrows(IllegalStateException.class,
        () -> ParsingUtils.stringToFloatRange(":"));
    Assertions.assertThrows(NumberFormatException.class,
        () -> ParsingUtils.stringToFloatRange("2A3;2E5"));
  }

}
