package io.github.mzmine.modules.tools.isotopepatternpreview;

public class IsotopePatternTableData {

  final double mz;
  final double abundance;
  final String composition;

  public IsotopePatternTableData(double mz, double abundance, String composition) {
    this.mz = mz;
    this.abundance = abundance;
    this.composition = composition;
  }

  public double getMz() {
    return mz;
  }

  public double getAbundance() {
    return abundance;
  }

  public String getComposition() {
    return composition;
  }

}
