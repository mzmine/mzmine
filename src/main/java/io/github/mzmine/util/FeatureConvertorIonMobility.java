package io.github.mzmine.util;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.RawFileType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class FeatureConvertorIonMobility {

  /**
   * @param originalFeature The feature to collapse
   * @return A rudimentary modular feature with just scan numbers & data points assigned.
   */
  public static ModularFeature collapseMobilityDimensionOfModularFeature(
      ModularFeature originalFeature) {
    ModularFeatureList flist = (ModularFeatureList) originalFeature.getFeatureList();
    if (flist == null) {
      throw new NullPointerException("Feature list of the ModularFeature is null.");
    }
    if (!(originalFeature.getRawDataFile() instanceof IMSRawDataFile)) {
      throw new IllegalArgumentException(
          "Cannot collapse mobility dimension for features that were not created from IMSRawDataFiles.");
    }

    // extract data points
    ModularFeature newFeature = new ModularFeature(flist);
    newFeature.set(RawFileType.class, originalFeature.getRawDataFile());

    List<? extends DataPoint> originalDataPoints = originalFeature.getDataPoints();
    List<RetentionTimeMobilityDataPoint> mobilityDataPoints =
        new ArrayList<>(originalDataPoints.size());
    for (DataPoint dp : originalDataPoints) {
      if (dp instanceof RetentionTimeMobilityDataPoint) {
        mobilityDataPoints.add((RetentionTimeMobilityDataPoint) dp);
      } else {
        throw new IllegalArgumentException("IMS feature contains invalid data points.");
      }
    }

    // group by frame & sort ascending
    Map<Integer, Set<RetentionTimeMobilityDataPoint>> sortedDataPoints = new TreeMap<>(
        Integer::compareTo);
    for (RetentionTimeMobilityDataPoint dp : mobilityDataPoints) {
      Set<RetentionTimeMobilityDataPoint> entry = sortedDataPoints
          .computeIfAbsent(dp.getFrameNumber(), HashSet::new);
      entry.add(dp);
    }

    double maxIntensity = 0;
    // sum intensity over mobility dimension
    for (Entry<Integer, Set<RetentionTimeMobilityDataPoint>> entry : sortedDataPoints.entrySet()) {
      int frameNumber = entry.getKey();
      double mz = 0;
      double intensity = 0;
      for (RetentionTimeMobilityDataPoint dp : entry.getValue()) {
        mz += dp.getMZ();
        intensity += dp.getIntensity();
        if (intensity > maxIntensity) {
          maxIntensity = intensity;
        }
      }
      DataPoint summedDataPoint = new SimpleDataPoint(mz / entry.getValue().size(), intensity);
      newFeature.getScanNumbers().add(frameNumber);
      newFeature.getDataPoints().add(summedDataPoint);
    }
    newFeature.setHeight((float) maxIntensity);

    double mz = 0;
    final double totalIntensity = newFeature.getDataPoints().stream()
        .mapToDouble(DataPoint::getIntensity).sum();
    for(DataPoint dp : newFeature.getDataPoints()) {
      mz += dp.getMZ() * (dp.getIntensity() / totalIntensity);
    }
    newFeature.setMZ(mz);

    // i don't think we need anything else to rt-resolve a feature
    return newFeature;
  }
}
