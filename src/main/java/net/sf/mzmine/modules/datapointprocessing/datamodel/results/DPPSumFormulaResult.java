package net.sf.mzmine.modules.datapointprocessing.datamodel.results;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.sf.mzmine.main.MZmineCore;

public class DPPSumFormulaResult extends DPPResult<String> {

  private final double ppm;
  private static final NumberFormat format = new DecimalFormat("0.00");
  
  public DPPSumFormulaResult(String key, String formula, double ppm) {
    super(key, formula);
    this.ppm = ppm;
  }

  @Override
  public String generateLabel() {
    return getValue() + " (Δ " + format.format(ppm) + " ppm)";
  }

  @Override
  public Classification getClassification() {
    return Classification.STRING;
  }

  public double getPpm() {
    return ppm;
  }
}
