package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MathUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RtStandard {

  private final FeatureListRow row;
  private final HashMap<@NotNull FeatureList, @Nullable FeatureListRow> standards; // must be a hash map. supports null values.
  private Float medianRt = null;

  public RtStandard(FeatureListRow row, HashMap<FeatureList, FeatureListRow> standards) {
    this.row = row;
    this.standards = standards;
  }

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

  public float getMedianRt() {
    if (medianRt == null) {
      medianRt = (float) MathUtils.calcQuantileSorted(
          standards.values().stream().filter(Objects::nonNull)
              .mapToDouble(FeatureListRow::getAverageRT).sorted().toArray(), 0.5);
    }
    return medianRt;
  }

  public FeatureListRow row() {
    return row;
  }

  public HashMap<FeatureList, FeatureListRow> standards() {
    return standards;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (RtStandard) obj;
    return Objects.equals(this.row, that.row) && Objects.equals(this.standards, that.standards);
  }

  @Override
  public int hashCode() {
    return Objects.hash(row, standards);
  }

  @Override
  public String toString() {
    return "RtStandard[" + "row=" + row + ", " + "standards=" + standards + ']';
  }

}
