package net.sf.mzmine.modules.datapointprocessing.datamodel.results;

public class DPPSumFormulaResult extends DPPResult<String> {

  private final double ppm;
  
  public DPPSumFormulaResult(String key, String formula, double ppm) {
    super(key, formula);
    this.ppm = ppm;
  }

  @Override
  public String generateLabel() {
    return getName() + ": " + getValue();
  }

  @Override
  public Classification getClassification() {
    return Classification.STRING;
  }

  public double getPpm() {
    return ppm;
  }
}
