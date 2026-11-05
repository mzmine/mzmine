package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LipidClassSimulationTest {

  private static final LipidClasses PE = LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES;
  // PE 18:1_16:0
  private static final ExampleLipidChains EXAMPLE = new ExampleLipidChains(new int[]{18, 16},
      new int[]{1, 0});

  @Test
  void defaultExamplesAreDeterministic() {
    final ExampleLipidChains chains = ExampleLipidChains.defaultsFor(
        new LipidChainType[]{LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
            LipidChainType.AMID_CHAIN});
    Assertions.assertArrayEquals(new int[]{18, 18}, chains.carbons());
    Assertions.assertArrayEquals(new int[]{1, 1}, chains.dbes());
  }

  @Test
  void simulatesFattyAcidAnionsForAcylChainRule() {
    final List<List<SimulatedIon>> ions = LipidRuleSimulator.simulate(PE, EXAMPLE);
    final LipidFragmentationRule[] rules = PE.getFragmentationRules();
    Assertions.assertEquals(rules.length, ions.size());
    boolean found = false;
    for (int i = 0; i < rules.length; i++) {
      if (rules[i].getIonizationType() == IonizationType.NEGATIVE_HYDROGEN
          && rules[i].getLipidFragmentationRuleType() != null
          && rules[i].getLipidFragmentationRuleType().getChainType() == LipidChainType.ACYL_CHAIN
          && rules[i].getMolecularFormula().isBlank()) {
        final List<Double> mzs = ions.get(i).stream().map(SimulatedIon::mz).toList();
        // FA 16:0 and FA 18:1 anions
        Assertions.assertEquals(2, mzs.size(), mzs.toString());
        Assertions.assertEquals(255.2330, mzs.get(0), 0.001);
        Assertions.assertEquals(281.2486, mzs.get(1), 0.001);
        Assertions.assertEquals("Acyl chain 18:1", ions.get(i).get(1).chain());
        found = true;
      }
    }
    Assertions.assertTrue(found, "PE should have an acyl chain fragment rule in negative mode");
  }

  @Test
  void spectrumTestFindsSpeciesAndChains() {
    final SpeciesLevelAnnotation lipid = LipidRuleSimulator.exampleLipid(PE, EXAMPLE);
    Assertions.assertNotNull(lipid);
    final double precursor = LipidRuleSimulator.precursorMz(lipid,
        IonizationType.NEGATIVE_HYDROGEN);
    // spectrum from all simulated [M-H]- ions
    final List<DataPoint> peaks = new ArrayList<>();
    final List<List<SimulatedIon>> ions = LipidRuleSimulator.simulate(PE, EXAMPLE);
    for (int i = 0; i < ions.size(); i++) {
      if (PE.getFragmentationRules()[i].getIonizationType() == IonizationType.NEGATIVE_HYDROGEN) {
        ions.get(i).forEach(ion -> peaks.add(new SimpleDataPoint(ion.mz() + 0.0005, 1000)));
      }
    }
    peaks.add(new SimpleDataPoint(123.4567, 50)); // unexplained noise
    peaks.sort(Comparator.comparingDouble(DataPoint::getMZ));

    final List<SpectrumTestResult> results = LipidSpectrumTester.test(PE,
        IonizationType.NEGATIVE_HYDROGEN, precursor + 0.001, peaks.toArray(DataPoint[]::new),
        new MZTolerance(0.005, 5), new MZTolerance(0.005, 10),
        new LipidAnnotationChainParameters());
    Assertions.assertFalse(results.isEmpty());
    final SpectrumTestResult best = results.getFirst();
    Assertions.assertEquals("PE 34:1", best.species());
    Assertions.assertEquals(best.rules().size(), best.matchedRules(), "all rules should match");
    Assertions.assertEquals(peaks.size() - 1, best.explainedPeaks());
    Assertions.assertTrue(best.annotations().stream()
            .anyMatch(a -> a.molecularSpecies() && a.annotation().equals("PE 16:0_18:1")),
        best.annotations().toString());
  }

  @Test
  void derivesBackboneOfPe() {
    final var result = LipidBackboneDeriver.derive(PE.getChainTypes(), "C39H76NO8P", 34, 1);
    Assertions.assertTrue(result.isOk(), result.error());
    Assertions.assertEquals("C5H14NO6P", result.formula());
  }

  @Test
  void derivingBackboneReportsImpossibleInput() {
    final var result = LipidBackboneDeriver.derive(PE.getChainTypes(), "C10H20O2", 34, 1);
    Assertions.assertFalse(result.isOk());
    Assertions.assertNotNull(result.error());
  }

  @Test
  void parsesCommonPeakListFormats() {
    Assertions.assertEquals(3, PeakListParser.parse("""
        m/z\tintensity
        196.0380\t1200
        255.2330 800
        281.2486,1e3
        """).length);
    final DataPoint[] library = PeakListParser.parse("196.038:1200 255.233:800");
    Assertions.assertEquals(2, library.length);
    Assertions.assertEquals(255.233, library[1].getMZ(), 1e-6);
    Assertions.assertEquals(100, PeakListParser.parse("281.2486")[0].getIntensity());
    Assertions.assertEquals(0, PeakListParser.parse("PEPMASS=716.52\nBEGIN IONS").length);
  }
}
