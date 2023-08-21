package util.lipidannotationtest;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.GlyceroAndGlyceroPhospholipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.SphingolipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.glyceroandglycerophospholipids.GlyceroAndGlycerophospholipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.sphingolipids.SphingolipidAnnotationChainParameters;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LipidFragmentFactoryTest {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final MZTolerance MZ_TOLERANCE = new MZTolerance(0.001, 5);
  //Test for any PC or SM lipid
  private static final DataPoint TEST_DATA_POINT_HEADGROUP_FRAGMENT = new SimpleDataPoint(184.0734,
      1);
  //Test for PI 32:1
  private static final DataPoint TEST_DATA_POINT_HEADGROUP_FRAGMENT_NL = new SimpleDataPoint(
      645.4501, 1);
  private static final RawDataFile TEST_FILE = new RawDataFileImpl("testfile", null, null,
      Color.BLACK);
  private static final Scan TEST_SCAN = new SimpleScan(TEST_FILE, 1, 2, 0.1f * 1,
      new DDAMsMsInfoImpl(300, 1, 20f, null, null, 2, ActivationMethod.UNKNOWN,
          Range.closed(299d, 301d)), new double[]{700, 800, 900, 1000, 1100},
      new double[]{1700, 1800, 1900, 11000, 11100}, MassSpectrumType.CENTROIDED,
      PolarityType.POSITIVE, "", Range.closed(0d, 1d));
  public static final ParameterSetParameter<GlyceroAndGlycerophospholipidAnnotationChainParameters> lipidChainParameters = new ParameterSetParameter<GlyceroAndGlycerophospholipidAnnotationChainParameters>(
      "Side chain parameters", "Optionally modify lipid chain parameters",
      new GlyceroAndGlycerophospholipidAnnotationChainParameters());
  public static final ParameterSetParameter<SphingolipidAnnotationChainParameters> lipidChainParametersSphingolipids = new ParameterSetParameter<SphingolipidAnnotationChainParameters>(
      "Side chain parameters", "Optionally modify lipid chain parameters",
      new SphingolipidAnnotationChainParameters());

  @Test
  void findCommonLipidFragments() {
    ILipidAnnotation testLipidAnnotationHeadgroupFragment = LIPID_FACTORY.buildSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 0, 0);
    LipidFragmentationRule[] lipidFragmentationRulesHeadgroupFragment = LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(TEST_DATA_POINT_HEADGROUP_FRAGMENT.getMZ()),
        testLipidAnnotationHeadgroupFragment, IonizationType.POSITIVE_HYDROGEN,
        lipidFragmentationRulesHeadgroupFragment, TEST_DATA_POINT_HEADGROUP_FRAGMENT, TEST_SCAN,
        lipidChainParameters.getEmbeddedParameters());

    List<LipidFragment> lipidHeadgroupFragments = lipidFragmentFactory.findCommonLipidFragment();
    LipidFragment testLipidHeadgroupFragment = new LipidFragment(
        LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
        LipidFragmentationRuleRating.MAJOR, 184.07332101009092, "C",
        TEST_DATA_POINT_HEADGROUP_FRAGMENT, LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 0, null,
        null, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidHeadgroupFragments, testLipidHeadgroupFragment);

    ILipidAnnotation testLipidAnnotationHeadgroupFragmentNL = LIPID_FACTORY.buildSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 32, 1, 0);
    LipidFragmentationRule[] lipidFragmentationRulesHeadgroupFragmentNL = LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactoryNL = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(TEST_DATA_POINT_HEADGROUP_FRAGMENT_NL.getMZ()),
        testLipidAnnotationHeadgroupFragmentNL, IonizationType.POSITIVE_HYDROGEN,
        lipidFragmentationRulesHeadgroupFragmentNL, TEST_DATA_POINT_HEADGROUP_FRAGMENT, TEST_SCAN,
        lipidChainParameters.getEmbeddedParameters());
    List<LipidFragment> lipidHeadgroupNLFragments = lipidFragmentFactory.findCommonLipidFragment();
    LipidFragment testLipidHeadgroupNLFragment = new LipidFragment(
        LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
        LipidFragmentationRuleRating.MAJOR, 184.0733210100, "C", TEST_DATA_POINT_HEADGROUP_FRAGMENT,
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 0, null, null, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidHeadgroupNLFragments, testLipidHeadgroupNLFragment);
  }

  @Test
  void findGlyceroAndGlyceroPhospholipidSpecificLipidFragments() {
    ILipidAnnotation testLipidAnnotationPI_18_0_20_4 = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, new int[]{18, 20}, new int[]{0, 4},
        new int[]{0, 0});
    DataPoint testDataPointAcylChain = new SimpleDataPoint(283.2643, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()),
        testLipidAnnotationPI_18_0_20_4, IonizationType.NEGATIVE_HYDROGEN, lipidFragmentationRules,
        testDataPointAcylChain, TEST_SCAN, lipidChainParameters.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT, LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL,
        LipidFragmentationRuleRating.MAJOR, 283.2643, "C", testDataPointAcylChain,
        LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 18, 0, 0, LipidChainType.ACYL_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  @Test
  void findGlyceroAndGlyceroPhospholipidSpecificLipidAcylChainNL() {
    ILipidAnnotation testLipidAnnotationPI_18_0_20_4 = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, new int[]{18, 20}, new int[]{0, 4},
        new int[]{0, 0});
    DataPoint testDataPointAcylChain = new SimpleDataPoint(601.2783, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()),
        testLipidAnnotationPI_18_0_20_4, IonizationType.NEGATIVE_HYDROGEN, lipidFragmentationRules,
        testDataPointAcylChain, TEST_SCAN, lipidChainParameters.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT_NL,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, LipidFragmentationRuleRating.MAJOR, 601.2783,
        "C", testDataPointAcylChain, LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 18, 0, 0,
        LipidChainType.ACYL_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  @Test
  void findGlyceroAndGlyceroPhospholipidSpecificLipidAcylChainMinuSFormulaFragmentNL() {
    ILipidAnnotation testLipidAnnotationPI_18_0_20_4 = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, new int[]{18, 20}, new int[]{0, 4},
        new int[]{0, 0});
    DataPoint testDataPointAcylChain = new SimpleDataPoint(619.2889, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()),
        testLipidAnnotationPI_18_0_20_4, IonizationType.NEGATIVE_HYDROGEN, lipidFragmentationRules,
        testDataPointAcylChain, TEST_SCAN, lipidChainParameters.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, LipidFragmentationRuleRating.MAJOR, 619.2889,
        "C", testDataPointAcylChain, LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 18, 0, 0,
        LipidChainType.ACYL_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  @Test
  void findGlyceroAndGlyceroPhospholipidSpecificLipidAcylChainPlusFormulaFragmentNL() {
    ILipidAnnotation testLipidAnnotationPI_18_0_20_4 = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, new int[]{18, 20}, new int[]{0, 4},
        new int[]{0, 0});
    DataPoint testDataPointAcylChain = new SimpleDataPoint(419.2568, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()),
        testLipidAnnotationPI_18_0_20_4, IonizationType.NEGATIVE_HYDROGEN, lipidFragmentationRules,
        testDataPointAcylChain, TEST_SCAN, lipidChainParameters.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, LipidFragmentationRuleRating.MAJOR, 419.2568,
        "C", testDataPointAcylChain, LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 18, 0, 0,
        LipidChainType.ACYL_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  @Test
  void findGlyceroAndGlyceroPhospholipidSpecificLipidTwoAcylChainsPlusFormulaFragment() {
    ILipidAnnotation testLipidAnnotationCL_70_4 = LIPID_FACTORY.buildSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 70, 4, 0);
    DataPoint testDataPointAcylChain = new SimpleDataPoint(601.519, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()), testLipidAnnotationCL_70_4,
        IonizationType.AMMONIUM, lipidFragmentationRules, testDataPointAcylChain, TEST_SCAN,
        lipidChainParameters.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT,
        LipidAnnotationLevel.SPECIES_LEVEL, LipidFragmentationRuleRating.MAJOR, 601.519, "C",
        testDataPointAcylChain, LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 36,
        3, 0, LipidChainType.TWO_ACYL_CHAINS_COMBINED, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  @Test
  void findGlyceroAndGlyceroPhospholipidSpecificLipidAlkylChainPlusFormulaFragment() {
    ILipidAnnotation testLipidAnnotationPI_18_0_20_4 = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
        LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS, new int[]{16, 20}, new int[]{0, 4},
        new int[]{0, 0});
    DataPoint testDataPointAcylChain = new SimpleDataPoint(377.2462, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()),
        testLipidAnnotationPI_18_0_20_4, IonizationType.NEGATIVE_HYDROGEN, lipidFragmentationRules,
        testDataPointAcylChain, TEST_SCAN, lipidChainParameters.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.ALKYLCHAIN_PLUS_FORMULA_FRAGMENT,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, LipidFragmentationRuleRating.MAJOR, 377.2462,
        "C", testDataPointAcylChain, LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS, 16, 0, 0,
        LipidChainType.ALKYL_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  @Test
  void findSphingolipidSpecificLipidSphingosinChainAndSubstructureNLFragment() {
    ILipidAnnotation testLipidAnnotationCer_18_1_2O_16_0_4 = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
        LipidClasses.CERAMIDEPHOSPHOCHOLINES, new int[]{18, 16}, new int[]{1, 4}, new int[]{0, 0});
    DataPoint testDataPointAcylChain = new SimpleDataPoint(264.2685, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.CERAMIDEPHOSPHOCHOLINES.getFragmentationRules();
    SphingolipidFragmentFactory lipidFragmentFactory = new SphingolipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()),
        testLipidAnnotationCer_18_1_2O_16_0_4, IonizationType.POSITIVE_HYDROGEN,
        lipidFragmentationRules, testDataPointAcylChain, TEST_SCAN,
        lipidChainParametersSphingolipids.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT,
        LipidAnnotationLevel.SPECIES_LEVEL, LipidFragmentationRuleRating.MAJOR, 264.2686, "C",
        testDataPointAcylChain, LipidClasses.CERAMIDEPHOSPHOCHOLINES, 18, 0, 0,
        LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  @Test
  void findSphingolipidSpecificLipidAmidMonoHydroxyChainPlusSubstructureFragment() {
    ILipidAnnotation testLipidAnnotationCer_18_0_2O_16_0_O = LIPID_FACTORY.buildMolecularSpeciesLevelLipid(
        LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID, new int[]{18, 16}, new int[]{0, 0},
        new int[]{0, 0});
    DataPoint testDataPointAcylChain = new SimpleDataPoint(225.2224, 1);
    LipidFragmentationRule[] lipidFragmentationRules = LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID.getFragmentationRules();
    SphingolipidFragmentFactory lipidFragmentFactory = new SphingolipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(testDataPointAcylChain.getMZ()),
        testLipidAnnotationCer_18_0_2O_16_0_O, IonizationType.ACETATE, lipidFragmentationRules,
        testDataPointAcylChain, TEST_SCAN,
        lipidChainParametersSphingolipids.getEmbeddedParameters());
    List<LipidFragment> lipidAcylChainFragments = lipidFragmentFactory.findLipidFragments();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT,
        LipidAnnotationLevel.SPECIES_LEVEL, LipidFragmentationRuleRating.MAJOR, 225.2224, "C",
        testDataPointAcylChain, LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID, 18, 0, 0,
        LipidChainType.AMID_MONO_HYDROXY_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragments, testLipidAcylChainFragment);
  }

  private static void compareTestAndBuildLipidFragments(List<LipidFragment> buildLipidFragments,
      LipidFragment testLipidFragment) {
    boolean allCriteriaTrue = false;
    for (LipidFragment fragment : buildLipidFragments) {
      if (testLipidFragment.getLipidClass().equals(fragment.getLipidClass())
          && testLipidFragment.getLipidChainType().equals(fragment.getLipidChainType())
          && testLipidFragment.getLipidFragmentInformationLevelType()
          .equals(fragment.getLipidFragmentInformationLevelType()) && BigDecimal.valueOf(
              testLipidFragment.getMzExact()).setScale(4, RoundingMode.HALF_DOWN)
          .equals(BigDecimal.valueOf(fragment.getMzExact()).setScale(4, RoundingMode.HALF_DOWN))) {
        allCriteriaTrue = true;
      }
    }
    Assertions.assertTrue(allCriteriaTrue);
  }

}

