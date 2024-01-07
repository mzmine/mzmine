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

package util.lipidannotationtest;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;

public class LipidAnnotationMsMsTestSpectra {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  // Glycerolipids////////////////////////////////////////////////////////////////////
  private LipidAnnotationMsMsTestResource MG_18_OMPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {376.339, 359.339, 341.329}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROLS, 18, 0));

  public LipidAnnotationMsMsTestResource getMG_18_OMPlusNH4() {
    return MG_18_OMPlusNH4;
  }

  private LipidAnnotationMsMsTestResource DG_18_O_20_4MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {627.522, 361.280, 341.302}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLS,
              new int[] {18, 20}, new int[] {0, 4}));

  public LipidAnnotationMsMsTestResource getDG_18_O_20_4MPlusNH4() {
    return DG_18_O_20_4MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource DG_O_34_1MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {563.540, 339.291}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROLS, 34, 1));

  public LipidAnnotationMsMsTestResource getDG_O_34_1MPlusNH4() {
    return DG_O_34_1MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource TG_16_O_18_2_22_6MPlusNH4 =
      new LipidAnnotationMsMsTestResource(
          new double[] {920.770, 903.744, 647.504, 623.504, 575.504}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.TRIACYLGLYCEROLS,
              new int[] {16, 18, 22}, new int[] {0, 2, 6}));

  public LipidAnnotationMsMsTestResource getTG_16_O_18_2_22_6MPlusNH4() {
    return TG_16_O_18_2_22_6MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource TG_16_O_18_2_22_6MPlusNa =
      new LipidAnnotationMsMsTestResource(new double[] {669.497, 645.477, 597.485}, //
          IonizationType.SODIUM, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.TRIACYLGLYCEROLS,
              new int[] {16, 18, 22}, new int[] {0, 2, 6}));

  public LipidAnnotationMsMsTestResource getTG_16_O_18_2_22_6MPlusNa() {
    return TG_16_O_18_2_22_6MPlusNa;
  }

  private LipidAnnotationMsMsTestResource DGTS_16_0_18_1MPlusH =
      new LipidAnnotationMsMsTestResource(
          new double[] {738.624, 500.395, 482.384, 474.370, 456.368, 236.149, 144.102}, //
          IonizationType.POSITIVE_HYDROGEN, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
              LipidClasses.DIACYLGLYCEROLTRIMETHYLHOMOSERIN, new int[] {16, 18}, new int[] {0, 1}));

  public LipidAnnotationMsMsTestResource getDGTS_16_0_18_1MPlusH() {
    return DGTS_16_0_18_1MPlusH;
  }

  private LipidAnnotationMsMsTestResource LDGTS_18_1MPlusH = new LipidAnnotationMsMsTestResource(
      new double[] {236.149, 144.102}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLTRIMETHYLHOMOSERIN, 18, 1));

  public LipidAnnotationMsMsTestResource getLDGTS_18_1MPlusH() {
    return LDGTS_18_1MPlusH;
  }

  private LipidAnnotationMsMsTestResource MGDG_16_O_18_1MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {577.513, 339.289, 313.276}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.MONOGALACTOSYLDIACYLGLYCEROL,
              new int[] {16, 18}, new int[] {0, 1}));

  public LipidAnnotationMsMsTestResource getMGDG_16_O_18_1MPlusNH4() {
    return MGDG_16_O_18_1MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource MGDG_16_O_18_1MPlusAcetate =
      new LipidAnnotationMsMsTestResource(
          new double[] {815.589, 755.568, 517.332, 491.323, 281.248, 255.232}, //
          IonizationType.ACETATE, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.MONOGALACTOSYLDIACYLGLYCEROL,
              new int[] {16, 18}, new int[] {0, 1}));

  public LipidAnnotationMsMsTestResource getMGDG_16_O_18_1MPlusAcetate() {
    return MGDG_16_O_18_1MPlusAcetate;
  }

  private LipidAnnotationMsMsTestResource DGDG_16_O_18_1MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {936.618, 577.519, 339.313, 313.274}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIGALACTOSYLDIACYLGLYCEROL,
              new int[] {16, 18}, new int[] {0, 1}));

  public LipidAnnotationMsMsTestResource getDGDG_16_O_18_1MPlusNH4() {
    return DGDG_16_O_18_1MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource SQDG_16_O_18_1MPlusNH4 =
      new LipidAnnotationMsMsTestResource(
          new double[] {838.571, 595.530, 577.519, 339.289, 313.274, 239.237}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.SULFOQUINOVOSYLDIACYLGLYCEROLS,
              new int[] {16, 18}, new int[] {0, 1}));

  public LipidAnnotationMsMsTestResource getSQDG_16_O_18_1MPlusNH4() {
    return SQDG_16_O_18_1MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource SQDG_16_O_16_0MMinusH =
      new LipidAnnotationMsMsTestResource(new double[] {793.514, 537.274, 255.232, 225.007}, //
          IonizationType.NEGATIVE_HYDROGEN, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.SULFOQUINOVOSYLDIACYLGLYCEROLS,
              new int[] {16, 16}, new int[] {0, 0}));

  public LipidAnnotationMsMsTestResource getSQDG_16_O_16_0MMinusH() {
    return SQDG_16_O_16_0MMinusH;
  }

  private LipidAnnotationMsMsTestResource PC_18_0_20_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[] {810.601, 544.340, 526.330, 524.372, 506.361, 184.073}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES,
          new int[] {18, 20}, new int[] {0, 4}));

  public LipidAnnotationMsMsTestResource getPC_18_0_20_4MPlusH() {
    return PC_18_0_20_4MPlusH;
  }

  private LipidAnnotationMsMsTestResource PC_O_38_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[] {796.622, 613.555, 184.074}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOCHOLINES, 38, 4));

  public LipidAnnotationMsMsTestResource getPC_O_38_4MPlusH() {
    return PC_O_38_4MPlusH;
  }

  private LipidAnnotationMsMsTestResource LPC_18_1MPlusH =
      new LipidAnnotationMsMsTestResource(new double[] {522.355, 184.074, 104.107}, //
          IonizationType.POSITIVE_HYDROGEN, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOCHOLINES, 18, 1));

  public LipidAnnotationMsMsTestResource getLPC_18_1MPlusH() {
    return LPC_18_1MPlusH;
  }

  private LipidAnnotationMsMsTestResource PE_38_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[] {768.554, 627.535, 287.237, 267.269}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 38, 4));

  public LipidAnnotationMsMsTestResource getPE_38_4MPlusH() {
    return PE_38_4MPlusH;
  }

  private LipidAnnotationMsMsTestResource PE_38_4MPlusNa = new LipidAnnotationMsMsTestResource(
      new double[] {790.536, 747.494, 649.517, 627.535}, //
      IonizationType.SODIUM, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 38, 4));

  public LipidAnnotationMsMsTestResource getPE_38_4MPlusNa() {
    return PE_38_4MPlusNa;
  }

  private LipidAnnotationMsMsTestResource PE_18_0_20_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {766.539, 303.232, 283.264, 196.038}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 38, 4));

  public LipidAnnotationMsMsTestResource getPE_18_0_20_4MMinusH() {
    return PE_18_0_20_4MMinusH;
  }

  private LipidAnnotationMsMsTestResource PE_O_34_1MPlusH =
      new LipidAnnotationMsMsTestResource(new double[] {704.549, 563.540, 265.253}, //
          IonizationType.POSITIVE_HYDROGEN, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES,
              34, 1));

  public LipidAnnotationMsMsTestResource getPE_O_34_1MPlusH() {
    return PE_O_34_1MPlusH;
  }

  private LipidAnnotationMsMsTestResource PE_O_34_1MMinusH =
      new LipidAnnotationMsMsTestResource(new double[] {702.544, 438.299, 281.248}, //
          IonizationType.NEGATIVE_HYDROGEN, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES,
              34, 1));

  public LipidAnnotationMsMsTestResource getPE_O_34_1MMinusH() {
    return PE_O_34_1MMinusH;
  }

  private LipidAnnotationMsMsTestResource LPE_18_1MPlusH = new LipidAnnotationMsMsTestResource(
      new double[] {480.308485, 339.289375, 216.06318, 265.252605, 142.02639}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOETHANOLAMINES, 18,
          1));

  public LipidAnnotationMsMsTestResource getLPE_18_1MPlusH() {
    return LPE_18_1MPlusH;
  }

  private LipidAnnotationMsMsTestResource LPE_18_1MMinusH =
      new LipidAnnotationMsMsTestResource(new double[] {478.294, 281.248, 196.038, 78.955}, //
          IonizationType.NEGATIVE_HYDROGEN, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOETHANOLAMINES, 18,
              1));

  public LipidAnnotationMsMsTestResource getLPE_18_1MMinusH() {
    return LPE_18_1MMinusH;
  }

  private LipidAnnotationMsMsTestResource PS_18_0_20_4MPlusH = new LipidAnnotationMsMsTestResource(
      new double[] {812.544, 627.535, 546.283, 528.272, 526.315, 508.304}, //
      IonizationType.POSITIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES,
          new int[] {18, 20}, new int[] {0, 4}));

  public LipidAnnotationMsMsTestResource getPS_18_0_20_4MPlusH() {
    return PS_18_0_20_4MPlusH;
  }

  private LipidAnnotationMsMsTestResource PS_18_0_20_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {810.530, 723.497, 419.257, 303.232, 283.264, 152.994, 78.964}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES,
          new int[] {18, 20}, new int[] {0, 4}));

  public LipidAnnotationMsMsTestResource getPS_18_0_20_4MMinusH() {
    return PS_18_0_20_4MMinusH;
  }

  private LipidAnnotationMsMsTestResource PS_38_4MPlusNa =
      new LipidAnnotationMsMsTestResource(new double[] {834.526, 747.494, 649.517, 207.988}, //
          IonizationType.SODIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES, 38, 4));

  public LipidAnnotationMsMsTestResource getPS_38_4MPlusNa() {
    return PS_38_4MPlusNa;
  }

  private LipidAnnotationMsMsTestResource PS_O_38_6MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {792.519, 705.487, 395.257, 377.246, 327.232, 78.956}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOSERINES, 38, 6));

  public LipidAnnotationMsMsTestResource getPS_O_38_6MMinusH() {
    return PS_O_38_6MMinusH;
  }

  private LipidAnnotationMsMsTestResource PG_38_4MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {816.575, 627.536, 361.274, 641.305}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS, 38, 4));

  public LipidAnnotationMsMsTestResource getPG_38_4MPlusNH4() {
    return PG_38_4MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource PG_18_0_20_4MMinusH =
      new LipidAnnotationMsMsTestResource(new double[] {797.534, 303.232, 283.264, 152.996}, //
          IonizationType.NEGATIVE_HYDROGEN, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS,
              new int[] {18, 20}, new int[] {0, 4}));

  public LipidAnnotationMsMsTestResource getPG_18_0_20_4MMinusH() {
    return PG_18_0_20_4MMinusH;
  }

  private LipidAnnotationMsMsTestResource PG_O_34_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {733.539, 283.264, 255.232, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOGLYCEROLS, 38, 4));

  public LipidAnnotationMsMsTestResource getPG_O_34_1MMinusH() {
    return PG_O_34_1MMinusH;
  }

  private LipidAnnotationMsMsTestResource LPG_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {509.289, 281.249, 245.040, 227.033, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOGLYCEROLS, 18, 1));

  public LipidAnnotationMsMsTestResource getLPG_18_1MMinusH() {
    return LPG_18_1MMinusH;
  }

  private LipidAnnotationMsMsTestResource LPG_O_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {495.309, 403.264, 331.260, 267.269, 152.996, 78.959}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOGLYCEROLS, 18, 1));

  public LipidAnnotationMsMsTestResource getLPG_O_18_1MMinusH() {
    return LPG_O_18_1MMinusH;
  }

  private LipidAnnotationMsMsTestResource BMP_18_1_22_4MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {842.591, 653.552, 389.305, 339.289}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
              LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS, new int[] {18, 22},
              new int[] {1, 4}));

  public LipidAnnotationMsMsTestResource getBMP_18_1_22_4MPlusNH4() {
    return BMP_18_1_22_4MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource BMP_40_5MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {842.591, 653.552, 389.305, 339.289}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(
              LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS, 40, 5));

  public LipidAnnotationMsMsTestResource getBMP_40_5MPlusNH4() {
    return BMP_40_5MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource PI_18_0_20_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {885.550, 581.309, 303.232, 283.264, 241.012}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS,
          new int[] {18, 20}, new int[] {0, 4}));

  public LipidAnnotationMsMsTestResource getPI_18_0_20_4MMinusH() {
    return PI_18_0_20_4MMinusH;
  }

  private LipidAnnotationMsMsTestResource PI_38_4MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {627.535}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 38, 4));

  public LipidAnnotationMsMsTestResource getPI_38_4MPlusNH4() {
    return PI_38_4MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource PI_38_4MPlusNa =
      new LipidAnnotationMsMsTestResource(new double[] {909.546, 627.535, 283.019}, //
          IonizationType.SODIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 38, 4));

  public LipidAnnotationMsMsTestResource getPI_38_4MPlusNa() {
    return PI_38_4MPlusNa;
  }

  private LipidAnnotationMsMsTestResource PI_36_4MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {843.539, 539.299, 377.246, 303.232, 241.012}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS, 36, 5));

  public LipidAnnotationMsMsTestResource getPI_36_4MMinusH() {
    return PI_36_4MMinusH;
  }

  private LipidAnnotationMsMsTestResource LPI_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {597.307, 417.229, 315.049, 281.249, 241.012, 152.99, 78.962}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOINOSITOLS, 18, 1));

  public LipidAnnotationMsMsTestResource getLPI_18_1MMinusH() {
    return LPI_18_1MMinusH;
  }

  private LipidAnnotationMsMsTestResource PA_16_0_18_1MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {673.48146, 435.25179, 409.23609, 417.24123, 391.22553, 224.06881, 255.23295,
          281.24865, 171.00644, 152.99586, 96.96964, 78.95852}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildMolecularSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHATES,
          new int[] {16, 18}, new int[] {0, 1}));

  public LipidAnnotationMsMsTestResource getPA_16_0_18_1MMinusH() {
    return PA_16_0_18_1MMinusH;
  }

  private LipidAnnotationMsMsTestResource LPA_16_0MMinusH =
      new LipidAnnotationMsMsTestResource(new double[] {409.236, 152.996, 78.959}, //
          IonizationType.NEGATIVE_HYDROGEN, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHATES, 16, 0));

  public LipidAnnotationMsMsTestResource getLPA_16_0MMinusH() {
    return LPA_16_0MMinusH;
  }

  private LipidAnnotationMsMsTestResource CL_70_4_MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {1430.011, 601.519, 577.519}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(
              LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 70, 4));

  public LipidAnnotationMsMsTestResource getCL_70_4_MPlusNH4() {
    return CL_70_4_MPlusNH4;
  }

  private LipidAnnotationMsMsTestResource CL_70_5_MMinusH = new LipidAnnotationMsMsTestResource(
      new double[] {1425.964, 695.466, 673.481, 415.226, 281.249, 279.233, 255.233, 152.996}, //
      IonizationType.NEGATIVE_HYDROGEN, //
      LIPID_FACTORY.buildSpeciesLevelLipid(
          LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 70, 5));

  public LipidAnnotationMsMsTestResource getCL_70_5_MMinusH() {
    return CL_70_5_MMinusH;
  }

  private LipidAnnotationMsMsTestResource CL_16_0_18_1_18_2_18_2MMinusH =
      new LipidAnnotationMsMsTestResource(
          new double[] {1425.964, 695.466, 673.481, 415.226, 281.249, 279.233, 255.233, 152.996}, //
          IonizationType.NEGATIVE_HYDROGEN, //
          LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
              LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS,
              new int[] {16, 18, 18, 18}, new int[] {0, 1, 2, 2}));

  public LipidAnnotationMsMsTestResource getCL_16_0_18_1_18_2_18_2MMinusH() {
    return CL_16_0_18_1_18_2_18_2MMinusH;
  }

}
