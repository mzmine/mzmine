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

package io.github.mzmine.util.collections;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CollectionUtilsTest {

  private List<String> list;
  private IntList randoms;
  private List<String> expected;

  @BeforeEach
  void setUp() {
    list = IntStream.range(0, 60).mapToObj(i -> "i: " + i)
        .collect(Collectors.toCollection(ArrayList<String>::new));
    var rand = new Random(System.currentTimeMillis());

    int removeN = list.size() - 5;
    IntSet values = new IntOpenHashSet();
    while (values.size() < removeN) {
      values.add(rand.nextInt(list.size() - 1));
    }
    randoms = new IntArrayList(values);
    randoms.sort(Comparator.naturalOrder());

    expected = new ArrayList<>(list);
    for (int i = randoms.size() - 1; i >= 0; i--) {
      expected.remove(randoms.getInt(i));
    }
  }


  @Test
  void removeIndicesInPlace() {
    var result = CollectionUtils.removeIndicesInPlaceSorted(new ArrayList<>(list), randoms);
    Assertions.assertEquals(expected, result);
  }

  @Test
  void removeIndicesInPlaceIllegalInput() {
    // wrong order
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> CollectionUtils.removeIndicesInPlaceSorted(new ArrayList<>(list),
            new IntArrayList(new int[]{1, 0})));
  }

  @Test
  void removeIndicesInPlaceBitSet() {
    var result = CollectionUtils.removeIndicesInPlaceBitSet(new ArrayList<>(list), randoms);
    Assertions.assertEquals(expected, result);
  }
}