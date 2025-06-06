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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
 * by retention time)
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class FeatureDataAccess implements IonTimeSeries<Scan> {

  protected final FeatureList flist;
  protected final List<FeatureListRow> rows;
  protected final int totalFeatures;
  // data file is only set for aligned lists to access all features of this data file
  @Nullable
  protected final RawDataFile dataFile;

  // current data
  protected Feature feature;
  protected IonTimeSeries<Scan> featureData;

  // mobilogram binning - only present for mobility data
  protected final @Nullable BinningMobilogramDataAccess mobilogramBinning;

  protected int currentFeatureIndex = -1;
  protected int currentRowIndex = -1;
  protected int currentRawFileIndex = -1;
  protected int currentNumberOfDataPoints = -1;

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time)
   *
   * @param flist target feature list. Loops through all features in all RawDataFiles
   */
  protected FeatureDataAccess(FeatureList flist) {
    this(flist, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time)
   *
   * @param flist    target feature list. Loops through all features in dataFile
   * @param dataFile define the data file in an aligned feature list
   */
  protected FeatureDataAccess(FeatureList flist, @Nullable RawDataFile dataFile) {
    this(flist, dataFile, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time)
   *
   * @param flist             target feature list. Loops through all features in dataFile
   * @param dataFile          define the data file in an aligned feature list
   * @param mobilogramBinning access mobilogram data, only present for mobility data, null
   *                          otherwise
   */
  protected FeatureDataAccess(FeatureList flist, @Nullable RawDataFile dataFile,
      @Nullable BinningMobilogramDataAccess mobilogramBinning) {
    this.flist = flist;
    this.dataFile = dataFile;
    this.mobilogramBinning = mobilogramBinning;

    // set rows and number of features
    int totalFeatures = 0;
    List<FeatureListRow> allRows = flist.getRows();
    // handle aligned flist
    if (flist.getNumberOfRawDataFiles() > 1) {
      if (dataFile != null) {
        List<FeatureListRow> result = new ArrayList<>();
        for (FeatureListRow row : allRows) {
          if (row.hasFeature(dataFile)) {
            result.add(row);
            totalFeatures++;
          }
        }
        this.rows = Collections.unmodifiableList(result);
      } else {
        this.rows = Collections.unmodifiableList(allRows);
        // all features
        for (FeatureListRow row : allRows) {
          totalFeatures += row.getNumberOfFeatures();
        }
      }
    } else {
      // single raw data file in feature list - use all rows
      this.rows = Collections.unmodifiableList(allRows);
      totalFeatures = rows.size();
    }
    this.totalFeatures = totalFeatures;
  }

  public Feature getFeature() {
    return feature;
  }

  /**
   * The maximum number of data points on a feature/chromatogram
   */
  protected int getMaxNumOfDetectedDataPoints() {
    // Find maximum number of detected data points
    int max = 0;
    if (dataFile != null) {
      for (FeatureListRow row : rows) {
        int scans = row.getFeature(dataFile).getNumberOfDataPoints();
        if (max < scans) {
          max = scans;
        }
      }
    } else {
      // aligned feature list or dataFile not set
      for (FeatureListRow row : rows) {
        for (Feature f : row.getFeatures()) {
          int scans = f.getNumberOfDataPoints();
          if (max < scans) {
            max = scans;
          }
        }
      }
    }
    return max;
  }

  @Override
  public Scan getSpectrum(int index) {
    return getSpectra().get(index);
  }

  /**
   * Set the data to the next feature, if available. Returns the feature for additional data access.
   * retention time and intensity values should be accessed from this data class via
   * {@link #getRetentionTime(int)} and {@link #getIntensity(int)}
   *
   * @return the feature or null
   */
  @Nullable
  public Feature nextFeature() {
    if (hasNextFeature()) {
      currentFeatureIndex++;
      // set next feature
      if (dataFile == null && flist.getNumberOfRawDataFiles() > 1) {
        // aligned feature list: find next feature
        do {
          currentRowIndex++;
          if (currentRowIndex >= rows.size()) {
            // start over at next data file
            currentRowIndex = 0;
            currentRawFileIndex++;
          }
          feature = getRow().getFeature(getRow().getRawDataFiles().get(currentRawFileIndex));
        } while (feature == null);
      } else {
        currentRowIndex++;
        // single data file
        feature = getRow().getFeature(
            dataFile != null ? dataFile : getRow().getRawDataFiles().get(0));
      }

      featureData = (IonTimeSeries<Scan>) feature.getFeatureData();
      return feature;
    } else {
      feature = null;
      featureData = null;
    }
    return null;
  }

  /**
   * @return The current feature list row
   */
  private FeatureListRow getRow() {
    return rows.get(currentRowIndex);
  }

  /**
   * The current list of scans has another element
   *
   * @return
   */
  public boolean hasNextFeature() {
    return currentFeatureIndex + 1 < getNumOfFeatures();
  }

  /**
   * The number of feature list rows with at least one feature (matching the filters)
   *
   * @return Number of feature list rows
   */
  public int getNumOfFeatureListRows() {
    return rows.size();
  }

  /**
   * For feature lists of one RawDataFile, num of features equals to
   * {@link #getNumOfFeatureListRows()}. For aligned feature lists two options are available: Either
   * all features or all features of a selected RawDataFile
   *
   * @return number of features
   */
  public int getNumOfFeatures() {
    return totalFeatures;
  }

  /**
   * @return current number of data points depending on FeatureDataType
   */
  @Override
  public int getNumberOfValues() {
    return currentNumberOfDataPoints;
  }

  /**
   * @return the maximum number of values
   */
  public abstract int getMaxNumberOfValues();

  //#######################################
  // Unsupported methods due to different intended use
  @Override
  public IonTimeSeries<Scan> subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Scan> subset) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  /**
   * Method to create subseries from this FeatureDataAccess or its underlying original series.
   * {@link FeatureFullDataAccess} uses this for optimizations in reusing scan lists that are kept
   * alive already as sublist. Base implementation just calls the original series.
   *
   * @param startIndex         the start index in {@link FeatureDataAccess}
   * @param endIndexExclusive  the end index in {@link FeatureDataAccess}
   * @param originalIndexRange the index range in the original series used internally of access
   * @return a subseries
   */
  protected IonTimeSeries<Scan> subSeries(final MemoryMapStorage storage, final int startIndex,
      final int endIndexExclusive, final IndexRange originalIndexRange) {
    // default is to just apply to original series
    final IonTimeSeries<? extends Scan> originalSeries = getOriginalSeries();
    return (IonTimeSeries<Scan>) FeatureDataUtils.subSeries(storage, originalSeries,
        originalIndexRange.min(), originalIndexRange.maxExclusive(), mobilogramBinning);
  }

  @Override
  public IonTimeSeries<Scan> subSeries(final MemoryMapStorage storage, final int startIndex,
      final int endIndexExclusive) {
    if (endIndexExclusive - startIndex <= 0) {
      return emptySeries();
    }
    // the default implementation will redirect to either the ion mobility data or other ion time series
    // this usually creates not sublist views of scans but new lists
    // When working on {@link FeatureFullDataAccess} - sublists are okay to save memory. See implementation there

    // find indices in original data - feature data access may use other data points as well
    final List<Scan> allSpectra = getSpectraModifiable();
    List<Scan> subFromAll = allSpectra.subList(startIndex, endIndexExclusive);
    final float startNumber = subFromAll.getFirst().getScanNumber();
    final float endNumber = subFromAll.getLast().getScanNumber();

    // from original series - different indices - use RT
    final IonTimeSeries<? extends Scan> original = getOriginalSeries();
    final List<? extends Scan> originalSpectra = original.getSpectraModifiable();
    final IndexRange indexRangeOriginal = BinarySearch.indexRange(Range.closed(startNumber, endNumber),
        originalSpectra, Scan::getScanNumber);

    return subSeries(storage, startIndex, endIndexExclusive, indexRangeOriginal);
  }


  public IonTimeSeries<? extends Scan> getOriginalSeries() {
    return getFeature().getFeatureData();
  }

  /**
   * Keeps mzs and scans and replaces intensities
   *
   * @param storage
   * @param newIntensityValues
   * @return
   */
  @Override
  public IonTimeSeries<Scan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newIntensityValues) {
    return copyAndReplace(storage, getMzValuesCopy(), newIntensityValues);
  }

  @Override
  public IonTimeSeries<Scan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull double[] newMzValues, @NotNull double[] newIntensityValues) {
    IonTimeSeries<Scan> oldData = (IonTimeSeries<Scan>) getFeature().getFeatureData();
    int numValues = oldData.getNumberOfValues();
    if (numValues < newIntensityValues.length) {
      // need to transfer data points to actual scans with detections.
      // this is the case when a {@link io.github.mzmine.datamodel.data_access.FeatureFullDataAccess} is used
      double[] actualMzs = new double[numValues];
      double[] actualIntensities = new double[numValues];

      int newDataIndex = -1;
      Scan newScan = null;
      for (int i = 0; i < numValues; i++) {
        Scan scan = oldData.getSpectrum(i);
        while (!Objects.equals(scan, newScan)) {
          // find matching scan in full data access
          newDataIndex++;
          if (newDataIndex >= newIntensityValues.length) {
            throw new IllegalStateException("Cannot find matching scan in " + this);
          }
          newScan = this.getSpectrum(newDataIndex);
        }
        actualMzs[i] = newMzValues[newDataIndex];
        actualIntensities[i] = newIntensityValues[newDataIndex];
      }

      return (IonTimeSeries<Scan>) oldData.copyAndReplace(storage, actualMzs, actualIntensities);
    } else {
      return (IonTimeSeries<Scan>) oldData.copyAndReplace(storage, newMzValues, newIntensityValues);
    }
  }

  @Override
  public IonSeries copy(MemoryMapStorage storage) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public double[] getIntensityValues(double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public MemorySegment getMZValueBuffer() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public double[] getMzValues(double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public double getIntensityForSpectrum(Scan scan) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public double getMzForSpectrum(Scan scan) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public void forEach(Consumer<? super DataPoint> action) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public Spliterator<DataPoint> spliterator() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public void saveValueToXML(XMLStreamWriter writer, List<Scan> allScans)
      throws XMLStreamException {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  @Override
  public void saveValueToXML(XMLStreamWriter writer, List<Scan> allScans, boolean includeRt)
      throws XMLStreamException {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all features and data points in a feature list");
  }

  /**
   * Usage of this method is strongly discouraged because it returns the internal buffer of this
   * data access. However, in exceptional use-cases such as resolving or smoothing XICs, a direct
   * access might be necessary to avoid copying arrays. Since the chromatograms might originate from
   * different raw data files, the number of data points in that raw file might be different from
   * the length of this buffer, which is set to the longest XIC. The current number of data points
   * can be accessed via {@link FeatureDataAccess#getNumberOfValues()}.
   * <p></p>
   * <b>NOTE:</b> In most cases, the use of  {@link FeatureDataAccess#getIntensity(int)} (int)} is
   * more appropriate.
   *
   * @return The intensity buffer of this data access.
   */
  public abstract double[] getIntensityValues();

  /**
   * Usage of this method is strongly discouraged because it returns the internal buffer of this
   * data access. However, in exceptional use-cases such as resolving or smoothing XICs, a direct
   * access might be necessary to avoid copying arrays. Since the chromatograms might originate from
   * different raw data files, the number of data points in that raw file might be different from
   * the length of this buffer, which is set to the longest XIC. The current number of data points
   * can be accessed via {@link FeatureDataAccess#getNumberOfValues()}.
   * <p></p>
   * <b>NOTE:</b> In most cases, the use of  {@link FeatureDataAccess#getMZ(int)} is more
   * appropriate.
   * <p>
   * If a copy of the active data range is required see {@link #getMzValuesCopy()}
   *
   * @return The m/z buffer of this data access.
   */
  public abstract double[] getMzValues();


  /**
   * @return copy of mz values array limited to actual number of values
   */
  public double[] getMzValuesCopy() {
    double[] mzs = getMzValues();
    double[] copy = new double[getNumberOfValues()];
    System.arraycopy(mzs, 0, copy, 0, copy.length);
    return copy;
  }

  /**
   * @return copy of intensity values array limited to actual number of values
   */
  public double[] getIntensityValuesCopy() {
    double[] intensities = getIntensityValues();
    double[] copy = new double[getNumberOfValues()];
    System.arraycopy(intensities, 0, copy, 0, copy.length);
    return copy;
  }

}
