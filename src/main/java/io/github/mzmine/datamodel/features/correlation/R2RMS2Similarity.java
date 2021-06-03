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

public class R2RMS2Similarity {

  private final FeatureListRow a;
  private final FeatureListRow b;
  //
  private final List<MS2Similarity> massDiffSim = new ArrayList<>();
  private final List<MS2Similarity> spectralSim = new ArrayList<>();
  private MS2Similarity gnpsSim = null;

  public R2RMS2Similarity(FeatureListRow a, FeatureListRow b) {
    super();
    this.a = a;
    this.b = b;
  }

  public void addMassDiffSim(MS2Similarity sim) {
    massDiffSim.add(sim);
  }

  public void addSpectralSim(MS2Similarity sim) {
    spectralSim.add(sim);
  }

  public void addGNPSSim(MS2Similarity sim) {
    gnpsSim = sim;
  }


  public int size() {
    return Math.max(gnpsSim != null ? 1 : 0, Math.max(massDiffSim.size(), spectralSim.size()));
  }

  public List<MS2Similarity> getMassDiffSim() {
    return massDiffSim;
  }

  public List<MS2Similarity> getSpectralSim() {
    return spectralSim;
  }

  public MS2Similarity getGNPSSim() {
    return gnpsSim;
  }

  public FeatureListRow getA() {
    return a;
  }

  public FeatureListRow getB() {
    return b;
  }

  public int getDiffMaxOverlap() {
    if (massDiffSim.isEmpty()) {
      return 0;
    }
    return massDiffSim.stream().mapToInt(MS2Similarity::getOverlap).max().getAsInt();
  }

  public int getDiffMinOverlap() {
    if (massDiffSim.isEmpty()) {
      return 0;
    }
    return massDiffSim.stream().mapToInt(MS2Similarity::getOverlap).min().getAsInt();
  }

  public double getDiffAvgOverlap() {
    if (massDiffSim.isEmpty()) {
      return 0;
    }
    return massDiffSim.stream().mapToInt(MS2Similarity::getOverlap).average().getAsDouble();
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

  public double getDiffAvgCosine() {
    if (massDiffSim.isEmpty()) {
      return 0;
    }
    return massDiffSim.stream().mapToDouble(MS2Similarity::getCosine).average().getAsDouble();
  }

  public double getDiffMaxCosine() {
    if (massDiffSim.isEmpty()) {
      return 0;
    }
    return massDiffSim.stream().mapToDouble(MS2Similarity::getCosine).max().getAsDouble();
  }

  public double getDiffMinCosine() {
    if (massDiffSim.isEmpty()) {
      return 0;
    }
    return massDiffSim.stream().mapToDouble(MS2Similarity::getCosine).min().getAsDouble();
  }

}
