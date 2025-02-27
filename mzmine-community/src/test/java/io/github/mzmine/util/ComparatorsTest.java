package io.github.mzmine.util;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ComparatorsTest {


  private static final Logger logger = Logger.getLogger(ComparatorsTest.class.getName());

  @Disabled
  @Test
  void testReversed() {
    // general question was if reversing a double comparator works on the whole
    // this compares first on detections, then on intensity and reverses the whole comparison
    final Comparator<TestPoint> reversed = Comparator.comparing(TestPoint::detections)
        .thenComparing(TestPoint::intensity).reversed();

    final List<TestPoint> list = Stream.of(p(2, 2.f), p(1, 5.3f), p(1, 3.f), p(2, 4.f), p(3, 0.3f))
        .sorted(reversed).toList();
    logger.info(list.toString());
  }

  private TestPoint p(int detections, float intensity) {
    return new TestPoint(detections, intensity);
  }
  private record TestPoint(int detections, float intensity) {

  }
}