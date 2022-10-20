/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureConvertorIonMobility {

  public static List<DataPoint> collapseMobilityDimensionOfDataPoints(
      SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> sortedDataPoints) {
    List<DataPoint> summedDataPoints = new ArrayList<>();
    // sum intensity over mobility dimension
    for (Entry<Frame, SortedSet<RetentionTimeMobilityDataPoint>> entry : sortedDataPoints
        .entrySet()) {
      double mz = 0;
      double intensity = 0;
      for (RetentionTimeMobilityDataPoint dp : entry.getValue()) {
        mz += dp.getMZ();
        intensity += dp.getIntensity();
      }
      DataPoint summedDataPoint = new SimpleDataPoint(mz / entry.getValue().size(), intensity);
      summedDataPoints.add(summedDataPoint);
    }
    return summedDataPoints;
  }

  /**
   * @param originalDataPoints
   * @return Keys (=Frame) sorted by ascending rt + scan number, values (=data points) sorted by ascending
   * mobility scan number.
   */
  public static SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> groupDataPointsByFrameId(
      @NotNull final Collection<? extends DataPoint> originalDataPoints) {

    // group by frame & sort ascending
    SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> sortedDataPoints = new TreeMap<>(
        Comparator.comparingInt(Scan::getScanNumber));
    for (DataPoint dp : originalDataPoints) {
      if (dp instanceof RetentionTimeMobilityDataPoint) {
        Set<RetentionTimeMobilityDataPoint> entry = sortedDataPoints
            .computeIfAbsent(((RetentionTimeMobilityDataPoint) dp).getFrame(),
                scan -> new TreeSet<>(
                    Comparator.comparingInt(o -> o.getMobilityScan().getMobilityScanNumber())));
        entry.add((RetentionTimeMobilityDataPoint) dp);
      } else {
        throw new IllegalArgumentException("IMS feature contains invalid data points.");
      }
    }

    return sortedDataPoints;
  }

  @Nullable
  public static Range<Integer> getDataPointsMobilityScanNumberRange(
      List<? extends DataPoint> dataPoints) {
    Range<Integer> range = null;
    for (DataPoint dp : dataPoints) {
      if (!(dp instanceof RetentionTimeMobilityDataPoint)) {
        throw new IllegalArgumentException("IMS feature contains invalid data points.");
      } else {
        if (range == null) {
          range = Range.singleton(
              ((RetentionTimeMobilityDataPoint) dp).getMobilityScan().getMobilityScanNumber());
        } else {
          if (!range.contains(
              ((RetentionTimeMobilityDataPoint) dp).getMobilityScan().getMobilityScanNumber())) {
            range = range.span(Range.singleton(
                ((RetentionTimeMobilityDataPoint) dp).getMobilityScan().getMobilityScanNumber()));
          }
        }
      }
    }
    return range;
  }
}
