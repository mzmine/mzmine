package util;

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

}
