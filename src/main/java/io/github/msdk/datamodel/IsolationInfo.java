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

package io.github.msdk.datamodel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.Range;

/**
 * Represents the ion isolation information. Each isolation can be accompanied by MS/MS
 * fragmentation, represented by FragmentationInfo. If no fragmentation occurred or if fragmentation
 * details are unknown, getFragmentationInfo() will return null. For convenience, this interface is
 * immutable, so it can be passed by reference and safely used by multiple threads.
 */
@Immutable
public interface IsolationInfo {

  /**
   * Returns the isolated m/z range.
   *
   * @return Isolated m/z range.
   */
  @Nonnull
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
