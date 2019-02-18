package net.sf.mzmine.modules.datapointprocessing.datamodel.results;

import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;

public class DPPResultsDataSet extends DataPointsDataSet {

  private static final long serialVersionUID = 1L;
  
  public DPPResultsDataSet(String label, ProcessedDataPoint[] mzPeaks) {
    super(label, mzPeaks);
  }

  public ProcessedDataPoint[] getDataPoints() {
    return (ProcessedDataPoint[]) mzPeaks;
  }
  
  // TODO: Label generator, Renderer to work with isotope patterns
}
