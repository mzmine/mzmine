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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import com.google.common.collect.Range;

/**
 * Represents the ion isolation information. Each isolation can be accompanied by MS/MS
 * fragmentation, represented by FragmentationInfo. If no fragmentation occurred or if fragmentation
 * details are unknown, getFragmentationInfo() will return null. For convenience, this interface is
 * immutable, so it can be passed by reference and safely used by multiple threads.
 */
@Unmodifiable
public interface IsolationInfo {

  /**
   * Returns the isolated m/z range.
   *
   * @return Isolated m/z range.
   */
  @NotNull
  Range<Double> getIsolationMzRange();

  /**
   * Returns the ion injection time in ms, for ion trap experiments. Shorter time indicates larger
   * amount of ions (trap fills faster).
   *
   * @return Ion injection time in ms, or null.
   */
  @Nullable
  Float getIonInjectTime();

  /**
   * Returns the precursor m/z. Null is returned if the precursor information is not specified in
   * the data or if there is no single precursor (such as data-independent acquisition or DIA
   * scans).
   *
   * @return Precursor m/z, or null.
   */
  @Nullable
  Double getPrecursorMz();

  /**
   * Returns the precursor charge. Null is returned if the charge is unknown or if the MS/MS scan
   * targets multiple ions (such as data-independent acquisition or DIA scans). Charge is always
   * represented by a positive integer.
   *
   * @return Precursor charge (positive integer), or null.
   */
  @Nullable
  Integer getPrecursorCharge();

  /**
   * Returns the precursor scan number. Null is returned if the precursor is unknown.
   *
   * @return Precursor scan number (positive integer), or null.
   */
  @Nullable
  Integer getPrecursorScanNumber();

  /**
   * Returns the details about the fragmentation that followed this isolation. Null is returned if
   * no fragmentation occurred or if the details are unknown.
   *
   * @return Fragmentation info, or null.
   */
  @Nullable
  ActivationInfo getActivationInfo();

}
