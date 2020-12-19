package io.github.mzmine.datamodel.identities.iontype.networks;


import io.github.mzmine.datamodel.identities.iontype.IonNetwork;

import java.util.Arrays;

public abstract class IonNetworkRelationInterf {

  public abstract String getName(IonNetwork net);

  public abstract String getDescription();

  public abstract IonNetwork[] getAllNetworks();

  public boolean isLowestIDNetwork(IonNetwork net) {
    return Arrays.stream(getAllNetworks()).noneMatch(n -> n.getID() < net.getID());
  }
}
