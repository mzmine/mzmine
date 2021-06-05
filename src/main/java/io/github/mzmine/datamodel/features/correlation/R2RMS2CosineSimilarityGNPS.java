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

import io.github.mzmine.datamodel.features.FeatureListRow;
import javax.annotation.Nonnull;

/**
 * Modified cosine similarity between two rows imported from GNPS results
 */
public class R2RMS2CosineSimilarityGNPS implements RowsRelationship {

  private final FeatureListRow a;
  private final FeatureListRow b;
  //
  private final double cosine;
  private final String annotation;
  private final String edgeType;

  /**
   * Modified cosine similarity imported from GNPS
   *
   * @param a      the two rows
   * @param b      the two rows
   * @param cosine cosine similarity
   */
  public R2RMS2CosineSimilarityGNPS(FeatureListRow a, FeatureListRow b, double cosine,
      String annotation, String edgeType) {
    this.a = a;
    this.b = b;
    this.cosine = cosine;
    this.annotation = annotation;
    this.edgeType = edgeType;
  }

  /**
   * The edge type string used by GNPS
   */
  public String getGNPSEdgeType() {
    return edgeType;
  }

  public double getCosineSimilarity() {
    return cosine;
  }

  @Override
  public double getScore() {
    return getCosineSimilarity();
  }

  @Nonnull
  @Override
  public Type getType() {
    return Type.MS2_GNPS_COSINE_SIM;
  }

  @Nonnull
  @Override
  public String getAnnotation() {
    return annotation == null || annotation.strip().isEmpty() ? "cos=" + getScoreFormatted()
        : annotation;
  }

  @Override
  public FeatureListRow getRowA() {
    return a;
  }

  @Override
  public FeatureListRow getRowB() {
    return b;
  }
}
