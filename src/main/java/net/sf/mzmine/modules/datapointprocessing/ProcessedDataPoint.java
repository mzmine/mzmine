package net.sf.mzmine.modules.datapointprocessing;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;

public class ProcessedDataPoint extends SimpleDataPoint {
  
  public ProcessedDataPoint(DataPoint dp) {
    super(dp);
    identities = new Vector<PeakIdentity>();
  }
  public ProcessedDataPoint(double mz, double intensity) {
    super(mz, intensity);
  }
  
  public ProcessedDataPoint(DataPoint dp, PeakIdentity preferredIdentity, List<PeakIdentity> identities) {
    super(dp);
    setPreferredIdentity(preferredIdentity);
    
  }
  
  PeakIdentity preferredIdentity;
  List<PeakIdentity> identities;
  HashMap<String, Object> properties;
  
  public void setPreferredIdentity(PeakIdentity identity) {
    this.preferredIdentity = identity;
  }
  
  public PeakIdentity getPreferredIdentiy() {
    return preferredIdentity;
  }
  
  public boolean addIdentity(PeakIdentity identity) {
    if(identity == null)
      return false;
    
    if(identities.contains(identity))
      return false;
    
    if(preferredIdentity == null)
      setPreferredIdentity(identity);
    
    return identities.add(identity);
  }
  
  public void removeIdenities(Iterable<PeakIdentity> identities) {
    for(PeakIdentity id : identities)
      this.identities.remove(id);
  }
  
  public boolean removeIdentity(PeakIdentity identity) {
    return identities.remove(identity);
  }
  
  public PeakIdentity[] getIdentities(){
    return identities.toArray(new PeakIdentity[0]);
  }
}
