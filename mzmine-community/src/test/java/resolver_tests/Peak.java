/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

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
    if (!Precision.equalFloatSignificance(topRt(), other.topRt())) {
      return false;
    }

    if ((rtRange != null && other.rtRange == null) || (rtRange == null && other.rtRange != null)
        || (rtRange != null && other.rtRange != null && !(
        Precision.equalFloatSignificance(rtRange.lowerEndpoint(), other.rtRange.lowerEndpoint())
            && Precision.equalFloatSignificance(rtRange.upperEndpoint(),
            other.rtRange.upperEndpoint())))) {
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
