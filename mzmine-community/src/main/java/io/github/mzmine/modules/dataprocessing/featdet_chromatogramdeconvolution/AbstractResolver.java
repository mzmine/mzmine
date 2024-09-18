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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureFullDataAccess;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.TimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract implementation of a {{@link Resolver}}. Does the convenience for every implementation,
 * such as processing the parameters and determining, which dimension shall be resolved. Accepts the
 * ranges returned by {@link Resolver#resolveRt(IntensitySeries)} and
 * {@link Resolver#resolveMobility(IntensitySeries)} and splits the given {@link IonTimeSeries} into
 * individual features.
 *
 * @author SteffenHeu https://github.com/SteffenHeu
 */
public abstract class AbstractResolver implements Resolver {

  protected final ParameterSet generalParameters;
  protected final ResolvingDimension dimension;
  protected final ModularFeatureList flist;
  protected final RawDataFile file;
  protected double[] xBuffer;
  protected BinningMobilogramDataAccess mobilogramDataAccess;
  protected double[] yBuffer;
  private SourceDataType mobilogramDataSource = SourceDataType.NOT_SET;
  private SourceDataType chromatogramDataSource = SourceDataType.NOT_SET;

  protected AbstractResolver(@NotNull final ParameterSet parameters,
      @NotNull final ModularFeatureList flist) {
    this.generalParameters = parameters;
    dimension = parameters.getParameter(GeneralResolverParameters.dimension).getValue();
    this.flist = flist;
    file = flist.getRawDataFile(0);
  }

  public RawDataFile getRawDataFile() {
    return file;
  }

  @Override
  public @NotNull <T extends IonTimeSeries<? extends Scan>> List<T> resolve(@NotNull T series,
      @Nullable MemoryMapStorage storage) {
    // To properly resolve, we need the scans of the original series. In a full access, all scans
    // will be included, even the ones where the feature was not detected in. Furthermore, we need
    // the original series to know about the data type.
    final IonTimeSeries<? extends Scan> originalSeries =
        series instanceof FeatureDataAccess acc ? acc.getFeature().getFeatureData() : series;

    final List<T> resolved = new ArrayList<>();
    if (dimension == ResolvingDimension.RETENTION_TIME) {
      final List<Range<Double>> resolvedRanges = resolveRt(series);

      // make a new subseries for each resolved range.
      for (final Range<Double> range : resolvedRanges) {
        final List<? extends Scan> subList = originalSeries.getSpectra().stream()
            .filter(s -> range.contains((double) s.getRetentionTime())).toList();
        if (subList.isEmpty()) {
          continue;
        }
        if (originalSeries instanceof IonMobilogramTimeSeries trace) {
          resolved.add(
              (T) trace.subSeries(storage, (List<Frame>) subList, getMobilogramDataAccess()));
        } else if (originalSeries instanceof SimpleIonTimeSeries chrom) {
          resolved.add((T) chrom.subSeries(storage, (List<Scan>) subList));
        } else {
          throw new IllegalStateException(
              "Resolving behaviour of " + originalSeries.getClass().getName() + " not specified.");
        }
      }
    } else if (dimension == ResolvingDimension.MOBILITY
        && originalSeries instanceof IonMobilogramTimeSeries originalTrace) {
      setSeriesToMobilogramDataAccess(series);
      final List<Range<Double>> resolvedRanges = resolveMobility(mobilogramDataAccess);

      // make a new sub series for each resolved range.
      for (Range<Double> resolvedRange : resolvedRanges) {
        final List<IonMobilitySeries> resolvedMobilograms = new ArrayList<>();
        for (IonMobilitySeries mobilogram : originalTrace.getMobilograms()) {
          // split every mobilogram
          final List<MobilityScan> subset = mobilogram.getSpectra().stream()
              .filter(scan -> resolvedRange.contains(scan.getMobility()))
              .collect(Collectors.toList());
          if (subset.isEmpty()) {
            continue;
          }
          // IonMobilitySeries are stored in ram until they are added to an IonMobilogramTimeSeries
          resolvedMobilograms.add((SimpleIonMobilitySeries) mobilogram.subSeries(null, subset));
        }
        if (resolvedMobilograms.isEmpty()) {
          continue;
        }

        resolved.add((T) IonMobilogramTimeSeriesFactory.of(storage, resolvedMobilograms,
            getMobilogramDataAccess()));
      }
    } else {
      throw new IllegalStateException(
          "Cannot resolve " + originalSeries.getClass().getName() + " in " + dimension
              + " mobility dimension.");
    }
    return resolved;
  }

  protected <T extends IonTimeSeries<? extends Scan>> void setSeriesToMobilogramDataAccess(
      @NotNull T series) {
    if (series instanceof SummedIntensityMobilitySeries mob) {
      getMobilogramDataAccess().setMobilogram(mob);
    } else if (series instanceof IonMobilogramTimeSeries mob) {
      if (!mob.getSpectrum(0).getDataFile().equals(file)) {
        throw new IllegalArgumentException("The given series is of a different raw data file.");
      }
      getMobilogramDataAccess().setMobilogram(mob.getSummedMobilogram());
    } else if (series instanceof FeatureDataAccess access && access.getFeature()
        .getFeatureData() instanceof IonMobilogramTimeSeries mob) {
      if (!mob.getSpectrum(0).getDataFile().equals(file)) {
        throw new IllegalArgumentException("The given series is of a different raw data file.");
      }
      getMobilogramDataAccess().setMobilogram(mob.getSummedMobilogram());
    } else {
      throw new IllegalArgumentException(
          "Unexpected type of ion series (" + series.getClass().getName()
              + "). Please contact the developers. ");
    }
  }

  @NotNull
  protected BinningMobilogramDataAccess getMobilogramDataAccess() {
    if (mobilogramDataAccess != null) {
      return mobilogramDataAccess;
    }

    if (file instanceof IMSRawDataFile imsFile) {
      mobilogramDataAccess = new BinningMobilogramDataAccess(imsFile,
          BinningMobilogramDataAccess.getPreviousBinningWith(flist, imsFile.getMobilityType()));
      return mobilogramDataAccess;
    }
    throw new RuntimeException("Could not initialize BinningMobilogramDataAccess.");
  }

  @Override
  public @NotNull <T extends IntensitySeries & TimeSeries> List<Range<Double>> resolveRt(
      @NotNull T series) {
    if (!validateChromatogramDataSource(series)) {
      // if the date comes from a different source, the results might be inconsistent.
      throw new IllegalArgumentException(
          "This resolver has been set to use data from a " + chromatogramDataSource.toString()
              + ". The current data os passed from a " + series.getClass().toString());
    }

    xBuffer = extractRtValues(series, xBuffer);

    if (series instanceof FeatureFullDataAccess featureFullDataAccess) {
      return resolve(xBuffer, featureFullDataAccess.getIntensityValues());
    } else {
      // intensities only need to be extracted if we are not using a FeatureFullDataAccess
      final int numValues = series.getNumberOfValues();
      if (yBuffer == null || yBuffer.length <= numValues) {
        yBuffer = new double[numValues];
      }
      Arrays.fill(yBuffer, 0d);
      yBuffer = series.getIntensityValues(yBuffer);

      return resolve(xBuffer, yBuffer);
    }
  }

  @Override
  public @NotNull <T extends IntensitySeries & MobilitySeries> List<Range<Double>> resolveMobility(
      @NotNull T series) {

    if (!validateMobilogramDataSource(series)) {
      // if the date comes from a different source, the results might be inconsistent.
      throw new IllegalArgumentException(
          "This resolver has been set to use data from a " + mobilogramDataSource.toString()
              + ". The current data os passed from a " + series.getClass().toString());
    }

    if (series instanceof BinningMobilogramDataAccess dataAccess) {
      return resolve(dataAccess.getMobilityValues(), dataAccess.getIntensityValues());
    } else {

      final int numValues = series.getNumberOfValues();
      if (xBuffer == null || xBuffer.length < numValues) {
        xBuffer = new double[numValues];
        yBuffer = new double[numValues];
      }

      Arrays.fill(xBuffer, 0d);
      Arrays.fill(yBuffer, 0d);
      IonMobilityUtils.extractMobilities(series, xBuffer);
      yBuffer = series.getIntensityValues(yBuffer);
      return resolve(xBuffer, yBuffer);
    }
  }

  private <T extends IntensitySeries & MobilitySeries> boolean validateMobilogramDataSource(
      T series) {
    if (mobilogramDataSource == SourceDataType.NOT_SET) {
      mobilogramDataSource =
          series instanceof BinningMobilogramDataAccess ? SourceDataType.DATA_ACCESS
              : SourceDataType.SERIES;
      return true;
    }
    if (mobilogramDataSource == SourceDataType.DATA_ACCESS
        && series instanceof BinningMobilogramDataAccess) {
      return true;
    }
    if (mobilogramDataSource == SourceDataType.SERIES
        && !(series instanceof BinningMobilogramDataAccess)) {
      return true;
    }

    return false;
  }

  private <T extends IntensitySeries & TimeSeries> boolean validateChromatogramDataSource(
      T series) {
    if (mobilogramDataSource == SourceDataType.NOT_SET) {
      mobilogramDataSource =
          series instanceof FeatureDataAccess ? SourceDataType.DATA_ACCESS : SourceDataType.SERIES;
      return true;
    }
    if (mobilogramDataSource == SourceDataType.DATA_ACCESS && series instanceof FeatureDataAccess) {
      return true;
    }
    if (mobilogramDataSource == SourceDataType.SERIES && !(series instanceof FeatureDataAccess)) {
      return true;
    }

    return false;
  }

  /**
   * Extracts the rt values of the scans into a buffer. If the size of the buffer is too small, a
   * new buffer will be allocated and returned.
   *
   * @param timeSeries The time series.
   * @param rtBuffer   The proposed buffer.
   * @return The buffer the rt values were written into.
   */
  protected double[] extractRtValues(@NotNull final TimeSeries timeSeries, double[] rtBuffer) {
    final int numValues = timeSeries.getNumberOfValues();
    if (rtBuffer == null || rtBuffer.length < numValues) {
      rtBuffer = new double[numValues];
    }
    Arrays.fill(rtBuffer, 0d);
    for (int i = 0; i < numValues; i++) {
      rtBuffer[i] = timeSeries.getRetentionTime(i);
    }
    return rtBuffer;
  }

  private enum SourceDataType {
    /**
     * No data has been processed yet.
     */
    NOT_SET,

    /**
     * A {@link FeatureFullDataAccess} or a {@link BinningMobilogramDataAccess} is used as a data
     * source.
     */
    DATA_ACCESS,

    /**
     * An actual {@link IonTimeSeries} or {@link IonMobilogramTimeSeries} is used as a data source.
     */
    SERIES;
  }
}
