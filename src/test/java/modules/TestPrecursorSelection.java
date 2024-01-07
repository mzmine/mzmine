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

package modules;

import com.google.common.collect.Range;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.TopNSelectionModule;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPrecursorSelection {

  private static final Logger logger = Logger.getLogger(TestPrecursorSelection.class.getName());

  @Test
  void testSelection1() {
    final MaldiTimsPrecursor p1 = new MaldiTimsPrecursor(null, 5, Range.closed(1f, 2f),
        List.of(30d));
    final MaldiTimsPrecursor p2 = new MaldiTimsPrecursor(null, 5, Range.closed(2f, 3f),
        List.of(30d));
    final MaldiTimsPrecursor p3 = new MaldiTimsPrecursor(null, 5, Range.closed(2.5f, 3.5f),
        List.of(30d));
    final MaldiTimsPrecursor p4 = new MaldiTimsPrecursor(null, 5, Range.closed(4f, 5f),
        List.of(30d));
    final MaldiTimsPrecursor p5 = new MaldiTimsPrecursor(null, 5, Range.closed(4f, 5f),
        List.of(50d));

    final List<MaldiTimsPrecursor> list = List.of(p1, p2, p3, p4, p5);
    final Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps = TopNSelectionModule.findOverlaps(
        list, 0);

    final List<List<MaldiTimsPrecursor>> lists = TopNSelectionModule.findRampsIterative(overlaps);
    for (List<MaldiTimsPrecursor> precursors : lists) {
      logger.info(precursors.toString());
    }

    Assertions.assertEquals(2, lists.size());
    Assertions.assertEquals(2, lists.get(0).size());
    Assertions.assertEquals(3, lists.get(1).size());
  }

  @Test
  void testSelection2() {
    final MaldiTimsPrecursor p1 = new MaldiTimsPrecursor(null, 5, Range.closed(0.954f, 0.981f),
        List.of(30d));
    final MaldiTimsPrecursor p2 = new MaldiTimsPrecursor(null, 5, Range.closed(0.871f, 0.918f),
        List.of(30d));
    final MaldiTimsPrecursor p3 = new MaldiTimsPrecursor(null, 5, Range.closed(0.928f, 0.961f),
        List.of(30d));
    final MaldiTimsPrecursor p4 = new MaldiTimsPrecursor(null, 5, Range.closed(0.978f, 1.002f),
        List.of(30d));
    final MaldiTimsPrecursor p5 = new MaldiTimsPrecursor(null, 5, Range.closed(1.000f, 1.023f),
        List.of(50d));
    final MaldiTimsPrecursor p6 = new MaldiTimsPrecursor(null, 5, Range.closed(1.002f, 1.018f),
        List.of(30d));
    final MaldiTimsPrecursor p7 = new MaldiTimsPrecursor(null, 5, Range.closed(0.961f, 0.973f),
        List.of(50d));
    final MaldiTimsPrecursor p8 = new MaldiTimsPrecursor(null, 5, Range.closed(0.910f, 0.935f),
        List.of(50d));

    final List<MaldiTimsPrecursor> list = List.of(p1, p2, p3, p4, p5, p6, p7, p8);
    final Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps = TopNSelectionModule.findOverlaps(
        list, 0);

//    final List<List<MaldiTimsPrecursor>> lists = TopNSelectionModule.generateTargetLists(overlaps,
//        list);
    final List<List<MaldiTimsPrecursor>> lists = TopNSelectionModule.findRampsIterative(overlaps);
    for (List<MaldiTimsPrecursor> precursors : lists) {
      logger.info(precursors.toString());
    }

    Assertions.assertEquals(3, lists.size());
    Assertions.assertEquals(3, lists.get(0).size());
    Assertions.assertEquals(3, lists.get(1).size());
    Assertions.assertEquals(2, lists.get(2).size());
  }
}
