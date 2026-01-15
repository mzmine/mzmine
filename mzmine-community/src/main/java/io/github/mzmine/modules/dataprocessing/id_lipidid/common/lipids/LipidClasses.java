/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidParsingUtils;
import java.util.Arrays;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * This enum contains all lipid classes. Each enum has information on: name, abbreviation,
 * LipidCoreClass, LipidMainClass, lipid backbone sum formula, number of acyl chains, number of
 * alkychains and class specific fragmentation rules
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public enum LipidClasses implements ILipidClass {

  //Fatty acyls
  FREEFATTYACIDS("Free fatty acids", "FA", LipidCategories.FATTYACYLS,
      LipidMainClasses.FATTYACIDSANDCONJUGATES, "H2O",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN},//
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.PRECURSOR, LipidAnnotationLevel.SPECIES_LEVEL)//
      }), //
  OXIDIZEDFREEFATTYACIDS("Oxidized fatty acids", "FA", LipidCategories.FATTYACYLS,
      LipidMainClasses.FATTYACIDSANDCONJUGATES, "H2O",
      new LipidChainType[]{LipidChainType.ACYL_MONO_HYDROXY_CHAIN},//
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2O"),//
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2CO2")//
      }), //

  FATTYACIDESTOLIDES("Fatty acid estolides", "FAHFA", LipidCategories.FATTYACYLS,
      LipidMainClasses.FATTYESTERS, "H2O",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_MONO_HYDROXY_CHAIN},//
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL),//
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H2O"),//
      }), //

  FATTYACYLCARNITINES("Fatty acyl carnitines", "CAR", LipidCategories.FATTYACYLS,
      LipidMainClasses.FATTYESTERS, "C7H16NO3", new LipidChainType[]{LipidChainType.ACYL_CHAIN},//
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C4H5O2+")//
      }), //

  //TODO needs test
  NACYLGLYCINE("N-Acyl glycine", "NA", LipidCategories.FATTYACYLS, LipidMainClasses.FATTYAMIDES,
      "C2H6NO2", new LipidChainType[]{LipidChainType.AMID_CHAIN, LipidChainType.ACYL_CHAIN},//
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H6NO2+"),//
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "NH4"),//
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H9N2O2"),//
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H11N2O3"),//

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C2H4NO2-"),//
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H"),//
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CHO2"),//
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H2O2"),//
      }), //

  // Glycerolipids
  MONOACYLGLYCEROLS("Monoacylglycerols", "MG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "NH3"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H5ON"), //
  }), //

  MONOALKYLGLYCEROLS("Monoalkylglycerols", "MG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.MONORADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[]{LipidChainType.ALKYL_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
  }), //
  DIACYLGLYCEROLS("Diacylglycerols", "DG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{ //
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
      new LipidChainType[]{LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O")}), //
  DIALKYLGLYCEROLS("Dialkylglycerols", "DG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.DIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[]{LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[]{
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
      }), //
  TRIACYLGLYCEROLS("Triacylglycerols", "TG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN,
          LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[] { //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "NH3"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H5NO"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL, //
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "NH3")}), //
  ALKYLDIACYLGLYCEROLS("Alkyldiacylglycerols", "TG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.TRIRADYLGLYCEROLS, "C3H8O3",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ALKYL_CHAIN,
          LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[]{//
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM), //
  }), //
  DIACYLGLYCEROLTRIMETHYLHOMOSERIN("Diacylglyceroltrimethylhomoserin", "DGTS",
      LipidCategories.GLYCEROLIPIDS, LipidMainClasses.OTHERGLYCEROLIPIDS, "C10H21O5N",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{ //
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
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      }), //

  //TODO add test
  MONOGALACTOSYLMONOACYLGLYCEROL("Monogalactosylmonoacylglycerol", "MGMG",
      LipidCategories.GLYCEROLIPIDS, LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C9H18O8",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H15NO6"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
          LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "OH"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
  }), //

  DIGALACTOSYLDIACYLGLYCEROL("Digalactosyldiacylglycerol", "DGDG", LipidCategories.GLYCEROLIPIDS,
      LipidMainClasses.GLYCOSYLDIACYLGLYCEROLS, "C15H28O13",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C12H21O11"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C12H23O12"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //

          //TODO add test
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C15H25O12-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C15H23O11-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
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
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, //
              "OH"), //

          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "OH"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)}), //
  DIALKYLGLYCEROPHOSPHOETHANOLAMINES("Dialkylglycerophosphoethanolamines", "PE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[]{LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //
  MONOACYLGLYCEROPHOSPHOETHANOLAMINES("Monoacylglycerophosphoethanolamines", "LPE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H14O6PN", new LipidChainType[]{LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[]{ //
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
      "C5H14O6PN", new LipidChainType[]{LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN), //
      }), //

  NACYLLYSOPHOSPHATIDYLETHANOLAMINE("N-acyl-lysophosphatidylethanolamine", "LNAPE",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOETHANOLAMINES,
      "C5H13O6P", new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.AMID_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL),//
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H2O"),//
      }),//

  DIACYLGLYCEROPHOSPHOSERINES("Diacylglycerophosphoserines", "PS",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOSERINES, "C6H14O8NP",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{ //
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
              "C3H8NO6P"), //
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
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H12O6NP"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H5O"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H3"), //

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
              "C9H16O10P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C9H14O9P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H12O9P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H10O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H8O7P-"), //
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
              "C6H12O5"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL),
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H2O"),
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C6H12O5")}), //

  ALKYLACYLGLYCEROPHOSPHOINOSITOLS("Alkylacylglycerophosphoinositols", "PI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[]{LipidChainType.ALKYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C6H10O8P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ALKYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H4O5P")}), //
  DIALKYLGLYCEROPHOSPHOINOSITOLS("Dialkylglycerophosphoinositols", "PI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[]{LipidChainType.ALKYL_CHAIN, LipidChainType.ALKYL_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN)//
      }), //
  MONOACYLGLYCEROPHOSPHOINOSITOLS("Monoacylglycerophosphoinositols", "LPI",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.GLYCEROPHOSPHOINOSITOLS, "C9H19PO11",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[]{ //
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
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN,
          LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
          LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H3O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H6O5P-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
//          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
//              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
//              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H4PO6"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H6PO5"), //
//      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
//          LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
//          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C6H10O5P"), //
//      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
//          LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
//          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C6H11P2O8"), //
  }),

  DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYGLYCEROLS(
      "Diacylglycerophosphoglycerophosphomonoradylglycerols", "MLCL",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C9H22O13P2",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN,
          LipidChainType.ACYL_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H6O5P-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H6PO5"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H4PO6"), //
  }),

  MONOACYLGLYCEROPHOSPHOGLYCEROPHOSPHOMONORADYGLYCEROLS(
      "Monoacylglycerophosphoglycerophosphomonoradylglycerols", "DLCL",
      LipidCategories.GLYCEROPHOSPHOLIPIDS, LipidMainClasses.CARDIOLIPIN, "C9H22O13P2",
      new LipidChainType[]{LipidChainType.ACYL_CHAIN, LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C3H6O5P-"), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
          new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
              LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
              LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H6PO5"), //
      }),

  // Sphingolipids
  SPHINGANINESANDSPHINGOSINES("Sphingosines and Sphinganines", "SPB", LipidCategories.SPHINGOLIPIDS,
      LipidMainClasses.SPHINGOIDBASES, "C3H9N",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H2O", LipidFragmentationRuleRating.MINOR), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H4O2", LipidFragmentationRuleRating.MINOR), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "CH4O2", LipidFragmentationRuleRating.MINOR), //
      }),//
  PHYTOSPHINGANINESANDPHYTOSPHINGOSINES("Phytosphingosines and Phytosphinganines", "SPB",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.SPHINGOIDBASES, "C3H9N",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H4O2", LipidFragmentationRuleRating.MINOR), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "H6O3", LipidFragmentationRuleRating.MINOR), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "CH4O2", LipidFragmentationRuleRating.MINOR), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
              "CH6O3", LipidFragmentationRuleRating.MINOR), //
      }),//

  CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID("Ceramides and dihydroceramides hydroxy fatty acid",
      "Cer", LipidCategories.SPHINGOLIPIDS, LipidMainClasses.CERAMIDES, "C3H8",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_MONO_HYDROXY_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H4O2", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C4O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H4O2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H6O3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H8O4"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H8NO"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2CO2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "CH4O3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H6O3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H6O4"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H8NO"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H6NO"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H"), //
  }),

  //TODO at ACer

  //TODO Needs Test
  CERAMIDEPHOSPHATES("Ceramide-1-phosphates", "CerP", LipidCategories.SPHINGOLIPIDS,
      LipidMainClasses.CERAMIDES, "C3H9O3P",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H3PO4"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H5PO5"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "HPO3"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2PO4-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "PO3-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.AMID_CHAIN_MINUS_FORMULA_FRAGMENT_NL,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "NH3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.NEGATIVE_HYDROGEN,
          LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT_NL,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H"), //
  }),

  //TODO find formate rules
  NACYLFOURHYDROXYPHINGANINES("N-acyl-4-hydroxysphinganines (phytoceramides)", "Cer",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.CERAMIDES, "C3H8",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_CHAIN}, new LipidFragmentationRule[]{ //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H6O3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H8O4"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "CH4COO"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H3O"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CHO"), //

      //TODO double check
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_CHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CH8NO"), //
  }),

  NACYLSPHINGOSINESANDNACYLSPHINGANINES(
      "N-acylsphingosines (ceramides) and N-acylsphinganines (dihydroceramides)", "Cer",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.CERAMIDES, "C3H8",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H4O2", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C4O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H4O2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H6O3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H8O4"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H8NO"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2CO2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H6O3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H6O4"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H8NO"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H"), //
  }),

  PHYTOCERAMIDEHYDROXYFATTYACID("Phytoceramides hydroxy fatty acid", "Cer",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.CERAMIDES, "C3H8",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_MONO_HYDROXY_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H4O2", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H6O3", LipidFragmentationRuleRating.MINOR), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H5O3"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H4O2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H3O"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H10O2N"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CH4NO"), //

      //TODO need to substract 1 H here... rule type is cUrrently missing
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2CO2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H3O"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H10O2N"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CH4NO"), //

  }),

  DIHEXOSYLCERAMIDES("Dihexosylceramides", "Hex2Cer", LipidCategories.SPHINGOLIPIDS,
      LipidMainClasses.NEUTRALGLYCOSPHINGOLIPIDS, "C15H28O10",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H10O5"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H12O6"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C12H22O11"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CH3O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H4O2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C8H14O7"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C14H24O12"), //

//TODO need formate rules
  }),

  TRIHEXOSYLCERAMIDES("Trihexosylceramides", "Hex3Cer", LipidCategories.SPHINGOLIPIDS,
      LipidMainClasses.NEUTRALGLYCOSPHINGOLIPIDS, "C21H38O15",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H10O5"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H12O6"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C12H22O11"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C18H32O16"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CH3O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H4O2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C8H14O7"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C14H24O12"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C20H34O17"), //
  }),//
  HEXOSYLCERAMIDES("Hexosylceramides", "HexCer", LipidCategories.SPHINGOLIPIDS,
      LipidMainClasses.NEUTRALGLYCOSPHINGOLIPIDS, "C9H18O5",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H10O5"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H12O6"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CH3O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C8H14O7"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "NH4"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H8NO"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C7H12O7"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C7H14O8"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.AMID_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "NH4"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.AMID_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C2H8NO"), //
  }),

  HEXOSYLCERAMIDEHYDROXYFATTYACID("Hexosylceramide hydroxy fatty acid", "HexCer",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.NEUTRALGLYCOSPHINGOLIPIDS, "C9H18O5",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_MONO_HYDROXY_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2O"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H12O6"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H14O7"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "CH3O2"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H4O2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C8H14O7"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.SPECIES_LEVEL, "H3"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "H2CO2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H11O6"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C7H12O7"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C8H16O8"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.SPECIES_LEVEL, "H3"), //
  }),

  HEXOSYLCERAMIDEHYDROXYFATTYACIDPHYTOSPHINGOSINE(
      "Hexosylceramide hydroxy fatty acid phytosphingosine", "HexCer",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.NEUTRALGLYCOSPHINGOLIPIDS, "C9H18O5",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_MONO_HYDROXY_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H10O5"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H12O6"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C6H14O7"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "HO"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H3O2"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H5O3"), //

      //TODO more testing required
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C2H4O2"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C8H14O7"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H10O2N"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "C3H3O"), //

  }),

  //TODO write tests
  CERAMIDEPHOSPHOCHOLINES("Ceramide Phosphocholines (Sphingomyelins)", "SM",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.PHOSPHOSPHINGOLIPIDS, "C8H20O3PN",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C5H15O4PN+"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C5H14NO4P"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C5H16NO5P"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H9N"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
          LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, "H4O2"), //

      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C5H14NO4P"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C5H15NO5PNa"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H9N"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C4H11O4PN-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "PO3-"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C4H11O4PN-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "PO3-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H6O2"), //
  }),

  //TODO write MS/MS test
  OXIDIZEDCERAMIDEPHOSPHOCHOLINES("Oxidized Ceramide Phosphocholines (Sphingomyelins)", "SM",
      LipidCategories.SPHINGOLIPIDS, LipidMainClasses.PHOSPHOSPHINGOLIPIDS, "C8H20O3PN",
      new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
          LipidChainType.AMID_MONO_HYDROXY_CHAIN}, new LipidFragmentationRule[]{ //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C5H15O4PN+"), //
      new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.POSITIVE_HYDROGEN,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H9N"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C4H11O4PN-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "CH3"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.FORMATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL, "PO3-"), //

      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
          "C4H11O4PN-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL, "PO3-"), //
      new LipidFragmentationRule(PolarityType.NEGATIVE, IonizationType.ACETATE,
          LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL, LipidAnnotationLevel.SPECIES_LEVEL,
          "C3H6O2"), //
  }), //

  // Sterols
  CHOLESTEROLESTERS("Cholesterol esters", "CE", LipidCategories.STEROLLIPIDS,
      LipidMainClasses.STEROLS, "C27H46O", new LipidChainType[]{LipidChainType.ACYL_CHAIN},
      new LipidFragmentationRule[]{ //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.AMMONIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C27H45+"), //
          new LipidFragmentationRule(PolarityType.POSITIVE, IonizationType.SODIUM,
              LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
              "C27H45+") //

      });


  private static final String XML_ELEMENT = "lipidclass";
  private static final String XML_LIPID_CLASS_NAME = "lipidclassname";

  private final String name;
  private final String abbr;
  private final LipidCategories coreClass;
  private final LipidMainClasses mainClass;
  private final String backBoneFormula;
  private final LipidChainType[] chainTypes;
  private final LipidFragmentationRule[] fragmentationRules;

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
    return Arrays.copyOf(chainTypes, chainTypes.length);
  }

  public LipidFragmentationRule[] getFragmentationRules() {
    return Arrays.copyOf(fragmentationRules, fragmentationRules.length);
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
