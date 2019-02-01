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
 */package net.sf.mzmine.datamodel.identities.iontype;

public enum IonModificationType {
  ADDUCT, UNDEFINED_ADDUCT, NEUTRAL_LOSS, CLUSTER, ISOTOPE, UNKNOWN, MIXED;
  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }

  /**
   * The common type or MIXED
   * 
   * @param adducts
   * @return
   */
  public static IonModificationType getType(IonModification[] adducts) {
    IonModificationType t = adducts[0].getType();
    for (int i = 1; i < adducts.length; i++)
      if (!t.equals(adducts[i].getType()))
        return IonModificationType.MIXED;
    return t;
  }
}
