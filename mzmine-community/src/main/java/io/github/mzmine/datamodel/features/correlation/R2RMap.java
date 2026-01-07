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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MathUtils;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Map an object to two rows
 *
 * @author Robin Schmid
 */
public class R2RMap<T> extends ConcurrentHashMap<Integer, T> {

  public R2RMap() {
  }

  /**
   * A unique undirected key is computed from the two row.getIDs
   *
   * @param a Feature list row with getID >=0
   * @param b Feature list row with getID >=0
   * @return unique undirected ID
   */
  public static int toKey(FeatureListRow a, FeatureListRow b) {
    return MathUtils.undirectedPairing(a.getID(), b.getID());
  }

  /**
   * Maps a value to two rows by computing an undirected key. Arguments a and b are interchangeable
   * and yield the same mapping.
   *
   * @param value values is mapped to the pair of FeatureListRows a and b
   */
  public void add(FeatureListRow a, FeatureListRow b, T value) {
    this.put(toKey(a, b), value);
  }

  /**
   * Maps a value to two rows by computing an undirected key. Arguments a and b are interchangeable
   * and yield the same mapping.
   *
   * @param value values is mapped to the pair of FeatureListRows a and b
   */
  public void put(FeatureListRow a, FeatureListRow b, T value) {
    super.put(toKey(a, b), value);
  }

  /**
   * Arguments a and b yield the same result in any order. Uses an undirected key to pair a and b.
   *
   * @return the value mapped to the pair of a-b (== b-a) or null if no mapping exists
   */
  public T get(FeatureListRow a, FeatureListRow b) {
    return get(toKey(a, b));
  }

  /**
   * Performance optimised version to get a stream of all correlated rows in this {@link R2RMap}.
   * Mapping is based on the ID of the two rows. Make sure the row and allRows originate from the
   * same feature list as this R2RMap relates to. Rows from other feature lists with common ids will
   * be falsely correlated.
   *
   * @param row     the row to search relationships for
   * @param allRows a collection of all rows to check for correlation
   */
  public Stream<T> streamAllCorrelatedRows(FeatureListRow row, Collection<FeatureListRow> allRows) {
    return allRows.stream().<T>mapMulti((otherRow, consumer) -> {
      final T relationship = get(row, otherRow);
      if (relationship != null) {
        consumer.accept(relationship);
      }
    });
  }

  /**
   * The order of arguments does not matter
   *
   * @return true if get(a,b) is not null
   */
  public boolean contains(final FeatureListRow a, final FeatureListRow b) {
    return get(a, b) != null;
  }
}
