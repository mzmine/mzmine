package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

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
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleTarget;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.LipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates the ions each fragmentation rule of a lipid class produces for an example lipid. Uses
 * the same fragment calculation as the lipid annotation.
 */
public final class LipidRuleSimulator {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  // decision: a huge absolute tolerance around a single peak matches every theoretical fragment
  private static final MZTolerance MATCH_ALL = new MZTolerance(1_000_000, 0);
  private static final RawDataFile DUMMY_FILE = RawDataFile.createDummyFile();

  private LipidRuleSimulator() {
  }

  /**
   * @param lipidClass a complete lipid class
   * @param example    chains of the example lipid, same length as the chain types
   * @return the example lipid or null if it cannot be built, e.g., invalid backbone
   */
  public static @Nullable SpeciesLevelAnnotation exampleLipid(@NotNull ILipidClass lipidClass,
      @NotNull ExampleLipidChains example) {
    return LIPID_FACTORY.buildSpeciesLevelLipid(lipidClass, example.totalCarbons(),
        example.totalDbes(), 0);
  }

  /**
   * @param lipidClass a complete lipid class
   * @param example    chains of the example lipid, same length as the chain types
   * @return the ions of each rule in the order of the class rules. Empty lists for rules that
   * produce no ion, e.g., MS1 only rules or chain rules for chains the class does not have.
   */
  public static @NotNull List<List<SimulatedIon>> simulate(@NotNull ILipidClass lipidClass,
      @NotNull ExampleLipidChains example) {
    final LipidFragmentationRule[] rules = lipidClass.getFragmentationRules();
    final List<List<SimulatedIon>> result = new ArrayList<>();
    final SpeciesLevelAnnotation lipid = exampleLipid(lipidClass, example);
    for (final LipidFragmentationRule rule : rules) {
      result.add(lipid == null ? List.of() : simulateRule(lipidClass, lipid, rule, example));
    }
    return result;
  }

  /**
   * @return the precursor m/z of the example lipid for the ionization or null if the ionization
   * cannot be applied
   */
  public static @Nullable Double precursorMz(@NotNull SpeciesLevelAnnotation lipid,
      @NotNull IonizationType ionization) {
    return ionization.ionizeFormula(lipid.getMolecularFormula())
        .map(FormulaUtils::calculateMzRatio).orElse(null);
  }

  private static @NotNull List<SimulatedIon> simulateRule(@NotNull ILipidClass lipidClass,
      @NotNull SpeciesLevelAnnotation lipid, @NotNull LipidFragmentationRule rule,
      @NotNull ExampleLipidChains example) {
    final LipidFragmentationRuleType type = rule.getLipidFragmentationRuleType();
    if (type == null) {
      return List.of();
    }
    final LipidChainType chainType = type.getChainType();
    final Set<String> allowedChains = allowedChains(lipidClass.getChainTypes(), example,
        chainType);
    if (chainType != null && allowedChains.isEmpty()) {
      return List.of();
    }

    final LipidFragmentFactory factory = new LipidFragmentFactory(MATCH_ALL, lipid,
        rule.getIonizationType(), new LipidFragmentationRule[]{rule},
        dummyScan(rule.getPolarityType()), chainRange(example));
    final List<SimulatedIon> ions = new ArrayList<>();
    final Set<Long> seen = new HashSet<>();
    for (final LipidFragment fragment : factory.findLipidFragments()) {
      final String chain = fragment.getChainLength() == null ? null
          : fragment.getChainLength() + ":" + fragment.getNumberOfDBEs();
      if (chainType != null && (chain == null || !allowedChains.contains(chain))) {
        continue;
      }
      // the same fragment can be produced by identical chains, keep one
      if (seen.add(Math.round(fragment.getMzExact() * 1e5))) {
        ions.add(new SimulatedIon(fragment.getMzExact(), fragment.getIonFormula(),
            chain == null ? null : label(chainType) + " " + chain));
      }
    }
    ions.sort(Comparator.comparingDouble(SimulatedIon::mz));
    return ions;
  }

  /**
   * @return chains of the example as "C:DBE" that a rule of this chain type refers to
   */
  private static @NotNull Set<String> allowedChains(@NotNull LipidChainType[] chainTypes,
      @NotNull ExampleLipidChains example, @Nullable LipidChainType ruleChainType) {
    final Set<String> allowed = new HashSet<>();
    if (ruleChainType == null) {
      return allowed;
    }
    if (ruleChainType == LipidChainType.TWO_ACYL_CHAINS_COMBINED) {
      // every pair of acyl chains
      for (int i = 0; i < chainTypes.length; i++) {
        for (int j = i + 1; j < chainTypes.length; j++) {
          if (chainTypes[i] == LipidChainType.ACYL_CHAIN
              && chainTypes[j] == LipidChainType.ACYL_CHAIN) {
            allowed.add((example.carbons()[i] + example.carbons()[j]) + ":" + (example.dbes()[i]
                + example.dbes()[j]));
          }
        }
      }
      return allowed;
    }
    for (int i = 0; i < chainTypes.length; i++) {
      if (chainTypes[i] == ruleChainType) {
        allowed.add(example.carbons()[i] + ":" + example.dbes()[i]);
      }
    }
    return allowed;
  }

  private static @NotNull String label(@Nullable LipidChainType chainType) {
    final LipidRuleTarget target = chainType == null ? null : LipidRuleTarget.ofChainType(chainType);
    return target == null ? "Chain" : target.toString();
  }

  /**
   * Chain ranges that cover all example chains. Fragments of other chains are filtered afterwards.
   */
  private static @NotNull LipidAnnotationChainParameters chainRange(
      @NotNull ExampleLipidChains example) {
    final LipidAnnotationChainParameters params = (LipidAnnotationChainParameters) new LipidAnnotationChainParameters().cloneParameterSet();
    final int minC = Arrays.stream(example.carbons()).min().orElse(0);
    final int maxC = Arrays.stream(example.carbons()).max().orElse(0);
    final int minDbe = Arrays.stream(example.dbes()).min().orElse(0);
    final int maxDbe = Arrays.stream(example.dbes()).max().orElse(0);
    params.setParameter(LipidAnnotationChainParameters.minChainLength, minC);
    params.setParameter(LipidAnnotationChainParameters.maxChainLength, maxC);
    params.setParameter(LipidAnnotationChainParameters.minDBEs, minDbe);
    params.setParameter(LipidAnnotationChainParameters.maxDBEs, maxDbe);
    params.setParameter(LipidAnnotationChainParameters.onlySearchForEvenChainLength, false);
    return params;
  }

  private static @NotNull Scan dummyScan(@NotNull PolarityType polarity) {
    final Scan scan = new SimpleScan(DUMMY_FILE, -1, 2, 0f, null, new double[]{500},
        new double[]{100}, MassSpectrumType.CENTROIDED, polarity, "Simulation", null);
    scan.addMassList(SimpleMassList.create(null, new SimpleDataPoint[]{new SimpleDataPoint(500, 100)}));
    return scan;
  }
}
