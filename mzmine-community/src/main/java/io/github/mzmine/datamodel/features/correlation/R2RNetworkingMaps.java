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
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Contains all the {@link R2RMap} for networking of rows
 */
public class R2RNetworkingMaps {

  private static final Logger logger = Logger.getLogger(R2RNetworkingMaps.class.getName());
  // a map that stores row-2-row relationship maps for MS1, MS2, and other relationships
  // key is the type of the relationship
  private final Map<String, R2RMap<RowsRelationship>> r2rMaps = new ConcurrentHashMap<>();

  public void addAllRowsRelationships(R2RMap<? extends RowsRelationship> map, Type relationship) {
    addAllRowsRelationships(map, relationship.toString());
  }

  public void addAllRowsRelationships(R2RMap<? extends RowsRelationship> map, String type) {
    R2RMap<RowsRelationship> rowMap = r2rMaps.computeIfAbsent(type, key -> new R2RMap<>());
    rowMap.putAll(map);
    logger.fine(() -> "Added n=%d feature row relationships (network edges) of type: %s".formatted(
        map.size(), type));
  }

  public void addRowsRelationship(FeatureListRow a, FeatureListRow b,
      RowsRelationship relationship) {
    R2RMap<RowsRelationship> rowMap = r2rMaps.computeIfAbsent(relationship.getType(),
        key -> new R2RMap<>());
    rowMap.add(a, b, relationship);
  }

  @Nullable
  public R2RMap<RowsRelationship> removeAllRowRelationships(Type type) {
    return removeAllRowRelationships(type.toString());
  }

  @Nullable
  public R2RMap<RowsRelationship> removeAllRowRelationships(String type) {
    if (r2rMaps.containsKey(type)) {
      logger.fine("Clearing all feature row relationships (network edges) of type: " + type);
    }
    return r2rMaps.remove(type);
  }

  public void clearAll() {
    if (!r2rMaps.isEmpty()) {
      logger.fine(
          "Clearing all feature row relationships (network edges) of all types: " + r2rMaps.size());
    }
    r2rMaps.clear();
  }

  public Map<String, R2RMap<RowsRelationship>> getRowsMaps() {
    return r2rMaps;
  }

  public Optional<R2RMap<RowsRelationship>> getRowsMap(String type) {
    return Optional.ofNullable(r2rMaps.get(type));
  }


  public boolean isEmpty() {
    return r2rMaps.isEmpty();
  }

  public void addAll(final R2RNetworkingMaps maps) {
    for (final Entry<String, R2RMap<RowsRelationship>> map : maps.getRowsMaps().entrySet()) {
      addAllRowsRelationships(map.getValue(), map.getKey());
    }
  }
}
