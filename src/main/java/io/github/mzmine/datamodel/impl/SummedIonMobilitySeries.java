package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MsSeries;
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
      for (int j = 0; j < mobilogram.getNumberOfDataPoints(); j++) {
        Integer scannum = mobilogram.getScan(j).getMobilityScamNumber();
        Double intensity = intensities.get(scannum);
        if(intensity != null) {
          intensity += mobilogram.getIntensityValue(j);
          intensities.put(scannum, intensity);
        }
        else {
          mobilities
              .put(mobilogram.getScan(j).getMobilityScamNumber(), mobilogram.getMobility(j));
          intensities.put(scannum, mobilogram.getIntensityValue(j));
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

  public MsSeries<MobilityScan> copy(MemoryMapStorage storage) {
    return null;
  }
}
