package net.sf.mzmine.modules.datapointprocessing.datamodel.results;

public class DPPSumFormulaResult extends DPPResult<String> {

  public DPPSumFormulaResult(String key, String value) {
    super(key, value);
  }

  @Override
  public String generateLabel() {
    return getName() + ": " + getValue();
  }

  @Override
  public Classification getClassification() {
    return Classification.STRING;
  }
}
