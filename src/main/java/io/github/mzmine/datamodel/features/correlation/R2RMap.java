/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MathUtils;
import java.util.concurrent.ConcurrentHashMap;

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

}
