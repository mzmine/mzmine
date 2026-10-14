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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.SimpleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SequencedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleDoubleRangeMapTest {

  @Test
  void getEntryIncludesBounds() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(10, 20), "a");
    map.put(SimpleRange.ofDouble(30, 40), "b");

    Assertions.assertNull(map.get(Math.nextDown(10d)));
    Assertions.assertEquals("a", map.get(10));
    Assertions.assertEquals("a", map.get(15));
    Assertions.assertEquals("a", map.get(20));
    Assertions.assertNull(map.get(Math.nextUp(20d)));
    Assertions.assertNull(map.get(25));
    Assertions.assertEquals("b", map.get(30));
    Assertions.assertEquals("b", map.get(40));
    Assertions.assertNull(map.get(41));
    Assertions.assertNull(new SimpleDoubleRangeMap<String>().getEntry(1));
  }

  @Test
  void noTrimmingWithoutOverlap() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(10, 20), "a");
    map.put(SimpleRange.ofDouble(30, 40), "b");

    final SimpleDoubleRange proposed = SimpleRange.ofDouble(Math.nextUp(20d), 25);
    Assertions.assertEquals(proposed, map.createNonOverlappingRange(proposed));
    Assertions.assertEquals(SimpleRange.ofDouble(0, 5),
        map.createNonOverlappingRange(SimpleRange.ofDouble(0, 5)));
    Assertions.assertEquals(SimpleRange.ofDouble(50, 55),
        map.createNonOverlappingRange(SimpleRange.ofDouble(50, 55)));
  }

  @Test
  void trimsSeamlesslyToNeighbors() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(10, 20), "a");
    map.put(SimpleRange.ofDouble(30, 40), "b");

    // left overlap
    Assertions.assertEquals(SimpleRange.ofDouble(Math.nextUp(20d), 25),
        map.createNonOverlappingRange(SimpleRange.ofDouble(15, 25)));
    // right overlap
    Assertions.assertEquals(SimpleRange.ofDouble(22, Math.nextDown(30d)),
        map.createNonOverlappingRange(SimpleRange.ofDouble(22, 35)));
    // both sides, touching bounds count as overlap because ranges are closed
    Assertions.assertEquals(SimpleRange.ofDouble(Math.nextUp(20d), Math.nextDown(30d)),
        map.createNonOverlappingRange(SimpleRange.ofDouble(20, 30)));

    final SimpleRangeEntry<String> entry = map.putNonOverlapping(SimpleRange.ofDouble(18, 32), "c");
    Assertions.assertEquals(SimpleRange.ofDouble(Math.nextUp(20d), Math.nextDown(30d)),
        entry.range());
    Assertions.assertEquals("a", map.get(20));
    Assertions.assertEquals("c", map.get(Math.nextUp(20d)));
    Assertions.assertEquals("c", map.get(Math.nextDown(30d)));
    Assertions.assertEquals("b", map.get(30));
  }

  @Test
  void enclosedRangeThrows() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(10, 20), "a");
    map.put(SimpleRange.ofDouble(22, 23), "b");

    Assertions.assertThrows(IllegalStateException.class,
        () -> map.createNonOverlappingRange(SimpleRange.ofDouble(21, 25)));
    // also after trimming the lower bound
    Assertions.assertThrows(IllegalStateException.class,
        () -> map.createNonOverlappingRange(SimpleRange.ofDouble(15, 25)));
  }

  @Test
  void coveredRangeThrows() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(10, 20), "a");
    map.put(SimpleRange.ofDouble(Math.nextUp(20d), 30), "b");

    Assertions.assertThrows(IllegalStateException.class,
        () -> map.createNonOverlappingRange(SimpleRange.ofDouble(12, 18)));
    Assertions.assertThrows(IllegalStateException.class,
        () -> map.createNonOverlappingRange(SimpleRange.ofDouble(15, 25)));
  }

  @Test
  void putRejectsOverlap() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(10, 20), "a");

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> map.put(SimpleRange.ofDouble(20, 25), "b"));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> map.put(SimpleRange.ofDouble(5, 10), "b"));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> map.put(SimpleRange.ofDouble(12, 15), "b"));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> map.put(SimpleRange.ofDouble(5, 25), "b"));
    Assertions.assertEquals(1, map.size());
  }

  @Test
  void randomInsertsMatchBruteForce() {
    final Random random = new Random(42);
    final SimpleDoubleRangeMap<Integer> map = new SimpleDoubleRangeMap<>(1);
    final List<SimpleDoubleRange> ranges = new ArrayList<>();
    for (int i = 0; i < 5000; i++) {
      final double center = random.nextDouble() * 1000;
      final SimpleDoubleRange proposed = SimpleRange.ofDouble(center - 0.05, center + 0.05);
      // only propose ranges around uncovered values, like SpectraMerging does
      if (map.getEntry(center) != null) {
        continue;
      }
      final SimpleRangeEntry<Integer> entry = map.putNonOverlapping(proposed, ranges.size());
      Assertions.assertTrue(entry.range().contains(center));
      ranges.add(entry.range());
    }
    Assertions.assertEquals(ranges.size(), map.size());

    for (int i = 0; i < 20000; i++) {
      final double value = random.nextDouble() * 1000;
      Integer expected = null;
      for (int r = 0; r < ranges.size(); r++) {
        if (ranges.get(r).contains(value)) {
          expected = r;
          break;
        }
      }
      Assertions.assertEquals(expected, map.get(value), "value " + value);
    }

    final List<SimpleRangeEntry<Integer>> entries = map.entries();
    Assertions.assertEquals(ranges.size(), entries.size());
    for (int i = 1; i < entries.size(); i++) {
      Assertions.assertTrue(entries.get(i - 1).range().upper() < entries.get(i).range().lower());
    }
  }

  @Test
  void sortedInsertsStayBalanced() {
    // ascending inserts degrade an unbalanced tree to a list, which would take minutes here
    final int n = 200_000;
    final SimpleDoubleRangeMap<Integer> map = new SimpleDoubleRangeMap<>();
    for (int i = 0; i < n; i++) {
      map.put(SimpleRange.ofDouble(i, i + 0.5), i);
    }
    for (int i = n - 1; i >= 0; i -= 7) {
      Assertions.assertEquals(i, map.get(i + 0.25));
      Assertions.assertNull(map.get(i + 0.75));
    }
    Assertions.assertEquals(n, map.entries().size());
  }

  @Test
  void removeReturnsEntryAndFreesRange() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(10, 20), "a");
    map.put(SimpleRange.ofDouble(30, 40), "b");
    map.put(SimpleRange.ofDouble(50, 60), "c");

    Assertions.assertNull(map.remove(25));
    Assertions.assertNull(map.remove(5));
    Assertions.assertEquals(3, map.size());

    final SimpleRangeEntry<String> removed = map.remove(40);
    Assertions.assertNotNull(removed);
    Assertions.assertEquals("b", removed.value());
    Assertions.assertEquals(SimpleRange.ofDouble(30, 40), removed.range());
    Assertions.assertEquals(2, map.size());
    Assertions.assertNull(map.get(35));
    Assertions.assertNull(map.remove(35));
    Assertions.assertEquals("a", map.get(15));
    Assertions.assertEquals("c", map.get(55));

    // the freed space can be used again
    Assertions.assertEquals(SimpleRange.ofDouble(Math.nextUp(20d), Math.nextDown(50d)),
        map.putNonOverlapping(SimpleRange.ofDouble(15, 55), "d").range());
    Assertions.assertEquals(List.of("a", "d", "c"),
        map.entries().stream().map(SimpleRangeEntry::value).toList());

    Assertions.assertEquals("a", map.remove(10).value());
    Assertions.assertEquals("c", map.remove(60).value());
    Assertions.assertEquals("d", map.remove(30).value());
    Assertions.assertTrue(map.isEmpty());
    Assertions.assertTrue(map.entries().isEmpty());
    Assertions.assertNull(map.remove(30));
  }

  @Test
  void randomInsertsAndRemovesMatchBruteForce() {
    final Random random = new Random(7);
    final SimpleDoubleRangeMap<Integer> map = new SimpleDoubleRangeMap<>(1);
    final List<SimpleRangeEntry<Integer>> expected = new ArrayList<>();
    int nextValue = 0;

    for (int i = 0; i < 20000; i++) {
      final double value = random.nextDouble() * 500;
      SimpleRangeEntry<Integer> containing = null;
      for (final SimpleRangeEntry<Integer> entry : expected) {
        if (entry.range().contains(value)) {
          containing = entry;
          break;
        }
      }

      if (random.nextInt(3) == 0) {
        // remove about a third of the time
        Assertions.assertEquals(containing, map.remove(value), "remove " + value);
        expected.remove(containing);
      } else if (containing == null) {
        final SimpleDoubleRange proposed = SimpleRange.ofDouble(value - 0.05, value + 0.05);
        final boolean enclosesExisting = expected.stream().anyMatch(
            e -> e.range().lower() > proposed.lower() && e.range().upper() < proposed.upper());
        if (enclosesExisting) {
          // removals can leave narrow ranges that a new proposal fully encloses
          Assertions.assertThrows(IllegalStateException.class,
              () -> map.putNonOverlapping(proposed, -1));
        } else {
          expected.add(map.putNonOverlapping(proposed, nextValue++));
        }
      } else {
        Assertions.assertEquals(containing, map.getEntry(value), "get " + value);
      }
      Assertions.assertEquals(expected.size(), map.size());
    }

    final List<SimpleRangeEntry<Integer>> entries = map.entries();
    Assertions.assertEquals(expected.size(), entries.size());
    for (int i = 1; i < entries.size(); i++) {
      Assertions.assertTrue(entries.get(i - 1).range().upper() < entries.get(i).range().lower());
    }
    for (final SimpleRangeEntry<Integer> entry : expected) {
      Assertions.assertEquals(entry, map.getEntry(entry.range().lower()));
      Assertions.assertEquals(entry, map.getEntry(entry.range().upper()));
    }
  }

  @Test
  void sortedRemovesStayBalanced() {
    // removing in ascending order degrades an unbalanced tree, which would take minutes here
    final int n = 200_000;
    final SimpleDoubleRangeMap<Integer> map = new SimpleDoubleRangeMap<>();
    for (int i = 0; i < n; i++) {
      map.put(SimpleRange.ofDouble(i, i + 0.5), i);
    }
    for (int i = 0; i < n; i += 2) {
      Assertions.assertEquals(i, map.remove(i + 0.25).value());
    }
    Assertions.assertEquals(n / 2, map.size());
    for (int i = 0; i < n; i++) {
      Assertions.assertEquals(i % 2 == 0 ? null : i, map.get(i + 0.25));
    }
    for (int i = n - 1; i >= 0; i -= 2) {
      Assertions.assertEquals(i, map.remove(i).value());
    }
    Assertions.assertTrue(map.isEmpty());
  }

  @Test
  void asMapOfRangesIsAscendingCopy() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.put(SimpleRange.ofDouble(50, 60), "c");
    map.put(SimpleRange.ofDouble(10, 20), "a");
    map.put(SimpleRange.ofDouble(30, 40), null);

    final SequencedMap<SimpleDoubleRange, String> ranges = map.asMapOfRanges();
    Assertions.assertEquals(List.of(SimpleRange.ofDouble(10, 20), SimpleRange.ofDouble(30, 40),
        SimpleRange.ofDouble(50, 60)), List.copyOf(ranges.keySet()));
    Assertions.assertEquals("a", ranges.get(SimpleRange.ofDouble(10, 20)));
    Assertions.assertTrue(ranges.containsKey(SimpleRange.ofDouble(30, 40)));
    Assertions.assertNull(ranges.get(SimpleRange.ofDouble(30, 40)));
    Assertions.assertNull(ranges.get(SimpleRange.ofDouble(10, 15)));
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> ranges.put(SimpleRange.ofDouble(0, 1), "x"));

    // a copy, later changes are not reflected
    map.remove(55);
    Assertions.assertEquals(3, ranges.size());
    Assertions.assertEquals(2, map.asMapOfRanges().size());
    Assertions.assertTrue(new SimpleDoubleRangeMap<String>().asMapOfRanges().isEmpty());
  }

  @Test
  void entriesAreAscending() {
    final SimpleDoubleRangeMap<String> map = new SimpleDoubleRangeMap<>();
    map.putNonOverlapping(SimpleRange.ofDouble(50, 60), "c");
    map.putNonOverlapping(SimpleRange.ofDouble(10, 20), "a");
    map.putNonOverlapping(SimpleRange.ofDouble(15, 30), "b");

    Assertions.assertEquals(List.of("a", "b", "c"),
        map.entries().stream().map(SimpleRangeEntry::value).toList());
    Assertions.assertEquals(3, map.size());
    map.clear();
    Assertions.assertTrue(map.isEmpty());
  }
}
