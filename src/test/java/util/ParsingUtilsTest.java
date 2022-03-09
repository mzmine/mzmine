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

    Assertions.assertEquals(null, ParsingUtils.stringToDoubleRange(":"));
  }

  @Test
  void testStringToFloatRange() {
    final Range<Float> range1 = Range.closed(5.345f, 17.32E10f);
    final Range<Float> range2 = Range.closed(-0.3347E-10f, 0.3348f);

    final String string1 = ParsingUtils.rangeToString((Range) range1);
    final String string2 = ParsingUtils.rangeToString((Range) range2);

    Assertions.assertEquals("[5.345;1.73199999E11]", string1);
    Assertions.assertEquals("[-3.347E-11;0.3348]", string2);

    Assertions.assertEquals(range1, ParsingUtils.stringToFloatRange(string1));
    Assertions.assertEquals(range2, ParsingUtils.stringToFloatRange(string2));

    Assertions.assertEquals(null, ParsingUtils.stringToFloatRange(":"));
  }

}
