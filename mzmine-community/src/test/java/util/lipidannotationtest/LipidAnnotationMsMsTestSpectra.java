/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package util.lipidannotationtest;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;

public class LipidAnnotationMsMsTestSpectra {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  //Fatty acyls
  private final LipidAnnotationMsMsTestResource FA_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{281.249}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.FREEFATTYACIDS, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getFA_18_1MMinusH() {
    return FA_18_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource FA_18_1_OMMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{281.249, 253.254}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.OXIDIZEDFREEFATTYACIDS, 18, 0, 0));

  public LipidAnnotationMsMsTestResource getFA_18_1_OMMinusH() {
    return FA_18_1_OMMinusH;
  }

  private final LipidAnnotationMsMsTestResource FA_12_0_OMMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{215.1643, 169.1585}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.OXIDIZEDFREEFATTYACIDS, 12, 0, 0));

  public LipidAnnotationMsMsTestResource getFA_12_0_OMMinusH() {
    return FA_12_0_OMMinusH;
  }

  private final LipidAnnotationMsMsTestResource FAHFA_16_0_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{535.473, 297.244, 255.233}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.FATTYACIDESTOLIDES,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getFAHFA_16_0_18_1MMinusH() {
    return FAHFA_16_0_18_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource CAR_18_1MPlus = new LipidAnnotationMsMsTestResource(
      new double[]{426.358, 85.028}, //
      IonizationType.POSITIVE, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.FATTYACYLCARNITINES, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getCAR_18_1MPlus() {
    return CAR_18_1MPlus;
  }

  // Glycerolipids////////////////////////////////////////////////////////////////////
  private final LipidAnnotationMsMsTestResource MG_18_OMPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{376.342, 359.318, 341.302}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROLS, 18, 0, 0));

  public LipidAnnotationMsMsTestResource getMG_18_OMPlusNH4() {
    return MG_18_OMPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource DG_18_O_20_4MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{627.522, 361.280, 341.302}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLS, new int[]{18, 20},
          new int[]{0, 4}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getDG_18_O_20_4MPlusNH4() {
    return DG_18_O_20_4MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource DG_O_34_1MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{563.540, 339.291}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROLS,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getDG_O_34_1MPlusNH4() {
    return DG_O_34_1MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource TG_16_O_18_2_22_6MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{920.770, 903.744, 647.504, 623.504, 575.504}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.TRIACYLGLYCEROLS,
          new int[]{16, 18, 22}, new int[]{0, 2, 6}, new int[]{0, 0, 0}));

  public LipidAnnotationMsMsTestResource getTG_16_O_18_2_22_6MPlusNH4() {
    return TG_16_O_18_2_22_6MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource TG_16_O_18_2_22_6MPlusNa = new LipidAnnotationMsMsTestResource(
      new double[]{669.485, 645.477, 597.485}, //
      IonizationType.SODIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.TRIACYLGLYCEROLS,
          new int[]{16, 18, 22}, new int[]{0, 2, 6}, new int[]{0, 0, 0}));

  public LipidAnnotationMsMsTestResource getTG_16_O_18_2_22_6MPlusNa() {
    return TG_16_O_18_2_22_6MPlusNa;
  }

  private final LipidAnnotationMsMsTestResource DGTS_16_0_18_1MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{738.624, 500.395, 482.384, 474.370, 456.368, 236.149, 144.102}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLTRIMETHYLHOMOSERIN,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getDGTS_16_0_18_1MPlusH() {
    return DGTS_16_0_18_1MPlusH;
  }

  private final LipidAnnotationMsMsTestResource LDGTS_18_1MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{236.149, 144.102}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLTRIMETHYLHOMOSERIN, 18, 1,
          0));

  public LipidAnnotationMsMsTestResource getLDGTS_18_1MPlusH() {
    return LDGTS_18_1MPlusH;
  }

  private final LipidAnnotationMsMsTestResource MGDG_16_O_18_1MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{577.513, 339.289, 313.276}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.MONOGALACTOSYLDIACYLGLYCEROL,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getMGDG_16_O_18_1MPlusNH4() {
    return MGDG_16_O_18_1MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource MGDG_16_O_18_1MPlusAcetate = new LipidAnnotationMsMsTestResource(
      new double[]{815.589, 755.568, 517.332, 491.323, 281.248, 255.232}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.MONOGALACTOSYLDIACYLGLYCEROL,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getMGDG_16_O_18_1MPlusAcetate() {
    return MGDG_16_O_18_1MPlusAcetate;
  }

  private final LipidAnnotationMsMsTestResource DGDG_16_O_18_1MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{936.618, 595.553, 577.519, 339.289, 313.274}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIGALACTOSYLDIACYLGLYCEROL,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getDGDG_16_O_18_1MPlusNH4() {
    return DGDG_16_O_18_1MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource SQDG_16_O_18_1MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{838.571, 595.530, 577.519, 339.289, 313.274, 239.237}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.SULFOQUINOVOSYLDIACYLGLYCEROLS,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getSQDG_16_O_18_1MPlusNH4() {
    return SQDG_16_O_18_1MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource SQDG_16_O_16_0MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{793.514, 537.274, 255.232, 225.007}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.SULFOQUINOVOSYLDIACYLGLYCEROLS,
          new int[]{16, 16}, new int[]{0, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getSQDG_16_O_16_0MMinusH() {
    return SQDG_16_O_16_0MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PC_18_0_20_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{810.601, 544.340, 526.330, 524.372, 506.361, 184.073}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES,
          new int[]{18, 20}, new int[]{0, 4}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getPC_18_0_20_4MPlusH() {
    return PC_18_0_20_4MPlusH;
  }

  private final LipidAnnotationMsMsTestResource PC_O_38_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{796.622, 613.555, 184.074}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOCHOLINES, 38, 4, 0));

  public LipidAnnotationMsMsTestResource getPC_O_38_4MPlusH() {
    return PC_O_38_4MPlusH;
  }

  private final LipidAnnotationMsMsTestResource LPC_18_1MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{522.355, 184.074, 104.107}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOCHOLINES, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getLPC_18_1MPlusH() {
    return LPC_18_1MPlusH;
  }

  private final LipidAnnotationMsMsTestResource PE_38_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{768.554, 627.535, 287.237, 267.269}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 38, 4,
          0));

  public LipidAnnotationMsMsTestResource getPE_38_4MPlusH() {
    return PE_38_4MPlusH;
  }

  private final LipidAnnotationMsMsTestResource PE_38_4MPlusNa = new LipidAnnotationMsMsTestResource(
      new double[]{790.536, 747.494, 649.517, 627.535}, //
      IonizationType.SODIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 38, 4,
          0));

  public LipidAnnotationMsMsTestResource getPE_38_4MPlusNa() {
    return PE_38_4MPlusNa;
  }

  private final LipidAnnotationMsMsTestResource PE_18_0_20_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{766.539, 303.232, 283.264, 196.038}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 38, 4,
          0));

  public LipidAnnotationMsMsTestResource getPE_18_0_20_4MMinusH() {
    return PE_18_0_20_4MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PE_O_34_1MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{704.549, 563.540, 265.253}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES, 34, 1,
          0));

  public LipidAnnotationMsMsTestResource getPE_O_34_1MPlusH() {
    return PE_O_34_1MPlusH;
  }

  private final LipidAnnotationMsMsTestResource PE_O_34_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{702.544, 438.299, 281.248}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES, new int[]{16, 18}, new int[]{0, 1},
          new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getPE_O_34_1MMinusH() {
    return PE_O_34_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource LPE_18_1MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{480.308485, 339.289375, 216.06318, 265.252605, 142.02639}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOETHANOLAMINES, 18, 1,
          0));

  public LipidAnnotationMsMsTestResource getLPE_18_1MPlusH() {
    return LPE_18_1MPlusH;
  }

  private final LipidAnnotationMsMsTestResource LPE_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{478.294, 281.248, 196.038, 78.955}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOETHANOLAMINES, 18, 1,
          0));

  public LipidAnnotationMsMsTestResource getLPE_18_1MMinusH() {
    return LPE_18_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource LNAPE_16_0_18_2MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{714.508, 476.278, 458.267, 255.232, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.NACYLLYSOPHOSPHATIDYLETHANOLAMINE, 34, 2,
          0));

  public LipidAnnotationMsMsTestResource getLNAPE_16_0_18_2MMinusH() {
    return LNAPE_16_0_18_2MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PS_18_0_20_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{812.544, 627.535, 546.283, 528.272, 526.315, 508.304}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES,
          new int[]{18, 20}, new int[]{0, 4}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getPS_18_0_20_4MPlusH() {
    return PS_18_0_20_4MPlusH;
  }

  private final LipidAnnotationMsMsTestResource PS_18_0_20_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{810.530, 723.497, 419.257, 303.232, 283.264, 152.994, 78.964}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES,
          new int[]{18, 20}, new int[]{0, 4}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getPS_18_0_20_4MMinusH() {
    return PS_18_0_20_4MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PS_38_4MPlusNa = new LipidAnnotationMsMsTestResource(
      new double[]{834.526, 747.494, 649.517, 207.998}, //
      IonizationType.SODIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES, 38, 4, 0));

  public LipidAnnotationMsMsTestResource getPS_38_4MPlusNa() {
    return PS_38_4MPlusNa;
  }

  private final LipidAnnotationMsMsTestResource PS_O_38_6MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{792.519, 705.487, 395.257, 377.246, 327.232, 78.956}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOSERINES, 38, 6, 0));

  public LipidAnnotationMsMsTestResource getPS_O_38_6MMinusH() {
    return PS_O_38_6MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PG_38_4MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{816.575, 627.536, 361.274, 641.305}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS, 38, 4, 0));

  public LipidAnnotationMsMsTestResource getPG_38_4MPlusNH4() {
    return PG_38_4MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource PG_18_0_20_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{797.534, 303.232, 283.264, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS,
          new int[]{18, 20}, new int[]{0, 4}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getPG_18_0_20_4MMinusH() {
    return PG_18_0_20_4MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PG_O_34_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{733.539, 283.264, 255.232, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOGLYCEROLS, 38, 4,
          0));

  public LipidAnnotationMsMsTestResource getPG_O_34_1MMinusH() {
    return PG_O_34_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource LPG_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{509.289, 281.249, 245.040, 227.033, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOGLYCEROLS, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getLPG_18_1MMinusH() {
    return LPG_18_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource LPG_O_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{495.309, 403.264, 331.260, 267.269, 152.996, 78.959}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOGLYCEROLS, 18, 1,
          0));

  public LipidAnnotationMsMsTestResource getLPG_O_18_1MMinusH() {
    return LPG_O_18_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource BMP_18_1_22_4MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{842.591, 653.552, 389.305, 339.289}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS, new int[]{18, 22}, new int[]{1, 4},
          new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getBMP_18_1_22_4MPlusNH4() {
    return BMP_18_1_22_4MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource BMP_40_5MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{842.591, 653.552, 389.305, 339.289}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS,
          40, 5, 0));

  public LipidAnnotationMsMsTestResource getBMP_40_5MPlusNH4() {
    return BMP_40_5MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource PI_18_0_20_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{885.550, 581.309, 303.232, 283.264, 241.012}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS,
          new int[]{18, 20}, new int[]{0, 4}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getPI_18_0_20_4MMinusH() {
    return PI_18_0_20_4MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PI_38_4MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{627.535}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 38, 4, 0));

  public LipidAnnotationMsMsTestResource getPI_38_4MPlusNH4() {
    return PI_38_4MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource PI_38_4MPlusNa = new LipidAnnotationMsMsTestResource(
      new double[]{909.546, 627.535, 283.019}, //
      IonizationType.SODIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 38, 4, 0));

  public LipidAnnotationMsMsTestResource getPI_38_4MPlusNa() {
    return PI_38_4MPlusNa;
  }

  private final LipidAnnotationMsMsTestResource PI_36_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{843.539, 539.299, 377.246, 303.232, 241.012}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS, 36, 5,
          0));

  public LipidAnnotationMsMsTestResource getPI_36_4MMinusH() {
    return PI_36_4MMinusH;
  }

  private final LipidAnnotationMsMsTestResource LPI_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{597.307, 417.229, 315.049, 281.249, 241.012, 152.99, 78.962}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOINOSITOLS, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getLPI_18_1MMinusH() {
    return LPI_18_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource PA_16_0_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{673.48146, 435.25179, 409.23609, 417.24123, 391.22553, 224.06881, 255.23295,
          281.24865, 171.00644, 152.99586, 96.96964, 78.95852}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHATES,
          new int[]{16, 18}, new int[]{0, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getPA_16_0_18_1MMinusH() {
    return PA_16_0_18_1MMinusH;
  }

  private final LipidAnnotationMsMsTestResource LPA_16_0MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{409.236, 152.996, 78.959}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHATES, 16, 0, 0));

  public LipidAnnotationMsMsTestResource getLPA_16_0MMinusH() {
    return LPA_16_0MMinusH;
  }

  private final LipidAnnotationMsMsTestResource CL_70_4_MPlusNH4 = new LipidAnnotationMsMsTestResource(
      new double[]{1430.011, 601.519, 577.519}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS,
          new int[]{16, 18, 18, 18}, new int[]{0, 1, 1, 2}, new int[]{0, 0, 0, 0}));

  public LipidAnnotationMsMsTestResource getCL_70_4_MPlusNH4() {
    return CL_70_4_MPlusNH4;
  }

  private final LipidAnnotationMsMsTestResource CL_70_5_MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{1425.964, 695.466, 673.481, 415.226, 281.249, 279.233, 255.233, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(
          LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 70, 5, 0));

  public LipidAnnotationMsMsTestResource getCL_70_5_MMinusH() {
    return CL_70_5_MMinusH;
  }

  private final LipidAnnotationMsMsTestResource CL_16_0_18_1_18_2_18_2MMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{1425.964, 695.466, 673.481, 415.226, 281.249, 279.233, 255.233, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS,
          new int[]{16, 18, 18, 18}, new int[]{0, 1, 2, 2}, new int[]{0, 0, 0, 0}));

  public LipidAnnotationMsMsTestResource getCL_16_0_18_1_18_2_18_2MMinusH() {
    return CL_16_0_18_1_18_2_18_2MMinusH;
  }

  //Sphingolipids
  private final LipidAnnotationMsMsTestResource SPB_18_1_2O = new LipidAnnotationMsMsTestResource(
      new double[]{282.276, 264.261, 252.272}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.SPHINGANINESANDSPHINGOSINES, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getSPB_18_1_2O() {
    return SPB_18_1_2O;
  }

  private final LipidAnnotationMsMsTestResource SPB_18_1_3O = new LipidAnnotationMsMsTestResource(
      new double[]{282.276, 270.279, 264.257, 252.272}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.SPHINGANINESANDSPHINGOSINES, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getSPB_18_1_3O() {
    return SPB_18_1_3O;
  }

  private final LipidAnnotationMsMsTestResource Cer_18_0_O2_26_0_OMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{262.679, 644.670, 495.255, 396.420, 284.295, 266.284, 254.284}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID, new int[]{18, 26},
          new int[]{0, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_0_O2_26_0_OMPlusH() {
    return Cer_18_0_O2_26_0_OMPlusH;
  }

  private final LipidAnnotationMsMsTestResource Cer_18_1_O2_14_0MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{268.223999023438, 492.411041259766, 306.773071289063, 338.032073974609,
          288.714050292969, 224.207061767578, 265.848052978516, 282.202056884766, 222.082061767578,
          298.118072509766, 250.285003662109, 228.257064819336, 254.153030395508, 474.365051269531,
          244.0810546875, 236.216064453125, 280.253051757813, 264.229034423828, 252.18505859375},
      new double[]{151.947616577148, 158938.78125, 282.570495605469, 116.55738067627,
          117.695709228516, 8923.9736328125, 316.561401367188, 9529.279296875, 202.457977294922,
          290.960174560547, 366.401885986328, 1369.63806152344, 13241.578125, 18731.63671875,
          141.203704833984, 103851.9375, 263.320343017578, 79178.5234375, 6597.490234375},
      //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.NACYLSPHINGOSINESANDNACYLSPHINGANINES, new int[]{18, 14}, new int[]{0, 0},
          new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_1_O2_14_0MPlusH() {
    return Cer_18_1_O2_14_0MPlusH;
  }


  private final LipidAnnotationMsMsTestResource Cer_18_1_O2_24_0MPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{632.640, 614.623, 368.389, 282.279, 264.269, 252.269},
      //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.NACYLSPHINGOSINESANDNACYLSPHINGANINES, new int[]{18, 24}, new int[]{1, 0},
          new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_1_O2_24_0MPlusH() {
    return Cer_18_1_O2_24_0MPlusH;
  }

  private final LipidAnnotationMsMsTestResource Cer_18_1_O2_16_0_O_Acetate = new LipidAnnotationMsMsTestResource(
      new double[]{596.526, 518.494, 536.505, 506.495, 280.264, 237.223}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.NACYLSPHINGOSINESANDNACYLSPHINGANINES, new int[]{18, 16}, new int[]{1, 0},
          new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_1_O2_16_0_O_Acetate() {
    return Cer_18_1_O2_16_0_O_Acetate;
  }

  private final LipidAnnotationMsMsTestResource Cer_18_0_2O_16_0MPlusCH3COO = new LipidAnnotationMsMsTestResource(
      new double[]{598.542, 538.520, 506.494, 490.499, 280.264, 239.238, 237.249}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.NACYLSPHINGOSINESANDNACYLSPHINGANINES, new int[]{18, 16}, new int[]{0, 0},
          new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_0_2O_16_0MPlusCH3COO() {
    return Cer_18_0_2O_16_0MPlusCH3COO;
  }

  private final LipidAnnotationMsMsTestResource Cer_14_0_2O_20_0MPlusHCOO = new LipidAnnotationMsMsTestResource(
      new double[]{584.5254, 538.52, 506.4937, 490.4988, 244.2282, 352.3216, 336.3267, 310.3116,
          183.1754, 293.3088}, //
      IonizationType.FORMATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.NACYLSPHINGOSINESANDNACYLSPHINGANINES, new int[]{14, 20}, new int[]{0, 0},
          new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_14_0_2O_20_0MPlusHCOO() {
    return Cer_14_0_2O_20_0MPlusHCOO;
  }

  private final LipidAnnotationMsMsTestResource Cer_14_1_2O_18_0_OMPlusHCOO = new LipidAnnotationMsMsTestResource(
      new double[]{570.4734, 524.4679, 506.4573, 492.4417, 476.4467, 240.1969, 224.202, 299.2592,
          298.2751, 212.202, 281.2486, 183.1754, 181.1598, 253.2537}, //
      IonizationType.FORMATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID, new int[]{14, 18},
          new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_14_1_2O_18_0_OMPlusHCOO() {
    return Cer_14_1_2O_18_0_OMPlusHCOO;
  }


  private final LipidAnnotationMsMsTestResource Cer_18_1_2O_22_0_OMPlusCH3COO = new LipidAnnotationMsMsTestResource(
      new double[]{696.615, 636.594, 606.583, 588.573, 380.353, 337, 311, 309.312, 267.235,
          237.222}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID, new int[]{18, 22},
          new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_1_2O_22_0_OMPlusCH3COO() {
    return Cer_18_1_2O_22_0_OMPlusCH3COO;
  }

  private final LipidAnnotationMsMsTestResource Cer_20_1_2O_24_0_OMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{694.671, 676.661, 628.612, 366.375, 310.311, 292.300, 280.300, 81.070}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID, new int[]{20, 24},
          new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_20_1_2O_24_0_OMPlusH() {
    return Cer_20_1_2O_24_0_OMPlusH;
  }

  private final LipidAnnotationMsMsTestResource Cer_18_0_3O_16_0MPlusCH3COO = new LipidAnnotationMsMsTestResource(
      new double[]{614.537, 554.515, 518.494, 310.274, 298.274, 267.232, 255.233, 254.248}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.NACYLFOURHYDROXYPHINGANINES,
          new int[]{18, 16}, new int[]{0, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_0_3O_16_0MPlusCH3COO() {
    return Cer_18_0_3O_16_0MPlusCH3COO;
  }

  private final LipidAnnotationMsMsTestResource Cer_18_1_3O_24_0_OMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{682.634, 664.624, 646.613, 316.285, 298.274, 280.264, 262.253}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.PHYTOCERAMIDEHYDROXYFATTYACID,
          new int[]{18, 24}, new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_1_3O_24_0_OMPlusH() {
    return Cer_18_1_3O_24_0_OMPlusH;
  }

  private final LipidAnnotationMsMsTestResource Cer_14_0_3O_16_0_O_HCOO = new LipidAnnotationMsMsTestResource(
      new double[]{560.4526, 514.4471, 326.269, 271.2279, 225.2224, 169.1587}, //
      IonizationType.FORMATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.PHYTOCERAMIDEHYDROXYFATTYACID,
          new int[]{14, 16}, new int[]{0, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_14_0_3O_16_0_O_HCOO() {
    return Cer_14_0_3O_16_0_O_HCOO;
  }

  private final LipidAnnotationMsMsTestResource Cer_18_0_3O_20_0_OMPlusCH3COO = new LipidAnnotationMsMsTestResource(
      new double[]{686.594, 626.573, 382.332, 327.291, 281.285, 225.221}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.PHYTOCERAMIDEHYDROXYFATTYACID,
          new int[]{18, 20}, new int[]{0, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCer_18_0_3O_20_0_OMPlusCH3COO() {
    return Cer_18_0_3O_20_0_OMPlusCH3COO;
  }

  private final LipidAnnotationMsMsTestResource CerP_18_1_2O_12_0_OMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{562.424, 544.411, 482.457, 464.447, 446.436, 264.269}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.CERAMIDEPHOSPHATES,
          new int[]{18, 12}, new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getCerP_18_1_2O_12_0_OMPlusH() {
    return CerP_18_1_2O_12_0_OMPlusH;
  }

  private final LipidAnnotationMsMsTestResource CerP_18_1_2O_12_0_OMMinusH = new LipidAnnotationMsMsTestResource(
      new double[]{560.408, 542.398, 378.241, 360.231, 96.969, 78.959}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.CERAMIDEPHOSPHATES, 30, 1, 0));

  public LipidAnnotationMsMsTestResource getCerP_18_1_2O_12_0_OMMinusH() {
    return CerP_18_1_2O_12_0_OMMinusH;
  }

  private final LipidAnnotationMsMsTestResource HexCer_18_1_2O_24_0MMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{812.698, 794.687, 632.635, 614.624, 368.389, 282.279, 264.269, 252.269}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.HEXOSYLCERAMIDES,
          new int[]{18, 24}, new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHexCer_18_1_2O_24_0MMPlusH() {
    return HexCer_18_1_2O_24_0MMPlusH;
  }

  private final LipidAnnotationMsMsTestResource Hex2Cer_18_1_2O_16_0MMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{844.614, 700.572, 682.562, 520.509, 282.279, 264.269, 252.269}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIHEXOSYLCERAMIDES,
          new int[]{18, 16}, new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHex2Cer_18_1_2O_16_0MMPlusH() {
    return Hex2Cer_18_1_2O_16_0MMPlusH;
  }

  private final LipidAnnotationMsMsTestResource Hex2Cer_18_1_2O_16_0Acetate = new LipidAnnotationMsMsTestResource(
      new double[]{860.611, 698.558, 536.505}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIHEXOSYLCERAMIDES, 34, 1, 0));

  public LipidAnnotationMsMsTestResource getHex2Cer_18_1_2O_16_0Acetate() {
    return Hex2Cer_18_1_2O_16_0Acetate;
  }

  private final LipidAnnotationMsMsTestResource Hex3Cer_18_1_2O_16_0MMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{1006.665, 844.614, 700.572, 682.562, 520.509, 282.279, 264.269, 252.269}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.TRIHEXOSYLCERAMIDES,
          new int[]{18, 16}, new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHex3Cer_18_1_2O_16_0MMPlusH() {
    return Hex3Cer_18_1_2O_16_0MMPlusH;
  }

  private final LipidAnnotationMsMsTestResource Hex3Cer_42_1_2OAcetate = new LipidAnnotationMsMsTestResource(
      new double[]{1134.788, 972.736, 810.683, 648.636}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.TRIHEXOSYLCERAMIDES, 42, 1, 0));

  public LipidAnnotationMsMsTestResource getHex3Cer_42_1_2OAcetate() {
    return Hex3Cer_42_1_2OAcetate;
  }

  private final LipidAnnotationMsMsTestResource HexCer_18_1_2O_20_0MMPlusCH3COO = new LipidAnnotationMsMsTestResource(
      new double[]{814.641, 592.567, 336.327, 293.285, 237.222, 179.056}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.HEXOSYLCERAMIDES,
          new int[]{18, 20}, new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHexCer_18_1_2O_20_0MMPlusCH3COO() {
    return HexCer_18_1_2O_20_0MMPlusCH3COO;
  }

  private final LipidAnnotationMsMsTestResource HexCer_20_1_2O_16_0MMPlusHCOO = new LipidAnnotationMsMsTestResource(
      new double[]{564.535, 546.523, 460.333, 308.296, 282.280, 265.255}, //
      IonizationType.FORMATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.HEXOSYLCERAMIDES,
          new int[]{20, 16}, new int[]{1, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHexCer_20_1_2O_16_0MMPlusHCOO() {
    return HexCer_20_1_2O_16_0MMPlusHCOO;
  }


  private final LipidAnnotationMsMsTestResource HexCer_18_1_2O_24_1_OMMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{808.666, 646.614, 628.603, 282.279, 264.269, 252.269}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.HEXOSYLCERAMIDEHYDROXYFATTYACID,
          new int[]{18, 24}, new int[]{1, 1}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHexCer_18_1_2O_24_1_OMMPlusH() {
    return HexCer_18_1_2O_24_1_OMMPlusH;
  }

  private final LipidAnnotationMsMsTestResource HexCer_16_1_2O_26_1_OMMPlusCH3COO = new LipidAnnotationMsMsTestResource(
      new double[]{824.662, 662.609, 406.368, 179.056}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.HEXOSYLCERAMIDEHYDROXYFATTYACID, 42, 2, 0));

  public LipidAnnotationMsMsTestResource getHexCer_16_1_2O_26_1_OMMPlusCH3COO() {
    return HexCer_16_1_2O_26_1_OMMPlusCH3COO;
  }

  private final LipidAnnotationMsMsTestResource HexCer_18_0_3O_24_0_OMMPlusH = new LipidAnnotationMsMsTestResource(
      new double[]{846.703, 684.650, 666.640, 318.300, 300.290, 282.279, 264.265}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.HEXOSYLCERAMIDEHYDROXYFATTYACIDPHYTOSPHINGOSINE, new int[]{18, 24},
          new int[]{0, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHexCer_18_0_3O_24_0_OMMPlusH() {
    return HexCer_18_0_3O_24_0_OMMPlusH;
  }

  private final LipidAnnotationMsMsTestResource HexCer_18_0_3O_16_0_OAcetate = new LipidAnnotationMsMsTestResource(
      new double[]{792.584, 732.563, 570.510, 326.269, 271.228, 225.222}, //
      IonizationType.ACETATE, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
          LipidClasses.HEXOSYLCERAMIDEHYDROXYFATTYACIDPHYTOSPHINGOSINE, new int[]{18, 16},
          new int[]{0, 0}, new int[]{0, 0}));

  public LipidAnnotationMsMsTestResource getHexCer_18_0_3O_16_0_OAcetate() {
    return HexCer_18_0_3O_16_0_OAcetate;
  }

  //Sterol Lipids
  private final LipidAnnotationMsMsTestResource CE_18_1NH4 = new LipidAnnotationMsMsTestResource(
      new double[]{668.634, 369.352}, //
      IonizationType.AMMONIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.CHOLESTEROLESTERS, 18, 1, 0));

  public LipidAnnotationMsMsTestResource getCE_18_1NH4() {
    return CE_18_1NH4;
  }
}
