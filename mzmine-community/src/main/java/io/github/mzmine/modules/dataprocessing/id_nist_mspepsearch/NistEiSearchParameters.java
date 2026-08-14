/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.RIColumn;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;

/**
 * Options for the low resolution EI searches ({@link NistSearchMode#GC_EI_IDENTITY},
 * {@link NistSearchMode#GC_EI_SIMILARITY} and {@link NistSearchMode#GC_EI_HYBRID}).
 */
public class NistEiSearchParameters extends SimpleParameterSet {

  public static final OptionalParameter<ComboParameter<IntegerMode>> integerMz = new OptionalParameter<>(
      new ComboParameter<>("Integer m/z", """
          Merge fractional m/z to unit mass before searching, as the NIST EI libraries are unit mass.
          Only needed if your spectra are not already centroided to unit mass.""",
          IntegerMode.values(), IntegerMode.SUM), false);

  /*
   * MSPepSearch's "penalize rare compounds" flag (p) is deliberately not exposed. Its help states it
   * only applies to the main and replicate libraries of 2020 or earlier plus NIST 23, and against a
   * NIST 26 installation it terminates the process with an access violation (exit code
   * 0xC0000005) instead of reporting an error.
   */

  public static final OptionalModuleParameter<NistRetentionIndexParameters> retentionIndex = new OptionalModuleParameter<>(
      "Use retention index", """
      Use GC retention indices during the search (MSPepSearch /RI).
      The library retention index is stored on the annotation, and the difference to the measured \
      retention index of the row is shown in the delta RI column. Requires the rows to have a \
      retention index.""", new NistRetentionIndexParameters(), false);

  public NistEiSearchParameters() {
    super(integerMz, retentionIndex);
  }

  /**
   * The {@code /RI} sub options. The tolerance and column type are also used to compute the delta RI
   * of the resulting annotations.
   */
  public static class NistRetentionIndexParameters extends SimpleParameterSet {

    public static final ComboParameter<RIColumn> column = new ComboParameter<>("Column type", """
        The GC column type whose retention index is used.
        Semipolar corresponds to the semi-standard non-polar column, which is what most NIST \
        retention indices were measured on.""",
        new RIColumn[]{RIColumn.SEMIPOLAR, RIColumn.NONPOLAR, RIColumn.POLAR}, RIColumn.SEMIPOLAR);

    public static final BooleanParameter overrideFromSpectrum = new BooleanParameter(
        "Spectrum overrides column type", """
        MSPepSearch o: a column type given in the query spectrum overrides the selection above.""",
        false);

    public static final BooleanParameter useOtherNonPolar = new BooleanParameter(
        "Fall back to other non-polar column", """
        MSPepSearch a: use the other non-polar column type when the selected one has no data.""",
        false);

    public static final BooleanParameter assumeUnspecified = new BooleanParameter(
        "Assume unspecified column type matches", """
        MSPepSearch u: treat user library spectra without a column type as if they were measured on \
        the selected column.""", true);

    public static final OptionalParameter<ComboParameter<RIPenaltyRate>> penalty = new OptionalParameter<>(
        new ComboParameter<>("Penalize match factor", """
            Reduce the match factor of hits whose retention index is outside the tolerance \
            (MSPepSearch rXX).
            Leave this off to report retention indices without letting them change the score.""",
            RIPenaltyRate.values(), RIPenaltyRate.AVERAGE), true);

    public static final ComboParameter<Integer> tolerance = new ComboParameter<>("RI tolerance",
        "MSPepSearch tNN: the retention index tolerance, in retention index units.",
        new Integer[]{5, 10, 20, 30, 50, 100, 200}, 10);

    public NistRetentionIndexParameters() {
      super(column, tolerance, penalty, overrideFromSpectrum, useOtherNonPolar, assumeUnspecified);
    }
  }

  /**
   * The MSPepSearch {@code rXX} penalty rates.
   */
  public enum RIPenaltyRate {

    VERY_WEAK("Very weak", "VW"), WEAK("Weak", "WK"), AVERAGE("Average", "AV"),
    STRONG("Strong", "ST"), VERY_STRONG("Very strong", "VS"), INFINITE("Infinite", "IN");

    private final String label;
    private final String code;

    RIPenaltyRate(final String label, final String code) {
      this.label = label;
      this.code = code;
    }

    public String getCode() {
      return code;
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
