/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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


import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import org.jetbrains.annotations.NotNull;

/**
 * Relationship between two IonNetworks
 */
public class IonNetworkModificationRelation extends AbstractIonNetworkRelation {

  // the linked network
  private final IonNetwork a;
  private final IonNetwork b;
  private final IonType modA;
  private final IonType modB;

  public IonNetworkModificationRelation(IonNetwork a, IonNetwork link, IonType mod) {
    // a is smaller neutral mass
    if (a.getNeutralMass() < link.getNeutralMass()) {
      this.a = a;
      this.b = link;
    } else {
      this.b = a;
      this.a = link;
    }

    if (mod.totalMass() <= 0) {
      this.modA = mod;
      this.modB = mod.createOpposite();
    } else {
      this.modB = mod;
      this.modA = mod.createOpposite();
    }
  }

  public IonType getMods() {
    return modA;
  }

  public IonNetwork getLink() {
    return b;
  }

  @Override
  public String getName(IonNetwork ionNetwork) {
    return String.format("M%d+%s→M%d", a.getID(), modA.toString(), b.getID());
  }

  @Override
  @NotNull
  public String getDescription() {
    return modB.toString();
  }

  @Override
  public IonNetwork[] getAllNetworks() {
    return new IonNetwork[]{a, b};
  }
}
