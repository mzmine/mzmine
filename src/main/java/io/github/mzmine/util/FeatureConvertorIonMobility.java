/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FeatureConvertorIonMobility {

  public static List<DataPoint> collapseMobilityDimensionOfDataPoints(
      SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> sortedDataPoints) {
    List<DataPoint> summedDataPoints = new ArrayList<>();
    // sum intensity over mobility dimension
    for (Entry<Frame, SortedSet<RetentionTimeMobilityDataPoint>> entry : sortedDataPoints.entrySet()) {
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

  public static SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> groupDataPointsByFrameId(
      @Nonnull final Collection<? extends DataPoint> originalDataPoints) {

    // group by frame & sort ascending
    SortedMap<Frame, SortedSet<RetentionTimeMobilityDataPoint>> sortedDataPoints = new TreeMap<>(
        Comparator.comparingInt(Scan::getScanNumber));
    for (DataPoint dp : originalDataPoints) {
      if (dp instanceof RetentionTimeMobilityDataPoint) {
        Set<RetentionTimeMobilityDataPoint> entry = sortedDataPoints
            .computeIfAbsent(((RetentionTimeMobilityDataPoint) dp).getFrame(),
                scan -> new TreeSet<>(
                    Comparator.comparingInt(o -> o.getMobilityScan().getMobilityScamNumber())));
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
              ((RetentionTimeMobilityDataPoint) dp).getMobilityScan().getMobilityScamNumber());
        } else {
          if (!range.contains(
              ((RetentionTimeMobilityDataPoint) dp).getMobilityScan().getMobilityScamNumber())) {
            range = range.span(Range.singleton(
                ((RetentionTimeMobilityDataPoint) dp).getMobilityScan().getMobilityScamNumber()));
          }
        }
      }
    }
    return range;
  }
}
