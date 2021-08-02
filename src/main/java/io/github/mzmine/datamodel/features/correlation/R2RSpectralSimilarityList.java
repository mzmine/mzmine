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
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Spectral similarity computed in MZmine.
 */
public class R2RSpectralSimilarityList extends AbstractRowsRelationship {

  private final Type type;
  private final List<SpectralSimilarity> spectralSim = new ArrayList<>();

  /**
   * @param a    row a
   * @param b    row b
   * @param type the similarity type
   */
  public R2RSpectralSimilarityList(FeatureListRow a, FeatureListRow b, Type type) {
    super(a, b);
    this.type = type;
  }

  public synchronized void addSpectralSim(SpectralSimilarity sim) {
    spectralSim.add(sim);
  }

  public int size() {
    return spectralSim.size();
  }

  public List<SpectralSimilarity> getSpectralSim() {
    return spectralSim;
  }

  public int getMaxOverlap() {
    return spectralSim.stream().mapToInt(SpectralSimilarity::overlap).max().orElse(0);
  }

  public int getMinOverlap() {
    return spectralSim.stream().mapToInt(SpectralSimilarity::overlap).min().orElse(0);
  }

  public double getMeanOverlap() {
    return spectralSim.stream().mapToInt(SpectralSimilarity::overlap).average().orElse(0);
  }

  public double getMeanCosineSim() {
    return spectralSim.stream().mapToDouble(SpectralSimilarity::cosine).average().orElse(0);
  }

  public double getMaxCosineSim() {
    return spectralSim.stream().mapToDouble(SpectralSimilarity::cosine).max().orElse(0);
  }

  public double getMinCosineSim() {
    return spectralSim.stream().mapToDouble(SpectralSimilarity::cosine).min().orElse(0);
  }

  @Override
  public double getScore() {
    return getMaxCosineSim();
  }

  @NotNull
  @Override
  public String getAnnotation() {
    return "cos=" + getScoreFormatted();
  }

  @NotNull
  @Override
  public Type getType() {
    return type;
  }
}
