/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.RowGroup;
import java.util.List;

public abstract class MS2SimilarityProviderGroup extends RowGroup {

  public MS2SimilarityProviderGroup(List<RawDataFile> raw, int groupID) {
    super(raw, groupID);
  }

  /**
   * A map for row-2-row MS2 similarity
   *
   * @return
   */
  public abstract R2RMap<R2RMS2CosineSimilarity> getMS2SimilarityMap();

  /**
   * Similarity map for row-2-row MS2 comparison
   *
   * @param map
   * @return
   */
  public abstract void setMS2SimilarityMap(R2RMap<R2RMS2CosineSimilarity> map);
}
