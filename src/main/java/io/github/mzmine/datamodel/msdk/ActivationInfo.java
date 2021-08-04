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
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents the fragmentation information of MS/MS experiments. For convenience, this interface is
 * immutable, so it can be passed by reference and safely used by multiple threads.
 */
@Unmodifiable
public interface ActivationInfo {

  /**
   * Returns the type of the fragmentation (MS/MS) experiment. If unknown, FragmentationType.UNKNOWN
   * is returned.
   *
   * @return Fragmentation type
   */
  @NotNull
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
