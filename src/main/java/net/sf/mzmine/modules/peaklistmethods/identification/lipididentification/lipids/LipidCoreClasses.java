package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

public enum LipidCoreClasses {
  FATTYACYLS("Fatty Acyls"), //
  GLYCEROLIPIDS("Glycerolipids"), //
  GLYCEROPHOSPHOLIPIDS("Glycerophospholipids"), //
  SPHINGOLIPIDS("Sphingolipids");//
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
