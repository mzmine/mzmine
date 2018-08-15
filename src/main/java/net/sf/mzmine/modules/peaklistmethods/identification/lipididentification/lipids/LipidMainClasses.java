package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

public enum LipidMainClasses {

  FATTYACIDS("Fatty acids", LipidCoreClasses.FATTYACYLS), //
  FATTYALCOHOLS("Fatty alcohols", LipidCoreClasses.FATTYACYLS), //
  FATTYALDEHYDES("Fatty aldehydes", LipidCoreClasses.FATTYACYLS), //
  FATTYESTERS("Fatty esters", LipidCoreClasses.FATTYACYLS), //
  FATTYAMIDS("Fatty amids", LipidCoreClasses.FATTYACYLS), //
  FATTYNITRILES("Fatty esters", LipidCoreClasses.FATTYACYLS), //
  FATTYETHERS("Fatty ehters", LipidCoreClasses.FATTYACYLS), //
  HYDROCARBONS("Hydrocarbons", LipidCoreClasses.FATTYACYLS), //

  MONORADYLGLYCEROLS("Monoradylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  DIRADYLGLYCEROLS("Diradylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  TRIRADYLGLYCEROLS("Triradylglycerols", LipidCoreClasses.GLYCEROLIPIDS), //
  PHOSPHATIDYLCHOLINE("Phosphatidylcholine", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOETHANOLAMINES("Glycerophosphoethanolamines", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOSERINES("Glycerophosphoserines", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROLS("Glycerophosphoglycerols", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOGLYCEROPHOSPHATES("Glycerophosphoglycerophosphates",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHOINOSITOLS("Glycerophosphoinositols", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  GLYCEROPHOSPHATES("Glycerophosphates", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  CARDIOLIPIN("Cardiolipin", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //
  CDPGLYCEROLS("CDP-Glycerols", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS), //

  CERAMIDES("Ceramides", LipidCoreClasses.SPHINGOLIPIDS), //
  PHOSPHOSPHINGOLIPIDS("Phosphosphingolipids", LipidCoreClasses.SPHINGOLIPIDS);//


  private String name;
  private LipidCoreClasses coreClass;

  LipidMainClasses(String name, LipidCoreClasses coreClass) {
    this.name = name;
    this.coreClass = coreClass;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LipidCoreClasses getCoreClass() {
    return coreClass;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
