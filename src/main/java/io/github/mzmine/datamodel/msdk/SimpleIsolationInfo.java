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


import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of IsolationInfo
 */
public class SimpleIsolationInfo implements IsolationInfo {

  private @NotNull Range<Double> isolationMzRange;
  private @Nullable Float ionInjectTime;
  private @Nullable Double precursorMz;
  private @Nullable Integer precursorCharge;
  private @Nullable ActivationInfo activationInfo;
  private @Nullable Integer precursorScanNumber;

  /**
   * <p>
   * Constructor for SimpleIsolationInfo.
   * </p>
   *
   * @param isolationMzRange a {@link Range} object.
   */
  public SimpleIsolationInfo(@NotNull Range<Double> isolationMzRange) {
    Preconditions.checkNotNull(isolationMzRange);
    this.isolationMzRange = isolationMzRange;
    ionInjectTime = null;
    precursorMz = null;
    precursorCharge = null;
    activationInfo = null;
    precursorScanNumber = null;
  }

  /**
   * <p>
   * Constructor for SimpleIsolationInfo.
   * </p>
   *
   * @param isolationMzRange a {@link Range} object.
   * @param ionInjectTime a {@link Float} object.
   * @param precursorMz a {@link Double} object.
   * @param precursorCharge a {@link Integer} object.
   * @param activationInfo a {@link ActivationInfo} object.
   */
  public SimpleIsolationInfo(@NotNull Range<Double> isolationMzRange, @Nullable Float ionInjectTime,
      @Nullable Double precursorMz, @Nullable Integer precursorCharge,
      @Nullable ActivationInfo activationInfo, @Nullable Integer precursorScanNumber) {
    Preconditions.checkNotNull(isolationMzRange);
    this.isolationMzRange = isolationMzRange;
    this.ionInjectTime = ionInjectTime;
    this.precursorMz = precursorMz;
    this.precursorCharge = precursorCharge;
    this.activationInfo = activationInfo;
    this.precursorScanNumber = precursorScanNumber;
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public Range<Double> getIsolationMzRange() {
    return isolationMzRange;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Float getIonInjectTime() {
    return ionInjectTime;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Double getPrecursorMz() {
    return precursorMz;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Integer getPrecursorCharge() {
    return precursorCharge;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Integer getPrecursorScanNumber() {
    return precursorScanNumber;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public ActivationInfo getActivationInfo() {
    return activationInfo;
  }

}
