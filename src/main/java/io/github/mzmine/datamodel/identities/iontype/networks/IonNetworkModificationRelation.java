/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
