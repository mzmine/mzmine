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
