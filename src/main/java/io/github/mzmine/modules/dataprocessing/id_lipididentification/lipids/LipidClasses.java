/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;

/**
 * This enum contains all lipid classes. Each enum has information on: name, abbreviation,
 * LipidCoreClass, LipidMainClass, lipid backbone sum formula, number of acyl chains, number of
 * alkychains, class specific positive fragments and class specific negative fragments
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public enum LipidClasses implements ILipidClass {

  // // Fattyacyls
  // FATTYACIDS("Fatty acids", "FA", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYACIDS, "CHO2",
  // 1, 0, new String[] {""}, new String[] {""}), //
  // FATTYALCOHOLS("Fatty alcohols", "Alcohol", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYALCOHOLS, "HO", 0, 1, new String[] {""}, new String[]
  // {""}), //
  // FATTYALDEHYDES("Fatty aldehydes", "Aldehyde", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYALDEHYDES, "CHO", 1, 0, new String[] {""}, new String[]
  // {""}), //
  // FATTYESTERS("Fatty esters", "Ester", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYESTERS,
  // "CO2", 1, 1, new String[] {""}, new String[] {""}), //
  // FATTYAMIDS("Fatty amids", "Amid", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYAMIDS,
  // "CH2ON", 1, 0, new String[] {""}, new String[] {""}), //
  // FATTYNITRILES("Fatty nitriles", "Nitrile", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYNITRILES, "CN", 1, 0, new String[] {""}, new String[]
  // {""}), //
  // FATTYETHERS("Fatty ethers", "Ethers", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYETHERS,
  // "O", 0, 2, new String[] {""}, new String[] {""}), //
  // HYDROCARBONS("Hydrocarbons", "Hydrocarbon", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.HYDROCARBONS, "", 0, 1, new String[] {""}, new String[]
  // {""}), //
  // MONORHAMNOLIPIDS("mono-Rhamnolipid", "mRL", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.RHAMNOLIPIDS, "C8H10O9", 2, 0, new String[] {""},
  // new String[] {"M-FA+H2", "FA+H2", "fragment M-C6H12O6", "fragment C6H11O5"}),
  // //
  // DIRHAMNOLIPIDS("di-Rhamnolipid", "diRL", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.RHAMNOLIPIDS, "C14H20O13", 2, 0, new String[] {""},
  // new String[] {"M-FA+H2", "FA+H2", "fragment M-C12H22O9", "fragment
  // M-C6H12O6",
  // "fragment C12H21O9", "fragment C6H11O5"}), //
  // HYDROXYALKANOYLOXYALKANOIC("Hydroxyalkanoyloxyalkanoic", "HAA",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.RHAMNOLIPIDS, "C2O5", 2, 0, new String[] {""},
  // new String[] {"M-FA+H2", "fragment C2H4O2"}), //
  // SOPHOROLIPIDACID("Sophorolipid acid form", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C13H21O13", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC1("Sophorolipid acid form Ac1", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C15H23O14", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC2("Sophorolipid acid form Ac2", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C17H25O15", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC3("Sophorolipid acid form Ac3", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C19H27O16", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC4("Sophorolipid acid form Ac3", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C21H29O17", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTON("Sophorolipid lacton form", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C13H19O12", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC1("Sophorolipid lacton form Ac1", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C15H21O13", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC2("Sophorolipid lacton form Ac2", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C17H23O14", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC3("Sophorolipid lacton form Ac3", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C19H25O15", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC4("Sophorolipid lacton form Ac4", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C21H27O16", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // MANNOSYLERYTHRITOLA("Mannosylerythritol A", "MEL A",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.MANNOSYLERYTHRITOL, "C16H22O13", 2, 0,
  // new String[] {"M-C4H13O4N", "M-FA-C4H13O4N", "M-C4H13O5N"}, new String[]
  // {""}), //
  // MANNOSYLERYTHRITOLBC("Mannosylerythritol B/C", "MEL B/C",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.MANNOSYLERYTHRITOL, "C14H20O12", 2, 0,
  // new String[] {"M-C4H13O4N", "M-FA-C4H13O4N", "M-C4H13O5N"}, new String[]
  // {""}), //
  // MANNOSYLERYTHRITOLD("Mannosylerythritol D", "MEL D",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.MANNOSYLERYTHRITOL, "C12H18O11", 2, 0,
  // new String[] {"M-C4H13O4N", "M-FA-C4H13O4N", "M-C4H13O5N"}, new String[]
  // {""}), //
  //
  // // Glycerolipids
  // MONOACYLGLYCEROLS("Monoacylglycerols", "MG", LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.MONORADYLGLYCEROLS, "C4H7O4", 1, 0, new String[] {""}, new
  // String[] {""}), //
  // MONOALKYLGLYCEROLS("Monoalkylglycerols", "MG",
  // LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.MONORADYLGLYCEROLS, "C3H7O3", 0, 1, new String[] {""}, new
  // String[] {""}), //
  // DIACYLGLYCEROLS("Diacylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.DIRADYLGLYCEROLS, "C5H6O5", 2, 0, new String[] {"M-FA"}, new
  // String[] {""}), //
  // DIALKYLGLYCEROLS("Dialkylglycerols", "DG", LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.DIRADYLGLYCEROLS, "C3H6O3", 0, 2, new String[] {""}, new
  // String[] {""}), //
  // ALKYLACYLGLYCEROLS("Alkylacylglycerols", "DG",
  // LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.DIRADYLGLYCEROLS, "C4H6O4", 1, 1, new String[] {""}, new
  // String[] {""}), //
  // TRIACYLGLYCEROLS("Triacylglycerols", "TG", LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.TRIRADYLGLYCEROLS, "C6H5O6", 3, 0, new String[] {"M-FA"},
  // new String[] {""}), //
  // ALKYLDIACYLGLYCEROLS("Alkyldiacylglycerols", "TG",
  // LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.TRIRADYLGLYCEROLS, "C5H5O5", 2, 1, new String[] {""}, new
  // String[] {""}), //
  // DIALKYLMONOACYLGLYCEROLS("Dialkylmonoacylglycerols", "TG",
  // LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.TRIRADYLGLYCEROLS, "C4H5O4", 1, 2, new String[] {""}, new
  // String[] {""}), //
  // DIACYLGLYCEROLTRIMETHYLHOMOSERIN("Diacylglyceroltrimethylhomoserin", "DGTS",
  // LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS,
  // "C12H19O7N", 2, 0,
  // new String[] {"fragment C7H14NO2 ", "fragment C10H22NO5", "M-FA",
  // "M-FA-H2O"},
  // new String[] {"FA"}), //
  // MONOACYLGLYCEROLTRIMETHYLHOMOSERIN("Monoacylglyceroltrimethylhomoserin",
  // "LysoDGTS",
  // LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS,
  // "C11H21O6N", 1, 0,
  // new String[] {"fragment C7H14NO2 ", "M-FA", "M-FA-H2O"}, new String[]
  // {"FA"}), //
  // SULFOQUINOVOSYLMONOACYLGLYCEROLS("Sulfoquinovosylmonoacylglycerols", "SQMG",
  // LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLMONOACYLGLYCEROLS,
  // "C10H17O11S", 1,
  // 0, new String[] {""}, new String[] {""}), //
  // MONOGALACTOSYLDIACYLGLYCEROL("Monogalactosyldiacylglycerol", "MGDG",
  // LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS,
  // "C11H16O10", 2, 0,
  // new String[] {"M-FA", "M-FA-H20", "M-FA-C6H11O6"}, new String[] {"FA"}), //
  // DIGALACTOSYLDIACYLGLYCEROL("Digalactosyldiacylglycerol", "DGDG",
  // LipidCoreClasses.GLYCEROLIPIDS,
  // LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C17H26O15", 2, 0, new String[]
  // {"M-FA"},
  // new String[] {"FA"}), //
  // SULFOQUINOVOSYLDIACYLGLYCEROLS("Sulfoquinovosyldiacylglycerols", "SQDG",
  // LipidCoreClasses.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS,
  // "C11H16O12S", 2, 0,
  // new String[] {"M-FA", "M-C6H9O7S", "M-FA-C6H9O7S"},
  // new String[] {"FA", "M-FA", "fragment C6H9O7S"}), //
  // // Glycerophospholipids
  // DIACYLGLYCEROPHOSPHOCHOLINES("Diacylglycerophosphocholines", "PC",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE,
  // "C10H18O8PN", 2,
  // 0, new String[] {"M-FA", "M-FA-H2O", "fragment C5H15NO4P"}, new String[]
  // {"FA"}), //
  // DIALKYLGLYCEROPHOSPHOCHOLINES("Dialkylglycerophosphocholines", "PC",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE,
  // "C8H18O6PN", 0,
  // 2, new String[] {""}, new String[] {""}), //
  // ALKYLACYLGLYCEROPHOSPHOCHOLINES("Alkylacylglycerophosphocholines", "PC",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE,
  // "C9H18O7PN", 1,
  // 1, new String[] {""}, new String[] {""}), //
  // MONOACYLGLYCEROPHOSPHOCHOLINES("Monoacylglycerophosphocholines", "PC",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE,
  // "C9H19O7PN", 1,
  // 0, new String[] {"M-FA", "M-FA-H2O", "fragment C5H15NO4P"}, new String[]
  // {""}), //
  // MONOALKYLGLYCEROPHOSPHOCHOLINES("Monoalkylglycerophosphocholines", "PC",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE,
  // "C8H19O6PN", 0,
  // 1, new String[] {""}, new String[] {""}), //
  // DIACYLGLYCEROPHOSPHOETHANOLAMINES("Diacylglycerophosphoethanolamines", "PE",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
  // "C7H12O8PN", 2, 0, new String[] {"M-FA", "M-FA-H2O", "M-C2H8NO4P"},
  // new String[] {"FA", "M-FA", "M-FA-H2O", "fragment C5H11NO5P"}), //
  // DIALKYLGLYCEROPHOSPHOETHANOLAMINES("Dialkylglycerophosphoethanolamines",
  // "PE",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
  // "C5H12O6PN", 0, 2, new String[] {""}, new String[] {""}), //
  // ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES("Alkylacylglycerophosphoethanolamines",
  // "PE",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
  // "C6H12O7PN", 1, 1, new String[] {""}, new String[] {""}), //
  // MONOACYLGLYCEROPHOSPHOETHANOLAMINES("Monoacylglycerophosphoethanolamines",
  // "PE",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
  // "C6H13O7PN", 1, 0, new String[] {"M-C2H8NO4P"},
  // new String[] {"FA", "M-FA", "M-FA-H2O", "M-C2H7NO"}), //
  // MONOALKYLGLYCEROPHOSPHOETHANOLAMINES("Monoalkylglycerophosphoethanolamines",
  // "PE",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
  // "C5H13O6PN", 0, 1, new String[] {""}, new String[] {""}), //
  // DIACYLGLYCEROPHOSPHOSERINES("Diacylglycerophosphoserines", "PS",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOSERINES, "C8H12O10PN",
  // 2, 0, new String[] {""}, new String[] {"FA", "fragment M-C3H5NO2", "fragment
  // M-C3H5NO2-FA"}), //
  // DIALKYLGLYCEROPHOSPHOSERINES("Dialkylglycerophosphoserines", "PS",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H12O8PN", 0,
  // 2, new String[] {""}, new String[] {""}), //
  // ALKYLACYLGLYCEROPHOSPHOSERINES("Alkylacylglycerophosphoserines", "PS",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOSERINES, "C7H12O9PN", 1,
  // 1, new String[] {""}, new String[] {""}), //
  // MONOACYLGLYCEROPHOSPHOSERINES("Monoacylglycerophosphoserines", "PS",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOSERINES, "C7H13O9PN", 1,
  // 0, new String[] {""}, new String[] {"fragment M-C3H5NO2"}), //
  // MONOALKYLGLYCEROPHOSPHOSERINES("Monoalkylglycerophosphoserines", "PS",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H13O8PN", 0,
  // 1, new String[] {""}, new String[] {""}), //
  // DIACYLGLYCEROPHOSPHOGLYCEROLS("Diacylglycerophosphoglycerols", "PG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C8H13O10P",
  // 2, 0, new String[] {""}, new String[] {"FA", "M-FA", "M-FA-H2O"}), //
  // DIALKYLGLYCEROPHOSPHOGLYCEROLS("Dialkylglycerophosphoglycerols", "PG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H13O8P",
  // 0, 2, new String[] {""}, new String[] {""}), //
  // ALKYLACYLGLYCEROPHOSPHOGLYCEROLS("Alkylacylglycerophosphoglycerols", "PG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C7H13O9P",
  // 1, 1, new String[] {""}, new String[] {""}), //
  // MONOACYLGLYCEROPHOSPHOGLYCEROLS("Monoacylglycerophosphoglycerols", "PG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C7H14O9P",
  // 1, 0, new String[] {""}, new String[] {""}), //
  // MONOALKYLGLYCEROPHOSPHOGLYCEROLS("Monoalkylglycerophosphoglycerols", "PG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H14O8P",
  // 0, 1, new String[] {""}, new String[] {""}), //
  // DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Diacylglycerophosphoglycerophosphates",
  // "PGP",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
  // "C8H14O13P2", 2, 0, new String[] {""}, new String[] {""}), //
  // DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Dialkylglycerophosphoglycerophosphates",
  // "PGP",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
  // "C6H14O11P2", 0, 2, new String[] {""}, new String[] {""}), //
  // ALKYLACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Alkylacylglycerophosphoglycerophosphates",
  // "PGP",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
  // "C7H14O12P2", 1, 1, new String[] {""}, new String[] {""}), //
  // MONOACYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Monoacylglycerophosphoglycerophosphates",
  // "PGP",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
  // "C7H15O12P2", 1, 0, new String[] {""}, new String[] {""}), //
  // MONOALKYLGLYCEROPHOSPHOGLYCEROPHOSPHATES("Monoalkylglycerophosphoglycerophosphates",
  // "PGP",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOGLYCEROPHOSPHATES,
  // "C6H15O11P2", 0, 1, new String[] {""}, new String[] {""}), //
  // DIACYLGLYCEROPHOSPHOINOSITOLS("Diacylglycerophosphoinositols", "PI",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C11H17O13P",
  // 2, 0, new String[] {""}, new String[] {"FA", "M-FA", "M-FA-C6H12O6"}), //
  // DIALKYLGLYCEROPHOSPHOINOSITOLS("Dialkylglycerophosphoinositols", "PI",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H17O11P",
  // 0, 2, new String[] {""}, new String[] {""}), //
  // ALKYLACYLGLYCEROPHOSPHOINOSITOLS("Alkylacylglycerophosphoinositols", "PI",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C10H17O11P",
  // 1, 1, new String[] {""}, new String[] {""}), //
  // MONOACYLGLYCEROPHOSPHOINOSITOLS("Monoacylglycerophosphoinositols", "PI",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C10H18O11P",
  // 1, 0, new String[] {""}, new String[] {""}), //
  // MONOALKYLGLYCEROPHOSPHOINOSITOLS("Monoalkylglycerophosphoinositols", "PI",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H18O10P",
  // 0, 1, new String[] {""}, new String[] {""}), //
  // DIACYLGLYCEROPHOSPHOINOSITOLMONOMANNOSIDE("Diacylglycerophosphoinositolmonomannoside",
  // "AC2PIM1",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C17H27O18P", 2, 0, new String[] {""},
  // new String[] {"FA", "M-FA", "M-C6H10O5", "M-FA-C12H20O10"}), //
  // DIACYLGLYCEROPHOSPHOINOSITOLDIMANNOSIDE("Diacylglycerophosphoinositoldimannoside",
  // "AC2PIM2",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C23H37O23P", 2, 0, new String[] {""}, new String[] {"FA", "M-FA",
  // "M-C6H10O5"}), //
  // TRIACYLPHOSPHATIDYLINOSITOLDIMANNOSIDE("Triacylglycerophosphoinositoldimannoside",
  // "AC3PIM2",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C24H36O24P", 3, 0, new String[] {""},
  // new String[] {"FA", "M-FA", "M-FA+H2O", "M-FA-FA", "M-FA-FA-C3H5O",
  // "M-C6H10O5"}), //
  // TETRAACYLPHOSPHATIDYLINOSITOLDIMANNOSIDE("Tetraacylglycerophosphoinositoldimannoside",
  // "AC3PIM2",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C25H35O25P", 4, 0, new String[] {""},
  // new String[] {"FA", "M-FA", "M-FA-FA", "M-FA-FA-C3H5O"}), //
  // DIACYLGLYCEROPHOSPHATES("Diacylglycerophosphates", "PA",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHATES, "C5H7O8P", 2, 0, new String[] {"M-PO4H3",
  // "M-FA"},
  // new String[] {"FA", "M-FA", "M-FA-H2O"}), //
  // DIALKYLGLYCEROPHOSPHATES("Dialkylglycerophosphates", "PA",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHATES, "C3H6O6P", 0, 2, new String[] {""}, new
  // String[] {""}), //
  // ALKYLACYLGLYCEROPHOSPHATES("Alkylacylglycerophosphates", "PA",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES,
  // "C4H7O7P", 1, 1,
  // new String[] {""}, new String[] {""}), //
  // MONOACYLGLYCEROPHOSPHATES("Monoacylglycerophosphates", "PA",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES,
  // "C4H8O7P", 1, 0,
  // new String[] {""}, new String[] {"FA"}), //
  // MONOALKYLGLYCEROPHOSPHATES("Monoalkylglycerophosphates", "PA",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES,
  // "C3H8O6P", 0, 1,
  // new String[] {""}, new String[] {""}), //
  // DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
  // "Diacylglycerophosphoglycerophosphodiradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C13H18O17P2", 4, 0,
  // new String[] {""}, new String[] {"M-FA", "FA+FA+C6H11P2O8", "FA+FA+C6H10O5P",
  // "FA+FA+C3H6PO4",
  // "FA+FA+C6H11P2O8", "FA+C3H6PO4+H2O", "FA+C3H6PO4", "FA"}), //
  // DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
  // "Diacylglycerophosphoglycerophosphomonoradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C12H19O16P2", 3, 0,
  // new String[] {""}, new String[] {""}), //
  // DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
  // "Dialkylglycerophosphoglycerophosphodiradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C9H18O13P2", 0, 4,
  // new String[] {""}, new String[] {""}), //
  // DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
  // "Dialkylglycerophosphoglycerophosphomonoradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C9H19O13P2", 0, 3,
  // new String[] {""}, new String[] {""}), //
  // CDPDIACYLGLYCEROLS("CDP-diacylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C14H17O15P2N3", 2, 0, new String[] {""}, new
  // String[] {""}), //
  // CDPDIALKYLGLYCEROLS("CDP-Dialkylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C12H17O13P2N3", 0, 2, new String[] {""}, new
  // String[] {""}), //
  // CDPALKYLACYLGLYCEROLS("CDP-Alkylacylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C13H17O14P2N3", 1, 1, new String[] {""}, new
  // String[] {""}), //
  // CDPMONOACYLGLYCEROLS("CDP-Monoacylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C13H18O14P2N3", 1, 0, new String[] {""}, new
  // String[] {""}), //
  // CDPMONOALKYLGLYCEROLS("CDP-Monoalkylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C12H18O13P2N3", 0, 1, new String[] {""}, new
  // String[] {""}), //
  //
  // // Sphingolipids
  // NACYLSPHINGOSINES("N-acylsphingosines", "Cer",
  // LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES, "CHO3N", 1, 1, new String[] {""}, new String[]
  // {""}), //
  // NACYL4HYDROXYSPHINGANINES("N-acyl-4-hydroxysphinganines", "Cer",
  // LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES, "CHO4N", 1, 1,
  // new String[] {"M-OH", "M-OH-H2O", "M-FA-OH", "M-FA-OH-H2O"}, new String[]
  // {""}), //
  // ACYLCERAMIDES("Acylceramides", "Cer", LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES,
  // "C2O4N", 2, 1, new String[] {""}, new String[] {""}), //
  // CERAMIDE1PHOSPHATES("Ceramide 1-phosphates", "CerP",
  // LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES, "CH2O6PN", 1, 1, new String[] {""},
  // new String[] {"M-H2O", "M-FA", "M-FA-H2O", "fragment H2PO4", "fragment
  // PO3"}); //

  // Fattyacyls
  // FATTYACIDS("Fatty acids", "FA",
  // LipidCoreClasses.FATTYACYLS, LipidMainClasses.FATTYACIDS,
  // "C10H21O5N",
  // new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
  // new LipidFragmentationRule[] {//
  // new LipidFragmentationRule(PolarityType.POSITIVE,
  // IonizationType.POSITIVE_HYDROGEN,
  // LipidFragmentationRuleType.HEADGROUP_FRAGMENT,
  // LipidFragmentInformationLevelType.SPECIES_LEVEL, "C7H14NO2"), //
  // new LipidFragmentationRule(PolarityType.POSITIVE,
  // IonizationType.POSITIVE_HYDROGEN,
  // LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
  // LipidFragmentInformationLevelType.MOLECULAR_SPECIES_LEVEL), //
  // new LipidFragmentationRule(PolarityType.POSITIVE,
  // IonizationType.POSITIVE_HYDROGEN,
  // LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT, //
  // LipidFragmentInformationLevelType.MOLECULAR_SPECIES_LEVEL, //
  // "H2O")}), //
  // FATTYACIDS("Fatty acids", "FA", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYACIDS, "CHO2",
  // 1, 0, new String[] {""}, new String[] {""}), //
  // FATTYALCOHOLS("Fatty alcohols", "Alcohol", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYALCOHOLS, "HO", 0, 1, new String[] {""}, new String[]
  // {""}), //
  // FATTYALDEHYDES("Fatty aldehydes", "Aldehyde", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYALDEHYDES, "CHO", 1, 0, new String[] {""}, new String[]
  // {""}), //
  // FATTYESTERS("Fatty esters", "Ester", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYESTERS,
  // "CO2", 1, 1, new String[] {""}, new String[] {""}), //
  // FATTYAMIDS("Fatty amids", "Amid", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYAMIDS,
  // "CH2ON", 1, 0, new String[] {""}, new String[] {""}), //
  // FATTYNITRILES("Fatty nitriles", "Nitrile", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYNITRILES, "CN", 1, 0, new String[] {""}, new String[]
  // {""}), //
  // FATTYETHERS("Fatty ethers", "Ethers", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.FATTYETHERS,
  // "O", 0, 2, new String[] {""}, new String[] {""}), //
  // HYDROCARBONS("Hydrocarbons", "Hydrocarbon", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.HYDROCARBONS, "", 0, 1, new String[] {""}, new String[]
  // {""}), //
  // MONORHAMNOLIPIDS("mono-Rhamnolipid", "mRL", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.RHAMNOLIPIDS, "C8H10O9", 2, 0, new String[] {""},
  // new String[] {"M-FA+H2", "FA+H2", "fragment M-C6H12O6", "fragment C6H11O5"}),
  // //
  // DIRHAMNOLIPIDS("di-Rhamnolipid", "diRL", LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.RHAMNOLIPIDS, "C14H20O13", 2, 0, new String[] {""},
  // new String[] {"M-FA+H2", "FA+H2", "fragment M-C12H22O9", "fragment
  // M-C6H12O6",
  // "fragment C12H21O9", "fragment C6H11O5"}), //
  // HYDROXYALKANOYLOXYALKANOIC("Hydroxyalkanoyloxyalkanoic", "HAA",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.RHAMNOLIPIDS, "C2O5", 2, 0, new String[] {""},
  // new String[] {"M-FA+H2", "fragment C2H4O2"}), //
  // SOPHOROLIPIDACID("Sophorolipid acid form", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C13H21O13", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC1("Sophorolipid acid form Ac1", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C15H23O14", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC2("Sophorolipid acid form Ac2", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C17H25O15", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC3("Sophorolipid acid form Ac3", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C19H27O16", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDACIDAC4("Sophorolipid acid form Ac3", "SLP acid",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C21H29O17", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTON("Sophorolipid lacton form", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C13H19O12", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC1("Sophorolipid lacton form Ac1", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C15H21O13", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC2("Sophorolipid lacton form Ac2", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C17H23O14", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC3("Sophorolipid lacton form Ac3", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C19H25O15", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // SOPHOROLIPIDLACTONAC4("Sophorolipid lacton form Ac4", "SLP lacton",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.SOPHOROLIPIDS, "C21H27O16", 1, 0, new String[] {""},
  // new String[] {"FA-H+O", "FA-H2"}), //
  // MANNOSYLERYTHRITOLA("Mannosylerythritol A", "MEL A",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.MANNOSYLERYTHRITOL, "C16H22O13", 2, 0,
  // new String[] {"M-C4H13O4N", "M-FA-C4H13O4N", "M-C4H13O5N"}, new String[]
  // {""}), //
  // MANNOSYLERYTHRITOLBC("Mannosylerythritol B/C", "MEL B/C",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.MANNOSYLERYTHRITOL, "C14H20O12", 2, 0,
  // new String[] {"M-C4H13O4N", "M-FA-C4H13O4N", "M-C4H13O5N"}, new String[]
  // {""}), //
  // MANNOSYLERYTHRITOLD("Mannosylerythritol D", "MEL D",
  // LipidCoreClasses.FATTYACYLS,
  // LipidMainClasses.MANNOSYLERYTHRITOL, "C12H18O11", 2, 0,
  // new String[] {"M-C4H13O4N", "M-FA-C4H13O4N", "M-C4H13O5N"}, new String[]
  // {""}), //

  // Glycerolipids
  MONOACYLGLYCEROLS("Monoacylglycerols", "MG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "NH3"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H5ON"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "H5ON")}), //
  MONOALKYLGLYCEROLS("Monoalkylglycerols", "MG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
      }), //
  DIACYLGLYCEROLS("Diacylglycerols", "DG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "NH3"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H5ON"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O")}), //
  DIALKYLGLYCEROLS("Dialkylglycerols", "DG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] {
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
      }), //
  ALKYLACYLGLYCEROLS("Alkylacylglycerols", "DG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] {new LipidFragmentationRule(PolarityType.POSITIVE,
          IonizationType.AMMONIUM, LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, //
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O")}), //
  TRIACYLGLYCEROLS("Triacylglycerols", "TG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN,
          LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "Na"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "NH3"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H5ON"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "NH3")}), //
  ALKYLDIACYLGLYCEROLS("Alkyldiacylglycerols", "TG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ALKYL_CHAIN,
          LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] {//
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
      }), //
  DIALKYLMONOACYLGLYCEROLS("Dialkylmonoacylglycerols", "TG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN,
          LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
      }), //
  DIACYLGLYCEROLTRIMETHYLHOMOSERIN("Diacylglyceroltrimethylhomoserin", "DGTS",
      LipidCategories.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS, "C10H21O5N",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C10H22NO5+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C7H14NO2+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "H2O")}), //
  MONOACYLGLYCEROLTRIMETHYLHOMOSERIN("Monoacylglyceroltrimethylhomoserin", "LDGTS",
      LipidCategories.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS, "C10H21O5N",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] {
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C10H22NO5+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C7H14NO2+"), ////
      }), //
  MONOGALACTOSYLDIACYLGLYCEROL("Monogalactosyldiacylglycerol", "MGDG",
      LipidCategories.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C9H18O8",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H15NO6"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C6H11O6"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      }), //
  DIGALACTOSYLDIACYLGLYCEROL("Digalactosyldiacylglycerol", "DGDG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C15H28O13",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C12H21O11"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C12H23O12"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //
      }), //
  SULFOQUINOVOSYLDIACYLGLYCEROLS("Sulfoquinovosyldiacylglycerols", "SQDG",
      LipidCategories.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C9H18O10S",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H13O7SN"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H15O8SN"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "NH3"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H9O7S-"), //
      }), //
  SULFOQUINOVOSYLMONOACYLGLYCEROLS("Sulfoquinovosylmonoacylglycerols", "SQMG",
      LipidCategories.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLMONOACYLGLYCEROLS, "C9H18O10S",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //

  // Glycerophospholipids
  DIACYLGLYCEROPHOSPHOCHOLINES("Diacylglycerophosphocholines", "PC",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H20O6PN",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C5H15NO4P+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "H2O"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H9N"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C5H14NO4P"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H5O4PNa+"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL),//
      }), //
  DIALKYLGLYCEROPHOSPHOCHOLINES("Dialkylglycerophosphocholines", "PC",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H20O6PN",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
      }), //
  ALKYLACYLGLYCEROPHOSPHOCHOLINES("Alkylacylglycerophosphocholines", "PC",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H20O6PN",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C5H15NO4P+"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O2"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O2"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  MONOACYLGLYCEROPHOSPHOCHOLINES("Monoacylglycerophosphocholines", "LPC",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H20O6PN",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C5H15NO4P+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H9N+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H2O"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O2") //
      }), //
  MONOALKYLGLYCEROPHOSPHOCHOLINES("Monoalkylglycerophosphocholines", "LPC",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H20O6PN",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
      }), //
  DIACYLGLYCEROPHOSPHOETHANOLAMINES("Diacylglycerophosphoethanolamines", "PE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H8NO4P"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "OH"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H5N"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H8NO4P"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C5H11NO5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  DIALKYLGLYCEROPHOSPHOETHANOLAMINES("Dialkylglycerophosphoethanolamines", "PE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
  ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES("Alkylacylglycerophosphoethanolamines", "PE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H8NO4P"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "OH"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "OH"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  MONOACYLGLYCEROPHOSPHOETHANOLAMINES("Monoacylglycerophosphoethanolamines", "LPE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H8NO4P"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C5H11NO5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  MONOALKYLGLYCEROPHOSPHOETHANOLAMINES("Monoalkylglycerophosphoethanolamines", "LPE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
  DIACYLGLYCEROPHOSPHOSERINES("Diacylglycerophosphoserines", "PS",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H14O8NP",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H8NO6P"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "H2O"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H8NO6PNa+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H3PO4"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2PO4-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "PO3-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H5NO2"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  DIALKYLGLYCEROPHOSPHOSERINES("Dialkylglycerophosphoserines", "PS",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H14O8NP",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
  ALKYLACYLGLYCEROPHOSPHOSERINES("Alkylacylglycerophosphoserines", "PS",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H14O8NP",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H6O5P"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H5NO2"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  MONOACYLGLYCEROPHOSPHOSERINES("Monoacylglycerophosphoserines", "LPS",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H14O8NP",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
  MONOALKYLGLYCEROPHOSPHOSERINES("Monoalkylglycerophosphoserines", "LPS",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H14O8NP",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
  DIACYLGLYCEROPHOSPHOGLYCEROLS("Diacylglycerophosphoglycerols", "PG",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H15O8P",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H12O6PN"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H14O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2PO4-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "PO3-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H2O"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  DIALKYLGLYCEROPHOSPHOGLYCEROLS("Dialkylglycerophosphoglycerols", "PG",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H15O8P",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
  ALKYLACYLGLYCEROPHOSPHOGLYCEROLS("Alkylacylglycerophosphoglycerols", "PG",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H15O8P",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H12O6PN"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H14O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2PO4-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "PO3-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H2O"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  MONOACYLGLYCEROPHOSPHOGLYCEROLS("Monoacylglycerophosphoglycerols", "LPG",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H15O8P",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-")}), //
  MONOALKYLGLYCEROPHOSPHOGLYCEROLS("Monoalkylglycerophosphoglycerols", "LPG",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H15O8P",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-")}), //
  MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS("Monoacylglycerophosphomonoradylglycerols", "BMP",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H15O8P",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H12O6PN"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5"), //


          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H14O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2PO4-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "PO3-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H2O"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  DIACYLGLYCEROPHOSPHOINOSITOLS("Diacylglycerophosphoinositols", "PI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H16O9PN"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H13O9PNa"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H13O9PNa+"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H10O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  DIALKYLGLYCEROPHOSPHOINOSITOLS("Dialkylglycerophosphoinositols", "PI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN)//
      }), //
  ALKYLACYLGLYCEROPHOSPHOINOSITOLS("Alkylacylglycerophosphoinositols", "PI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H10O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ALKYLCHAIN_MINUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O4P")}), //
  MONOACYLGLYCEROPHOSPHOINOSITOLS("Monoacylglycerophosphoinositols", "LPI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H10O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ALKYLCHAIN_MINUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O4P")}), //
  MONOALKYLGLYCEROPHOSPHOINOSITOLS("Monoalkylglycerophosphoinositols", "LPI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H10O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ALKYLCHAIN_MINUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O4P")}), //
  // DIACYLGLYCEROPHOSPHOINOSITOLMONOMANNOSIDE("Diacylglycerophosphoinositolmonomannoside",
  // "AC2PIM1",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C17H27O18P", 2, 0, new String[] {""},
  // new String[] {"FA", "M-FA", "M-C6H10O5", "M-FA-C12H20O10"}), //
  // DIACYLGLYCEROPHOSPHOINOSITOLDIMANNOSIDE("Diacylglycerophosphoinositoldimannoside",
  // "AC2PIM2",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C23H37O23P", 2, 0, new String[] {""}, new String[] {"FA", "M-FA",
  // "M-C6H10O5"}), //
  // TRIACYLPHOSPHATIDYLINOSITOLDIMANNOSIDE("Triacylglycerophosphoinositoldimannoside",
  // "AC3PIM2",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C24H36O24P", 3, 0, new String[] {""},
  // new String[] {"FA", "M-FA", "M-FA+H2O", "M-FA-FA", "M-FA-FA-C3H5O",
  // "M-C6H10O5"}), //
  // TETRAACYLPHOSPHATIDYLINOSITOLDIMANNOSIDE("Tetraacylglycerophosphoinositoldimannoside",
  // "AC3PIM2",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.GLYCEROPHOSPHOINOSITOLGLYCANS,
  // "C25H35O25P", 4, 0, new String[] {""},
  // new String[] {"FA", "M-FA", "M-FA-FA", "M-FA-FA-C3H5O"}), //
  DIACYLGLYCEROPHOSPHATES("Diacylglycerophosphates", "PA", LipidCategories.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.GLYCEROPHOSPHATES, "C3H9O6P",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2PO4-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "PO3-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      }), //
  DIALKYLGLYCEROPHOSPHATES("Dialkylglycerophosphates", "PA", LipidCategories.GLYCEROPHOSPHOLIPIDS,
      LipidMainClasses.GLYCEROPHOSPHATES, "C3H9O6P",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN)//
      }), //
  ALKYLACYLGLYCEROPHOSPHATES("Alkylacylglycerophosphates", "PA",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C3H9O6P",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN)//
      }), //
  MONOACYLGLYCEROPHOSPHATES("Monoacylglycerophosphates", "LPA",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C3H9O6P",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "PO3-") //
      }), //
  MONOALKYLGLYCEROPHOSPHATES("Monoalkylglycerophosphates", "LPA",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHATES, "C3H9O6P",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN)//
      }), //
  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
      "Diacylglycerophosphoglycerophosphodiradylglycerols", "CL",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C9H22O13P2",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN,
          LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.SPECIES_LEVEL, "C3H5"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.SPECIES_LEVEL, "C6H11P2O8"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.SPECIES_LEVEL, "C3H6PO4"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.SPECIES_LEVEL, "C6H10O5P"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.SPECIES_LEVEL, "C6H11P2O8"), //
      }), //
  // DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
  // "Diacylglycerophosphoglycerophosphodiradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C13H18O17P2", 4, 0,
  // new String[] {""}, new String[] {"M-FA", "FA+FA+C6H11P2O8", "FA+FA+C6H10O5P",
  // "FA+FA+C3H6PO4",
  // "FA+FA+C6H11P2O8", "FA+C3H6PO4+H2O", "FA+C3H6PO4", "FA"}), //
  // DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
  // "Diacylglycerophosphoglycerophosphomonoradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C12H19O16P2", 3, 0,
  // new String[] {""}, new String[] {""}), //
  // DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS(
  // "Dialkylglycerophosphoglycerophosphodiradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C9H18O13P2", 0, 4,
  // new String[] {""}, new String[] {""}), //
  // DIALKYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYLGLYCEROLS(
  // "Dialkylglycerophosphoglycerophosphomonoradylglycerols", "CL",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN,
  // "C9H19O13P2", 0, 3,
  // new String[] {""}, new String[] {""}), //
  // CDPDIACYLGLYCEROLS("CDP-diacylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C14H17O15P2N3", 2, 0, new String[] {""}, new
  // String[] {""}), //
  // CDPDIALKYLGLYCEROLS("CDP-Dialkylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C12H17O13P2N3", 0, 2, new String[] {""}, new
  // String[] {""}), //
  // CDPALKYLACYLGLYCEROLS("CDP-Alkylacylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C13H17O14P2N3", 1, 1, new String[] {""}, new
  // String[] {""}), //
  // CDPMONOACYLGLYCEROLS("CDP-Monoacylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C13H18O14P2N3", 1, 0, new String[] {""}, new
  // String[] {""}), //
  // CDPMONOALKYLGLYCEROLS("CDP-Monoalkylglycerols", "CDP-DG",
  // LipidCoreClasses.GLYCEROPHOSPHOLIPIDS,
  // LipidMainClasses.CDPGLYCEROLS, "C12H18O13P2N3", 0, 1, new String[] {""}, new
  // String[] {""}), //
  //
  // // Sphingolipids
  // NACYLSPHINGOSINES("N-acylsphingosines", "Cer",
  // LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES, "CHO3N", 1, 1, new String[] {""}, new String[]
  // {""}), //
  // NACYL4HYDROXYSPHINGANINES("N-acyl-4-hydroxysphinganines", "Cer",
  // LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES, "CHO4N", 1, 1,
  // new String[] {"M-OH", "M-OH-H2O", "M-FA-OH", "M-FA-OH-H2O"}, new String[]
  // {""}), //
  // ACYLCERAMIDES("Acylceramides", "Cer", LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES,
  // "C2O4N", 2, 1, new String[] {""}, new String[] {""}), //
  // CERAMIDE1PHOSPHATES("Ceramide 1-phosphates", "CerP",
  // LipidCoreClasses.SPHINGOLIPIDS,
  // LipidMainClasses.CERAMIDES, "CH2O6PN", 1, 1, new String[] {""},
  // new String[] {"M-H2O", "M-FA", "M-FA-H2O", "fragment H2PO4", "fragment
  // PO3"}); //

  ;

  private String name;
  private String abbr;
  private LipidCategories coreClass;
  private LipidMainClasses mainClass;
  private String backBoneFormula;
  LipidChainType[] chainTypes;
  LipidFragmentationRule[] fragmentationRules;

  LipidClasses(String name, String abbr, LipidCategories coreClass, LipidMainClasses mainClass,
      String backBoneFormula, LipidChainType[] chainTypes,
      LipidFragmentationRule[] fragmentationRules) {
    this.name = name;
    this.abbr = abbr;
    this.coreClass = coreClass;
    this.mainClass = mainClass;
    this.backBoneFormula = backBoneFormula;
    this.chainTypes = chainTypes;
    this.fragmentationRules = fragmentationRules;
  }

  public String getName() {
    return name;
  }

  public String getAbbr() {
    return abbr;
  }

  public LipidCategories getCoreClass() {
    return coreClass;
  }

  public LipidMainClasses getMainClass() {
    return mainClass;
  }

  public String getBackBoneFormula() {
    return backBoneFormula;
  }

  public LipidChainType[] getChainTypes() {
    return chainTypes;
  }

  public LipidFragmentationRule[] getFragmentationRules() {
    return fragmentationRules;
  }

  @Override
  public String toString() {
    return this.abbr + " " + this.name;
  }
}
