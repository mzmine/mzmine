package lipidannotationtest;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import junit.framework.Assert;

public class LipidAnnotationTest {

  private static final LipidAnnotationMsMsTestSpectra MSMS_TEST_SPECTRA =
      new LipidAnnotationMsMsTestSpectra();
  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  // Glycerlipids##############################################################################
  @Test
  public void msMsRuleTestMG_NH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getMG_18_OMPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestDG_NH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getDG_18_O_20_4MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  // TODO DG O- test
  @Test
  public void msMsRuleTestTG_NH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getTG_16_O_18_2_22_6MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestTG_Na() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getTG_16_O_18_2_22_6MPlusNa();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestDGTS_MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getDGTS_16_0_18_1MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestLDGTS_MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLDGTS_18_1MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestMGDG_MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getMGDG_16_O_18_1MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestMGDG_MPlusAcetate() {
    LipidAnnotationMsMsTestResource testSpectrum =
        MSMS_TEST_SPECTRA.getMGDG_16_O_18_1MPlusAcetate();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestDGDG_MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getDGDG_16_O_18_1MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestSQDG_16_O_18_1MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getSQDG_16_O_18_1MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  public void msMsRuleTestSQDG_16_O_16_0MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getSQDG_16_O_16_0MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  // Glycerophospholipids ########################################################################

  @Test
  public void msMsRuleTestPC_18_0_20_4MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPC_18_0_20_4MPlusH();
    checkLipidAnnotation(testSpectrum);
  }


  private void checkLipidAnnotation(LipidAnnotationMsMsTestResource testSpectrum) {
    Set<MatchedLipid> matchedLipids = new HashSet<>();
    ILipidAnnotation lipidAnnotation = testSpectrum.getTestLipid();
    SpeciesLevelAnnotation speciesLevelAnnotation = null;
    if (lipidAnnotation instanceof SpeciesLevelAnnotation) {
      speciesLevelAnnotation = (SpeciesLevelAnnotation) lipidAnnotation;
    } else if (lipidAnnotation instanceof MolecularSpeciesLevelAnnotation) {
      speciesLevelAnnotation = convertMolecularSpeciesLevelToSpeciesLevel(
          (MolecularSpeciesLevelAnnotation) lipidAnnotation);
    }

    LipidFragmentationRule[] rules = speciesLevelAnnotation.getLipidClass().getFragmentationRules();
    Set<LipidFragment> annotatedFragments = new HashSet<>();
    DataPoint[] massList = convertTestSpectrumToDataPoints(testSpectrum);
    MZTolerance mzTolerance = new MZTolerance(0.05, 5);
    if (rules != null && rules.length > 0) {
      for (int j = 0; j < massList.length; j++) {
        Range<Double> mzTolRangeMSMS = mzTolerance.getToleranceRange(massList[j].getMZ());
        LipidFragment annotatedFragment =
            MSMS_LIPID_TOOLS.checkForClassSpecificFragment(mzTolRangeMSMS, speciesLevelAnnotation,
                testSpectrum.getIonizationType(), rules, massList[j], null);
        if (annotatedFragment != null) {
          annotatedFragments.add(annotatedFragment);
        }
      }
    }

    Assert.assertTrue("No fragments detected", annotatedFragments.size() >= 1);
    if (!annotatedFragments.isEmpty()) {

      // check for class specific fragments like head group fragment
      if (testSpectrum.getTestLipid() instanceof SpeciesLevelAnnotation) {
        matchedLipids
            .add(MSMS_LIPID_TOOLS.confirmSpeciesLevelAnnotation(0.0, speciesLevelAnnotation,
                annotatedFragments, massList, 0.0, mzTolerance, testSpectrum.getIonizationType()));
      } else if (testSpectrum.getTestLipid() instanceof MolecularSpeciesLevelAnnotation) {

        // predict molecular species level annotations
        matchedLipids.addAll(MSMS_LIPID_TOOLS.predictMolecularSpeciesLevelAnnotation(
            annotatedFragments, speciesLevelAnnotation, 0.0, massList, 0.0, mzTolerance,
            testSpectrum.getIonizationType()));
      }
    }
    Assert.assertTrue("No lipid was matched", matchedLipids.size() >= 1);

    printMsMsSpectrumTestReport(matchedLipids, testSpectrum);
  }



  private DataPoint[] convertTestSpectrumToDataPoints(
      LipidAnnotationMsMsTestResource testSpectrum) {
    DataPoint[] dataPoints = new DataPoint[testSpectrum.getMzFragments().length];
    for (int i = 0; i < dataPoints.length; i++) {
      dataPoints[i] = new SimpleDataPoint(testSpectrum.getMzFragments()[i], 1);
    }
    return dataPoints;
  }

  private SpeciesLevelAnnotation convertMolecularSpeciesLevelToSpeciesLevel(
      MolecularSpeciesLevelAnnotation lipidAnnotation) {
    int numberOfCarbons =
        lipidAnnotation.getLipidChains().stream().mapToInt(ILipidChain::getNumberOfCarbons).sum();
    int numberOfDBEs =
        lipidAnnotation.getLipidChains().stream().mapToInt(ILipidChain::getNumberOfDBEs).sum();
    return LIPID_FACTORY.buildSpeciesLevelLipid(lipidAnnotation.getLipidClass(), numberOfCarbons,
        numberOfDBEs);
  }

  private void printMsMsSpectrumTestReport(Set<MatchedLipid> matchedLipids,
      LipidAnnotationMsMsTestResource testResource) {
    for (MatchedLipid matchedLipid : matchedLipids) {
      Assert.assertTrue(matchedLipid.getLipidAnnotation().getAnnotation()
          .equals(testResource.getTestLipid().getAnnotation()));
      Set<LipidFragment> matchedFragments = matchedLipid.getMatchedFragments();
      System.out.println("\n---Test Result for " + testResource.getTestLipid().getAnnotation() + " "
          + testResource.getIonizationType() + " ---------");
      System.out.println("Matched " + matchedFragments.size() + " of "
          + testResource.getMzFragments().length + " possible signals");
      System.out.println("MS/MS score " + matchedLipid.getMsMsScore());
      System.out.println("Pseudo signals:");
      for (LipidFragment fragment : matchedFragments) {
        System.out.println("\t" + fragment.getLipidFragmentInformationLevelType().toString());
        System.out.println("\tAccurate m/z: " + fragment.getDataPoint().getMZ());
        System.out.println("\tExact m/z: " + fragment.getMzExact());
      }
    }
  }

}
