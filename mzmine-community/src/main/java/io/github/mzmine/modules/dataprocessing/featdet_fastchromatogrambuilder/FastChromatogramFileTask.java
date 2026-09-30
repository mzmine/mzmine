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

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the chromatograms of one raw data file for {@link FastChromatogramBuilderTask}. The
 * feature list is not added to the project, the main task adds all lists in the order of the data
 * files.
 */
final class FastChromatogramFileTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FastChromatogramFileTask.class.getName());

  private final @NotNull RawDataFile dataFile;
  private final @NotNull Scan[] scans;
  private final @NotNull MZTolerance mzTolerance;
  private final int minimumConsecutiveScans;
  private final double minGroupIntensity;
  private final double minHighestPoint;
  private final @NotNull String suffix;
  private final @NotNull ParameterSet parameters;
  private final @NotNull Class<? extends MZmineModule> callingModule;
  private volatile double progress = 0d;
  private @Nullable ModularFeatureList featureList;
  private @Nullable FastChromatogramBuilderStatistics statistics;

  /**
   * @param scans      the selected scans of the data file, checked for mass lists and retention
   *                   time order
   * @param parameters stored as applied method, contains the used tolerance
   */
  FastChromatogramFileTask(@NotNull RawDataFile dataFile, @NotNull Scan[] scans,
      @NotNull MZTolerance mzTolerance, int minimumConsecutiveScans, double minGroupIntensity,
      double minHighestPoint, @NotNull String suffix, @NotNull ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> callingModule) {
    super(storage, moduleCallDate);
    this.dataFile = dataFile;
    this.scans = scans;
    this.mzTolerance = mzTolerance;
    this.minimumConsecutiveScans = minimumConsecutiveScans;
    this.minGroupIntensity = minGroupIntensity;
    this.minHighestPoint = minHighestPoint;
    this.suffix = suffix;
    this.parameters = parameters;
    this.callingModule = callingModule;
  }

  @Override
  public String getTaskDescription() {
    return "Detecting chromatograms in " + dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final ScanDataAccess access = EfficientDataAccess.of(dataFile, ScanDataType.MASS_LIST,
        Arrays.asList(scans));
    final FastChromatogramBuilder builder = new FastChromatogramBuilder(mzTolerance,
        minimumConsecutiveScans, minGroupIntensity, minHighestPoint);
    final List<BuiltChromatogram> chromatograms;
    try {
      chromatograms = builder.build(new ScanDataAccessScans(access), this::isCanceled,
          p -> progress = 0.8 * p);
    } catch (MissingMassListException e) {
      // mass lists are checked before, only fails if they are removed concurrently
      error(e.getMessage(), e);
      return;
    }
    if (chromatograms == null || isCanceled()) {
      return;
    }
    statistics = builder.getStatistics();
    logger.fine(() -> "Fast chromatogram builder on %s: %s".formatted(dataFile, statistics));

    final ModularFeatureList flist = new ModularFeatureList(dataFile + " " + suffix,
        getMemoryMapStorage(), dataFile);
    // ensure that the default columns are available
    DataTypeUtils.addDefaultChromatographicTypeColumns(flist);

    final Ms2ScanIndex ms2Index = new Ms2ScanIndex(dataFile);
    int id = 1;
    for (final BuiltChromatogram chromatogram : chromatograms) {
      if (isCanceled()) {
        return;
      }
      final ModularFeature feature = createFeature(flist, chromatogram, ms2Index);
      final ModularFeatureListRow row = new ModularFeatureListRow(flist, id++, feature);
      flist.addRow(row);
      // activate shape for this row
      row.set(FeatureShapeType.class, true);
      progress = 0.8 + 0.2 * id / chromatograms.size();
    }

    // sort and reset IDs here to have the same sorting for every feature list
    FeatureListUtils.sortByDefault(flist, true);
    flist.setSelectedScans(dataFile, Arrays.asList(scans));
    dataFile.getAppliedMethods().forEach(m -> flist.getAppliedMethods().add(m));
    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(callingModule, parameters, getModuleCallDate()));
    featureList = flist;

    progress = 1d;
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Adds a zero intensity to every scan next to a detected data point without own data point, like
   * the ADAP chromatogram builder, so that resolvers see the edges of each signal.
   */
  @NotNull
  private ModularFeature createFeature(@NotNull ModularFeatureList flist,
      @NotNull BuiltChromatogram chromatogram, @NotNull Ms2ScanIndex ms2Index) {
    final int n = chromatogram.getNumberOfDataPoints();
    final int numScans = scans.length;
    final int size = countWithFlankingZeros(chromatogram, numScans);
    final double zeroMz = chromatogram.getMeanMz();
    final double[] mzs = new double[size];
    final double[] intensities = new double[size];
    final Scan[] seriesScans = new Scan[size];
    double minMz = Double.POSITIVE_INFINITY;
    double maxMz = Double.NEGATIVE_INFINITY;
    int out = 0;
    int lastScan = -1;
    for (int k = 0; k < n; k++) {
      final int scan = chromatogram.getScanIndex(k);
      if (scan - 1 >= 0 && scan - 1 > lastScan) {
        seriesScans[out] = scans[scan - 1];
        mzs[out] = zeroMz;
        intensities[out++] = 0d;
      }
      final double mz = chromatogram.getMz(k);
      seriesScans[out] = scans[scan];
      mzs[out] = mz;
      intensities[out++] = chromatogram.getIntensity(k);
      minMz = Math.min(minMz, mz);
      maxMz = Math.max(maxMz, mz);
      lastScan = scan;
      final int nextDetected = k + 1 < n ? chromatogram.getScanIndex(k + 1) : numScans;
      if (scan + 1 < numScans && scan + 1 < nextDetected) {
        seriesScans[out] = scans[scan + 1];
        mzs[out] = zeroMz;
        intensities[out++] = 0d;
        lastScan = scan + 1;
      }
    }
    if (out < size) {
      // only zeros were skipped, cannot happen as the size is counted with the same rules
      throw new IllegalStateException("Unexpected number of chromatogram data points");
    }
    // the zeros carry the mean m/z, which lies within the detected m/z range
    minMz = Math.min(minMz, zeroMz);
    maxMz = Math.max(maxMz, zeroMz);

    final SimpleIonTimeSeries series = new SimpleIonTimeSeries(flist.getMemoryMapStorage(), mzs,
        intensities, Arrays.asList(seriesScans));
    final ModularFeature feature = new ModularFeature(flist, dataFile, series,
        FeatureStatus.DETECTED);

    // use wider mz range to group MS2 with chromatogram, same as the ADAP builder
    final SimpleDoubleRange toleranceRange = mzTolerance.getSimpleToleranceRange(feature.getMZ());
    final float minRt = seriesScans[0].getRetentionTime();
    final float maxRt = seriesScans[size - 1].getRetentionTime();
    feature.setAllMS2FragmentScans(
        ms2Index.findFragmentScans(minRt, maxRt, Math.min(toleranceRange.lower(), minMz),
            Math.max(toleranceRange.upper(), maxMz)));
    return feature;
  }

  private static int countWithFlankingZeros(@NotNull BuiltChromatogram chromatogram, int numScans) {
    final int n = chromatogram.getNumberOfDataPoints();
    int size = 0;
    int lastScan = -1;
    for (int k = 0; k < n; k++) {
      final int scan = chromatogram.getScanIndex(k);
      if (scan - 1 >= 0 && scan - 1 > lastScan) {
        size++;
      }
      size++;
      lastScan = scan;
      final int nextDetected = k + 1 < n ? chromatogram.getScanIndex(k + 1) : numScans;
      if (scan + 1 < numScans && scan + 1 < nextDetected) {
        size++;
        lastScan = scan + 1;
      }
    }
    return size;
  }

  @NotNull RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @return the feature list after the task finished, otherwise null
   */
  @Nullable ModularFeatureList getFeatureList() {
    return featureList;
  }

  /**
   * @return the statistics after the task finished, otherwise null
   */
  @Nullable FastChromatogramBuilderStatistics getStatistics() {
    return statistics;
  }
}
