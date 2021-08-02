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
import javax.annotation.Nullable;

/**
 * A grouped MS feature that consists of several individual features (m/z and RT pairs). A typical
 * example is several isotopes grouped together, or several adducts of the same ion (M+H, M+Na,
 * etc).
 *
 * @since 0.0.8
 */
public interface GroupedFeature extends Feature {

  /**
   * <p>getIndividualFeatures.</p>
   *
   * @return a {@link List} object.
   */
  @Nonnull
  List<Feature> getIndividualFeatures();

  /**
   * <p>getCharge.</p>
   *
   * @return a {@link Integer} object.
   */
  @Nullable
  Integer getCharge();

}
