package net.sf.mzmine.modules.peaklistmethods.identification.localdbsearch;

import java.text.MessageFormat;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class SpectralDBPeakIdentity extends SimplePeakIdentity {

  private final String name;
  private final double mz;
  private final double rt;
  private final String adduct;
  private final DataPoint[] dps;

  public SpectralDBPeakIdentity(String name, double mz, double rt, String adduct, String formula,
      String method, DataPoint[] dps) {
    super(MessageFormat.format("{0} ({1}) {2}", name, mz, formula), formula, method, "", "");
    this.name = name;
    this.mz = mz;
    this.rt = rt;
    this.adduct = adduct;
    this.dps = dps;
  }

  public String getCompoundName() {
    return name;
  }

  public double getMz() {
    return mz;
  }

  public String getAdduct() {
    return adduct;
  }

  public DataPoint[] getDataPoints() {
    return dps;
  }

  public double getRetentionTime() {
    return rt;
  }



}
