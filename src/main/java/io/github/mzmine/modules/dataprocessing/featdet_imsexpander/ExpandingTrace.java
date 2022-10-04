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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

public class ExpandingTrace {

  private final ModularFeatureListRow f;
  private final Range<Float> rtRange;
  private final Range<Double> mzRange;

  private final Map<MobilityScan, DataPoint> dataPoints = new HashMap<>();

  public ExpandingTrace(@NotNull final ModularFeatureListRow f, Range<Double> mzRange) {
    this.f = f;
    rtRange = f.getBestFeature().getRawDataPointsRTRange();
    this.mzRange = mzRange;
  }

  public ExpandingTrace(@NotNull final ModularFeatureListRow f, Range<Double> mzRange, Range<Float> rtRange) {
    this.f = f;
    this.rtRange = rtRange;
    this.mzRange = mzRange;
  }

  public ModularFeatureListRow getRow() {
    return f;
  }

  /**
   * Offers a data point to this trace.
   *
   * @param access
   * @param index
   * @return true if the data points is added to this trace.
   */
  public boolean offerDataPoint(@NotNull MobilityScanDataAccess access, int index) {
    if (!rtRange.contains(access.getRetentionTime()) || !mzRange.contains(
        access.getMzValue(index))) {
      return false;
    }

    synchronized (dataPoints) {
      return dataPoints.putIfAbsent(access.getCurrentMobilityScan(),
          new SimpleDataPoint(access.getMzValue(index), access.getIntensityValue(index))) == null;
    }
  }

  public IonMobilogramTimeSeries toIonMobilogramTimeSeries(MemoryMapStorage storage,
      BinningMobilogramDataAccess mobilogramDataAccess) {

    final List<IonMobilitySeries> mobilograms = new ArrayList<>();

    int scanStart = 0;
    int scanEnd = 0;
    Frame lastFrame = null;

    final List<MobilityScan> scans = dataPoints.keySet().stream().sorted().toList();
    final List<DataPoint> dataPointsList = this.dataPoints.entrySet().stream()
        .sorted(Comparator.comparing(Entry::getKey)).map(Entry::getValue).toList();

    for (int i = 0; i < scans.size(); i++) {
      final MobilityScan scan = scans.get(i);
      if (lastFrame == null) {
        lastFrame = scan.getFrame();
      } else if (lastFrame != scan.getFrame()) {
        scanEnd = i;

        final double[][] dps = DataPointUtils.getDataPointsAsDoubleArray(
            dataPointsList.subList(scanStart, scanEnd));

        final IonMobilitySeries mobilogram = new SimpleIonMobilitySeries(null, dps[0], dps[1],
            scans.subList(scanStart, scanEnd));
        mobilograms.add(mobilogram);
        scanStart = i;
        lastFrame = scan.getFrame();
      }
    }

    if (scanStart <= scanEnd) {
      final double[][] dps = DataPointUtils.getDataPointsAsDoubleArray(
          dataPointsList.subList(scanStart, dataPoints.size()));
      final IonMobilitySeries mobilogram = new SimpleIonMobilitySeries(null, dps[0], dps[1],
          scans.subList(scanStart, scans.size()));
      mobilograms.add(mobilogram);
    }

    return IonMobilogramTimeSeriesFactory.of(storage, mobilograms, mobilogramDataAccess);
  }

  public Range<Float> getRtRange() {
    return rtRange;
  }

  public Range<Double> getMzRange() {
    return mzRange;
  }

  public int getNumberOfMobilityScans() {
    return dataPoints.size();
  }
}
