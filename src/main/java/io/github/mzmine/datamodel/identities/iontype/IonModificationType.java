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

package io.github.mzmine.datamodel.identities.iontype;

import java.util.List;

/**
 * Type of ion modification.
 */
public enum IonModificationType {
  ADDUCT, UNDEFINED_ADDUCT, NEUTRAL_LOSS, CLUSTER, ISOTOPE, UNKNOWN, MIXED;

  /**
   * The common type or MIXED
   *
   * @param adducts list of ion modifications
   * @return the common type of all modifications - or MIXED
   */
  public static IonModificationType getType(IonModification[] adducts) {
    if (adducts == null || adducts.length == 0) {
      return UNKNOWN;
    }
    IonModificationType t = adducts[0].getType();
    for (int i = 1; i < adducts.length; i++) {
      if (!t.equals(adducts[i].getType())) {
        return IonModificationType.MIXED;
      }
    }
    return t;
  }

  /**
   * The common type or MIXED
   *
   * @param adducts list of ion modifications
   * @return the common type of all modifications - or MIXED
   */
  public static IonModificationType getType(List<IonModification> adducts) {
    if (adducts == null || adducts.isEmpty()) {
      return UNKNOWN;
    }
    IonModificationType t = adducts.get(0).getType();
    for (int i = 1; i < adducts.size(); i++) {
      if (!t.equals(adducts.get(i).getType())) {
        return IonModificationType.MIXED;
      }
    }
    return t;
  }

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
