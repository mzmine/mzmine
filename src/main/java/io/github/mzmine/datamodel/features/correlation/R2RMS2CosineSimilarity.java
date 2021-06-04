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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * MS2 similarity computed in MZmine.
 */
public class R2RMS2CosineSimilarity implements RowsRelationship {

  private final FeatureListRow a;
  private final FeatureListRow b;
  //
  private final List<MS2Similarity> spectralSim = new ArrayList<>();

  public R2RMS2CosineSimilarity(FeatureListRow a, FeatureListRow b) {
    super();
    this.a = a;
    this.b = b;
  }

  public void addSpectralSim(MS2Similarity sim) {
    spectralSim.add(sim);
  }

  public int size() {
    return spectralSim.size();
  }

  public List<MS2Similarity> getSpectralSim() {
    return spectralSim;
  }

  public int getSpectralMaxOverlap() {
    if (spectralSim.isEmpty()) {
      return 0;
    }
    return spectralSim.stream().mapToInt(MS2Similarity::getOverlap).max().getAsInt();
  }

  public int getSpectralMinOverlap() {
    if (spectralSim.isEmpty()) {
      return 0;
    }
    return spectralSim.stream().mapToInt(MS2Similarity::getOverlap).min().getAsInt();
  }

  public double getSpectralAvgOverlap() {
    if (spectralSim.isEmpty()) {
      return 0;
    }
    return spectralSim.stream().mapToInt(MS2Similarity::getOverlap).average().getAsDouble();
  }

  // COSINE
  public double getSpectralAvgCosine() {
    if (spectralSim.isEmpty()) {
      return 0;
    }
    return spectralSim.stream().mapToDouble(MS2Similarity::getCosine).average().getAsDouble();
  }

  public double getSpectralMaxCosine() {
    if (spectralSim.isEmpty()) {
      return 0;
    }
    return spectralSim.stream().mapToDouble(MS2Similarity::getCosine).max().getAsDouble();
  }

  public double getSpectralMinCosine() {
    if (spectralSim.isEmpty()) {
      return 0;
    }
    return spectralSim.stream().mapToDouble(MS2Similarity::getCosine).min().getAsDouble();
  }

  @Override
  public double getScore() {
    return getSpectralMaxCosine();
  }

  @Nonnull
  @Override
  public Type getType() {
    return Type.MS2_COSINE_SIM;
  }

  @Nonnull
  @Override
  public String getAnnotation() {
    return "cos="+getScoreFormatted();
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
