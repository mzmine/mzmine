package io.github.mzmine.datamodel.identities.ms2.interf;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public abstract class AbstractMSMSDataPointIdentity extends AbstractMSMSIdentity {

  protected DataPoint dp;

  public AbstractMSMSDataPointIdentity(MZTolerance mzTolerance, DataPoint dp) {
    super(mzTolerance);
    this.dp = dp;
  }

  public AbstractMSMSDataPointIdentity(DataPoint dp) {
    this(null, dp);
  }


  public DataPoint getDp() {
    return dp;
  }

  public double getMZ() {
    return dp.getMZ();
  }

}
