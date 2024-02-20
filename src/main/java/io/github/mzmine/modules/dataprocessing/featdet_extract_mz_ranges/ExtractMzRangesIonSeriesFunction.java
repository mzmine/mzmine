/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
package io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries.IntensityMode;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.operations.AbstractTaskSubSupplier;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task to extrat {@link BuildingIonSeries} that can be turned into IonTimeSeries etc.
 */
public class ExtractMzRangesIonSeriesFunction extends AbstractTaskSubSupplier<BuildingIonSeries[]> {

  private static final Logger logger = Logger.getLogger(
      ExtractMzRangesIonSeriesFunction.class.getName());
  private final List<Range<Double>> mzRangesSorted;
  private final ScanDataAccess dataAccess;
  private int processedScans, totalScans;
  private String description;

  /**
   * @param mzRangesSorted sorted by mz ascending
   */
  public ExtractMzRangesIonSeriesFunction(@NotNull RawDataFile dataFile,
      @NotNull ScanSelection scanSelection, @NotNull List<Range<Double>> mzRangesSorted,
      @NotNull ScanDataType scanDataType, @Nullable AbstractTask parentTask) {
    super(parentTask);

    dataAccess = EfficientDataAccess.of(dataFile, scanDataType, scanSelection);
    this.mzRangesSorted = mzRangesSorted;
    description = "Extracting %d ion series from data file %s".formatted(mzRangesSorted.size(),
        dataFile.getName());
  }

  /**
   * @param mzRangesSorted sorted by mz ascending
   */
  public ExtractMzRangesIonSeriesFunction(@NotNull RawDataFile dataFile, List<? extends Scan> scans,
      @NotNull List<Range<Double>> mzRangesSorted, @NotNull ScanDataType scanDataType,
      @Nullable AbstractTask parentTask) {
    super(parentTask);

    dataAccess = EfficientDataAccess.of(dataFile, scanDataType, scans);
    this.mzRangesSorted = mzRangesSorted;
  }

  @Override
  public @NotNull String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) processedScans / totalScans;
  }

  /**
   * Extract all mz ranges. Will listen to parentTask and will update
   * {@link #getFinishedPercentage()}
   *
   * @return {@link BuildingIonSeries} can be converted to other IonSeries like
   * {@link IonTimeSeries}
   */
  @Override
  @NotNull
  public BuildingIonSeries[] get() {
    if (mzRangesSorted.isEmpty()) {
      return new BuildingIonSeries[0];
    }

    totalScans = dataAccess.getNumberOfScans();
    // store data points for each range
    BuildingIonSeries[] chromatograms = new BuildingIonSeries[mzRangesSorted.size()];
    for (int i = 0; i < chromatograms.length; i++) {
      chromatograms[i] = new BuildingIonSeries(dataAccess.getNumberOfScans(),
          IntensityMode.HIGHEST);
    }

    // binary search the start
    double lowestMz = mzRangesSorted.getFirst().lowerEndpoint();

    int currentScan = -1;
    while (dataAccess.nextScan() != null) {
      int currentTree = 0;
      currentScan++;
      processedScans++;

      // Canceled?
      if (isCanceled()) {
        return new BuildingIonSeries[0];
      }
      // check value for tree and for all next trees in range
      int nDataPoints = dataAccess.getNumberOfDataPoints();

      // start index
      int dp = dataAccess.binarySearch(lowestMz, DefaultTo.GREATER_EQUALS);
      if (dp == -1) {
        continue;
      }

      for (; dp < nDataPoints; dp++) {
        double mz = dataAccess.getMzValue(dp);
        // check all next trees
        for (int t = currentTree; t < mzRangesSorted.size(); t++) {
          if (mz > mzRangesSorted.get(t).upperEndpoint()) {
            // out of bounds for current tree
            currentTree++;
          } else if (mz < mzRangesSorted.get(t).lowerEndpoint()) {
            break; // below current tree - next datapoint
          } else {
            // found match
            double intensity = dataAccess.getIntensityValue(dp);
            chromatograms[t].addValue(currentScan, mz, intensity);
          }
        }
        // all trees done
        if (currentTree >= mzRangesSorted.size()) {
          break;
        }
      }
    }
    return chromatograms;
  }

}
