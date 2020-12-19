package io.github.mzmine.datamodel.identities.ms2;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSDataPointIdentity;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class MSMSDataPointIdentity extends AbstractMSMSDataPointIdentity {

  private String name;
  private DataPoint dp;

  public MSMSDataPointIdentity(MZTolerance mzTolerance, DataPoint dp, String name) {
    super(mzTolerance, dp);
    setName(name);
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
