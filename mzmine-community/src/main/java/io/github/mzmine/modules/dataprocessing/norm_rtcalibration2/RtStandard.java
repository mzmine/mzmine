package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MathUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RtStandard {

  private final HashMap<@NotNull RawDataFile, @Nullable FeatureListRow> standards; // must be a hash map. supports null values.
  private Float medianRt = null;
  private Float avgRt = null;

  public RtStandard(HashMap<RawDataFile, FeatureListRow> standards) {
    this.standards = standards;
  }

  public RtStandard(List<FeatureList> standards) {
    final HashMap<RawDataFile, FeatureListRow> map = new HashMap<>();
    standards.forEach(standard -> map.put(standard.getRawDataFile(0), null));
    this(map);
  }

  public boolean isValid() {
    return standards.values().stream().allMatch(Objects::nonNull);
  }

  public int getNumberOfMatches() {
    return (int) standards.values().stream().filter(Objects::nonNull).count();
  }

  private float getMedianRt() {
    if (medianRt == null) {
      medianRt = (float) MathUtils.calcQuantileSorted(
          standards.values().stream().filter(Objects::nonNull)
              .mapToDouble(FeatureListRow::getAverageRT).sorted().toArray(), 0.5);
    }
    return medianRt;
  }

  private float getAverageRt() {
    if (avgRt == null) {
      avgRt = (float) standards.values().stream().filter(Objects::nonNull)
          .mapToDouble(FeatureListRow::getAverageRT).average().getAsDouble();
    }
    return avgRt;
  }

  public float getRt(RTMeasure measure) {
    return switch (measure) {
      case MEDIAN -> getMedianRt();
      case AVERAGE -> getAverageRt();
    };
  }

  public HashMap<RawDataFile, FeatureListRow> standards() {
    return standards;
  }

  @Override
  public String toString() {
    return "RtStandard[" + "standards=" + standards + ']';
  }

}
