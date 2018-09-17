/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids;

/**
 * This enum contains all lipid classes. Each enum has information on: name, abbreviation,
 * LipidCoreClass, LipidMainClass, lipid backbone sum formula, number of acyl chains, number of
 * alkychains, class specific positive fragments and class specific negative fragments
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public enum LipidClasses {

  // Fattyacyls
  FATTYACIDS("Fatty acids", "FA", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYACIDS, "CHO2",
      1, 0, new String[] {""}, new String[] {""}), //
  FATTYALCOHOLS("Fatty alcohols", "Alcohol", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYALCOHOLS, "HO", 0, 1, new String[] {""}, new String[] {""}), //
  FATTYALDEHYDES("Fatty aldehydes", "Aldehyde", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYALDEHYDES, "CHO", 1, 0, new String[] {""}, new String[] {""}), //
  FATTYESTERS("Fatty esters", "Ester", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYESTERS,
      "CO2", 1, 1, new String[] {""}, new String[] {""}), //
  FATTYAMIDS("Fatty amids", "Amid", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYAMIDS,
      "CH2ON", 1, 0, new String[] {""}, new String[] {""}), //
  FATTYNITRILES("Fatty nitriles", "Nitrile", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.FATTYNITRILES, "CN", 1, 0, new String[] {""}, new String[] {""}), //
  FATTYETHERS("Fatty ethers", "Ethers", LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYETHERS,
      "O", 0, 2, new String[] {""}, new String[] {""}), //
  HYDROCARBONS("Hydrocarbons", "Hydrocarbon", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.HYDROCARBONS, "", 0, 1, new String[] {""}, new String[] {""}), //
  MONORHAMNOLIPIDS("mono-Rhamnolipid", "mRL", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.RHAMNOLIPIDS, "C8H10O9", 2, 0, new String[] {""},
      new String[] {"M-FA+H2", "FA+H2", "fragment M-C6H12O6", "fragment C6H11O5"}), //
  DIRHAMNOLIPIDS("di-Rhamnolipid", "diRL", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.RHAMNOLIPIDS, "C14H20O13", 2, 0, new String[] {""},
      new String[] {"M-FA+H2", "FA+H2", "fragment M-C12H22O9", "fragment M-C6H12O6",
          "fragment C12H21O9", "fragment C6H11O5"}), //
  SOPHOROLIPIDACID("Sophorolipid acid form", "SLP acid", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.SOPHOROLIPIDS, "C13H21O13", 1, 0, new String[] {""}, new String[] {""}), //
  SOPHOROLIPIDLACTON("Sophorolipid lacton form", "SLP lacton", LipidCoreClasses.FATTYACYLS,
      LipidMainClasses.SOPHOROLIPIDS, "C13H19O12", 1, 0, new String[] {""}, new String[] {""}), //

  // Glycerolipids
  MONOACYLGLYCEROLS("Monoacylglycerols", "MG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C4H7O4", 1, 0, new String[] {""}, new String[] {""}), //
  MONOALKYLGLYCEROLS("Monoalkylglycerols", "MG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C3H7O3", 0, 1, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROLS("Diacylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C5H6O5", 2, 0, new String[] {"M-FA"}, new String[] {""}), //
  DIALKYLGLYCEROLS("Dialkylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H6O3", 0, 2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROLS("Alkylacylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C4H6O4", 1, 1, new String[] {""}, new String[] {""}), //
  TRIACYLGLYCEROLS("Triacylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C6H5O6", 3, 0, new String[] {"M-FA"}, new String[] {""}), //
  ALKYLDIACYLGLYCEROLS("Alkyldiacylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C5H5O5", 2, 1, new String[] {""}, new String[] {""}), //
  DIALKYLMONOACYLGLYCEROLS("Dialkylmonoacylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C4H5O4", 1, 2, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROLTRIMETHYLHOMOSERIN("Diacylglyceroltrimethylhomoserin", "DGTS",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS, "C12H19O7N", 2, 0,
      new String[] {"fragment C7H14NO2 ", "fragment C10H22NO5", "M-FA", "M-FA-H2O"},
      new String[] {"FA"}), //
  SULFOQUINOVOSYLMONOACYLGLYCEROLS("Sulfoquinovosylmonoacylglycerols", "SQMG",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLMONOACYLGLYCEROLS, "C10H17O11S", 1,
      0, new String[] {""}, new String[] {""}), //
  MONOGALACTOSYLDIACYLGLYCEROL("Monogalactosyldiacylglycerol", "MGDG",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C11H16O10", 2, 0,
      new String[] {"M-FA", "M-FA-H20", "M-FA-C6H11O6"}, new String[] {"FA"}), //
  DIGALACTOSYLDIACYLGLYCEROL("Digalactosyldiacylglycerol", "DGDG", LipidCoreClasses.GLYCEROLIPIDS,
      LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C17H26O15", 2, 0, new String[] {""},
      new String[] {"FA"}), //
  SULFOQUINOVOSYLDIACYLGLYCEROLS("Sulfoquinovosyldiacylglycerols", "SQDG",
      LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C11H16O12S", 2, 0,
      new String[] {""}, new String[] {"FA", "M-FA", "fragment C6H9O7S"}), //

  // Glycerophospholipids
  DIACYLGLYCEROPHOSPHOCHOLINES("Diacylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C10H18O8PN", 2,
      0, new String[] {"M-FA", "M-FA-H2O", "fragment C5H15NO4P"}, new String[] {"FA"}), //
  DIALKYLGLYCEROPHOSPHOCHOLINES("Dialkylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H18O6PN", 0,
      2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROPHOSPHOCHOLINES("Alkylacylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C9H18O7PN", 1,
      1, new String[] {""}, new String[] {""}), //
  MONOACYLGLYCEROPHOSPHOCHOLINES("Monoacylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C9H19O7PN", 1,
      0, new String[] {""}, new String[] {""}), //
  MONOALKYLGLYCEROPHOSPHOCHOLINES("Monoalkylglycerophosphocholines", "PC",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H19O6PN", 0,
      1, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROPHOSPHOETHANOLAMINES("Diacylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C7H12O8PN", 2, 0, new String[] {""},
      new String[] {"FA", "M-FA", "M-FA-H2O", "fragment C5H11NO5P"}), //
  DIALKYLGLYCEROPHOSPHOETHANOLAMINES("Dialkylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H12O6PN", 0, 2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES("Alkylacylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C6H12O7PN", 1, 1, new String[] {""}, new String[] {""}), //
  MONOACYLGLYCEROPHOSPHOETHANOLAMINES("Monoacylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C6H13O7PN", 1, 0, new String[] {""}, new String[] {"FA", "M-FA", "M-FA-H2O", "M-C2H7NO"}), //
  MONOALKYLGLYCEROPHOSPHOETHANOLAMINES("Monoalkylglycerophosphoethanolamines", "PE",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H13O6PN", 0, 1, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROPHOSPHOSERINES("Diacylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C8H12O10PN",
      2, 0, new String[] {""}, new String[] {"FA", "fragment M-C3H5NO2", "fragment M-C3H5NO2-FA"}), //
  DIALKYLGLYCEROPHOSPHOSERINES("Dialkylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H12O8PN", 0,
      2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROPHOSPHOSERINES("Alkylacylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C7H12O9PN", 1,
      1, new String[] {""}, new String[] {""}), //
  MONOACYLGLYCEROPHOSPHOSERINES("Monoacylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C7H13O9PN", 1,
      0, new String[] {""}, new String[] {""}), //
  MONOALKYLGLYCEROPHOSPHOSERINES("Monoalkylglycerophosphoserines", "PS",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H13O8PN", 0,
      1, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROPHOSPHOGLYCEROLS("Diacylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C8H13O10P",
      2, 0, new String[] {""}, new String[] {"FA", "M-FA", "M-FA-H2O"}), //
  DIALKYLGLYCEROPHOSPHOGLYCEROLS("Dialkylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H13O8P",
      0, 2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROPHOSPHOGLYCEROLS("Alkylacylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C7H13O9P",
      1, 1, new String[] {""}, new String[] {""}), //
  MONOACYLGLYCEROPHOSPHOGLYCEROLS("Monoacylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C7H14O9P",
      1, 0, new String[] {""}, new String[] {""}), //
  MONOALKYLGLYCEROPHOSPHOGLYCEROLS("Monoalkylglycerophosphoglycerols", "PG",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H14O8P",
      0, 1, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Diacylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C8H14O13P2", 2, 0, new String[] {""}, new String[] {""}), //
  DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Dialkylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C6H14O11P2", 0, 2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Alkylacylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C7H14O12P2", 1, 1, new String[] {""}, new String[] {""}), //
  MONOACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Monoacylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C7H15O12P2", 1, 0, new String[] {""}, new String[] {""}), //
  MONOALKYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Monoalkylglycerophosphoglycerophosphates", "PGP",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
      "C6H15O11P2", 0, 1, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROPHOSPHOINOSITOLS("Diacylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C11H17O13P",
      2, 0, new String[] {""}, new String[] {"FA", "M-FA", "M-FA-C6H12O6"}), //
  DIALKYLGLYCEROPHOSPHOINOSITOLS("Dialkylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H17O11P",
      0, 2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROPHOSPHOINOSITOLS("Alkylacylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C10H17O11P",
      1, 1, new String[] {""}, new String[] {""}), //
  MONOACYLGLYCEROPHOSPHOINOSITOLS("Monoacylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C10H18O11P",
      1, 0, new String[] {""}, new String[] {""}), //
  MONOALKYLGLYCEROPHOSPHOINOSITOLS("Monoalkylglycerophosphoinositols", "PI",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H18O10P",
      0, 1, new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROPHOSPHATES("Diacylglycerophosphates", "PA", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.GLYCEROPHOSPHATES, "C5H7O8P", 2, 0, new String[] {""},
      new String[] {"FA", "M-FA", "M-FA-H2O"}), //
  DIALKYLGLYCEROPHOSPHATES("Dialkylglycerophosphates", "PA", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.GLYCEROPHOSPHATES, "C3H6O6P", 0, 2, new String[] {""}, new String[] {""}), //
  ALKYLACYLGLYCEROPHOSPHATES("Alkylacylglycerophosphates", "PA",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C4H7O7P", 1, 1,
      new String[] {""}, new String[] {""}), //
  MONOACYLGLYCEROPHOSPHATES("Monoacylglycerophosphates", "PA",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C4H8O7P", 1, 0,
      new String[] {""}, new String[] {""}), //
  MONOALKYLGLYCEROPHOSPHATES("Monoalkylglycerophosphates", "PA",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C3H8O6P", 0, 1,
      new String[] {""}, new String[] {""}), //
  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
      "Diacylglycerophosphoglycerophosphodiradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C13H18O17P2", 4, 0,
      new String[] {""}, new String[] {"M-FA", "FA+FA+C6H11P2O8", "FA+FA+C6H10O5P", "FA+FA+C3H6PO4",
          "FA+FA+C6H11P2O8", "FA+C3H6PO4+H2O", "FA+C3H6PO4", "FA"}), //
  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
      "Diacylglycerophosphoglycerophosphomonoradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C12H19O16P2", 3, 0,
      new String[] {""}, new String[] {""}), //
  DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
      "Dialkylglycerophosphoglycerophosphodiradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C9H18O13P2", 0, 4,
      new String[] {""}, new String[] {""}), //
  DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
      "Dialkylglycerophosphoglycerophosphomonoradylglycerols", "CL",
      LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C9H19O13P2", 0, 3,
      new String[] {""}, new String[] {""}), //
  CDPDIACYLGLYCEROLS("CDP-diacylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C14H17O15P2N3", 2, 0, new String[] {""}, new String[] {""}), //
  CDPDIALKYLGLYCEROLS("CDP-Dialkylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C12H17O13P2N3", 0, 2, new String[] {""}, new String[] {""}), //
  CDPALKYLACYLGLYCEROLS("CDP-Alkylacylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C13H17O14P2N3", 1, 1, new String[] {""}, new String[] {""}), //
  CDPMONOACYLGLYCEROLS("CDP-Monoacylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C13H18O14P2N3", 1, 0, new String[] {""}, new String[] {""}), //
  CDPMONOALKYLGLYCEROLS("CDP-Monoalkylglycerols", "CDP-DG", LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.CDPGLYCEROLS, "C12H18O13P2N3", 0, 1, new String[] {""}, new String[] {""}), //

  // Sphingolipids
  NACYLSPHINGOSINES("N-acylsphingosines", "Cer", LipidCoreClasses.SPHINGOLIPIDS,
      LipidMainClasses.CERAMIDES, "CHO3N", 1, 1, new String[] {""}, new String[] {""}), //
  NACYL4HYDROXYSPHINGANINES("N-acyl-4-hydroxysphinganines", "Cer", LipidCoreClasses.SPHINGOLIPIDS,
      LipidMainClasses.CERAMIDES, "CHO4N", 1, 1,
      new String[] {"M-OH", "M-OH-H2O", "M-FA-OH", "M-FA-OH-H2O"}, new String[] {""}), //
  ACYLCERAMIDES("Acylceramides", "Cer", LipidCoreClasses.SPHINGOLIPIDS, LipidMainClasses.CERAMIDES,
      "C2O4N", 2, 1, new String[] {""}, new String[] {""}), //
  CERAMIDE1PHOSPHATES("Ceramide 1-phosphates", "CerP", LipidCoreClasses.SPHINGOLIPIDS,
      LipidMainClasses.CERAMIDES, "CH2O6PN", 1, 1, new String[] {""},
      new String[] {"M-H2O", "M-FA", "M-FA-H2O", "fragment H2PO4", "fragment PO3"}); //

  private String name;
  private String abbr;
  private LipidCoreClasses coreClass;
  private LipidMainClasses mainClass;
  private String backBoneFormula;
  private int numberOfAcylChains;
  private int numberofAlkyChains;
  private String[] msmsFragmentsPositiveIonization;
  private String[] msmsFragmentsNegativeIonization;

  LipidClasses(String name, String abbr, LipidCoreClasses coreClass, LipidMainClasses mainClass,
      String backBoneFormula, int numberOfAcylChains, int numberOfAlkylChains,
      String[] msmsFragmentsPositiveIonization, String[] msmsFragmentsNegativeIonization) {
    this.name = name;
    this.abbr = abbr;
    this.coreClass = coreClass;
    this.mainClass = mainClass;
    this.backBoneFormula = backBoneFormula;
    this.numberOfAcylChains = numberOfAcylChains;
    this.numberofAlkyChains = numberOfAlkylChains;
    this.msmsFragmentsPositiveIonization = msmsFragmentsPositiveIonization;
    this.msmsFragmentsNegativeIonization = msmsFragmentsNegativeIonization;
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

  public String[] getMsmsFragmentsPositiveIonization() {
    return msmsFragmentsPositiveIonization;
  }

  public void setMsmsFragmentsPositiveIonization(String[] msmsFragmentsPositiveIonization) {
    this.msmsFragmentsPositiveIonization = msmsFragmentsPositiveIonization;
  }

  public String[] getMsmsFragmentsNegativeIonization() {
    return msmsFragmentsNegativeIonization;
  }

  public void setMsmsFragmentsNegativeIonization(String[] msmsFragmentsNegativeIonization) {
    this.msmsFragmentsNegativeIonization = msmsFragmentsNegativeIonization;
  }

  @Override
  public String toString() {
    return this.abbr + " " + this.name;
  }
}
