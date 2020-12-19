package io.github.mzmine.datamodel.identities.iontype.networks;

import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;

public class IonNetRelationFeatureIdentity extends SimpleFeatureIdentity {
  private IonNetwork net;

  public IonNetRelationFeatureIdentity(IonNetwork net, String name) {
    super(name);
    this.net = net;
  }

  public IonNetwork getNetwork() {
    return net;
  }

}
