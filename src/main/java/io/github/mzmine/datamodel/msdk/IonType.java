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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Annotation of ionization type
 */
@Unmodifiable
public interface IonType {

  /**
   * <p>
   * getName.
   * </p>
   *
   * @return Name of ionization type, such as [M+2H]2+.
   */
  @NotNull
  String getName();

  /**
   * <p>
   * getPolarity.
   * </p>
   *
   * @return Polarity of ionization. See #PolarityType.java for values.
   */
  @NotNull
  PolarityType getPolarity();

  /**
   * <p>
   * getNumberOfMolecules.
   * </p>
   *
   * @return Number of molecules in this ion.
   */
  int getNumberOfMolecules();

  /**
   * <p>
   * getAdductFormula.
   * </p>
   *
   * @return Chemical formula for adduct.
   */
  @NotNull
  String getAdductFormula();

  /**
   * <p>
   * getCharge.
   * </p>
   *
   * @return Charge of ion.
   */
  Integer getCharge();

}
