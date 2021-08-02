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

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Implementation of Sample
 */
public class SimpleSample implements Sample {

  private @Nonnull String name;
  private RawDataFile rawDataFile;
  private File originalFile;

  /**
   * <p>Constructor for SimpleSample.</p>
   *
   * @param name a {@link String} object.
   */
  public SimpleSample(@Nonnull String name) {
    this(name, null);
  }

  /**
   * <p>Constructor for SimpleSample.</p>
   *
   * @param name a {@link String} object.
   * @param rawDataFile a {@link RawDataFile} object.
   */
  public SimpleSample(@Nonnull String name, @Nullable RawDataFile rawDataFile) {
    Preconditions.checkNotNull(name);
    this.name = name;
    this.rawDataFile = rawDataFile;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * @param name a {@link String} object.
   */
  public void setName(@Nonnull String name) {
    Preconditions.checkNotNull(name);
    this.name = name;
  }

  /** {@inheritDoc} */
  @Override
  public @Nullable RawDataFile getRawDataFile() {
    return rawDataFile;
  }

  /**
   * {@inheritDoc}
   *
   * @param rawDataFile a {@link RawDataFile} object.
   */
  public void setRawDataFile(@Nullable RawDataFile rawDataFile) {
    this.rawDataFile = rawDataFile;
  }

  /** {@inheritDoc} */
  @Override
  public @Nullable File getOriginalFile() {
    return originalFile;
  }

  /**
   * {@inheritDoc}
   *
   * @param originalFile a {@link File} object.
   */
  public void setOriginalFile(@Nullable File originalFile) {
    this.originalFile = originalFile;
  }

}
