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

package io.github.mzmine.datamodel.identities.iontype.networks;

import io.github.mzmine.datamodel.identities.iontype.IonNetwork;

/**
 * A relationship of two molecules a+b --> c - H2O represented by 3 IonNetworks
 */
public class IonNetworkHeteroCondensedRelation extends AbstractIonNetworkRelation {

  // the linked network
  private final IonNetwork a;
  private final IonNetwork b;
  private final IonNetwork condensed;

  public IonNetworkHeteroCondensedRelation(IonNetwork a, IonNetwork b, IonNetwork condensed) {
    // condensed has to have highest mass
    if (a.getNeutralMass() > condensed.getNeutralMass()) {
      IonNetwork tmp = a;
      a = condensed;
      condensed = tmp;
    }
    if (b.getNeutralMass() > condensed.getNeutralMass()) {
      IonNetwork tmp = b;
      b = condensed;
      condensed = tmp;
    }

    this.a = a;
    this.b = b;
    this.condensed = condensed;
  }

  @Override
  public String getName(IonNetwork ionNetwork) {
    return String.format("%d+%d→AB(%d)+H₂O", a.getID(), b.getID(), condensed.getID());
  }

  @Override
  public IonNetwork[] getAllNetworks() {
    return new IonNetwork[]{a, b, condensed};
  }

  @Override
  public String getDescription() {
    return "condensation (X+Y→XY+H₂O)";
  }
}
