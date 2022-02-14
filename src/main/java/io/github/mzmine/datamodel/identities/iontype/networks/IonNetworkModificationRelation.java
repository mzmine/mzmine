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
 * Relationship between two IonNetworks
 */
public class IonNetworkModificationRelation extends AbstractIonNetworkRelation {

  // the linked network
  private final IonNetwork a;
  private final IonNetwork b;
  private final IonModification modA;
  private final IonModification modB;

  public IonNetworkModificationRelation(IonNetwork a, IonNetwork link, List<IonModification> mods) {
    this(a, link, CombinedIonModification.create(mods));
  }

  public IonNetworkModificationRelation(IonNetwork a, IonNetwork link, IonModification[] mods) {
    this(a, link, CombinedIonModification.create(mods));
  }

  public IonNetworkModificationRelation(IonNetwork a, IonNetwork link, IonModification mod) {
    // a is smaller neutral mass
    if (a.getNeutralMass() < link.getNeutralMass()) {
      this.a = a;
      this.b = link;
    } else {
      this.b = a;
      this.a = link;
    }

    if (mod.getMass() <= 0) {
      this.modA = mod;
      this.modB = mod.createOpposite();
    } else {
      this.modB = mod;
      this.modA = mod.createOpposite();
    }
  }

  public IonModification getMods() {
    return modA;
  }

  public IonNetwork getLink() {
    return b;
  }

  @Override
  public String getName(IonNetwork ionNetwork) {
    return String.format("M%d+%s→M%d", a.getID(), modA.getName(), b.getID());
  }

  private String parseNameA() {
    String name = "M(" + b.getID() + ")";
    if (modA != null) {
      name += modA.parseName();
    }
    return name;
  }

  private String parseNameB() {
    String name = "M(" + a.getID() + ")";
    if (modB != null) {
      name += modB.parseName();
    }
    return name;
  }

  @Override
  public String getDescription() {
    String desc = "";
    desc += modB.parseName();
    return desc;
  }

  @Override
  public IonNetwork[] getAllNetworks() {
    return new IonNetwork[]{a, b};
  }
}
