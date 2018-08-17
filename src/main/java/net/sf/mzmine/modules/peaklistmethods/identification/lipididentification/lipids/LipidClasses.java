package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

public enum LipidClasses {
  FATTYACIDS("Fatty acids", "FA", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYACIDS, "CHO2",
      1, 0), //
  FATTYALCOHOLS("Fatty alcohols", "Alcohol", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYALCOHOLS, "HO", 0, 1), //
  FATTYALDEHYDES("Fatty aldehydes", "Aldehyde", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYALDEHYDES, "CHO", 1, 0), //
  FATTYESTERS("Fatty esters", "Ester", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYESTERS,
      "CO2", 1, 1), //
  FATTYAMIDS("Fatty amids", "Amid", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYAMIDS,
      "CH2ON", 1, 0), //
  FATTYNITRILES("Fatty nitriles", "Nitrile", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYNITRILES, "CN", 1, 0), //
  FATTYETHERS("Fatty ethers", "Ethers", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYETHERS,
      "O", 0, 2), //
  HYDROCARBONS("Hydrocarbons", "Hydrocarbon", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.HYDROCARBONS, "", 0, 1), //

  MONOACYLGLYCEROLS("Monoacylglycerols", "MG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C4H7O4", 1, 0), //
  MONOALKYLGLYCEROLS("Monoalkylglycerols", "MG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C3H7O3", 0, 1), //
  DIACYLGLYCEROLS("Diacylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C5H6O5", 2, 0), //
  DIALKYLGLYCEROLS("Dialkylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H6O3", 0, 2), //
  ALKYLACYLGLYCEROLS("Alkylacylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C4H6O4", 1, 1), //
  TRIACYLGLYCEROLS("Triacylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C6H5O6", 3, 0), //
  ALKYLDIACYLGLYCEROLS("Alkyldiacylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C5H5O5", 2, 1), //
  DIALKYLMONOACYLGLYCEROLS("Dialkylmonoacylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C4H5O4", 1, 2), //
  DIACYLGLYCEROLTRIMETHYLHOMOSERIN("Diacylglyceroltrimethylhomoserin", "DGTS",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS, "C12H19O7N", 2, 0), //
  SULFOQUINOVOSYLMONOACYLGLYCEROLS("Sulfoquinovosylmonoacylglycerols", "SQMG",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLMONOACYLGLYCEROLS, "C10H17O11S", 1,
      0), //
  MONOGALACTOSYLDIACYLGLYCEROL("Monogalactosyldiacylglycerol", "MGDG",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C11H16O10", 2, 0), //
  DIGALACTOSYLDIACYLGLYCEROL("Digalactosyldiacylglycerol", "DGDG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C17H26O15", 2, 0), //
  SULFOQUINOVOSYLDIACYLGLYCEROLS("Sulfoquinovosyldiacylglycerols", "SQDG",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C11H16O12S", 2, 0), //


  DIACYLGLYCEROPHOSPHOCHOLINES("Diacylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C10H18O8PN", 2,
      0), //
  DIALKYLGLYCEROPHOSPHOCHOLINES("Dialkylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H18O6PN", 0,
      2), //
  ALKYLACYLGLYCEROPHOSPHOCHOLINES("Alkylacylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C9H18O7PN", 1,
      1), //
  MONOACYLGLYCEROPHOSPHOCHOLINES("Monoacylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C9H19O7PN", 1,
      0), //
  MONOALKYLGLYCEROPHOSPHOCHOLINES("Monoalkylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H19O6PN", 0,
      1), //
  DIACYLGLYCEROPHOSPHOETHANOLAMINES("Diacylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C7H12O8PN", 2, 0), //
  DIALKYLGLYCEROPHOSPHOETHANOLAMINES("Dialkylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H12O6PN", 0, 2), //
  ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES("Alkylacylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C6H12O7PN", 1, 1), //
  MONOACYLGLYCEROPHOSPHOETHANOLAMINES("Monoacylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C6H13O7PN", 1, 0), //
  MONOALKYLGLYCEROPHOSPHOETHANOLAMINES("Monoalkylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H13O6PN", 0, 1), //
  DIACYLGLYCEROPHOSPHOSERINES("Diacylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C8H12O10PN",
      2, 0), //
  DIALKYLGLYCEROPHOSPHOSERINES("Dialkylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H12O8PN", 0,
      2), //
  ALKYLACYLGLYCEROPHOSPHOSERINES("Alkylacylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C7H12O9PN", 1,
      1), //
  MONOACYLGLYCEROPHOSPHOSERINES("Monoacylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C7H13O9PN", 1,
      0), //
  MONOALKYLGLYCEROPHOSPHOSERINES("Monoalkylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H13O8PN", 0,
      1), //
  DIACYLGLYCEROPHOSPHOGLYCEROLS("Diacylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C8H13O10P",
      2, 0), //
  DIALKYLGLYCEROPHOSPHOGLYCEROLS("Dialkylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H13O8P",
      0, 2), //
  ALKYLACYLGLYCEROPHOSPHOGLYCEROLS("Alkylacylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C7H13O9P",
      1, 1), //
  MONOACYLGLYCEROPHOSPHOGLYCEROLS("Monoacylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C7H14O9P",
      1, 0), //
  MONOALKYLGLYCEROPHOSPHOGLYCEROLS("Monoalkylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H14O8P",
      0, 1), //
  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Diacylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C8H14O13P2", 2, 0), //
  DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Dialkylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C6H14O11P2", 0, 2), //
  ALKYLACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Alkylacylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C7H14O12P2", 1, 1), //
  MONOACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Monoacylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C7H15O12P2", 1, 0), //
  MONOALKYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Monoalkylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C6H15O11P2", 0, 1), //
  DIACYLGLYCEROPHOSPHOINOSITOLS("Diacylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C11H17O13P",
      2, 0), //
  DIALKYLGLYCEROPHOSPHOINOSITOLS("Dialkylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H17O11P",
      0, 2), //
  ALKYLACYLGLYCEROPHOSPHOINOSITOLS("Alkylacylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C10H17O11P",
      1, 1), //
  MONOACYLGLYCEROPHOSPHOINOSITOLS("Monoacylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C10H18O11P",
      1, 0), //
  MONOALKYLGLYCEROPHOSPHOINOSITOLS("Monoalkylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H18O10P",
      0, 1), //
  DIACYLGLYCEROPHOSPHATES("Diacylglycerophosphates", "PA", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.GLYCEROPHOSPHATES, "C5H7O8P", 2, 0), //
  DIALKYLGLYCEROPHOSPHATES("Dialkylglycerophosphates", "PA", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.GLYCEROPHOSPHATES, "C3H6O6P", 0, 2), //
  ALKYLACYLGLYCEROPHOSPHATES("Alkylacylglycerophosphates", "PA",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C4H7O7P", 1, 1), //
  MONOACYLGLYCEROPHOSPHATES("Monoacylglycerophosphates", "PA",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C4H8O7P", 1, 0), //
  MONOALKYLGLYCEROPHOSPHATES("Monoalkylglycerophosphates", "PA",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C3H8O6P", 0, 1), //
  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
      "Diacylglycerophosphoglycerophosphodiradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C13H18O17P2", 4, 0), //
  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
      "Diacylglycerophosphoglycerophosphomonoradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C12H19O16P2", 3, 0), //
  DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
      "Dialkylglycerophosphoglycerophosphodiradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C9H18O13P2", 0, 4), //
  DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
      "Dialkylglycerophosphoglycerophosphomonoradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C9H19O13P2", 0, 3), //
  CDPDIACYLGLYCEROLS("CDP-diacylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C14H17O15P2N3", 2, 0), //
  CDPDIALKYLGLYCEROLS("CDP-Dialkylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C12H17O13P2N3", 0, 2), //
  CDPALKYLACYLGLYCEROLS("CDP-Alkylacylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C13H17O14P2N3", 1, 1), //
  CDPMONOACYLGLYCEROLS("CDP-Monoacylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C13H18O14P2N3", 1, 0), //
  CDPMONOALKYLGLYCEROLS("CDP-Monoalkylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C12H18O13P2N3", 0, 1), //

  NACYLSPHINGOSINES("N-acylsphingosines (ceramides)", "Cer", LipidCoreClasses.SPHINGOLIPIDS,
      LipidMainClasses.CERAMIDES, "CHO3N", 1, 1), //
  NACYL4HYDROXYSPHINGANINES("N-acyl-4-hydroxysphinganines (phytoceramides)", "Cer",
      LipidCoreClasses.SPHINGOLIPIDS, LipidMainClasses.CERAMIDES, "CHO4N", 1, 1), //
  ACYLCERAMIDES("Acylceramides", "Cer", LipidCoreClasses.SPHINGOLIPIDS, LipidMainClasses.CERAMIDES,
      "C2O4N", 2, 1), //
  CERAMIDE1PHOSPHATES("Ceramide 1-phosphates", "CerP", LipidCoreClasses.SPHINGOLIPIDS,
      LipidMainClasses.CERAMIDES, "CH2O6PN", 1, 1); //


  private String name;
  private String abbr;
  private LipidCoreClasses coreClass;
  private LipidMainClasses mainClass;
  private String backBoneFormula;
  private int numberOfAcylChains;
  private int numberofAlkyChains;

  LipidClasses(String name, String abbr, LipidCoreClasses coreClass, LipidMainClasses mainClass,
      String backBoneFormula, int numberOfAcylChains, int numberOfAlkylChains) {
    this.name = name;
    this.abbr = abbr;
    this.coreClass = coreClass;
    this.mainClass = mainClass;
    this.backBoneFormula = backBoneFormula;
    this.numberOfAcylChains = numberOfAcylChains;
    this.numberofAlkyChains = numberOfAlkylChains;
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

  public int getNumberOfAcylChains() {
    return numberOfAcylChains;
  }

  public void setNumberOfAcylChains(int numberOfAcylChains) {
    this.numberOfAcylChains = numberOfAcylChains;
  }

  public int getNumberofAlkyChains() {
    return numberofAlkyChains;
  }

  public void setNumberofAlkyChains(int numberofAlkyChains) {
    this.numberofAlkyChains = numberofAlkyChains;
  }

  @Override
  public String toString() {
    return this.abbr + " " + this.name;
  }
}
