package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

public enum LipidMainClasses {

  MONORADYLGLYCEROLS("Monoradylglycerols", "MG", LipidCoreClasses.GLYCEROLIPIDS), //
  DIRADYLGLYCEROLS("Diradylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS), //
  TRIRADYLGLYCEROLS("Triradylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS); //
  // GLYCEROPHOSPHOETHANOLAMINES, //
  // GLYCEROPHOSPHOSERINES, //
  // GLYCEROPHOSPHOGLYCEROLS, //
  // GLYCEROPHOSPHOGLYCEROPHOSPHATES, //
  // GLYCEROPHOSPHOINOSITOLS, //
  // GLYCEROPHOSPHATES, //
  // CARDIOLIPIN, //
  // CDPGLYCEROLS;//

  private String name;
  private String abbr;
  private LipidCoreClasses coreClass;

  LipidMainClasses(String name, String abbr, LipidCoreClasses coreClass) {
    this.name = name;
    this.abbr = abbr;
    this.coreClass = coreClass;
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


}
