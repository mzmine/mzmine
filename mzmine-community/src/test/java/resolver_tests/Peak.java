package resolver_tests;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.util.maths.Precision;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/**
 * Model peak for testing resolvers. not public api. Generate from resolver output via
 * {@link Peak#constructorCall()}
 *
 */
record Peak(float topRt, @Nullable Range<Float> rtRange, @Nullable String desc) {

  public Peak(float topRt) {
    this(topRt, null, null);
  }

  static Peak top(float topRt) {
    return new Peak(topRt);
  }

  static Peak topRange(float topRt, Range<Float> rtRange) {
    return new Peak(topRt, rtRange, null);
  }

  boolean find(List<Peak> otherPeaks) {

    for (Peak otherPeak : otherPeaks) {
      if (matches(otherPeak)) {
        return true;
      }
    }
    return false;
  }

  boolean matches(@Nullable Peak other) {
    if (other == null) {
      return false;
    }
    if (!Precision.equalSignificance(topRt(), other.topRt(), 4)) {
      return false;
    }

    if ((rtRange != null && other.rtRange == null) || (rtRange == null && other.rtRange != null)
        || (rtRange != null && other.rtRange != null && !(Precision.equalSignificance(
        rtRange.lowerEndpoint(), other.rtRange.lowerEndpoint(), 4) && Precision.equalSignificance(
        rtRange.upperEndpoint(), other.rtRange.upperEndpoint(), 4)))) {
      return false;
    }
    return true;
  }

  static <T extends Scan> void printConstructorCalls(List<IonTimeSeries<T>> peaks) {
    String calls = peaks.stream().map(Peak::of).map(Peak::constructorCall)
        .collect(Collectors.joining(",\n"));
    System.out.println(calls);
  }

  static <T extends Scan> Peak of(IonTimeSeries<T> series) {
    return new Peak(series.getRetentionTime(FeatureDataUtils.getMostIntenseIndex(series)),
        Range.closed(series.getRetentionTime(0),
            series.getRetentionTime(series.getNumberOfValues() - 1)), null);
  }

  static <T extends Scan> List<Peak> of(List<IonTimeSeries<T>> series) {
    return series.stream().map(Peak::of).toList();
  }

  public String constructorCall() {
    if (rtRange != null) {
      return "Peak.topRange(%ff, Range.closed(%ff, %ff))".formatted(topRt, rtRange.lowerEndpoint(),
          rtRange.upperEndpoint());
    }
    return "Peak.top(%ff)\n".formatted(topRt);
  }
}
