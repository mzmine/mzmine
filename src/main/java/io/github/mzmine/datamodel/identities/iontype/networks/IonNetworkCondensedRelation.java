/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.identities.iontype.networks;


import io.github.mzmine.datamodel.identities.iontype.CombinedIonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import java.text.MessageFormat;
import java.util.List;

/**
 * Relationship between two IonNetworks: 2a --> b - H2O (condensation reaction)
 */
public class IonNetworkCondensedRelation implements IonNetworkRelation {

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
    if (ionNetwork.getID() == monomer.getID()) {
      return parseNameA();
    } else if (ionNetwork.getID() == condensedMultimer.getID()) {
      return parseNameB();
    }
    return "";
  }

  private String parseNameA() {
    return MessageFormat.format("M({0}_condensed)", condensedMultimer.getID());
  }

  private String parseNameB() {
    return MessageFormat.format("2Mcondensed({0}){1}", monomer.getID(),
        multimerModification != null ? " " + multimerModification.parseName() : "");
  }

  @Override
  public String getDescription() {
    return "condensation (2X-->XX+H2O) " +
           (multimerModification != null ? multimerModification.parseName() : "");
  }

  @Override
  public IonNetwork[] getAllNetworks() {
    return new IonNetwork[]{monomer, condensedMultimer};
  }
}
