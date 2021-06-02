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
