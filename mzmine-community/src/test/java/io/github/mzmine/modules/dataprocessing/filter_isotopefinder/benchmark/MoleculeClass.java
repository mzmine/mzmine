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

/**
 * Broad molecule size class used by the benchmark corpus. Controls the maximum charge searched and
 * the CDK minimum abundance used when generating the isotope pattern (larger molecules require a
 * higher abundance cutoff to keep the peak list bounded and generation fast).
 */
public enum MoleculeClass {

  // minAbundance also acts as the CDK peak-pruning floor: heavy poly-halogen envelopes (Cl/Br up to
  // 20) and large averagine formulas explode into huge fine-structure combs at very low cutoffs, so a
  // realistic high-res noise-floor cutoff keeps the peak lists (and generation time) bounded.
  SMALL(3, 0.003), PEPTIDE(5, 0.01), PROTEIN(20, 0.02);

  private final int maxCharge;
  private final double minAbundance;

  MoleculeClass(final int maxCharge, final double minAbundance) {
    this.maxCharge = maxCharge;
    this.minAbundance = minAbundance;
  }

  /**
   * Maximum charge the isotope finder should search for this molecule class.
   */
  public int maxCharge() {
    return maxCharge;
  }

  /**
   * CDK minimum relative abundance used when generating the isotope pattern for this class.
   */
  public double minAbundance() {
    return minAbundance;
  }
}
