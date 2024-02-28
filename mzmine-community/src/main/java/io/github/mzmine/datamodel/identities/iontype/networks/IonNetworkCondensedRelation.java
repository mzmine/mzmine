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
