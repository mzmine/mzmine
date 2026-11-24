package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.simulation;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleTarget;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.fragmentation.LipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tests a lipid class against a measured MS2 spectrum the same way the lipid annotation module
 * does: find species that match the precursor m/z, match all rules of the adduct and validate
 * species and molecular species annotations.
 */
public final class LipidSpectrumTester {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final RawDataFile DUMMY_FILE = RawDataFile.createDummyFile();
  private static final int MAX_CANDIDATES = 3;

  private LipidSpectrumTester() {
  }

  /**
   * @param lipidClass  the class to test
   * @param ionization  adduct of the precursor
   * @param precursorMz measured precursor m/z
   * @param peaks       measured MS2 peaks sorted by m/z
   * @param ms1Tol      tolerance to find species for the precursor
   * @param ms2Tol      tolerance to match fragments
   * @param chains      chain ranges, the same as in the lipid annotation module
   * @return results for the species closest to the precursor m/z, best first. Empty if no species
   * of the class matches the precursor.
   */
  public static @NotNull List<SpectrumTestResult> test(@NotNull ILipidClass lipidClass,
      @NotNull IonizationType ionization, double precursorMz, @NotNull DataPoint[] peaks,
      @NotNull MZTolerance ms1Tol, @NotNull MZTolerance ms2Tol,
      @NotNull LipidAnnotationChainParameters chains) {
    final List<Candidate> candidates = findCandidates(lipidClass, ionization, precursorMz, ms1Tol,
        chains);
    final Scan scan = createScan(ionization, peaks);
    final List<SpectrumTestResult> results = new ArrayList<>();
    for (final Candidate candidate : candidates) {
      results.add(testCandidate(lipidClass, ionization, precursorMz, candidate, scan, peaks,
          ms2Tol, chains));
    }
    return results;
  }

  /**
   * Species of the class that match the precursor m/z, closest first.
   */
  private static @NotNull List<Candidate> findCandidates(@NotNull ILipidClass lipidClass,
      @NotNull IonizationType ionization, double precursorMz, @NotNull MZTolerance ms1Tol,
      @NotNull LipidAnnotationChainParameters chains) {
    final int numChains = lipidClass.getChainTypes().length;
    final int minC = chains.getValue(LipidAnnotationChainParameters.minChainLength) * numChains;
    final int maxC = chains.getValue(LipidAnnotationChainParameters.maxChainLength) * numChains;
    final int minDbe = chains.getValue(LipidAnnotationChainParameters.minDBEs) * numChains;
    final int maxDbe = chains.getValue(LipidAnnotationChainParameters.maxDBEs) * numChains;
    final List<Candidate> candidates = new ArrayList<>();
    for (int c = Math.max(minC, numChains); c <= maxC; c++) {
      for (int dbe = minDbe; dbe <= maxDbe; dbe++) {
        final SpeciesLevelAnnotation lipid = LIPID_FACTORY.buildSpeciesLevelLipid(lipidClass, c,
            dbe, 0);
        if (lipid == null) {
          continue;
        }
        final Double mz = ionization.ionizeFormula(lipid.getMolecularFormula())
            .map(FormulaUtils::calculateMzRatio).orElse(null);
        if (mz != null && ms1Tol.checkWithinTolerance(mz, precursorMz)) {
          candidates.add(new Candidate(lipid, mz));
        }
      }
    }
    candidates.sort(Comparator.comparingDouble(c -> Math.abs(c.mz() - precursorMz)));
    return candidates.subList(0, Math.min(MAX_CANDIDATES, candidates.size()));
  }

  private static @NotNull SpectrumTestResult testCandidate(@NotNull ILipidClass lipidClass,
      @NotNull IonizationType ionization, double precursorMz, @NotNull Candidate candidate,
      @NotNull Scan scan, @NotNull DataPoint[] peaks, @NotNull MZTolerance ms2Tol,
      @NotNull LipidAnnotationChainParameters chains) {
    final LipidFragmentationRule[] rules = lipidClass.getFragmentationRules();
    final List<LipidFragment> fragments = new LipidFragmentFactory(ms2Tol, candidate.lipid(),
        ionization, rules, scan, chains).findLipidFragments();

    final List<RuleTestResult> ruleResults = new ArrayList<>();
    final Set<Double> explained = new HashSet<>();
    for (int i = 0; i < rules.length; i++) {
      final LipidFragmentationRule rule = rules[i];
      if (rule.getIonizationType() != ionization || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      final List<MatchedRuleFragment> matches = new ArrayList<>();
      for (final LipidFragment fragment : fragments) {
        if (fragment.getRuleType() == rule.getLipidFragmentationRuleType() && Objects.equals(
            fragment.getOriginatingRuleFormula(), rule.getMolecularFormula())) {
          matches.add(new MatchedRuleFragment(fragment.getMzExact(),
              fragment.getDataPoint().getMZ(), fragment.getDataPoint().getIntensity(),
              fragment.getIonFormula(), chainLabel(fragment)));
          explained.add(fragment.getDataPoint().getMZ());
        }
      }
      matches.sort(Comparator.comparingDouble(MatchedRuleFragment::intensity).reversed());
      ruleResults.add(new RuleTestResult(i, rule, matches));
    }

    final List<AnnotationTestResult> annotations = annotate(lipidClass, candidate, precursorMz,
        fragments, peaks, ms2Tol, ionization);
    final double ppm = (precursorMz - candidate.mz()) / candidate.mz() * 1e6;
    return new SpectrumTestResult(candidate.lipid().getAnnotation(), candidate.mz(), ppm,
        ruleResults, annotations, explained.size(), peaks.length);
  }

  /**
   * Same validation as the lipid annotation module.
   */
  private static @NotNull List<AnnotationTestResult> annotate(@NotNull ILipidClass lipidClass,
      @NotNull Candidate candidate, double precursorMz, @NotNull List<LipidFragment> fragments,
      @NotNull DataPoint[] peaks, @NotNull MZTolerance ms2Tol,
      @NotNull IonizationType ionization) {
    final List<AnnotationTestResult> annotations = new ArrayList<>();
    if (fragments.isEmpty()) {
      return annotations;
    }
    final LipidCategories category = lipidClass.getCoreClass();
    final Set<LipidFragment> fragmentSet = new HashSet<>(fragments);
    final MatchedLipid species = LipidAnnotationUtils.getSpeciesLevelMatchedLipidFactory(category)
        .validateSpeciesLevelAnnotation(precursorMz, candidate.lipid(), fragmentSet, peaks, 0d,
            ms2Tol, ionization);
    if (species != null) {
      annotations.add(toResult(species));
    }
    final var molecularFactory = LipidAnnotationUtils.getMolecularSpeciesLevelMatchedLipidFactory(
        category);
    final Set<MatchedLipid> predicted = molecularFactory.predictMolecularSpeciesLevelMatches(
        new HashSet<>(fragments), candidate.lipid(), precursorMz, peaks, 0d, ms2Tol, ionization);
    if (predicted != null) {
      for (final MatchedLipid match : predicted) {
        if (species != null) {
          match.getMatchedFragments().addAll(species.getMatchedFragments());
        }
        final MatchedLipid validated = molecularFactory.validateMolecularSpeciesLevelAnnotation(
            precursorMz, match.getLipidAnnotation(), match.getMatchedFragments(), peaks, 0d,
            ms2Tol, ionization);
        if (validated != null) {
          annotations.add(toResult(validated));
        }
      }
    }
    // the same molecular species can be predicted from different fragment combinations
    final Map<String, AnnotationTestResult> best = new LinkedHashMap<>();
    for (final AnnotationTestResult result : annotations) {
      best.merge(result.annotation(), result,
          (a, b) -> a.msmsScore() >= b.msmsScore() ? a : b);
    }
    final List<AnnotationTestResult> unique = new ArrayList<>(best.values());
    unique.sort(Comparator.comparingDouble(AnnotationTestResult::msmsScore).reversed());
    return unique;
  }

  private static @NotNull AnnotationTestResult toResult(@NotNull MatchedLipid match) {
    return new AnnotationTestResult(match.getLipidAnnotation().getAnnotation(),
        match.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation,
        Objects.requireNonNullElse(match.getMsMsScore(), 0d));
  }

  private static @Nullable String chainLabel(@NotNull LipidFragment fragment) {
    if (fragment.getChainLength() == null || fragment.getLipidChainType() == null) {
      return null;
    }
    final LipidChainType chainType = fragment.getLipidChainType();
    final LipidRuleTarget target = LipidRuleTarget.ofChainType(chainType);
    final LipidFragmentationRuleType type = fragment.getRuleType();
    final String name = target != null ? target.toString()
        : type != null ? type.getTarget().toString() : chainType.toString();
    return name + " " + fragment.getChainLength() + ":" + fragment.getNumberOfDBEs();
  }

  private static @NotNull Scan createScan(@NotNull IonizationType ionization,
      @NotNull DataPoint[] peaks) {
    final double[] mzs = new double[peaks.length];
    final double[] intensities = new double[peaks.length];
    for (int i = 0; i < peaks.length; i++) {
      mzs[i] = peaks[i].getMZ();
      intensities[i] = peaks[i].getIntensity();
    }
    final Scan scan = new SimpleScan(DUMMY_FILE, -1, 2, 0f, null, mzs, intensities,
        MassSpectrumType.CENTROIDED, ionization.getPolarity(), "Test spectrum", null);
    scan.addMassList(SimpleMassList.create(null, peaks));
    return scan;
  }

  private record Candidate(@NotNull SpeciesLevelAnnotation lipid, double mz) {

  }
}
