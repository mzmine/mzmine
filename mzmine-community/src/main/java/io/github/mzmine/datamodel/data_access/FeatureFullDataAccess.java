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

import static io.github.mzmine.datamodel.featuredata.impl.StorageUtils.sliceDoubles;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.collections.IndexRange;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
 * by retention time). Full data access uses all scans of the whole chromatogram and adds zeros for
 * missing data points. This is important for a few chromatogram deconvolution algorithms,
 * smoothing, etc. However, if applied to already resolved features, zero intensities do not mean no
 * signal.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FeatureFullDataAccess extends FeatureDataAccess {

  // current data
  protected final double[] detectedMzs;
  protected final double[] detectedIntensities;

  protected final double[] mzs;
  protected final double[] intensities;

  // all scans of the whole chromatogram (for the current raw data file)
  private List<Scan> allScans;

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time). Full data access uses all scans of the whole chromatogram and adds zeros
   * for missing data points. This is important for a few chromatogram deconvolution algorithms,
   * smoothing, etc. However, if applied to already resolved features, zero intensities do not mean
   * no signal.
   *
   * @param flist target feature list. Loops through all features in all RawDataFiles
   */
  protected FeatureFullDataAccess(FeatureList flist) {
    this(flist, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time). Full data access uses all scans of the whole chromatogram and adds zeros
   * for missing data points. This is important for a few chromatogram deconvolution algorithms,
   * smoothing, etc. However, if applied to already resolved features, zero intensities do not mean
   * no signal.
   *
   * @param flist    target feature list. Loops through all features in dataFile
   * @param dataFile define the data file in an aligned feature list
   */
  protected FeatureFullDataAccess(FeatureList flist, @Nullable RawDataFile dataFile) {
    this(flist, dataFile, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time). Full data access uses all scans of the whole chromatogram and adds zeros
   * for missing data points. This is important for a few chromatogram deconvolution algorithms,
   * smoothing, etc. However, if applied to already resolved features, zero intensities do not mean
   * no signal.
   *
   * @param flist                       target feature list. Loops through all features in dataFile
   * @param dataFile                    define the data file in an aligned feature list
   * @param binningMobilogramDataAccess access mobilogram data, only present for mobility data, null
   *                                    otherwise. Checks are done internally
   */
  protected FeatureFullDataAccess(FeatureList flist, @Nullable RawDataFile dataFile,
      @Nullable BinningMobilogramDataAccess binningMobilogramDataAccess) {
    super(flist, dataFile, binningMobilogramDataAccess);

    // return all scans that were used to create the chromatograms in the first place
    int max = 0;
    if (dataFile == null && flist.getNumberOfRawDataFiles() > 1) {
      for (RawDataFile raw : flist.getRawDataFiles()) {
        int scans = flist.getSeletedScans(raw).size();
        if (max < scans) {
          max = scans;
        }
      }
    } else {
      // one raw data file
      max = flist.getSeletedScans(dataFile != null ? dataFile : flist.getRawDataFile(0)).size();
    }

    mzs = new double[max];
    intensities = new double[max];
    // detected data points currently on feature/chromatogram
    int detected = getMaxNumOfDetectedDataPoints();

    detectedMzs = new double[detected];
    detectedIntensities = new double[detected];
  }

  @Override
  public List<Scan> getSpectra() {
    assert allScans != null;
    return allScans;
  }

  @Override
  public List<Scan> getSpectraModifiable() {
    return allScans;
  }

  @Override
  public float getRetentionTime(int index) {
    assert index < getNumberOfValues() && index >= 0;

    return getSpectrum(index).getRetentionTime();
  }

  /**
   * Get mass to charge value at index
   *
   * @param index data point index
   * @return mass to charge value at index
   */
  @Override
  public double getMZ(int index) {
    assert index < getNumberOfValues() && index >= 0;
    return mzs[index];
  }

  /**
   * Get intensity at index
   *
   * @param index data point index
   * @return intensity at index
   */
  @Override
  public double getIntensity(int index) {
    assert index < getNumberOfValues() && index >= 0;
    return intensities[index];
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
    super.nextFeature();
    if (feature != null) {
      // add detected data points and zero for missing values
      allScans = (List<Scan>) flist.getSeletedScans(feature.getRawDataFile());
      // read detected data in batch
      List<Scan> detectedScans = feature.getScanNumbers();
      featureData.getMzValues(detectedMzs);
      featureData.getIntensityValues(detectedIntensities);

      int detectedIndex = 0;
      for (int i = 0; i < intensities.length; i++) {
        if (allScans.get(i) == detectedScans.get(detectedIndex)) {
          intensities[i] = detectedIntensities[detectedIndex];
          mzs[i] = detectedMzs[detectedIndex];
          detectedIndex++;
        } else {
          intensities[i] = 0d;
        }
        if (detectedIndex == detectedScans.size() && i < intensities.length - 1) {
          Arrays.fill(intensities, i + 1, intensities.length, 0d);
          break;
        }
      }
      if (detectedIndex != detectedScans.size()) {
        throw new IllegalStateException(
            "Less scans added than actually detected in Full Feature data access. This may point to wrong sorting of scans between detected and all scans of a feature list.");
      }
      currentNumberOfDataPoints = allScans.size();
    } else {
      // clear
      allScans = null;
    }
    return feature;
  }


  @Override
  public IonTimeSeries<Scan> subSeries(final MemoryMapStorage storage, final int startIndex,
      final int endIndexExclusive, final IndexRange originalIndexRange) {

    if (endIndexExclusive - startIndex <= 0) {
      return emptySeries();
    }

    // sublist:
    // PRO: the original list is kept alive either way (e.g. the Scan list in FeatureList) - saves memory
    // CONTRA: the original list is not referenced and and will be kept alive by sublist

    // in case of resolving and smoothing etc it makes sense to keep the original list or a sublist
    // in this case the feature list keeps the MS1 scans list alive

    // it is ok to create sublists here and reuse the original list of scans
    // this is because the list is already kept in memory by the feature list
    // so sublist saves memory - other implementations that may run on variable scan lists rather create copies

    // from all scans
    List<Scan> subFromAll = getSpectraModifiable().subList(startIndex, endIndexExclusive);

    // from original series - different indices - use RT
    final IonTimeSeries<? extends Scan> original = getOriginalSeries();
    List<? extends Scan> subFromOriginal = originalIndexRange.sublist(
        original.getSpectraModifiable(), false);

    // subAll needs to be a continuous section in subFromOriginal to use the optimization of sublist
    if (CollectionUtils.isContinuousRegionByIdentity(subFromAll, subFromOriginal)) {
      // reuse data
      return subSeriesReuseBuffers(storage, originalIndexRange.min(),
          originalIndexRange.maxExclusive(), original, subFromAll);
    } else {
      if (original instanceof IonMobilogramTimeSeries imsSeries) {
        // special solution for IMS data - subFromAll scans list did not match - maybe there was a missing scan
        // the missing scans do not have mobilograms - hard to reextract - TODO revisit
        // therefore just call the original method to subseries, creating a clone of the scan list
        return (IonTimeSeries<Scan>) (IonTimeSeries) imsSeries.subSeries(storage,
            originalIndexRange.min(), originalIndexRange.maxExclusive(), mobilogramBinning);
      }

      // for other series we create new ion time series with the data and scans list
      // use the global indices for this for all scans list and data
      double[] mzs = new double[subFromAll.size()];
      double[] intensities = new double[subFromAll.size()];
      for (int i = startIndex; i < endIndexExclusive; i++) {
        mzs[i - startIndex] = getMZ(i);
        intensities[i - startIndex] = getIntensity(i);
      }
      return new SimpleIonTimeSeries(storage, mzs, intensities, subFromAll);
    }
  }


  /**
   * This is only applied if subFromAll scans is a continuous sub region in the previously set
   * spectra list
   *
   * @param startIndex        in original data
   * @param endIndexExclusive in original data
   * @param original          original series to subseries of
   * @param subFromAll        sub list from all scans
   * @return a subseries reusing data
   */
  private IonTimeSeries<Scan> subSeriesReuseBuffers(final MemoryMapStorage storage,
      final int startIndex, final int endIndexExclusive,
      final IonTimeSeries<? extends Scan> original, final List<Scan> subFromAll) {
    // can reuse the original data buffers and sublists of scans
    final MemorySegment mzs = sliceDoubles(original.getMZValueBuffer(), startIndex,
        endIndexExclusive);
    final MemorySegment intensities = sliceDoubles(original.getIntensityValueBuffer(), startIndex,
        endIndexExclusive);

    //noinspection unchecked
    return (IonTimeSeries<Scan>) switch (original) {
      case IonMobilogramTimeSeries imsSeries -> {
        if (mobilogramBinning == null) {
          throw new IllegalStateException(
              "mobilogramBinning is null during subseries of IMS series in data access");
        }

        final List<IonMobilitySeries> mobilograms = imsSeries.getMobilograms()
            .subList(startIndex, endIndexExclusive);
        mobilogramBinning.setMobilogram(mobilograms);

        yield new SimpleIonMobilogramTimeSeries(mzs, intensities, storage, mobilograms,
            (List<Frame>) (List) subFromAll, mobilogramBinning.toSummedMobilogram(storage));
      }
      // use subFromAll to save memory by pointing all to the original list of scans
      default -> new SimpleIonTimeSeries(mzs, intensities, subFromAll);
    };
  }

  @Override
  public int getMaxNumberOfValues() {
    return mzs.length;
  }

  @Override
  public double[] getIntensityValues() {
    return intensities;
  }

  @Override
  public double[] getMzValues() {
    return mzs;
  }

  @Override
  public IonTimeSeries<Scan> emptySeries() {
    return featureData.emptySeries();
  }

}
