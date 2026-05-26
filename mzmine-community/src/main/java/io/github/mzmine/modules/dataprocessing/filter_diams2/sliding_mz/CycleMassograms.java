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

package io.github.mzmine.modules.dataprocessing.filter_diams2.sliding_mz;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single MS2 cycle with all fragment ion traces. Use the {@link CycleMassograms(List)}
 * constructor.
 */
public record CycleMassograms(@NotNull List<Scan> ms2Scans, @NotNull Range<Float> rtRange,
                              @NotNull RangeMap<Double, @NotNull ModularFeature> massograms,
                              double @NotNull [] isolationCenters,
                              @NotNull List<@NotNull SimpleDoubleRange> isolationRanges,
                              ModularFeatureList dummyFlist) {

  public static final double isolationWidthFactor = 5;
  private static final Logger logger = Logger.getLogger(CycleMassograms.class.getName());
  public static AtomicLong allRequest = new AtomicLong();
  public static AtomicLong cachedRequests = new AtomicLong();


  public CycleMassograms(@NotNull List<Scan> ms2Scans, ModularFeatureList dummyFlist) {

    final List<SimpleDoubleRange> isolationRanges = new ArrayList<>();
    final double[] isolationCenters = new double[ms2Scans.size()];

    for (int i = 0; i < ms2Scans.size(); i++) {
      Scan scan = ms2Scans.get(i);
      final MsMsInfo msMsInfo = scan.getMsMsInfo();
      if (msMsInfo == null) {
        throw new IllegalStateException(
            "No msms info for scan %s".formatted(ScanUtils.scanToString(scan)));
      }

      final Range<Double> reportedIsolation = msMsInfo.getIsolationWindow();
      final double center = RangeUtils.rangeCenter(reportedIsolation);
      final double halfLength =
          RangeUtils.rangeLength(reportedIsolation) * isolationWidthFactor * 0.5;

      isolationRanges.add(new SimpleDoubleRange(center - halfLength, center + halfLength));
      isolationCenters[i] = center;
    }

    this(ms2Scans,
        Range.closed(ms2Scans.getFirst().getRetentionTime(), ms2Scans.getLast().getRetentionTime()),
        TreeRangeMap.create(), isolationCenters, isolationRanges, dummyFlist);
  }

  public Double2ObjectMap<ModularFeature> getTraces(final double[] relevantMzs,
      final MZTolerance tolerance, @Nullable MemoryMapStorage storage) {

    final List<Range<Double>> toExtract = new ArrayList<>();

    for (double mz : relevantMzs) {
      final ModularFeature series = massograms.get(mz);
      if (series == null) {
        Range<Double> range = SpectraMerging.createNewNonOverlappingRange(massograms,
            tolerance.getToleranceRange(mz));
        // need to put the actual range so we dont create overlaps due to them being absent
        massograms.put(range, new ModularFeature(dummyFlist, ms2Scans.getFirst().getDataFile(),
            FeatureStatus.DETECTED)); // put empty since we cannot put null
        toExtract.add(range);
      }
    }

    allRequest.getAndAdd(relevantMzs.length);
    cachedRequests.getAndAdd(relevantMzs.length - toExtract.size());

    if (!toExtract.isEmpty()) {
      final ExtractMzRangesIonSeriesFunction extract = new ExtractMzRangesIonSeriesFunction(
          ms2Scans.getFirst().getDataFile(), ms2Scans, toExtract, ScanDataType.MASS_LIST, null);
      @NotNull BuildingIonSeries[] buildingIonSeries = extract.get();
      List<? extends IonTimeSeries<? extends Scan>> result = Arrays.stream(buildingIonSeries)
          // important to use full ion time series, so we can refer to the same index all the time
          .map(b -> b.toFullIonTimeSeries(storage, ms2Scans)).toList();
      for (int i = 0; i < result.size(); i++) {
        ModularFeature massogram = massograms.get(RangeUtils.rangeCenter(toExtract.get(i)));
        massogram.set(FeatureDataType.class, result.get(i));
        FeatureDataUtils.recalculateIonSeriesDependingTypes(massogram,
            FeatureDataUtils.DEFAULT_CENTER_FUNCTION, false);
      }
    }

    final Double2ObjectMap<ModularFeature> result = new Double2ObjectOpenHashMap<>(relevantMzs.length);
    for (double mzs : relevantMzs) {
      final ModularFeature series = massograms.get(mzs);
      result.put(mzs, series);
    }

    return result;
  }

  public @NotNull SimpleDoubleRange isolationRange(int i) {
    return isolationRanges.get(i);
  }

  public double isolationCenter(int i) {
    return isolationCenters[i];
  }
}