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

class LipidAnnotationTest {

  private static final LipidAnnotationMsMsTestSpectra MSMS_TEST_SPECTRA =
      new LipidAnnotationMsMsTestSpectra();
  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  // Glycerlipids##############################################################################
  @Test
  void msMsRuleTestMG_NH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getMG_18_OMPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestDG_NH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getDG_18_O_20_4MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestDG_O_NH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getDG_O_34_1MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestTG_NH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getTG_16_O_18_2_22_6MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestTG_Na() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getTG_16_O_18_2_22_6MPlusNa();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestDGTS_MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getDGTS_16_0_18_1MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestLDGTS_MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLDGTS_18_1MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestMGDG_MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getMGDG_16_O_18_1MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestMGDG_MPlusAcetate() {
    LipidAnnotationMsMsTestResource testSpectrum =
        MSMS_TEST_SPECTRA.getMGDG_16_O_18_1MPlusAcetate();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestDGDG_MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getDGDG_16_O_18_1MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestSQDG_16_O_18_1MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getSQDG_16_O_18_1MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestSQDG_16_O_16_0MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getSQDG_16_O_16_0MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  // // Glycerophospholipids
  // ########################################################################

  @Test
  void msMsRuleTestPC_18_0_20_4MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPC_18_0_20_4MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPC_O_18_0_20_4MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPC_O_38_4MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestLPC_O_18_1MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLPC_18_1MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPE_38_4MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPE_38_4MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPE_38_4MPlusNa() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPE_38_4MPlusNa();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPE_18_0_20_4MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPE_18_0_20_4MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPE_O_34_1MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPE_O_34_1MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPE_O_34_1MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPE_O_34_1MMinusH();
    checkLipidAnnotation(testSpectrum);
  }


  @Test
  void msMsRuleTestLPE_18_1MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLPE_18_1MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestLPE_18_1MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLPE_18_1MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPS_18_0_20_4MPlusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPS_18_0_20_4MPlusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPS_18_0_20_4MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPS_18_0_20_4MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPS_38_4MPlusNa() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPS_38_4MPlusNa();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPS_O_16_0_22_6MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPS_O_38_6MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPG_38_4MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPG_38_4MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPG_18_0_20_4MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPG_18_0_20_4MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPG_34_1MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPG_O_34_1MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestLPG_18_1MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLPG_18_1MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestLPG_O_18_1MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLPG_O_18_1MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestBMP_40_5MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getBMP_40_5MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestBMP_18_1_22_4MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getBMP_18_1_22_4MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPI_18_0_20_4MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPI_18_0_20_4MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPI_38_4MPlusNH4() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPI_38_4MPlusNH4();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPI_38_4MPlusNa() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPI_38_4MPlusNa();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPI_O_16_0_20_4MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPI_36_4MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestLPI_18_1MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLPI_18_1MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestPA_16_0_18_1MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getPA_16_0_18_1MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

  @Test
  void msMsRuleTestLPA_16_0MMinusH() {
    LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getLPA_16_0MMinusH();
    checkLipidAnnotation(testSpectrum);
  }

   @Test
   void msMsRuleTestCL_70_4_MPlusNH4() {
   LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getCL_70_4_MPlusNH4();
   checkLipidAnnotation(testSpectrum);
   }

   @Test
   void msMsRuleTestCL_70_5_MMinusH() {
   LipidAnnotationMsMsTestResource testSpectrum = MSMS_TEST_SPECTRA.getCL_70_5_MMinusH();
   checkLipidAnnotation(testSpectrum);
   }

   @Test
   void msMsRuleTestCL_16_0_18_1_18_2_18_2MMinusH() {
   LipidAnnotationMsMsTestResource testSpectrum =
   MSMS_TEST_SPECTRA.getCL_16_0_18_1_18_2_18_2MMinusH();
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
