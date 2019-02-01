/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */package net.sf.mzmine.datamodel.identities;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import net.sf.mzmine.util.FormulaUtils;

public class NeutralMolecule {

  protected @Nullable IMolecularFormula cdkFormula;
  protected @Nullable String molFormula;
  protected @Nonnull String name;
  protected @Nonnull double mass;

  public NeutralMolecule(String name, double mass) {
    this(name, "", mass);
  }

  public NeutralMolecule(String name, String molFormula, double mass) {
    super();
    this.name = name;
    this.molFormula = molFormula;
    cdkFormula = FormulaUtils.createMajorIsotopeMolFormula(molFormula);
    this.mass = mass;
  }

  public double getMass() {
    return mass;
  }

  public double getAbsMass() {
    return Math.abs(mass);
  }

  public String getName() {
    return name;
  }

  public String parseName() {
    String sign = this.getMass() < 0 ? "-" : "+";
    return sign + getName();
  }

  public String getMolFormula() {
    return molFormula;
  }

  public IMolecularFormula getCDKFormula() {
    return cdkFormula;
  }


  @Override
  public int hashCode() {
    return Objects.hash(name, mass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!obj.getClass().equals(getClass()))
      return false;
    if (!(obj instanceof NeutralMolecule))
      return false;
    NeutralMolecule other = (NeutralMolecule) obj;
    if (!name.equals(other.name))
      return false;
    if (!Objects.equals(mass, other.getMass()))
      return false;
    return true;
  }
}
