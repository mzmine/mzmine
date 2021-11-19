
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

package io.github.mzmine.datamodel.identities;

import io.github.mzmine.util.FormulaUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class NeutralMolecule {

  @Nullable
  protected final IMolecularFormula cdkFormula;
  @Nullable
  protected final String molFormula;
  protected final double mass;
  @NotNull
  protected String name;

  public NeutralMolecule(@NotNull String name, double mass) {
    this(name, null, mass);
  }

  public NeutralMolecule(@NotNull String name, @Nullable String molFormula, double mass) {
    this.name = name;
    this.molFormula = molFormula;
    if (molFormula != null && molFormula.length() > 0) {
      cdkFormula = FormulaUtils.createMajorIsotopeMolFormula(molFormula);
    } else {
      cdkFormula = null;
    }
    this.mass = mass;
  }

  public double getMass() {
    return mass;
  }

  public double getAbsMass() {
    return Math.abs(mass);
  }

  /**
   * the raw name
   */
  @NotNull
  public String getName() {
    return name;
  }

  /**
   * The parsed name (default + or - the name depending on the mass difference)
   */
  public String parseName() {
    String sign = this.getMass() < 0 ? "-" : "+";
    return sign + getName();
  }

  @Nullable
  public String getMolFormula() {
    return molFormula;
  }

  @Nullable
  public IMolecularFormula getCDKFormula() {
    return cdkFormula;
  }


  @Override
  public int hashCode() {
    return Objects.hash(name, mass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(getClass())) {
      return false;
    }
    NeutralMolecule other = (NeutralMolecule) obj;
    return Objects.equals(name, other.name) && Objects.equals(mass, other.getMass());
  }
}
