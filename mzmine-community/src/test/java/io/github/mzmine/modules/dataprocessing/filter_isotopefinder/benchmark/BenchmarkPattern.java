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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One line of the benchmark JSONL corpus: a synthetic isotope-pattern spectrum plus the generation
 * parameters and the ground truth used to score the isotope finder. Serialized/deserialized with
 * Jackson (records are supported natively by jackson-databind, matching JSON keys to record
 * component names).
 *
 * @param id                  stable unique id of this pattern
 * @param axis                dominant stressor of this pattern (e.g. charge, resolution_merged,
 *                            cutoff, noise, interference, polyhalogen, protein_highz, clean)
 * @param moleculeClass       {@link MoleculeClass} name (SMALL | PEPTIDE | PROTEIN)
 * @param sourceFormula       the neutral molecular formula the pattern was generated from
 * @param monoNeutralMass     neutral monoisotopic mass of the source formula
 * @param polarity            {@link io.github.mzmine.datamodel.PolarityType} name
 * @param trueCharge          the true charge state of the generated pattern
 * @param mergeWidth          CDK merge width used to generate the pattern
 * @param resolutionLabel     RESOLVED (fine structure kept) | MERGED (one peak per nominal offset)
 * @param minAbundance        CDK minimum abundance used to generate the pattern
 * @param cutoffFraction      intensity cutoff applied as a fraction of the base peak
 * @param nNoise              number of random noise peaks added
 * @param noiseSeed           RNG seed used for the noise peaks
 * @param nInterference       number of interference (decoy) peaks added
 * @param interferenceFormula formula of the interferent, or null when interference is off
 * @param interferenceSeed    seed used for the interference (recorded for reproducibility)
 * @param seed                stable base seed of this pattern
 * @param mz                  spectrum m/z values (sorted ascending), the finder's input
 * @param intensity           spectrum intensity values aligned with {@link #mz}
 * @param trueMonoMz          m/z of the monoisotopic ion (may be absent from the spectrum after a
 *                            cutoff removes it, e.g. protein humps)
 * @param trueOffsetsMz       m/z of the true isotope peaks present in the (degraded) spectrum
 * @param borderlineOffsetsMz subset of {@link #trueOffsetsMz} below 5% of the base peak
 * @param falseOffsetsMz      m/z of the injected false peaks (noise + interference)
 * @param elements            distinct element symbols present in the source formula
 * @param trueHeavyElements   {@link #elements} minus C and H
 */
public record BenchmarkPattern(@NotNull String id, @NotNull String axis,
                               @NotNull String moleculeClass, @NotNull String sourceFormula,
                               double monoNeutralMass, @NotNull String polarity, int trueCharge,
                               double mergeWidth, @NotNull String resolutionLabel,
                               double minAbundance, double cutoffFraction, int nNoise,
                               long noiseSeed, int nInterference,
                               @Nullable String interferenceFormula, long interferenceSeed,
                               long seed, @NotNull double[] mz, @NotNull double[] intensity,
                               double trueMonoMz, @NotNull double[] trueOffsetsMz,
                               @NotNull double[] borderlineOffsetsMz,
                               @NotNull double[] falseOffsetsMz, @NotNull String[] elements,
                               @NotNull String[] trueHeavyElements) {

}
