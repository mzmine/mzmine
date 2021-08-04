/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.msdk;

import io.github.mzmine.datamodel.PolarityType;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Range;

/**
 * Represents a single MS scan in a raw data file. This interface extends
 * {@link MassSpectrum}, therefore the actual data points can be
 * accessed through the inherited methods of MsSpectrum.
 *
 * If the scan is not added to any file, its data points are stored in memory. However, once the
 * scan is added into a raw data file by calling setRawDataFile(), its data points will be stored in
 * a temporary file that belongs to that RawDataFile. When RawDataFile.dispose() is called, the data
 * points are discarded so the MsScan instance cannot be used anymore.
 */
public interface MsScan extends MassSpectrum {

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
  @NotNull
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
  @NotNull
  Integer getMsLevel();

  /**
   * Returns the type of the MS scan. If unknown, MsScanType.UNKNOWN is returned.
   *
   * @return MS scan type
   */
  @NotNull
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
  @NotNull
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
  @NotNull
  List<IsolationInfo> getIsolations();

}
