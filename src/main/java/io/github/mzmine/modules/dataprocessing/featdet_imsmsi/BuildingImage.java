package io.github.mzmine.modules.dataprocessing.featdet_imsmsi;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BuildingImage {
  private final ModularFeature originalFeature;
  private final Map<MobilityScan, RetentionTimeMobilityDataPoint> dps;

  public BuildingImage(ModularFeature f) {
    originalFeature = f;
    dps = new HashMap<>();
  }

  public boolean addDataPoint(RetentionTimeMobilityDataPoint dp) {
    return dps.putIfAbsent(dp.getMobilityScan(), dp) == null;
  }

  public ModularFeature getOriginalFeature() {
    return originalFeature;
  }

  public Collection<RetentionTimeMobilityDataPoint> getDataPoints() {
    return dps.values();
  }
}
