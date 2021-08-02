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

/**
 * <p>
 * Sample interface.
 * </p>
 */
public interface Sample {

  /**
   * <p>
   * getName.
   * </p>
   *
   * @return Short descriptive name
   */
  @Nonnull
  String getName();

  /**
   * Returns a raw data file or null if this sample has no associated raw data.
   *
   * @return a {@link RawDataFile} object.
   */
  @Nullable
  RawDataFile getRawDataFile();

  /**
   * Returns the original file name and path where the file was loaded from, or null if this file
   * was created by MSDK.
   *
   * @return Original filename and path.
   */
  @Nullable
  File getOriginalFile();

}
