package net.sf.mzmine.modules.datapointprocessing.datamodel;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;

/**
 * This class stores the results of DataPointProcessingTasks. It offers more functionality, e.g.
 * assigning identities.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ProcessedDataPoint extends SimpleDataPoint {

  PeakIdentity preferredIdentity;
  List<PeakIdentity> identities;
  HashMap<String, Object> properties;
  
  /**
   * Generates an array of ProcessedDataPoints from DataPoints.
   * 
   * @param dp DataPoints to convert.
   * @return Array of ProcessedDataPoints from DataPoints.
   */
  public static ProcessedDataPoint[] convert(DataPoint[] dp) {
    if (dp == null)
      return new ProcessedDataPoint[0];

    ProcessedDataPoint[] pdp = new ProcessedDataPoint[dp.length];
    for (int i = 0; i < pdp.length; i++)
      pdp[i] = new ProcessedDataPoint(dp[i]);
    return pdp;
  }

  public ProcessedDataPoint(DataPoint dp) {
    super(dp);
    identities = new Vector<PeakIdentity>();
  }

  public ProcessedDataPoint(double mz, double intensity) {
    super(mz, intensity);
    identities = new Vector<PeakIdentity>();
  }

  public ProcessedDataPoint(DataPoint dp, PeakIdentity preferredIdentity,
      List<PeakIdentity> identities) {
    super(dp);
    setPreferredIdentity(preferredIdentity);
    this.identities = identities;
  }

  public void setPreferredIdentity(PeakIdentity identity) {
    this.preferredIdentity = identity;
  }

  /**
   * 
   * @return The preferred identity of this data point. Might be null, if no identity has been set
   *         at all. If any identity has been set, the first one will be assigned to
   *         preferredIdentity automatically.
   */
  public PeakIdentity getPreferredIdentiy() {
    return preferredIdentity;
  }

  public boolean addIdentity(PeakIdentity identity) {
    if (identity == null)
      return false;

    if (identities.contains(identity))
      return false;

    if (preferredIdentity == null)
      setPreferredIdentity(identity);

    return identities.add(identity);
  }

  public void removeIdenities(Iterable<PeakIdentity> identities) {
    for (PeakIdentity id : identities)
      this.identities.remove(id);
  }

  public boolean removeIdentity(PeakIdentity identity) {
    return identities.remove(identity);
  }

  public PeakIdentity[] getIdentities() {
    return identities.toArray(new PeakIdentity[0]);
  }

  /*
   * public boolean equals(ProcessedDataPoint p) { //TODO }
   */
}
