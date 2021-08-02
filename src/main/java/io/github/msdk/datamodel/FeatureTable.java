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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A feature table consists of a list of named columns, each with a specific type for the values
 * contained in it.
 */
@NotThreadSafe
public interface FeatureTable {

  /**
   * Returns an immutable list of rows
   *
   * @return a list of {@link FeatureTableRow}s.
   */
  @Nonnull
  List<FeatureTableRow> getRows();

  /**
   * Shortcut to return an immutable list of {@link Sample}s found in this feature table.
   *
   * @return the list of samples.
   */
  @Nonnull
  List<Sample> getSamples();

  /**
   * Remove all data associated to this feature table.
   */
  void dispose();

}
