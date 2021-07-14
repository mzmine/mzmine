package lipidannotationtest;

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

  private LipidAnnotationMsMsTestResource PI_38_4MPlusNH4 =
      new LipidAnnotationMsMsTestResource(new double[] {627.535}, //
          IonizationType.AMMONIUM, //
          LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 38, 4));

  public LipidAnnotationMsMsTestResource getPI_38_4MPlusNH4() {
    return PI_38_4MPlusNH4;
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
