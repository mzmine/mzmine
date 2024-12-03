package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public record RtStandard(FeatureListRow row, HashMap<FeatureList, FeatureListRow> standards) {

  public RtStandard(FeatureListRow row, List<FeatureList> standards) {
    final HashMap<FeatureList, FeatureListRow> map = new HashMap<>();
    standards.forEach(standard -> map.put(standard, null));
    this(row, map);
  }

  public boolean isValid() {
    return standards.values().stream().allMatch(Objects::nonNull);
  }

  public int getNumberOfMatches() {
    return (int) standards.values().stream().filter(Objects::nonNull).count();
  }
}
