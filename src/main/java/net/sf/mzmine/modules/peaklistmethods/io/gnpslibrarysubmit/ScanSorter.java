package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

import java.util.Comparator;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.exceptions.MissingMassListException;

public class ScanSorter implements Comparator<Scan> {
  private MassListSorter comp;
  private String massListName;

  public ScanSorter(String massListName, double noiseLevel, ScanSortMode sort) {
    this.massListName = massListName;
    comp = new MassListSorter(sort, noiseLevel);
  }

  @Override
  public int compare(Scan a, Scan b) {
    MassList ma = ScanUtils.getMassListOrFirst(a, massListName);
    MassList mb = ScanUtils.getMassListOrFirst(b, massListName);
    if (ma == null || mb == null)
      throw new RuntimeException(new MissingMassListException(massListName));
    return comp.compare(ma.getDataPoints(), mb.getDataPoints());
  }

}
