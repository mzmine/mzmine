package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MathUtils;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Map an object to two rows
 *
 * @author Robin Schmid
 */
public class R2RMap<T> extends ConcurrentHashMap<Integer, T> {

  private static final Logger LOG = Logger.getLogger(R2RMap.class.getName());
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public R2RMap() {
  }

  /**
   * Redirects to Map.put
   *
   * @param a
   * @param b
   * @param value
   */
  public void add(FeatureListRow a, FeatureListRow b, T value) {
    this.put(toKey(a, b), value);
  }

  public T get(FeatureListRow a, FeatureListRow b) {
    return get(toKey(a, b));
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

}
