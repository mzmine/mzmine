package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids2;

public enum LipidCoreClasses {
  FATTYACYLS("Fatty Acyls"), //
  GLYCEROLIPIDS("Glycerolipids"), //
  GLYCEROPHOSPHOLIPIDS("Glycerophospholipids");//
  private String name;

  LipidCoreClasses(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
