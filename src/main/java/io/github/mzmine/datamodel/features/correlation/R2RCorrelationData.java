package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrelationData {

  public enum NegativeMarker {
    // intensity range is not shared between these two rows
    // at least in one raw data file: the features are out of RT range
    // the features do not overlap with X % of their intensity
    AntiOverlap, //
    MinFeaturesRequirementNotMet, //
    OutOfRTRange; // Features are out of RT range

    public static NegativeMarker fromOverlapResult(MinimumFeatureFilter.OverlapResult overlap) {
      switch (overlap) {
        case AntiOverlap:
          return NegativeMarker.AntiOverlap;
        case BelowMinSamples:
          return NegativeMarker.MinFeaturesRequirementNotMet;
        case OutOfRTRange:
          return NegativeMarker.OutOfRTRange;
      }
      return null;
    }
  }

  // correlation of a to b
  // id A < id B
  private final FeatureListRow a;
  private final FeatureListRow b;

  // ANTI CORRELATION MARKERS
  // to be used to exclude rows from beeing grouped
  private List<NegativeMarker> negativMarkers;

  public R2RCorrelationData(FeatureListRow a, FeatureListRow b) {
    if (a.getID() < b.getID()) {
      this.a = a;
      this.b = b;
    } else {
      this.b = a;
      this.a = b;
    }
  }

  /**
   * Stream all R2RFullCorrelationData found in PKLRowGroups (is distinct)
   * 
   * @param FeatureList
   * @return
   */
  public static Stream<R2RFullCorrelationData> streamFrom(FeatureList FeatureList) {
    if (FeatureList.getGroups() == null)
      return Stream.empty();
    return FeatureList.getGroups().stream().filter(g -> g instanceof CorrelationRowGroup)
        .map(g -> ((CorrelationRowGroup) g).getCorr()).flatMap(Arrays::stream) // R2GCorr
        .flatMap(r2g -> r2g.getCorr() == null ? null
            : r2g.getCorr().stream() //
                .filter(r2r -> r2r.getRowA().equals(r2g.getRow()))); // a is always the lower id
  }

  public static Stream<R2RFullCorrelationData> streamFrom(FeatureListRow[] rows) {
    return Arrays.stream(rows).map(FeatureListRow::getGroup).filter(Objects::nonNull)
        .filter(g -> g instanceof CorrelationRowGroup).distinct().map(g -> ((CorrelationRowGroup) g).getCorr())
        .flatMap(Arrays::stream) // R2GCorr
        .flatMap(r2g -> r2g.getCorr() == null ? null
            : r2g.getCorr().stream() //
                .filter(r2r -> r2r.getRowA().equals(r2g.getRow()))); // a is always the lower id
  }

  /**
   * 
   * @return List of negativ markers (non-null)
   */
  public @Nonnull List<NegativeMarker> getNegativMarkers() {
    return negativMarkers == null ? new ArrayList<>() : negativMarkers;
  }

  public int getNegativMarkerCount() {
    return negativMarkers == null ? 0 : negativMarkers.size();
  }

  /**
   * Negativ marker for this correlation (exclude from further grouping)
   * 
   * @param nm
   */
  public void addNegativMarker(NegativeMarker nm) {
    if (negativMarkers == null)
      negativMarkers = new ArrayList<>();
    negativMarkers.add(nm);
  }

  public FeatureListRow getRowA() {
    return a;
  }

  public FeatureListRow getRowB() {
    return b;
  }

  public boolean hasFeatureShapeCorrelation() {
    return false;
  }

  public double getAvgShapeR() {
    return 0;
  }

  public double getAvgShapeCosineSim() {
    return 0;
  }


}
