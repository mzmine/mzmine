/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.datamodel.msdk;

import io.github.mzmine.datamodel.PolarityType;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;

/**
 * Represents a single MS scan in a raw data file. This interface extends
 * {@link MsSpectrum}, therefore the actual data points can be
 * accessed through the inherited methods of MsSpectrum.
 *
 * If the scan is not added to any file, its data points are stored in memory. However, once the
 * scan is added into a raw data file by calling setRawDataFile(), its data points will be stored in
 * a temporary file that belongs to that RawDataFile. When RawDataFile.dispose() is called, the data
 * points are discarded so the MsScan instance cannot be used anymore.
 */
public interface MsScan extends MsSpectrum {

  /**
   * Returns the raw data file that contains this scan. This might return null when the scan is
   * created, but once the scan is added to the raw data file by calling RawDataFile.addScan(), the
   * RawDataFile automatically calls the MsScan.setRawDataFile() method to update this reference.
   *
   * @return RawDataFile containing this MsScan, or null.
   * @see RawDataFile
   */
  @Nullable
  RawDataFile getRawDataFile();

  /**
   * Returns the number of this scan, represented by an integer, typically positive. Typically, the
   * scan number will be unique within the file. However, the data model does not guarantee that,
   * and in some cases multiple scans with the same number may be present in the file.
   *
   * @return Scan number
   */
  @Nonnull
  Integer getScanNumber();

  /**
   * Returns the instrument-specific textual definition of the scan parameters. For example, in
   * Thermo raw data this may look like:
   *
   * FTMS + p ESI Full ms2 209.09@hcd35.00 [50.00-230.00]
   *
   * The scan definition can be null if not specified in the raw data.
   *
   * @return Scan definition
   */
  @Nullable
  String getScanDefinition();

  /**
   * Returns the MS function of this scan, e.g. "Full ms", or "SRM".
   *
   * @return MS function.
   */
  @Nullable
  String getMsFunction();

  /**
   * Returns MS level (1 = default, 2 = MS/MS, 3 = MS3 etc.).
   * 
   * @return MS level
   */
  @Nonnull
  Integer getMsLevel();

  /**
   * Returns the type of the MS scan. If unknown, MsScanType.UNKNOWN is returned.
   *
   * @return MS scan type
   */
  @Nonnull
  default MsScanType getMsScanType() {
    return MsScanType.UNKNOWN;
  }

  /**
   * Returns the retention time in seconds
   *
   * @return RT
   */
  @Nullable
  Float getRetentionTime();

  /**
   * Returns the scanning range of the instrument. Note that this value is different from that
   * returned by getMzRange() from the MassSpectrum interface.
   *
   * getMzRange() returns the range of the actual data points (lowest and highest m/z)
   *
   * getScanningRange() returns the instrument scanning range that was configured in the experiment
   * setup.
   *
   * @return The scanning m/z range of the instrument
   */
  @Nullable
  Range<Double> getScanningRange();

  /**
   * Returns the polarity of this scan. If unknown, PolarityType.UNKNOWN is returned.
   *
   * @return Polarity of this scan.
   */
  @Nonnull
  default PolarityType getPolarity() {
    return PolarityType.UNKNOWN;
  }

  /**
   * Returns the fragmentation parameters of ion source-induced fragmentation, or null if no such
   * information is known.
   *
   * @return Fragmentation info of ion source-induced fragmentation, or null.
   */
  @Nullable
  ActivationInfo getSourceInducedFragmentation();

  /**
   * Returns a list of isolations performed for this scan. These isolations may also include
   * fragmentations (tandem MS).
   *
   * @return A mutable list of isolations. New isolation items can be added to this list.
   */
  @Nonnull
  List<IsolationInfo> getIsolations();

}
