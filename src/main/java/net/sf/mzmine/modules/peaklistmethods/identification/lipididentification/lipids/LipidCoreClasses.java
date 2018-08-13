package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

public enum LipidCoreClasses {

  FATTYACYLS("Fatty Acyls", "FA"), //
  GLYCEROLIPIDS("Glycerolipids", "GL"), //
  GLYCEROPHOSPHOLIPIDS("Glycerophospholipids", "GPL");//

  private String name;
  private String abbr;

  LipidCoreClasses(String name, String abbr) {
    this.name = name;
    this.abbr = abbr;
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


}
