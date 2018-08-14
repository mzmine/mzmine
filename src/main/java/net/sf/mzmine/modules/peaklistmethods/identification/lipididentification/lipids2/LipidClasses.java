package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids2;

public enum LipidClasses {
  // enum

  FATTYACIDS("Fatty acids", "FA", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYACIDS, "CHO2"), //
  FATTYALCOHOLS("Fatty alcohols", "Alcohols", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYALCOHOLS, "HO"), //
  FATTYALDEHYDES("Fatty aldehydes", "Aldehydes", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYALDEHYDES, "CHO");//



  // static
  // var
  private String name;
  private String abbr;
  private LipidCoreClasses coreClass;
  private LipidMainClasses mainClass;
  private String backBoneFormula;

  LipidClasses(String name, String abbr, LipidCoreClasses coreClass, LipidMainClasses mainClass,
      String backBoneFormula) {
    this.name = name;
    this.abbr = abbr;
    this.coreClass = coreClass;
    this.mainClass = mainClass;
    this.backBoneFormula = backBoneFormula;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAbbr() {
    return abbr;
  }

  public void setAbbr(String abbr) {
    this.abbr = abbr;
  }

  public LipidCoreClasses getCoreClass() {
    return coreClass;
  }

  public void setCoreClass(LipidCoreClasses coreClass) {
    this.coreClass = coreClass;
  }

  public LipidMainClasses getMainClass() {
    return mainClass;
  }

  public void setMainClass(LipidMainClasses mainClass) {
    this.mainClass = mainClass;
  }

  public String getBackBoneFormula() {
    return backBoneFormula;
  }

  public void setBackBoneFormula(String backBoneFormula) {
    this.backBoneFormula = backBoneFormula;
  }

  @Override
  public String toString() {
    return this.abbr + " " + this.name;
  }
}
