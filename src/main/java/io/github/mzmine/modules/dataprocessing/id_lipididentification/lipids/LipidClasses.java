/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidParsingUtils;

/**
 * This enum contains all lipid classes. Each enum has information on: name, abbreviation,
 * LipidCoreClass, LipidMainClass, lipid backbone sum formula, number of acyl chains, number of
 * alkychains and class specific fragmentation rules
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public enum LipidClasses implements ILipidClass {

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
  ALKYLACYLGLYCEROLS("Alkylacylglycerols", "DG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] {new LipidFragmentationRule(PolarityType.POSITIVE,
          IonizationType.AMMONIUM, LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, //
          LipidAnnotationLevel.SPECIES_LEVEL, "C3H5O")}), //
  DIALKYLGLYCEROLS("Dialkylglycerols", "DG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] {
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
      }), //
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
  DIALKYLGLYCEROPHOSPHOCHOLINES("Dialkylglycerophosphocholines", "PC",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.PHOSPHATIDYLCHOLINE, "C8H20O6PN",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
      }), //
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
  ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES("Alkylacylglycerophosphoethanolamines", "PE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H8NO4P"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.SPECIES_LEVEL, //
              "OH"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.SPECIES_LEVEL, "OH"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL)}), //
  DIALKYLGLYCEROPHOSPHOETHANOLAMINES("Dialkylglycerophosphoethanolamines", "PE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
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
  ALKYLACYLGLYCEROPHOSPHOGLYCEROLS("Alkylacylglycerophosphoglycerols", "PG",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOGLYCEROLS, "C6H15O8P",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
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
              LipidAnnotationLevel.SPECIES_LEVEL, "C3H5"), //


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
  DIALKYLGLYCEROPHOSPHOINOSITOLS("Dialkylglycerophosphoinositols", "PI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN)//
      }), //
  MONOACYLGLYCEROPHOSPHOINOSITOLS("Monoacylglycerophosphoinositols", "LPI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H10O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C9H16O10P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2PO4-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "PO3-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H7NO4P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H5NO3P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  MONOALKYLGLYCEROPHOSPHOINOSITOLS("Monoalkylglycerophosphoinositols", "LPI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[] {LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN)//
      }), //
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
      });

  private static final String XML_ELEMENT = "lipidclass";
  private static final String XML_LIPID_CLASS_NAME = "lipidclassname";
  private static final String XML_LIPID_CLASS_ABBR = "lipidclassabbr";
  private static final String XML_LIPID_CATEGORY = "lipidcategory";
  private static final String XML_LIPID_MAIN_CLASS = "lipidmainclass";
  private static final String XML_LIPID_CLASS_BACKBONE_FORMULA = "lipidclassbackboneformula";
  private static final String XML_LIPID_CLASS_CHAIN_TYPE = "lipidclasschaintypes";
  private static final String XML_LIPID_CLASS_FRAGMENTATION_RULES = "lipidclassfragmentationrules";

  private String name;
  private String abbr;
  private LipidCategories coreClass;
  private LipidMainClasses mainClass;
  private String backBoneFormula;
  private LipidChainType[] chainTypes;
  private LipidFragmentationRule[] fragmentationRules;

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

  public void saveToXML(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_ELEMENT, LipidClasses.class.getSimpleName());
    writer.writeStartElement(XML_LIPID_CLASS_NAME);
    writer.writeCharacters(name);
    writer.writeEndElement();
    writer.writeEndElement();
  }


  public static ILipidClass loadFromXML(XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException(
          "Cannot load lipid class from the current element. Wrong name.");
    }

    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(XML_LIPID_CLASS_NAME)) {
        return LipidParsingUtils.lipidClassNameToLipidClass(reader.getElementText());
      }

    }
    return null;
  }

}
