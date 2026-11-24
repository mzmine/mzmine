package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LipidFragmentFactoryChainRuleTest {

  private static final RawDataFile FILE = new RawDataFileImpl("testfile", null, null, Color.BLACK);
  private static final MZTolerance MZ_TOLERANCE = new MZTolerance(0.005, 5);
  // PE 34:1 = PE 16:0_18:1
  private static final SpeciesLevelAnnotation PE_34_1 = new LipidFactory().buildSpeciesLevelLipid(
      LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 34, 1, 0);

  @Test
  void acylChainPlusSignedFormulaFragment() {
    // 18:1 chain C18H34O2 + O2 - H2 as [M]- ion
    final List<LipidFragment> fragments = findFragments(
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "+O2-H2", "C18H32O4-");
    Assertions.assertTrue(fragments.stream()
        .anyMatch(f -> f.getChainLength() == 18 && f.getNumberOfDBEs() == 1));
  }

  @Test
  void acylChainMinusSignedFormulaFragment() {
    // minus rules apply the inverse: 18:1 chain C18H34O2 - (O - H2) as [M]-
    final List<LipidFragment> fragments = findFragments(
        LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT, "+O-H2", "C18H36O-");
    Assertions.assertTrue(fragments.stream()
        .anyMatch(f -> f.getChainLength() == 18 && f.getNumberOfDBEs() == 1));
  }

  @Test
  void amidMonoHydroxyPlusFormulaNeutralLossIsSupported() {
    // only verifies the new rule type is dispatched without failing
    Assertions.assertNotNull(findFragments(
        LipidFragmentationRuleType.AMID_MONO_HYDROXY_CHAIN_PLUS_FORMULA_FRAGMENT_NL, "H2O",
        "C2H4-"));
  }

  @Test
  void twoAcylChainsMinusFormulaNeutralLoss() {
    // [PE 34:1 - H]- = C39H75NO8P-, minus (combined chain C34H66O2 - H2O = C34H64O)
    final List<LipidFragment> fragments = findFragments(
        LipidFragmentationRuleType.TWO_ACYLCHAINS_MINUS_FORMULA_FRAGMENT_NL, "H2O",
        "C5H11NO7P-");
    Assertions.assertTrue(fragments.stream()
        .anyMatch(f -> f.getChainLength() == 34 && f.getNumberOfDBEs() == 1
            && f.getLipidChainType() == LipidChainType.TWO_ACYL_CHAINS_COMBINED));
  }

  private static @NotNull List<LipidFragment> findFragments(
      final @NotNull LipidFragmentationRuleType ruleType, final @NotNull String ruleFormula,
      final @NotNull String expectedIonFormula) {
    final double mz = FormulaUtils.calculateMzRatio(expectedIonFormula);
    final Scan scan = new SimpleScan(FILE, -1, 2, 0.1F, null, new double[]{mz},
        new double[]{100}, MassSpectrumType.CENTROIDED, PolarityType.NEGATIVE, "Pseudo", null);
    scan.addMassList(SimpleMassList.create(null, new DataPoint[]{new SimpleDataPoint(mz, 100)}));

    final LipidFragmentationRule rule = new LipidFragmentationRule(PolarityType.NEGATIVE,
        IonizationType.NEGATIVE_HYDROGEN, ruleType, LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL,
        ruleFormula);
    final LipidFragmentFactory factory = new LipidFragmentFactory(MZ_TOLERANCE, PE_34_1,
        IonizationType.NEGATIVE_HYDROGEN, new LipidFragmentationRule[]{rule}, scan,
        new LipidAnnotationChainParameters());
    return factory.findLipidFragments();
  }
}
