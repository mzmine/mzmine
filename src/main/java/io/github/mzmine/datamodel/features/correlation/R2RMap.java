package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Map an object to two rows
 * 
 * @author Robin Schmid
 *
 */
public class R2RMap<T> extends ConcurrentHashMap<String, T> {
  private static final Logger LOG = Logger.getLogger(R2RMap.class.getName());
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public R2RMap() {}

  /**
   * Redirects to Map.put
   * 
   * @param row
   * @param row2
   * @param value
   */
  public void add(FeatureListRow row, FeatureListRow row2, T value) {
    this.put(toKey(row, row2), value);
  }

  public T get(FeatureListRow row, FeatureListRow row2) {
    return get(toKey(row, row2));
  }

  /**
   * Key as lowID,highID
   * 
   * @param row
   * @param row2
   * @return
   */
  public static String toKey(FeatureListRow row, FeatureListRow row2) {
    int id = row.getID();
    int id2 = row2.getID();
    return Math.min(id, id2) + "," + Math.max(id, id2);
  }

  /**
   * The two row IDs the first is always the lower one
   * 
   * @param key
   * @return
   */
  public static int[] toKeyIDs(String key) {
    return Arrays.stream(key.split(",")).mapToInt(Integer::parseInt).toArray();
  }

}
