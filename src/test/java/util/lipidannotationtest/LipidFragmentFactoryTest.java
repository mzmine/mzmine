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
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.GlyceroAndGlyceroPhospholipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  @Test
  void findCommonLipidFragments() {
    ILipidAnnotation testLipidAnnotationHeadgroupFragment = LIPID_FACTORY.buildSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 0, 0);
    LipidFragmentationRule[] lipidFragmentationRulesHeadgroupFragment = LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactory = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(TEST_DATA_POINT_HEADGROUP_FRAGMENT.getMZ()),
        testLipidAnnotationHeadgroupFragment, IonizationType.POSITIVE_HYDROGEN,
        lipidFragmentationRulesHeadgroupFragment, TEST_DATA_POINT_HEADGROUP_FRAGMENT, TEST_SCAN);

    LipidFragment lipidHeadgroupFragment = lipidFragmentFactory.findCommonLipidFragment();
    LipidFragment testLipidHeadgroupFragment = new LipidFragment(
        LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
        184.07332101009092, TEST_DATA_POINT_HEADGROUP_FRAGMENT,
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 0, null, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidHeadgroupFragment, testLipidHeadgroupFragment);

    ILipidAnnotation testLipidAnnotationHeadgroupFragmentNL = LIPID_FACTORY.buildSpeciesLevelLipid(
        LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 32, 1, 0);
    LipidFragmentationRule[] lipidFragmentationRulesHeadgroupFragmentNL = LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS.getFragmentationRules();
    GlyceroAndGlyceroPhospholipidFragmentFactory lipidFragmentFactoryNL = new GlyceroAndGlyceroPhospholipidFragmentFactory(
        MZ_TOLERANCE.getToleranceRange(TEST_DATA_POINT_HEADGROUP_FRAGMENT_NL.getMZ()),
        testLipidAnnotationHeadgroupFragmentNL, IonizationType.POSITIVE_HYDROGEN,
        lipidFragmentationRulesHeadgroupFragmentNL, TEST_DATA_POINT_HEADGROUP_FRAGMENT, TEST_SCAN);
    LipidFragment lipidHeadgroupNLFragment = lipidFragmentFactory.findCommonLipidFragment();
    LipidFragment testLipidHeadgroupNLFragment = new LipidFragment(
        LipidFragmentationRuleType.HEADGROUP_FRAGMENT, LipidAnnotationLevel.SPECIES_LEVEL,
        184.0733210100, TEST_DATA_POINT_HEADGROUP_FRAGMENT,
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 0, null, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidHeadgroupNLFragment, testLipidHeadgroupNLFragment);
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
        testDataPointAcylChain, TEST_SCAN);
    LipidFragment lipidAcylChainFragment = lipidFragmentFactory.findLipidFragment();
    LipidFragment testLipidAcylChainFragment = new LipidFragment(
        LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT, LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL,
        283.2643, testDataPointAcylChain, LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 18, 0,
        LipidChainType.ACYL_CHAIN, TEST_SCAN);
    compareTestAndBuildLipidFragments(lipidAcylChainFragment, testLipidAcylChainFragment);
  }

  private static void compareTestAndBuildLipidFragments(LipidFragment buildLipidFragment,
      LipidFragment testLipidFragment) {
    Assertions.assertEquals(testLipidFragment.getLipidClass(), buildLipidFragment.getLipidClass());
    Assertions.assertEquals(testLipidFragment.getLipidChainType(),
        buildLipidFragment.getLipidChainType());
    Assertions.assertEquals(testLipidFragment.getLipidFragmentInformationLevelType(),
        buildLipidFragment.getLipidFragmentInformationLevelType());
    Assertions.assertEquals(
        BigDecimal.valueOf(testLipidFragment.getMzExact()).setScale(4, RoundingMode.HALF_DOWN),
        BigDecimal.valueOf(buildLipidFragment.getMzExact()).setScale(4, RoundingMode.HALF_DOWN));
  }

}

