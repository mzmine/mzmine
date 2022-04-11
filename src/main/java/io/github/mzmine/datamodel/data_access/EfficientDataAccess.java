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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.data_access;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A factory to get efficient data access to scans in RawDataFile and features in FeatureList.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class EfficientDataAccess {

  /**
   * The intended use of this memory access is to loop over all scans in a {@link RawDataFile} and
   * access data points via {@link ScanDataAccess#getMzValue(int)} and {@link
   * ScanDataAccess#getIntensityValue(int)}
   *
   * @param dataFile target data file to loop over all scans or mass lists
   * @param type     processed or raw data
   */
  public static ScanDataAccess of(RawDataFile dataFile, ScanDataType type) {
    return new FileScanDataAccess(dataFile, type);
  }

  /**
   * The intended use of this memory access is to loop over all selected scans in a {@link
   * RawDataFile} and access data points via {@link ScanDataAccess#getMzValue(int)} and {@link
   * ScanDataAccess#getIntensityValue(int)}
   *
   * @param dataFile  target data file to loop over all scans or mass lists
   * @param type      processed or raw data
   * @param selection scan selection (null for all scans)
   */
  public static ScanDataAccess of(RawDataFile dataFile, ScanDataType type,
      ScanSelection selection) {
    return new FilteredScanDataAccess(dataFile, type, selection);
  }

  /**
   * The intended use of this memory access is to loop over all selected scans in a {@link
   * RawDataFile} and access data points via {@link ScanDataAccess#getMzValue(int)} and {@link
   * ScanDataAccess#getIntensityValue(int)}
   *
   * @param dataFile target data file to loop over all scans or mass lists
   * @param type     processed or raw data
   * @param scans    list of scans
   */
  public static ScanDataAccess of(RawDataFile dataFile, ScanDataType type,
      List<? extends Scan> scans) {
    return new ScanListDataAccess(dataFile, type, scans);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time)
   *
   * @param flist target feature list. Loops through all features in all RawDataFiles
   * @param type  defines the data accession type
   */
  public static FeatureDataAccess of(FeatureList flist,
      FeatureDataType type) {
    return of(flist, type, null);
  }

  /**
   * Access the chromatographic data of features in a feature list sorted by scan ID (usually sorted
   * by retention time)
   *
   * @param flist    target feature list. Loops through all features in dataFile
   * @param type     defines the data accession type
   * @param dataFile define the data file in an aligned feature list
   */
  public static FeatureDataAccess of(FeatureList flist,
      FeatureDataType type, RawDataFile dataFile) {
    return switch (type) {
      case ONLY_DETECTED -> new FeatureDetectedDataAccess(flist, dataFile);
      case INCLUDE_ZEROS -> new FeatureFullDataAccess(flist, dataFile);
    };
  }

  public static MobilogramDataAccess of(final IonMobilogramTimeSeries ionTrace,
      final MobilogramAccessType accessType) {
    return new MobilogramDataAccess(ionTrace, accessType);
  }

  /**
   * Accesses summed mobilogram data for a specific raw data file. The data can be binned to receive
   * less noisy mobilograms.
   *
   * @param dataFile The {@link IMSRawDataFile}.
   * @param binWidth The bin width (absolute, depends on the {@link io.github.mzmine.datamodel.MobilityType}.
   * @return
   */
  public static BinningMobilogramDataAccess of(final IMSRawDataFile dataFile,
      final int binWidth) {
    return new BinningMobilogramDataAccess(dataFile, binWidth);
  }

  public static MobilityScanDataAccess of(@NotNull final IMSRawDataFile file,
      @NotNull final MobilityScanDataType type, @NotNull final ScanSelection selection) {
    return new MobilityScanDataAccess(file, type, selection);
  }

  /**
   * Different types to handle feature data: {@link #ONLY_DETECTED}: Use only detected data points,
   * currently assigned to the feature. Features might include leading and/or trailing zeros
   * depending on the state of processing. Leading and trailing zeros are added during chromatogram
   * detection, bit might be removed during feature resolving or other processing steps.; {@link
   * #INCLUDE_ZEROS}: Fill all missing data points in the chromatogram with zeros;
   */
  public enum FeatureDataType {
    ONLY_DETECTED, INCLUDE_ZEROS
  }

  /**
   * Different types to handle Scan data: {@link #RAW}: Use raw data as imported; {@link #CENTROID}:
   * Use processed centroid data ({@link MassList}
   */
  public enum ScanDataType {
    RAW, CENTROID
  }

  /**
   * Different types to handle mobility scan data: {@link #RAW}: Use raw data as imported; {@link
   * #CENTROID}: Use processed centroid data ({@link MassList}
   */
  public enum MobilityScanDataType {
    // basically just a copy of ScanDataType, but useful to distinguish the factory methods
    RAW, CENTROID
  }

  /**
   * {@link #ONLY_DETECTED} will only access data points stored in the mobilograms, which may or may
   * not contain leading and trailing zeros depending on the state of processing. {@link
   * #INCLUDE_ZEROS}: fill all missing data points in frame's mobility scans with 0 for the
   * respective mobilogram.
   */
  public enum MobilogramAccessType {
    ONLY_DETECTED, INCLUDE_ZEROS
  }
}
