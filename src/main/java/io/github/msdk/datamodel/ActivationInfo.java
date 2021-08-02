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

/**
 * Represents the fragmentation information of MS/MS experiments. For convenience, this interface is
 * immutable, so it can be passed by reference and safely used by multiple threads.
 */
@Immutable
public interface ActivationInfo {

  /**
   * Returns the type of the fragmentation (MS/MS) experiment. If unknown, FragmentationType.UNKNOWN
   * is returned.
   *
   * @return Fragmentation type
   */
  @Nonnull
  ActivationType getActivationType();

  /**
   * Returns the activation energy applied for this MS/MS scan. This value has no dimension and its
   * meaning depends on instrument. Null is returned if unknown.
   *
   * @return MS/MS activation energy, or null.
   */
  @Nullable
  Double getActivationEnergy();

}
