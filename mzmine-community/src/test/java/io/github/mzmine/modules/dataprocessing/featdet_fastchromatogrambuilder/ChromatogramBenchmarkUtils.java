/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.project.impl.RawDataFileImpl;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Conversions between raw data files, feature lists and the plain arrays used to evaluate
 * chromatogram builders.
 */
final class ChromatogramBenchmarkUtils {

  private ChromatogramBenchmarkUtils() {
  }

  /**
   * Creates an in memory raw data file with one MS1 scan and mass list per synthetic scan.
   */
  @NotNull
  static RawDataFile toRawDataFile(@NotNull SyntheticLcmsData data, @NotNull String name) {
    final RawDataFileImpl file = new RawDataFileImpl(name, null, null, Color.BLACK);
    for (int s = 0; s < data.numScans(); s++) {
      final double[] mzs = data.mzs[s];
      final double[] intensities = data.intensities[s];
      final SimpleScan scan = new SimpleScan(file, s + 1, 1, 0.01f * s, null, mzs, intensities,
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(50d, 2000d));
      scan.addMassList(new SimpleMassList(null, mzs, intensities));
      file.addScan(scan);
    }
    return file;
  }

  /**
   * Detected data points of every feature of a chromatogram list, zeros are removed.
   *
   * @param scans the selected scans, the index in this array is the scan index
   */
  @NotNull
  static List<EvaluatedChromatogram> toChromatograms(@NotNull FeatureList flist,
      @NotNull Scan[] scans) {
    final Map<Scan, Integer> scanIndex = new IdentityHashMap<>(scans.length);
    for (int i = 0; i < scans.length; i++) {
      scanIndex.put(scans[i], i);
    }
    final List<EvaluatedChromatogram> chromatograms = new ArrayList<>(flist.getNumberOfRows());
    for (final FeatureListRow row : flist.getRows()) {
      for (final Feature feature : row.getFeatures()) {
        final IonTimeSeries<? extends Scan> series = feature.getFeatureData();
        final IntArrayList indices = new IntArrayList(series.getNumberOfValues());
        final DoubleArrayList mzs = new DoubleArrayList(series.getNumberOfValues());
        final DoubleArrayList intensities = new DoubleArrayList(series.getNumberOfValues());
        for (int i = 0; i < series.getNumberOfValues(); i++) {
          final double intensity = series.getIntensity(i);
          if (intensity <= 0) {
            continue;
          }
          indices.add(scanIndex.get(series.getSpectrum(i)).intValue());
          mzs.add(series.getMZ(i));
          intensities.add(intensity);
        }
        chromatograms.add(new EvaluatedChromatogram(indices.toIntArray(), mzs.toDoubleArray(),
            intensities.toDoubleArray()));
      }
    }
    return chromatograms;
  }

  /**
   * @return m/z values of all mass lists, index is the scan index
   */
  @NotNull
  static double[][] massListMzs(@NotNull Scan[] scans) {
    final double[][] mzs = new double[scans.length][];
    for (int i = 0; i < scans.length; i++) {
      final MassList masses = scans[i].getMassList();
      mzs[i] = masses == null ? new double[0]
          : masses.getMzValues(new double[masses.getNumberOfDataPoints()]);
    }
    return mzs;
  }

  /**
   * @return intensities of all mass lists, index is the scan index, same order as
   * {@link #massListMzs(Scan[])}
   */
  @NotNull
  static double[][] massListIntensities(@NotNull Scan[] scans) {
    final double[][] intensities = new double[scans.length][];
    for (int i = 0; i < scans.length; i++) {
      final MassList masses = scans[i].getMassList();
      intensities[i] = masses == null ? new double[0]
          : masses.getIntensityValues(new double[masses.getNumberOfDataPoints()]);
    }
    return intensities;
  }

  /**
   * @return m/z of every feature of a feature list, same order as
   * {@link #toChromatograms(FeatureList, Scan[])}
   */
  @NotNull
  static double[] featureMzs(@NotNull FeatureList flist) {
    final DoubleArrayList mzs = new DoubleArrayList(flist.getNumberOfRows());
    for (final FeatureListRow row : flist.getRows()) {
      for (final Feature feature : row.getFeatures()) {
        mzs.add(feature.getMZ());
      }
    }
    return mzs.toDoubleArray();
  }

  /**
   * @return the value at the quantile of the sorted values, 0 if empty
   */
  static double quantile(@NotNull DoubleArrayList values, double quantile) {
    if (values.isEmpty()) {
      return 0;
    }
    final double[] sorted = values.toDoubleArray();
    Arrays.sort(sorted);
    return sorted[Math.min(sorted.length - 1, (int) (quantile * sorted.length))];
  }

  /**
   * @return allocated bytes of the current thread, includes garbage
   */
  static long allocatedBytes() {
    return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getCurrentThreadAllocatedBytes();
  }
}
