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

package io.github.mzmine.datamodel.msdk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Implementation of FragmentationInfo
 */
public class SimpleActivationInfo implements ActivationInfo {

  private @Nonnull ActivationType fragmentationType = ActivationType.UNKNOWN;
  private @Nullable Double activationEnergy;

  /**
   * <p>Constructor for SimpleActivationInfo.</p>
   *
   * @param activationEnergy a {@link Double} object.
   */
  public SimpleActivationInfo(@Nullable Double activationEnergy) {
    this.activationEnergy = activationEnergy;
  }

  /**
   * <p>Constructor for SimpleActivationInfo.</p>
   *
   * @param activationEnergy a {@link Double} object.
   * @param fragmentationType a {@link ActivationType} object.
   */
  public SimpleActivationInfo(@Nullable Double activationEnergy,
      @Nonnull ActivationType fragmentationType) {
    Preconditions.checkNotNull(fragmentationType);
    this.activationEnergy = activationEnergy;
    this.fragmentationType = fragmentationType;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public ActivationType getActivationType() {
    return fragmentationType;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public Double getActivationEnergy() {
    return activationEnergy;
  }

}
