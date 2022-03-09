package util;

import com.google.common.collect.Range;
import io.github.mzmine.util.ParsingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParsingUtilTest {

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

    Assertions.assertEquals(null,
        ParsingUtils.stringToDoubleRange(":"));
  }
}
