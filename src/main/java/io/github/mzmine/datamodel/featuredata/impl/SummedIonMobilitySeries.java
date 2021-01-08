/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SummedIonMobilitySeries {

  DoubleBuffer intensityValues;
  final double mz;
  DoubleBuffer mobilityValues;

  SummedIonMobilitySeries(MemoryMapStorage storage, List<SimpleIonMobilitySeries> mobilograms,
      double mz) {

    this.mz = mz;
    Map<Integer, Double> intensities = new TreeMap<>();
    Map<Integer, Double> mobilities = new TreeMap<>();

    for (int i = 0; i < mobilograms.size(); i++) {
      SimpleIonMobilitySeries mobilogram = mobilograms.get(i);
      for (int j = 0; j < mobilogram.getNumberOfValues(); j++) {
        Integer scannum = mobilogram.getScan(j).getMobilityScamNumber();
        Double intensity = intensities.get(scannum);
        if (intensity != null) {
          intensity += mobilogram.getIntensity(j);
          intensities.put(scannum, intensity);
        } else {
          mobilities
              .put(mobilogram.getScan(j).getMobilityScamNumber(), mobilogram.getMobility(j));
          intensities.put(scannum, mobilogram.getIntensity(j));
        }
      }
    }

    try {
      intensityValues = storage.storeData(intensities.values().stream().mapToDouble(Double::doubleValue).toArray());
      mobilityValues = storage.storeData(mobilities.values().stream().mapToDouble(Double::doubleValue).toArray());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public int getNumberOfDataPoints() {
    return getMobilityValues().capacity();
  }

  public double getIntensity(int index) {
    return getIntensityValues().get(index);
  }

  public double getMobility(int index) {
    return getMobilityValues().get(index);
  }

  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  public DoubleBuffer getMobilityValues() {
    return mobilityValues;
  }

  public IonSpectrumSeries<MobilityScan> copy(MemoryMapStorage storage) {
    return null;
  }
}
