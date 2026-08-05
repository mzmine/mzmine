/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.GenerationConfig.FormulaSpec;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark.GenerationConfig.SweepVariant;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.util.FormulaUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Generates the benchmark JSONL corpus for the isotope finder. Iterates the catalog x charge x
 * sweep defined in {@link GenerationConfig}, generates a CDK isotope pattern, applies seeded
 * degradations (cutoff, noise, interference), records the ground truth, and writes one JSON object
 * per line.
 * <p>
 * The seeded degradations are deterministic: seeds are derived from a stable hash of each pattern id
 * (no {@code Math.random} / wall-clock). Run via
 * {@code ./gradlew :mzmine-community:generateBenchmarkCorpus}.
 * <p>
 * <b>The corpus is NOT bit-reproducible, however</b>, because the underlying CDK isotope-pattern
 * enumeration is not: calling {@link SyntheticSpectra#fromFormula} twice with identical arguments in
 * the same JVM yields m/z values differing by ~1e-13, and those differences accumulate through the
 * peak merging. Measured over a full regeneration, m/z drifted by at most ~1.3 ppb (1.26 uDa) - some
 * 1000x below the tightest m/z tolerance the corpus is scored with - but ~0.4 % of cases ended up
 * with a different peak count, because a borderline peak crossed the {@code minAbundance} cutoff.
 * The practical effect on the accuracy metrics is negligible (one axis moved by 0.002; the rest were
 * identical to four decimals), but do NOT expect a regeneration to reproduce the committed file
 * byte for byte, and do not write a test that asserts it does.
 * <p>
 * <b>The generated corpus is committed to the repository on purpose</b>
 * ({@code src/test/resources/isotopefinder/corpus/patterns.jsonl}, ~15 MB). Although it is fully
 * reproducible from this generator, the CDK isotopologue enumeration for the large proteins in the
 * catalog is expensive, so regenerating it on every build (or in CI) is not viable. Committing the
 * snapshot also pins the exact inputs the committed accuracy baselines were measured on, so a
 * baseline diff reflects an engine change and never a corpus change. Do not "optimise" it away by
 * generating it at build time.
 * <p>
 * Changing anything that affects the generated cases (the catalog, the sweep, a degradation op)
 * therefore requires regenerating BOTH the corpus and the baselines
 * ({@code isotopeBenchmark}), and the two must be committed together.
 */
public final class BenchmarkPatternGenerator {

  private static final Logger logger = Logger.getLogger(BenchmarkPatternGenerator.class.getName());

  private static final String POSITIVE = PolarityType.POSITIVE.name();

  private BenchmarkPatternGenerator() {
  }

  public static void main(@NotNull final String[] args) throws IOException {
    final Path out = args.length > 0 ? Path.of(args[0]) : defaultOutput();
    final List<BenchmarkPattern> patterns = generate();
    write(patterns, out);
    summarize(patterns, out);
  }

  /**
   * @return the committed corpus path, relative to the {@code mzmine-community} module directory.
   * Must stay in sync with the path the {@code generateBenchmarkCorpus} Gradle task passes.
   */
  @NotNull
  private static Path defaultOutput() {
    return Path.of("src", "test", "resources", "isotopefinder", "corpus",
        BenchmarkCorpusLoader.RESOURCE.substring(
            BenchmarkCorpusLoader.RESOURCE.lastIndexOf('/') + 1));
  }

  /**
   * Generate every benchmark pattern (catalog x charge x sweep).
   */
  @NotNull
  public static List<BenchmarkPattern> generate() {
    final List<FormulaSpec> catalog = GenerationConfig.catalog();

    // one CDK enumeration per UNIQUE formula (dedupe; the molecule class carries the minAbundance floor)
    final Map<String, FormulaSpec> uniqueByFormula = new LinkedHashMap<>();
    for (final FormulaSpec spec : catalog) {
      uniqueByFormula.putIfAbsent(spec.formula(), spec);
    }

    // warm up the CDK isotope factory single-threaded before the parallel enumeration below
    SyntheticSpectra.fromFormula("C", 1, GenerationConfig.RESOLVED_MERGE_WIDTH, 0.01);

    // Pre-compute the charge-1 isotope pattern for each unique formula ONCE, in parallel. CDK's
    // isotopologue enumeration/merging is charge-invariant (done in neutral-mass space; charge only
    // rescales m/z), so every charge state is derived cheaply from the cached charge-1 spectrum via
    // SyntheticSpectra.atCharge. This removes the per-charge CDK cost (the generation bottleneck) and
    // parallelises the remaining (expensive) enumerations across formulas, so large proteins are viable.
    final Map<String, SimpleMassSpectrum> baseResolved = new ConcurrentHashMap<>();
    final Map<String, SimpleMassSpectrum> baseMerged = new ConcurrentHashMap<>();
    // unit-resolution base (charge-1, one centroid per nominal offset) computed only for the classes
    // that get unit-resolution cases (SMALL / PEPTIDE); low-res instruments do not resolve intact
    // proteins, so those are skipped here.
    final Map<String, SimpleMassSpectrum> baseUnit = new ConcurrentHashMap<>();
    final AtomicInteger prepared = new AtomicInteger();
    final int uniqueCount = uniqueByFormula.size();
    uniqueByFormula.values().parallelStream().forEach(spec -> {
      final double minAbundance = spec.cls().minAbundance();
      baseResolved.put(spec.formula(),
          SyntheticSpectra.fromFormula(spec.formula(), 1, GenerationConfig.RESOLVED_MERGE_WIDTH,
              minAbundance));
      baseMerged.put(spec.formula(),
          SyntheticSpectra.fromFormula(spec.formula(), 1, GenerationConfig.MERGED_MERGE_WIDTH,
              minAbundance));
      if (hasUnitResolutionCases(spec.cls())) {
        baseUnit.put(spec.formula(),
            SyntheticSpectra.fromFormula(spec.formula(), 1, GenerationConfig.UNIT_MERGE_WIDTH,
                minAbundance));
      }
      logger.info("Prepared base pattern " + prepared.incrementAndGet() + "/" + uniqueCount + " "
          + spec.formula() + " [" + spec.cls() + "]");
    });

    // build every case deterministically in catalog order (the parallelism above only fills the caches
    // and does not affect the output order or values)
    final int sign = PolarityType.POSITIVE.getSign();
    // pool of co-eluting decoys for InterferenceMode.REALISTIC: SMALL molecules only, each with its
    // cached charge-1 resolved pattern. Deterministic order (catalog order).
    // assumption: the decoy is generated at the TARGET's charge, so for a highly charged protein
    // target the interferent is a small molecule's envelope at that charge - chemically unusual, but
    // the properties the axis tests are the ones that hold regardless: the decoy has a different
    // envelope shape from the target and sits off the isotope grid, so it must be rejected without
    // forming a clean doubled-charge comb.
    final List<DecoyCandidate> decoyPool = new ArrayList<>();
    for (final FormulaSpec spec : uniqueByFormula.values()) {
      if (spec.cls() == MoleculeClass.SMALL) {
        decoyPool.add(new DecoyCandidate(spec.formula(), baseResolved.get(spec.formula())));
      }
    }
    final List<BenchmarkPattern> result = new ArrayList<>();
    for (final FormulaSpec spec : catalog) {
      final IMolecularFormula formula = FormulaUtils.parse(spec.formula());
      if (formula == null) {
        throw new IllegalStateException("Could not parse catalog formula: " + spec.formula());
      }
      final double monoNeutralMass = FormulaUtils.getMonoisotopicMass(formula);
      final String[] elements = distinctSymbols(formula);
      final String[] heavy = heavyElements(elements);
      final int halogens =
          FormulaUtils.countElement(formula, "Cl") + FormulaUtils.countElement(formula, "Br");
      final double minAbundance = spec.cls().minAbundance();
      final SimpleMassSpectrum base1Resolved = baseResolved.get(spec.formula());
      final SimpleMassSpectrum base1Merged = baseMerged.get(spec.formula());

      for (final int charge : GenerationConfig.chargesFor(spec.cls(), monoNeutralMass)) {
        // derive the charge-z spectra from the cached charge-1 patterns (no CDK recompute)
        final SimpleMassSpectrum patResolved = SyntheticSpectra.atCharge(base1Resolved, charge,
            sign);
        final SimpleMassSpectrum patMerged = SyntheticSpectra.atCharge(base1Merged, charge, sign);
        final double trueMonoMz = monoIonMz(monoNeutralMass, charge, PolarityType.POSITIVE);

        int variantIndex = 0;
        for (final SweepVariant variant : GenerationConfig.sweep()) {
          // retired slots emit nothing but still consume their index, so every later variant keeps
          // the ids and seeds its cases were generated with
          if (!variant.isRetired()) {
            result.add(build(spec, charge, monoNeutralMass, elements, heavy, halogens, minAbundance,
                trueMonoMz, patResolved, patMerged, variant, variantIndex, decoyPool));
          }
          variantIndex++;
        }
      }

      // special axis: single-charge (z=1) unit / low mass resolution cases (SMALL / PEPTIDE only)
      if (hasUnitResolutionCases(spec.cls())) {
        final SimpleMassSpectrum base1Unit = baseUnit.get(spec.formula());
        final double unitMonoMz = monoIonMz(monoNeutralMass, 1, PolarityType.POSITIVE);
        int unitIndex = 0;
        for (final GenerationConfig.UnitVariant variant : GenerationConfig.unitResolutionSweep()) {
          result.add(
              buildUnitResolution(spec, monoNeutralMass, elements, heavy, minAbundance, unitMonoMz,
                  base1Unit, variant, unitIndex));
          unitIndex++;
        }
      }
    }
    return result;
  }

  /**
   * Whether a molecule class receives the special single-charge unit-resolution cases. Unit / low
   * resolution instruments (quadrupole / ion trap) target small molecules and peptides; intact
   * proteins are not resolved at unit resolution, so they are excluded.
   */
  private static boolean hasUnitResolutionCases(@NotNull final MoleculeClass cls) {
    return cls == MoleculeClass.SMALL || cls == MoleculeClass.PEPTIDE;
  }

  /**
   * Build one single-charge unit-resolution case: take the collapsed charge-1 envelope (one
   * centroid per nominal offset), apply the optional intensity cutoff, quantise it onto a coarse
   * low-accuracy m/z axis (seeded jitter + 1-decimal rounding), then add optional noise (kept a
   * tolerance-scaled distance from the true peaks). The ground-truth true peaks are the quantised
   * (post-cutoff, pre-noise) positions, so they match the wide unit-resolution tolerance the case
   * is scored with.
   */
  @NotNull
  private static BenchmarkPattern buildUnitResolution(@NotNull final FormulaSpec spec,
      final double monoNeutralMass, @NotNull final String[] elements, @NotNull final String[] heavy,
      final double minAbundance, final double trueMonoMz,
      @NotNull final SimpleMassSpectrum base1Unit,
      @NotNull final GenerationConfig.UnitVariant variant, final int variantIndex) {

    final String axis = GenerationConfig.UNIT_RESOLUTION_AXIS;
    final String id = spec.cls().name() + "_" + spec.formula() + "_z1_" + axis + "_" + variantIndex;
    final long noiseSeed = stableSeed(id + "#noise");
    final long jitterSeed = stableSeed(id + "#jitter");
    final long seed = stableSeed(id);

    final SimpleMassSpectrum cut = SyntheticSpectra.applyIntensityCutoff(base1Unit,
        variant.cutoffFraction());
    final SimpleMassSpectrum unit = SyntheticSpectra.quantizeUnitResolution(cut,
        variant.jitterMaxDa(), new Random(jitterSeed));
    final double[] trueOffsetsMz = SyntheticSpectra.mzArray(unit);
    final double[] borderlineOffsetsMz = weakPeaks(unit, 0.05 * SyntheticSpectra.baseHeight(unit));

    SimpleMassSpectrum current = unit;
    double[] noiseMz = new double[0];
    if (variant.nNoise() > 0 && unit.getNumberOfDataPoints() > 0) {
      final double lo = unit.getMzValue(0) - 0.5;
      final double hi = unit.getMzValue(unit.getNumberOfDataPoints() - 1) + 0.5;
      final InjectionResult nr = SyntheticSpectra.addRandomNoise(current, variant.nNoise(),
          variant.noiseMaxRel(), lo, hi, GroundTruthCase.toleranceForAxis(axis),
          new Random(noiseSeed));
      current = nr.spectrum();
      noiseMz = nr.injectedMz();
    }

    final double[] mz = SyntheticSpectra.mzArray(current);
    final double[] intensity = SyntheticSpectra.intensityArray(current);

    return new BenchmarkPattern(id, axis, spec.cls().name(), spec.formula(), monoNeutralMass,
        POSITIVE, 1, GenerationConfig.UNIT_MERGE_WIDTH, GenerationConfig.UNIT, minAbundance,
        variant.cutoffFraction(), variant.nNoise(), noiseSeed, 0, null, 0L, seed, mz, intensity,
        trueMonoMz, trueOffsetsMz, borderlineOffsetsMz, noiseMz, elements, heavy);
  }

  @NotNull
  private static BenchmarkPattern build(@NotNull final FormulaSpec spec, final int charge,
      final double monoNeutralMass, @NotNull final String[] elements, @NotNull final String[] heavy,
      final int halogens, final double minAbundance, final double trueMonoMz,
      @NotNull final SimpleMassSpectrum patResolved, @NotNull final SimpleMassSpectrum patMerged,
      @NotNull final SweepVariant variant, final int variantIndex,
      @NotNull final List<DecoyCandidate> decoyPool) {

    final SimpleMassSpectrum base =
        variant.resolutionLabel().equals(GenerationConfig.RESOLVED) ? patResolved : patMerged;
    final String axis =
        variant.axisHint() != null ? variant.axisHint() : structuralAxis(spec, charge, halogens);
    final String id =
        spec.cls().name() + "_" + spec.formula() + "_z" + charge + "_" + axis + "_" + variantIndex;
    final long seed = stableSeed(id);
    final long noiseSeed = stableSeed(id + "#noise");
    final long interferenceSeed = stableSeed(id + "#interf");

    // cutoff -> the true peaks that remain present in the (degraded) spectrum
    final SimpleMassSpectrum cut = SyntheticSpectra.applyIntensityCutoff(base,
        variant.cutoffFraction());
    final double[] trueOffsetsMz = SyntheticSpectra.mzArray(cut);
    final double[] borderlineOffsetsMz = weakPeaks(cut, 0.05 * SyntheticSpectra.baseHeight(cut));

    SimpleMassSpectrum current = cut;
    double[] noiseMz = new double[0];
    if (variant.nNoise() > 0 && base.getNumberOfDataPoints() > 0) {
      final double lo = base.getMzValue(0) - 0.5;
      final double hi = base.getMzValue(base.getNumberOfDataPoints() - 1) + 0.5;
      final InjectionResult nr = SyntheticSpectra.addRandomNoise(current, variant.nNoise(),
          variant.noiseMaxRel(), lo, hi, GroundTruthCase.toleranceForAxis(axis),
          new Random(noiseSeed));
      current = nr.spectrum();
      noiseMz = nr.injectedMz();
    }

    double[] interferenceMz = new double[0];
    String interferenceFormula = null;
    int nInterference = 0;
    final SimpleMassSpectrum decoy = switch (variant.interference()) {
      case NONE -> null;
      case REALISTIC -> realisticDecoy(base, charge, interferenceSeed, decoyPool);
    };
    if (decoy != null) {
      final InjectionResult ir = SyntheticSpectra.addInterference(current, decoy);
      current = ir.spectrum();
      interferenceMz = ir.injectedMz();
      interferenceFormula = decoyPoolFormula(interferenceSeed, decoyPool);
      nInterference = decoy.getNumberOfDataPoints();
    }

    final double[] falseOffsetsMz = concat(noiseMz, interferenceMz);
    final double[] mz = SyntheticSpectra.mzArray(current);
    final double[] intensity = SyntheticSpectra.intensityArray(current);

    return new BenchmarkPattern(id, axis, spec.cls().name(), spec.formula(), monoNeutralMass,
        POSITIVE, charge, variant.mergeWidth(), variant.resolutionLabel(), minAbundance,
        variant.cutoffFraction(), variant.nNoise(), noiseSeed, nInterference, interferenceFormula,
        interferenceSeed, seed, mz, intensity, trueMonoMz, trueOffsetsMz, borderlineOffsetsMz,
        falseOffsetsMz, elements, heavy);
  }

  /**
   * Build a realistic co-eluting interferent for {@link InterferenceMode#REALISTIC}: a DIFFERENT
   * small molecule from the catalog, placed at a seeded <i>non-harmonic</i> m/z offset relative to
   * the target and scaled to a seeded fraction of the target's intensity.
   * <p>
   * The fractional part of the offset (in units of the charge-adjusted 13C spacing) is drawn away
   * from both {@code 0} (which would put the decoy on the target's own isotope grid) and
   * {@code 0.5} (which would interleave a copy of the target's comb into a near-perfect doubled-charge
   * ladder). The decoy therefore overlaps the isotope search window and must be rejected, without
   * synthesising a harmonic that no real spectrum would produce.
   *
   * @param targetBase the target's (undegraded) spectrum at this charge.
   * @param charge     the target charge; the decoy is generated at the same charge.
   * @param seed       the case's interference seed.
   * @param pool       the candidate decoy compounds.
   * @return the decoy spectrum, or null when no decoy is available.
   */
  @Nullable
  private static SimpleMassSpectrum realisticDecoy(@NotNull final SimpleMassSpectrum targetBase,
      final int charge, final long seed, @NotNull final List<DecoyCandidate> pool) {
    if (pool.isEmpty() || targetBase.getNumberOfDataPoints() == 0) {
      return null;
    }
    final Random rnd = new Random(seed);
    final DecoyCandidate candidate = pool.get(poolIndex(seed, pool.size()));
    final SimpleMassSpectrum atCharge = SyntheticSpectra.atCharge(candidate.charge1(), charge,
        PolarityType.POSITIVE.getSign());
    if (atCharge.getNumberOfDataPoints() == 0) {
      return null;
    }

    final double spacing = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE / charge;
    // whole isotope steps: places the decoy anywhere across the target's envelope
    final int steps = rnd.nextInt(4);
    // fractional part in [0.15, 0.45) or [0.55, 0.85) - off the isotope grid, off the half-spacing
    final double frac =
        rnd.nextBoolean() ? 0.15 + 0.30 * rnd.nextDouble() : 0.55 + 0.30 * rnd.nextDouble();
    final double shift =
        targetBase.getMzValue(0) - atCharge.getMzValue(0) + (steps + frac) * spacing;
    final SimpleMassSpectrum shifted = SyntheticSpectra.shift(atCharge, shift);

    // realistic relative abundance: 10 %..100 % of the target base peak (the adversarial axis uses
    // 100 % of an identical envelope, which is the hardest possible case)
    final double targetHeight = SyntheticSpectra.baseHeight(targetBase);
    final double decoyHeight = SyntheticSpectra.baseHeight(shifted);
    if (decoyHeight <= 0d || targetHeight <= 0d) {
      return shifted;
    }
    final double relative = 0.1 + 0.9 * rnd.nextDouble();
    return SyntheticSpectra.scale(shifted, relative * targetHeight / decoyHeight);
  }

  /**
   * @return the formula of the decoy {@link #realisticDecoy} picks for this seed, for the ground
   * truth record.
   */
  @Nullable
  private static String decoyPoolFormula(final long seed,
      @NotNull final List<DecoyCandidate> pool) {
    return pool.isEmpty() ? null : pool.get(poolIndex(seed, pool.size())).formula();
  }

  /**
   * @return a stable, non-negative index into a pool of {@code size} entries.
   */
  private static int poolIndex(final long seed, final int size) {
    return (int) Math.floorMod(seed, size);
  }

  /**
   * A candidate co-eluting interferent: its formula and its cached charge-1 resolved pattern.
   */
  private record DecoyCandidate(@NotNull String formula, @NotNull SimpleMassSpectrum charge1) {

  }

  /**
   * The dominant structural stressor for a clean baseline pattern.
   */
  @NotNull
  private static String structuralAxis(@NotNull final FormulaSpec spec, final int charge,
      final int halogens) {
    if (spec.cls() == MoleculeClass.PROTEIN && charge > 3) {
      return "protein_highz";
    }
    if (halogens >= 2) {
      return "polyhalogen";
    }
    if (charge > 1) {
      return "charge";
    }
    return "clean";
  }

  /**
   * m/z of the monoisotopic ion, matching the electron-mass correction CDK applies.
   */
  private static double monoIonMz(final double neutralMass, final int charge,
      @NotNull final PolarityType polarity) {
    final double electronCorrection =
        polarity.getSign() * -1 * charge * IsotopePatternCalculator.ELECTRON_MASS;
    return (neutralMass + electronCorrection) / charge;
  }

  @NotNull
  private static double[] weakPeaks(@NotNull final SimpleMassSpectrum s, final double threshold) {
    final List<Double> weak = new ArrayList<>();
    for (int i = 0; i < s.getNumberOfDataPoints(); i++) {
      if (s.getIntensityValue(i) < threshold) {
        weak.add(s.getMzValue(i));
      }
    }
    final double[] out = new double[weak.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = weak.get(i);
    }
    return out;
  }

  @NotNull
  private static String[] distinctSymbols(@NotNull final IMolecularFormula formula) {
    final Set<String> symbols = new LinkedHashSet<>();
    for (final IIsotope iso : formula.isotopes()) {
      symbols.add(iso.getSymbol());
    }
    return symbols.toArray(new String[0]);
  }

  @NotNull
  private static String[] heavyElements(@NotNull final String[] elements) {
    final List<String> heavy = new ArrayList<>();
    for (final String symbol : elements) {
      if (!"C".equals(symbol) && !"H".equals(symbol)) {
        heavy.add(symbol);
      }
    }
    return heavy.toArray(new String[0]);
  }

  @NotNull
  private static double[] concat(@NotNull final double[] a, @NotNull final double[] b) {
    final double[] out = new double[a.length + b.length];
    System.arraycopy(a, 0, out, 0, a.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
  }

  /**
   * Stable 64-bit FNV-1a hash of a string (deterministic across runs and platforms).
   */
  private static long stableSeed(@NotNull final String s) {
    long h = 0xcbf29ce484222325L;
    for (int i = 0; i < s.length(); i++) {
      h ^= s.charAt(i);
      h *= 0x100000001b3L;
    }
    return h;
  }

  private static void write(@NotNull final List<BenchmarkPattern> patterns, @NotNull final Path out)
      throws IOException {
    final Path parent = out.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    final ObjectMapper mapper = new ObjectMapper();
    try (final BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE)) {
      for (final BenchmarkPattern p : patterns) {
        w.write(mapper.writeValueAsString(p));
        w.write("\n");
      }
    }
  }

  private static void summarize(@NotNull final List<BenchmarkPattern> patterns,
      @NotNull final Path out) {
    final TreeMap<String, Integer> perClass = new TreeMap<>();
    final TreeMap<String, Integer> perAxis = new TreeMap<>();
    int minCharge = Integer.MAX_VALUE;
    int maxCharge = Integer.MIN_VALUE;
    final TreeMap<String, int[]> chargeRangePerClass = new TreeMap<>();
    boolean invariantOk = true;
    @Nullable String invariantViolation = null;

    for (final BenchmarkPattern p : patterns) {
      perClass.merge(p.moleculeClass(), 1, Integer::sum);
      perAxis.merge(p.axis(), 1, Integer::sum);
      minCharge = Math.min(minCharge, p.trueCharge());
      maxCharge = Math.max(maxCharge, p.trueCharge());
      final int[] range = chargeRangePerClass.computeIfAbsent(p.moleculeClass(),
          k -> new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
      range[0] = Math.min(range[0], p.trueCharge());
      range[1] = Math.max(range[1], p.trueCharge());
      if (p.trueCharge() > 3 && !MoleculeClass.PROTEIN.name().equals(p.moleculeClass())) {
        invariantOk = false;
        invariantViolation = p.id();
      }
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("Benchmark corpus written to ").append(out.toAbsolutePath()).append('\n');
    sb.append("Total patterns: ").append(patterns.size()).append('\n');
    sb.append("Per molecule class: ").append(perClass).append('\n');
    sb.append("Per axis: ").append(perAxis).append('\n');
    sb.append("Charge range overall: ").append(minCharge).append("..").append(maxCharge)
        .append('\n');
    for (final var e : chargeRangePerClass.entrySet()) {
      sb.append("  ").append(e.getKey()).append(" charge range: ").append(e.getValue()[0])
          .append("..").append(e.getValue()[1]).append('\n');
    }
    sb.append("Invariant (trueCharge>3 => PROTEIN): ")
        .append(invariantOk ? "OK" : "VIOLATED by " + invariantViolation);
    logger.info(sb.toString());

    if (!invariantOk) {
      throw new IllegalStateException("High-charge invariant violated: " + invariantViolation
          + " has charge>3 but is not PROTEIN");
    }
  }
}
