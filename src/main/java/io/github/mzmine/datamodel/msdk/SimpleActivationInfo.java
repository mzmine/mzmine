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

import io.github.mzmine.datamodel.ActivationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;

/**
 * Implementation of FragmentationInfo
 */
public class SimpleActivationInfo implements ActivationInfo {

  private @NotNull ActivationType fragmentationType = ActivationType.UNKNOWN;
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
      @NotNull ActivationType fragmentationType) {
    Preconditions.checkNotNull(fragmentationType);
    this.activationEnergy = activationEnergy;
    this.fragmentationType = fragmentationType;
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
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
