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
import org.jetbrains.annotations.NotNull;

/**
 * Cosine similarity between two rows (the best MS2 spectra)
 */
public class R2RSpectralSimilarity extends AbstractRowsRelationship {

  private final Type type;
  private final SpectralSimilarity similarity;

  /**
   * Modified cosine similarity imported from GNPS
   *
   * @param a          the two rows
   * @param b          the two rows
   * @param type       the similarity type
   * @param similarity cosine similarity
   */
  public R2RSpectralSimilarity(FeatureListRow a, FeatureListRow b, Type type,
      SpectralSimilarity similarity) {
    super(a, b);
    this.type = type;
    this.similarity = similarity;
  }

  @Override
  public double getScore() {
    return similarity.cosine();
  }

  @NotNull
  @Override
  public Type getType() {
    return type;
  }

  @NotNull
  @Override
  public String getAnnotation() {
    return "cos=" + getScoreFormatted();
  }

}
