/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.networking;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Network statistics
 *
 * @param clusterId     cluster ID of all connected nodes
 * @param communityId   community ID with latest community detection
 * @param edges         number of edges
 * @param clusterSize   cluster size, number of nodes
 * @param communitySize community size, number of nodes
 */
public record NetworkStats(int clusterId, int communityId, int edges, int clusterSize,
                           int communitySize) implements ModularDataRecord {

  @SuppressWarnings("rawtypes")
  public static List<DataType> getSubTypes() {
    return DataTypes.getList(MolNetClusterIdType.class, MolNetCommunityIdType.class,
        MolNetNumEdgesType.class, MolNetClusterSizeType.class, MolNetCommunitySizeType.class);
  }

  public static NetworkStats create(final @NotNull SimpleModularDataModel values) {
    int clusterId = requireNonNullElse(values.get(MolNetClusterIdType.class), -1);
    int communityId = requireNonNullElse(values.get(MolNetCommunityIdType.class), -1);
    int edges = requireNonNullElse(values.get(MolNetNumEdgesType.class), 0);
    int clusterSize = requireNonNullElse(values.get(MolNetClusterSizeType.class), 0);
    int communitySize = requireNonNullElse(values.get(MolNetCommunitySizeType.class), 0);

    return new NetworkStats(clusterId, communityId, edges, clusterSize, communitySize);
  }

  /**
   * Provides data for data types
   *
   * @param sub data column
   * @return the score for this column
   */
  @Override
  public Object getValue(final @NotNull DataType<?> sub) {
    return switch (sub) {
      case MolNetClusterIdType __ -> clusterId;
      case MolNetCommunityIdType __ -> communityId;
      case MolNetNumEdgesType __ -> edges;
      case MolNetClusterSizeType __ -> clusterSize;
      case MolNetCommunitySizeType __ -> communitySize;
      default -> throw new IllegalStateException("Unexpected value: " + sub);
    };
  }
}
