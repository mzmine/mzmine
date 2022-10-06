/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import io.github.mzmine.datamodel.impl.StoredMobilityScan;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A frame is a collection of mobility resolved spectra at one point in time.
 */
public interface Frame extends Scan {

  /**
   * Equivalent to {@link Scan#getScanNumber()}.
   *
   * @return the scan number
   */
  default int getFrameId() {
    return getScanNumber();
  }

  int getNumberOfMobilityScans();

  @NotNull MobilityType getMobilityType();

  @NotNull Range<Double> getMobilityRange();

  /**
   * Creates a {@link StoredMobilityScan} wrapper for the given index. Reuse when possible for ram
   * efficiency.
   *
   * @param num the number of the sub scan.
   * @return the mobility scan or null of no scan with that number exists.
   */
  @Nullable MobilityScan getMobilityScan(int num);

  /**
   * Creates {@link StoredMobilityScan} wrappers for all mobility scans of this file. It's
   * recommended to not save the full array list for processing purposes, since the mobility scans
   * are created on every call to this method. E.g. during feature detection (for the purpose of
   * saving EICs/mobilogram trace) the SAME instance of {@link MobilityScan} should be used and
   * reused (see {@link SimpleIonMobilogramTimeSeries#getSpectraModifiable()}) at all times. For
   * example the mobility scans shall not be accessed via a {@link Frame} every time but retrieved
   * once and reused.
   *
   * @return The mobility scans.
   */
  @NotNull List<MobilityScan> getMobilityScans();

  /**
   * Implications of {@link #getMobilityScans()} apply.
   *
   * @return The mobility scans sorted by ascending mobility.
   */
  @NotNull List<MobilityScan> getSortedMobilityScans();

  /**
   * @return The {@link MobilityScanStorage} of this frame. The storage is initialised during raw
   * data file import.
   */
  MobilityScanStorage getMobilityScanStorage();

  /**
   * @param mobilityScanIndex
   * @return The mobility of this sub spectrum.
   */
  double getMobilityForMobilityScanNumber(int mobilityScanIndex);

  /**
   * @return Mapping of sub scan number <-> mobility
   */
  @Nullable DoubleImmutableList getMobilities();

  /**
   * @return Set of ImsMsMsInfos for this frame. Empty set if this is not an MS/MS frame or no
   * precursors were fragmented or assigned.
   */
  @NotNull Set<PasefMsMsInfo> getImsMsMsInfos();

  /**
   * @param mobilityScanNumber The sub scan number of the given sub scan.
   * @return PasefMsMsInfo or null if no precursor was fragmented at that scan.
   */
  @Nullable PasefMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber);

  /**
   * @return Always 0.0
   */
  @Override
  default Double getPrecursorMz() {
    return null;
  }

  /**
   * @return Always 0
   */
  @Override
  default Integer getPrecursorCharge() {
    return null;
  }

  int getMaxMobilityScanRawDataPoints();

  int getTotalMobilityScanRawDataPoints();

  int getMaxMobilityScanMassListDataPoints();

  int getTotalMobilityScanMassListDataPoints();
}
