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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The formula catalog, per-formula sweep, and charge rule for the benchmark corpus. Kept
 * data-driven so the generator only iterates {@code catalog() x chargesFor() x sweep()}.
 * <p>
 * The catalog is generated programmatically (loops only, deterministic - no {@code Math.random}) to
 * cover a broad element / molecule-class space: a rising halogen series (the core request), a CHNO
 * drug-like grid, S / P / Si / metal families, polyhalogenated aromatics, averagine peptides and
 * proteins, plus the original hand-picked named real compounds. The family sizes are tuned so the
 * total (formula x charge) pairs land the corpus near ~5000 cases (7 sweep variants each).
 */
public final class GenerationConfig {

  /**
   * Merge width that keeps the isotope fine structure resolved.
   */
  public static final double RESOLVED_MERGE_WIDTH = 0.00005;

  /**
   * Merge width that collapses fine structure to ~one peak per nominal isotope offset.
   */
  public static final double MERGED_MERGE_WIDTH = 0.05;

  /**
   * Merge width modelling a unit-resolution (quadrupole / ion-trap) instrument: everything within
   * ~half a nominal mass merges into a single centroid per nominal isotope offset.
   */
  public static final double UNIT_MERGE_WIDTH = 0.5;

  public static final String RESOLVED = "RESOLVED";
  public static final String MERGED = "MERGED";
  public static final String UNIT = "UNIT";

  /**
   * Axis label for the special single-charge unit-resolution (low mass resolution) cases.
   */
  public static final String UNIT_RESOLUTION_AXIS = "unit_resolution";

  // Carbon sizes for the CHNO drug-like grid. Grid = these x N x O; sized to help hit the target.
  private static final int[] CHNO_CARBONS = {14, 20, 26, 32, 40, 48, 56, 64};
  private static final int[] CHNO_NITROGEN = {0, 2, 5, 8};
  private static final int[] CHNO_OXYGEN = {1, 4, 8, 14};

  private GenerationConfig() {
  }

  /**
   * A catalog entry: a neutral formula and its molecule class.
   */
  public record FormulaSpec(@NotNull String formula, @NotNull MoleculeClass cls) {

  }

  /**
   * One sweep variant applied to every (formula x charge). {@code axisHint} names the applied
   * stressor; when null the generator derives the structural axis (charge / polyhalogen /
   * protein_highz / clean).
   *
   * @param axisHint        the stressor label, or null to use the structural axis
   * @param resolutionLabel RESOLVED or MERGED
   * @param mergeWidth      CDK merge width for this resolution
   * @param cutoffFraction  intensity cutoff as a fraction of the base peak
   * @param nNoise          number of random noise peaks to add
   * @param noiseMaxRel     max noise intensity as a fraction of the base peak
   * @param interference    whether to add a co-eluting decoy interferent
   */
  public record SweepVariant(String axisHint, @NotNull String resolutionLabel, double mergeWidth,
                             double cutoffFraction, int nNoise, double noiseMaxRel,
                             boolean interference) {

  }

  /**
   * Formula catalog covering the important elements and molecule classes. Built programmatically
   * from per-family helpers so the corpus can scale to ~5000 cases while staying deterministic.
   */
  @NotNull
  public static List<FormulaSpec> catalog() {
    final List<FormulaSpec> list = new ArrayList<>();
    addNamedReal(list);
    addChlorinatedSeries(list);
    addBrominatedSeries(list);
    addMixedHalogenSeries(list);
    addChlorinatedOxygenSeries(list);
    addPolyhalogenatedAromatics(list);
    addChnoGrid(list);
    addSulfurFamily(list);
    addPhosphorusFamily(list);
    addSiliconFamily(list);
    addMetalFamily(list);
    addPeptides(list);
    addProteins(list);
    return list;
  }

  /**
   * The original hand-picked named real compounds, so named real chemistry stays represented.
   */
  private static void addNamedReal(@NotNull final List<FormulaSpec> list) {
    // SMALL: CHNO, polyhalogens (Cl/Br), Cu, Si, P+S
    list.add(new FormulaSpec("C8H10N4O2", MoleculeClass.SMALL));      // caffeine
    list.add(new FormulaSpec("C33H40N2O9", MoleculeClass.SMALL));     // reserpine
    list.add(new FormulaSpec("C6Cl6", MoleculeClass.SMALL));          // hexachlorobenzene
    list.add(new FormulaSpec("C14H9Cl5", MoleculeClass.SMALL));       // DDT
    list.add(new FormulaSpec("C12Br10O", MoleculeClass.SMALL));       // BDE-209
    list.add(new FormulaSpec("C40H60N2O3S", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C10H4Cl2Br2", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C32Cl16CuN8", MoleculeClass.SMALL));    // Pigment Green 7
    list.add(new FormulaSpec("C8H20O4Si", MoleculeClass.SMALL));      // a siloxane (Si)
    list.add(new FormulaSpec("C10H14NO5PS", MoleculeClass.SMALL));    // an organophosphate (P + S)
    // PEPTIDE
    list.add(new FormulaSpec("C50H71N13O12", MoleculeClass.PEPTIDE)); // angiotensin II
    list.add(new FormulaSpec("C63H98N18O13S", MoleculeClass.PEPTIDE));// substance P
    // PROTEIN
    list.add(new FormulaSpec("C257H383N65O77S6", MoleculeClass.PROTEIN));  // insulin
    list.add(new FormulaSpec("C378H629N105O118S", MoleculeClass.PROTEIN)); // ubiquitin
    list.add(new FormulaSpec("C600H900N150O180S3", MoleculeClass.PROTEIN));// ~13 kDa synthetic
  }

  /**
   * Chlorinated series: for n=1..20 a compound {@code C{20+4n}H{...}Cl{n}} where the carbon count
   * is >20 and rises with n; halogens displace H. The core of the user's request.
   */
  private static void addChlorinatedSeries(@NotNull final List<FormulaSpec> list) {
    for (int n = 1; n <= 20; n++) {
      final int c = 20 + 4 * n;
      final int h = Math.max(1, (int) Math.round(1.4 * c) - n);
      final StringBuilder sb = new StringBuilder();
      appendElement(sb, "C", c);
      appendElement(sb, "H", h);
      appendElement(sb, "Cl", n);
      list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
    }
  }

  /**
   * Brominated series: for n=1..20 a compound {@code C{20+4n}H{...}Br{n}}, carbons >20 and rising.
   */
  private static void addBrominatedSeries(@NotNull final List<FormulaSpec> list) {
    for (int n = 1; n <= 20; n++) {
      final int c = 20 + 4 * n;
      final int h = Math.max(1, (int) Math.round(1.4 * c) - n);
      final StringBuilder sb = new StringBuilder();
      appendElement(sb, "C", c);
      appendElement(sb, "H", h);
      appendElement(sb, "Br", n);
      list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
    }
  }

  /**
   * Mixed halogen series: for n=1..10 a compound {@code C{20+5n}H{...}Cl{n}Br{n}}, carbons >20 and
   * rising with the total heteroatom count.
   */
  private static void addMixedHalogenSeries(@NotNull final List<FormulaSpec> list) {
    for (int n = 1; n <= 10; n++) {
      final int c = 20 + 5 * n;
      final int h = Math.max(1, (int) Math.round(1.4 * c) - 2 * n);
      final StringBuilder sb = new StringBuilder();
      appendElement(sb, "C", c);
      appendElement(sb, "H", h);
      appendElement(sb, "Cl", n);
      appendElement(sb, "Br", n);
      list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
    }
  }

  /**
   * Oxygenated chlorinated series (ether / dioxin-like realism): for n=1..15 a compound
   * {@code C{20+4n}H{...}Cl{n}O2}, carbons >20 and rising with n.
   */
  private static void addChlorinatedOxygenSeries(@NotNull final List<FormulaSpec> list) {
    for (int n = 1; n <= 15; n++) {
      final int c = 20 + 4 * n;
      final int h = Math.max(1, (int) Math.round(1.4 * c) - n);
      final StringBuilder sb = new StringBuilder();
      appendElement(sb, "C", c);
      appendElement(sb, "H", h);
      appendElement(sb, "Cl", n);
      appendElement(sb, "O", 2);
      list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
    }
  }

  /**
   * Realistic polyhalogenated aromatics: PCB-like {@code C12H{10-n}Cl{n}}, PBDE-like
   * {@code C12H{10-n}Br{n}O}, plus a few chlorinated dioxins / furans.
   */
  private static void addPolyhalogenatedAromatics(@NotNull final List<FormulaSpec> list) {
    for (int n = 1; n <= 10; n++) {
      final StringBuilder pcb = new StringBuilder();
      appendElement(pcb, "C", 12);
      appendElement(pcb, "H", 10 - n);
      appendElement(pcb, "Cl", n);
      list.add(new FormulaSpec(pcb.toString(), MoleculeClass.SMALL));
    }
    for (int n = 1; n <= 10; n++) {
      final StringBuilder pbde = new StringBuilder();
      appendElement(pbde, "C", 12);
      appendElement(pbde, "H", 10 - n);
      appendElement(pbde, "Br", n);
      appendElement(pbde, "O", 1);
      list.add(new FormulaSpec(pbde.toString(), MoleculeClass.SMALL));
    }
    // chlorinated dibenzo-dioxins / furans (TCDD, OCDD, TCDF, OCDF, DiCDD)
    list.add(new FormulaSpec("C12H4Cl4O2", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C12Cl8O2", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C12H4Cl4O", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C12Cl8O", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C12H6Cl2O2", MoleculeClass.SMALL));
  }

  /**
   * CHNO drug-like grid: carbons x N x O, with H = round(1.5*c).
   */
  private static void addChnoGrid(@NotNull final List<FormulaSpec> list) {
    for (final int c : CHNO_CARBONS) {
      final int h = (int) Math.round(1.5 * c);
      for (final int n : CHNO_NITROGEN) {
        for (final int o : CHNO_OXYGEN) {
          final StringBuilder sb = new StringBuilder();
          appendElement(sb, "C", c);
          appendElement(sb, "H", h);
          appendElement(sb, "N", n);
          appendElement(sb, "O", o);
          list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
        }
      }
    }
  }

  /**
   * Sulfur family: CHNO backbones with S in {1,2,4,6} at several carbon sizes.
   */
  private static void addSulfurFamily(@NotNull final List<FormulaSpec> list) {
    final int[] carbons = {16, 24, 34, 44, 54};
    final int[] sulfur = {1, 2, 4, 6};
    for (final int c : carbons) {
      final int h = (int) Math.round(1.5 * c);
      final int n = Math.max(1, c / 12);
      for (final int s : sulfur) {
        final StringBuilder sb = new StringBuilder();
        appendElement(sb, "C", c);
        appendElement(sb, "H", h);
        appendElement(sb, "N", n);
        appendElement(sb, "O", 2);
        appendElement(sb, "S", s);
        list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
      }
    }
  }

  /**
   * Phosphorus family: P in {1,2,3} (+ O, and S on the larger ones) at a few carbon sizes.
   */
  private static void addPhosphorusFamily(@NotNull final List<FormulaSpec> list) {
    final int[] carbons = {10, 18, 26, 34};
    final int[] phosphorus = {1, 2, 3};
    for (final int c : carbons) {
      final int h = (int) Math.round(1.6 * c);
      for (final int p : phosphorus) {
        final StringBuilder sb = new StringBuilder();
        appendElement(sb, "C", c);
        appendElement(sb, "H", h);
        appendElement(sb, "O", 2 * p);
        appendElement(sb, "P", p);
        // larger backbones carry a sulfur (thiophosphate-like) for M+2 heavy-isotope diversity
        appendElement(sb, "S", c >= 26 ? 1 : 0);
        list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
      }
    }
  }

  /**
   * Silicon family: Si in {1,2,3,4} with C/H/O (siloxane-like), a few backbone sizes each.
   */
  private static void addSiliconFamily(@NotNull final List<FormulaSpec> list) {
    for (int si = 1; si <= 4; si++) {
      for (int k = 0; k < 3; k++) {
        final int c = 2 * si + 4 * k + 2;
        final int h = (int) Math.round(2.5 * c);
        final StringBuilder sb = new StringBuilder();
        appendElement(sb, "C", c);
        appendElement(sb, "H", h);
        appendElement(sb, "O", si);
        appendElement(sb, "Si", si);
        list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
      }
    }
  }

  /**
   * Metal / organometallic family: Fe, Cu, Zn, Ni, Mg, B (1-2 atoms) on a C/H/N/O(/Cl) backbone.
   * These stress M+2 / heavy-isotope handling with non-halogen heavy elements.
   */
  private static void addMetalFamily(@NotNull final List<FormulaSpec> list) {
    final String[] metals = {"Fe", "Cu", "Zn", "Ni", "Mg", "B"};
    for (int i = 0; i < metals.length; i++) {
      for (int m = 1; m <= 2; m++) {
        final int c = 10 + 4 * i;
        final int h = (int) Math.round(1.5 * c);
        final StringBuilder sb = new StringBuilder();
        appendElement(sb, "C", c);
        appendElement(sb, "H", h);
        appendElement(sb, "N", 2 * m);
        appendElement(sb, "O", 2 * m);
        // alternate backbones carry chlorine (metal halide complexes) for heavier M+2 patterns
        appendElement(sb, "Cl", i % 2 == 0 ? 0 : 2);
        appendElement(sb, metals[i], m);
        list.add(new FormulaSpec(sb.toString(), MoleculeClass.SMALL));
      }
    }
    // a few extra mixed metal + halide complexes
    list.add(new FormulaSpec("C6H6N4CuCl2", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C10H8N2O2Fe", MoleculeClass.SMALL));
    list.add(new FormulaSpec("C14H10O4Zn2", MoleculeClass.SMALL));
  }

  /**
   * Averagine-like peptides of increasing length (residues 8..59). Per residue: C=4.94, H=7.76,
   * N=1.36, O=1.48, S=0.04. Charges 1..3.
   */
  private static void addPeptides(@NotNull final List<FormulaSpec> list) {
    for (int r = 8; r <= 60; r += 3) {
      list.add(new FormulaSpec(averagineFormula(r), MoleculeClass.PEPTIDE));
    }
  }

  /**
   * Averagine proteins for masses ~5-20 kDa (residues ~48..176). {@code min(20, round(mass/700))}
   * yields high charges up to ~20. Each formula's (expensive) CDK enumeration runs once and in
   * parallel across formulas (see {@code BenchmarkPatternGenerator}), so this larger, denser
   * protein range stays tractable.
   */
  private static void addProteins(@NotNull final List<FormulaSpec> list) {
    for (int r = 48; r <= 176; r += 8) {
      list.add(new FormulaSpec(averagineFormula(r), MoleculeClass.PROTEIN));
    }
  }

  /**
   * Build an averagine formula for {@code residues} residues (C/H/N/O/S), keeping S >= 0.
   */
  @NotNull
  private static String averagineFormula(final int residues) {
    final int c = (int) Math.round(4.94 * residues);
    final int h = (int) Math.round(7.76 * residues);
    final int n = (int) Math.round(1.36 * residues);
    final int o = (int) Math.round(1.48 * residues);
    final int s = Math.max(0, (int) Math.round(0.04 * residues));
    final StringBuilder sb = new StringBuilder();
    appendElement(sb, "C", c);
    appendElement(sb, "H", h);
    appendElement(sb, "N", n);
    appendElement(sb, "O", o);
    appendElement(sb, "S", s);
    return sb.toString();
  }

  /**
   * Append {@code symbol}{@code count} to the formula, omitting the element when count &lt;= 0 and
   * omitting the count when it is 1 (matching the hand-written formula convention).
   */
  private static void appendElement(@NotNull final StringBuilder sb, @NotNull final String symbol,
      final int count) {
    if (count <= 0) {
      return;
    }
    sb.append(symbol);
    if (count > 1) {
      sb.append(count);
    }
  }

  /**
   * The per-formula sweep. Pruned from the full cross product to keep the corpus to a few hundred
   * lines while still covering both resolutions and a couple of cutoff / noise / interference
   * variants for every (moleculeClass x charge).
   */
  @NotNull
  public static List<SweepVariant> sweep() {
    return List.of(
        // baseline: clean, high resolution -> structural axis (charge / polyhalogen / protein_highz / clean)
        new SweepVariant(null, RESOLVED, RESOLVED_MERGE_WIDTH, 0.0, 0, 0.0, false),
        // merged fine structure
        new SweepVariant("resolution_merged", MERGED, MERGED_MERGE_WIDTH, 0.0, 0, 0.0, false),
        // intensity cutoff removes the weak tail (and the low mono of humps)
        new SweepVariant("cutoff", RESOLVED, RESOLVED_MERGE_WIDTH, 0.05, 0, 0.0, false),
        // low random noise
        new SweepVariant("noise", RESOLVED, RESOLVED_MERGE_WIDTH, 0.0, 3, 0.02, false),
        // high random noise
        new SweepVariant("noise", RESOLVED, RESOLVED_MERGE_WIDTH, 0.0, 8, 0.05, false),
        // co-eluting interferent
        new SweepVariant("interference", RESOLVED, RESOLVED_MERGE_WIDTH, 0.0, 0, 0.0, true),
        // combined stressors: small cutoff + noise + interference
        new SweepVariant("combined", RESOLVED, RESOLVED_MERGE_WIDTH, 0.01, 3, 0.02, true));
  }

  /**
   * One unit-resolution sweep variant, applied to every SMALL / PEPTIDE formula at charge 1 only.
   * Unit resolution collapses all fine structure to one centroid per nominal offset and reports it
   * on a coarse, low-accuracy m/z axis, so the special stressors here are the collapsed envelope
   * plus an m/z jitter (mass-accuracy error) - not the high-res cutoff/noise levels of
   * {@link #sweep()}.
   *
   * @param jitterMaxDa    max absolute m/z jitter applied to every peak (models low mass accuracy)
   * @param cutoffFraction intensity cutoff as a fraction of the base peak
   * @param nNoise         number of random noise peaks to add
   * @param noiseMaxRel    max noise intensity as a fraction of the base peak
   */
  public record UnitVariant(double jitterMaxDa, double cutoffFraction, int nNoise,
                            double noiseMaxRel) {

  }

  /**
   * The per-formula unit-resolution sweep. All variants share the {@link #UNIT_RESOLUTION_AXIS}
   * axis; together they span a clean coarse readout, a poor-mass-accuracy jitter, and a
   * jitter+noise combination.
   */
  @NotNull
  public static List<UnitVariant> unitResolutionSweep() {
    return List.of(
        // clean unit-resolution readout: collapsed envelope on a coarse (2-decimal) m/z axis
        new UnitVariant(0.0, 0.0, 0, 0.0),
        // poor mass accuracy: every centroid jittered by up to +-0.1 Da
        new UnitVariant(0.1, 0.0, 0, 0.0),
        // moderate jitter plus a few random noise peaks
        new UnitVariant(0.05, 0.0, 3, 0.05));
  }

  /**
   * The charge states to generate for a spec. High charges (&gt;3) are produced only for PROTEIN.
   */
  @NotNull
  public static int[] chargesFor(@NotNull final MoleculeClass cls, final double monoNeutralMass) {
    return switch (cls) {
      case SMALL -> new int[]{1, 2};
      case PEPTIDE -> new int[]{1, 2, 3};
      case PROTEIN -> proteinCharges(monoNeutralMass);
    };
  }

  /**
   * Several charge states spread across {@code round(mass/1500)..min(20, round(mass/700))},
   * deduped, at least 1. Yields ~4 charges for a typical protein and stays high (&gt;3).
   */
  @NotNull
  private static int[] proteinCharges(final double monoNeutralMass) {
    final int hi = Math.min(20, Math.max(1, (int) Math.round(monoNeutralMass / 700.0)));
    final int lo = Math.max(1, Math.min(hi, (int) Math.round(monoNeutralMass / 1500.0)));
    final Set<Integer> charges = new LinkedHashSet<>();
    final double[] fractions = {0.0, 1.0 / 3.0, 2.0 / 3.0, 1.0};
    for (final double f : fractions) {
      final int z = (int) Math.round(lo + f * (hi - lo));
      charges.add(Math.max(1, z));
    }
    final List<Integer> sorted = new ArrayList<>(charges);
    sorted.sort(Integer::compareTo);
    final int[] out = new int[sorted.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = sorted.get(i);
    }
    return out;
  }
}
