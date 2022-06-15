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
    final MaldiTimsPrecursor p1 = new MaldiTimsPrecursor(null, 5, Range.closed(1f, 2f), 30f);
    final MaldiTimsPrecursor p2 = new MaldiTimsPrecursor(null, 5, Range.closed(2f, 3f), 30f);
    final MaldiTimsPrecursor p3 = new MaldiTimsPrecursor(null, 5, Range.closed(2.5f, 3.5f), 30f);
    final MaldiTimsPrecursor p4 = new MaldiTimsPrecursor(null, 5, Range.closed(4f, 5f), 30f);
    final MaldiTimsPrecursor p5 = new MaldiTimsPrecursor(null, 5, Range.closed(4f, 5f), 50f);

    final List<MaldiTimsPrecursor> list = List.of(p1, p2, p3, p4, p5);
    final Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps = TopNSelectionModule.findOverlaps(
        list);

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
        30f);
    final MaldiTimsPrecursor p2 = new MaldiTimsPrecursor(null, 5, Range.closed(0.871f, 0.918f),
        30f);
    final MaldiTimsPrecursor p3 = new MaldiTimsPrecursor(null, 5, Range.closed(0.928f, 0.961f),
        30f);
    final MaldiTimsPrecursor p4 = new MaldiTimsPrecursor(null, 5, Range.closed(0.978f, 1.002f),
        30f);
    final MaldiTimsPrecursor p5 = new MaldiTimsPrecursor(null, 5, Range.closed(1.000f, 1.023f),
        50f);
    final MaldiTimsPrecursor p6 = new MaldiTimsPrecursor(null, 5, Range.closed(1.002f, 1.018f),
        30f);
    final MaldiTimsPrecursor p7 = new MaldiTimsPrecursor(null, 5, Range.closed(0.961f, 0.973f),
        50f);
    final MaldiTimsPrecursor p8 = new MaldiTimsPrecursor(null, 5, Range.closed(0.910f, 0.935f),
        50f);

    final List<MaldiTimsPrecursor> list = List.of(p1, p2, p3, p4, p5, p6, p7, p8);
    final Map<MaldiTimsPrecursor, List<MaldiTimsPrecursor>> overlaps = TopNSelectionModule.findOverlaps(
        list);

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
