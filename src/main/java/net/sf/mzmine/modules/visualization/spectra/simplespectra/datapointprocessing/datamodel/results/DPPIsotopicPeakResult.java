package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import java.text.NumberFormat;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;

public class DPPIsotopicPeakResult extends DPPResult<ProcessedDataPoint> {

  private final int charge;
  private final String isotope;
  private static final NumberFormat format = MZmineCore.getConfiguration().getMZFormat();

  public DPPIsotopicPeakResult(ProcessedDataPoint peak, String isotope, int charge) {
    super(peak);
    this.isotope = isotope;
    this.charge = charge;
  }

  public int getCharge() {
    return charge;
  }

  public String getIsotope() {
    return isotope;
  }

  @Override
  public String toString() {
    return format.format(value.getMZ()) + " (" + isotope + ")";
  }

  @Override
  public ResultType getResultType() {
    return ResultType.ISOTOPICPEAK;
  }
}
