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
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleMsScan extends AbstractMsSpectrum implements MsScan {

  private @Nullable RawDataFile dataFile;
  private @Nonnull Integer scanNumber;
  private @Nullable String scanDefinition;
  private @Nullable String msFunction;
  private @Nonnull Integer msLevel = 1;
  private @Nonnull PolarityType polarity = PolarityType.UNKNOWN;
  private @Nonnull MsScanType msScanType = MsScanType.UNKNOWN;
  private @Nullable Range<Double> scanningRange;
  private @Nullable Float rt;
  private @Nullable ActivationInfo sourceInducedFragInfo;

  private final @Nonnull List<IsolationInfo> isolations = new LinkedList<>();

  /**
   * <p>
   * Constructor for SimpleMsScan.
   * </p>
   *
   * @param scanNumber a {@link Integer} object.
   */
  public SimpleMsScan(@Nonnull Integer scanNumber) {
    this(scanNumber, null);
  }

  /**
   * <p>
   * Constructor for SimpleMsScan.
   * </p>
   *
   */
  public SimpleMsScan(@Nonnull Integer scanNumber, String msFunction) {
    Preconditions.checkNotNull(scanNumber);
    this.scanNumber = scanNumber;
    this.msFunction = msFunction;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public RawDataFile getRawDataFile() {
    return dataFile;
  }

  /**
   * {@inheritDoc}
   *
   * @param newRawDataFile a {@link RawDataFile} object.
   */
  public void setRawDataFile(@Nonnull RawDataFile newRawDataFile) {
    if ((this.dataFile != null) && (this.dataFile != newRawDataFile)) {
      throw new MSDKRuntimeException(
          "Cannot set the raw data file reference to this scan, because it has already been set");
    }
    this.dataFile = newRawDataFile;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public Integer getScanNumber() {
    return scanNumber;
  }

  /**
   * {@inheritDoc}
   *
   * @param scanNumber a {@link Integer} object.
   */
  public void setScanNumber(@Nonnull Integer scanNumber) {
    Preconditions.checkNotNull(scanNumber);
    this.scanNumber = scanNumber;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getScanDefinition() {
    return scanDefinition;
  }

  /**
   * {@inheritDoc}
   *
   * @param scanDefinition a {@link String} object.
   */
  public void setScanDefinition(@Nullable String scanDefinition) {
    this.scanDefinition = scanDefinition;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public String getMsFunction() {
    return msFunction;
  }

  /**
   * {@inheritDoc}
   *
   */
  public void setMsFunction(@Nullable String newFunction) {
    this.msFunction = newFunction;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Integer getMsLevel() {
    return msLevel;
  }

  /**
   * {@inheritDoc}
   *
   * @param msLevel a {@link Integer} object.
   */
  public void setMsLevel(@Nonnull Integer msLevel) {
    this.msLevel = msLevel;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Range<Double> getScanningRange() {
    return scanningRange;
  }

  /**
   * {@inheritDoc}
   *
   * @param newScanRange a {@link Range} object.
   */
  public void setScanningRange(@Nullable Range<Double> newScanRange) {
    this.scanningRange = newScanRange;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public PolarityType getPolarity() {
    return polarity;
  }

  /**
   * {@inheritDoc}
   *
   * @param newPolarity a {@link PolarityType} object.
   */
  public void setPolarity(@Nonnull PolarityType newPolarity) {
    Preconditions.checkNotNull(newPolarity);
    this.polarity = newPolarity;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public MsScanType getMsScanType() {
    return msScanType;
  }

  /**
   * {@inheritDoc}
   *
   * @param newMsScanType a {@link MsScanType} object.
   */
  public void setMsScanType(@Nonnull MsScanType newMsScanType) {
    Preconditions.checkNotNull(newMsScanType);
    this.msScanType = newMsScanType;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Float getRetentionTime() {
    return rt;
  }

  /**
   * {@inheritDoc}
   *
   * @param rt a {@link Float} object.
   */
  public void setRetentionTime(@Nullable Float rt) {
    this.rt = rt;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public ActivationInfo getSourceInducedFragmentation() {
    return sourceInducedFragInfo;
  }

  /**
   * {@inheritDoc}
   *
   * @param newFragmentationInfo a {@link ActivationInfo} object.
   */
  public void setSourceInducedFragmentation(@Nullable ActivationInfo newFragmentationInfo) {
    this.sourceInducedFragInfo = newFragmentationInfo;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public List<IsolationInfo> getIsolations() {
    return isolations;
  }

}
