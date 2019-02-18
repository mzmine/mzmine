package net.sf.mzmine.modules.datapointprocessing.datamodel.results;

import net.sf.mzmine.datamodel.IsotopePattern;

public class DPPIsotopePatternResult extends DPPResult<IsotopePattern>{

  public DPPIsotopePatternResult(String key, IsotopePattern value) {
    super(key, value);
  }

  @Override
  public String generateLabel() {
    return "Isotope pattern detected";
  }

  @Override
  public Classification getClassification() {
    return Classification.ISOTOPE_PATTERN;
  }
}
