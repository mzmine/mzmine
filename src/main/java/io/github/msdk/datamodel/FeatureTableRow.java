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

/**
 * <p>
 * FeatureTableRow interface.
 * </p>
 */
public interface FeatureTableRow {

  /**
   * Returns the feature table where this feature table row belongs. Each feature table row is
   * assigned to exactly one feature table.
   *
   * @return the feature table.
   */
  @Nonnull
  FeatureTable getFeatureTable();

  /**
   * Shortcut to return the m/z column value of this row
   *
   * @return the m/z column value of this row.
   */
  @Nullable
  Double getMz();
  
  /**
   * Shortcut to return the m/z column value of this row
   *
   * @return the m/z column value of this row.
   */
  @Nullable
  Float getRT();
  
  /**
   * Return the charge of this row
   *
   * @return charge
   */
  @Nullable
  Integer getCharge();

  /**
   * Return feature assigned to this row
   *
   * @param sample a {@link Sample} object.
   * @return a {@link Feature} object.
   */
  @Nullable
  Feature getFeature(@Nonnull Sample sample);

  /**
   * Return feature assigned to this row
   *
   * @param index a {@link Integer} object.
   * @return a {@link Feature} object.
   */
  @Nullable
  Feature getFeature(@Nonnull Integer index);

}
