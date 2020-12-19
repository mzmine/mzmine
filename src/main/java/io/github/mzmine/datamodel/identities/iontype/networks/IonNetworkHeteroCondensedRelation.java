package io.github.mzmine.datamodel.identities.iontype.networks;

import io.github.mzmine.datamodel.identities.iontype.IonNetwork;

public class IonNetworkHeteroCondensedRelation extends IonNetworkRelationInterf {

  // the linked network
  private IonNetwork a;
  private IonNetwork b;
  private IonNetwork condensed;

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
    if (ionNetwork.getID() == a.getID() || ionNetwork.getID() == b.getID()) {
      return "M(" + condensed.getID() + "_condensed)";
    } else if (ionNetwork.getID() == condensed.getID()) {
      return "2Mcondensed(" + a.getID() + "," + b.getID() + ")";
    }
    return "";
  }

  @Override
  public IonNetwork[] getAllNetworks() {
    return new IonNetwork[] {a, b, condensed};
  }

  @Override
  public String getDescription() {
    return "condensation (X+Y-->XY+H2O)";
  }
}
