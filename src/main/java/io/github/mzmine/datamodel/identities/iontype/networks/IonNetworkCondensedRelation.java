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


import io.github.mzmine.datamodel.identities.iontype.CombinedIonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import java.util.List;

/**
 * Relationship between two IonNetworks: 2a --> b - H2O (condensation reaction)
 */
public class IonNetworkCondensedRelation extends AbstractIonNetworkRelation {

  // the linked network
  private final IonNetwork monomer;
  private final IonNetwork condensedMultimer;
  // marker if one network is condensed of the other
  private final IonModification multimerModification;

  public IonNetworkCondensedRelation(IonNetwork monomer, IonNetwork condensedMultimer,
      List<IonModification> mods) {
    this(monomer, condensedMultimer, CombinedIonModification.create(mods));
  }

  public IonNetworkCondensedRelation(IonNetwork monomer, IonNetwork condensedMultimer,
      IonModification[] mods) {
    this(monomer, condensedMultimer, CombinedIonModification.create(mods));
  }

  public IonNetworkCondensedRelation(IonNetwork monomer, IonNetwork condensedMultimer,
      IonModification mod) {
    if (monomer.getNeutralMass() < condensedMultimer.getNeutralMass()) {
      this.monomer = monomer;
      this.condensedMultimer = condensedMultimer;
    } else {
      this.condensedMultimer = monomer;
      this.monomer = condensedMultimer;
    }

    multimerModification = mod;
  }

  public IonModification getMods() {
    return multimerModification;
  }

  public IonNetwork getMultimer() {
    return condensedMultimer;
  }

  @Override
  public String getName(IonNetwork ionNetwork) {
    return String.format("2(%d)→2M(%d)+H₂O", monomer.getID(), condensedMultimer.getID());
  }

  private String parseNameA() {
    return String.format("2(%d)→2M(%d)+H₂O", monomer.getID(), condensedMultimer.getID());
  }

  private String parseNameB() {
    return String.format("2M(%d)+H₂O→2(%d)", condensedMultimer.getID(), monomer.getID());
  }

  @Override
  public String getDescription() {
    return "condensation (2X→XX+H₂O) " + (multimerModification != null
        ? multimerModification.parseName() : "");
  }

  @Override
  public IonNetwork[] getAllNetworks() {
    return new IonNetwork[]{monomer, condensedMultimer};
  }
}
