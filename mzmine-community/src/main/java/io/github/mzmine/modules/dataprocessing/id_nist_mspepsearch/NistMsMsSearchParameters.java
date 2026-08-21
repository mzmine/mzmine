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
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * Options for the high resolution MS/MS search ({@link NistSearchMode#MSMS_HIRES}).
 */
public class NistMsMsSearchParameters extends SimpleParameterSet {

  public static final MZToleranceParameter precursorTolerance = new MZToleranceParameter(
      "Precursor m/z tolerance", """
      MSPepSearch /Z or /ZPPM: the precursor ion m/z uncertainty.
      MSPepSearch takes either a ppm or an absolute value, not the maximum of both: if the ppm value \
      is greater than zero it is used, otherwise the absolute value is.""", 0.005, 20);

  public static final MZToleranceParameter fragmentTolerance = new MZToleranceParameter(
      "Fragment m/z tolerance", """
      MSPepSearch /M or /MPPM: the product ion m/z uncertainty.
      MSPepSearch takes either a ppm or an absolute value, not the maximum of both: if the ppm value \
      is greater than zero it is used, otherwise the absolute value is.""", 0.01, 40);

  public static final BooleanParameter matchPrecursor = new BooleanParameter("Match precursor m/z",
      """
          Only compare library spectra whose precursor m/z matches, within the precursor tolerance \
          (MSPepSearch z).
          Turn this off to search without a precursor restriction, e.g. for in-source fragments \
          (MSPepSearch u).""", true);

  public static final BooleanParameter alternativePeakMatching = new BooleanParameter(
      "Alternative peak matching", """
      MSPepSearch a: use the alternative peak matching when computing match factors.
      Recommended by NIST.""", true);

  public static final BooleanParameter ignorePrecursorRegion = new BooleanParameter(
      "Ignore precursor region", """
      MSPepSearch i: ignore product ion peaks around the precursor m/z, so that the surviving \
      precursor does not dominate the score.""", false);

  public static final ComboParameter<HiResThreshold> threshold = new ComboParameter<>(
      "Score threshold", "MSPepSearch search threshold for MS/MS searches.",
      HiResThreshold.values(), HiResThreshold.HIGH);

  public NistMsMsSearchParameters() {
    super(precursorTolerance, fragmentTolerance, matchPrecursor, alternativePeakMatching,
        ignorePrecursorRegion, threshold);
  }

  /**
   * The MSPepSearch MS/MS search threshold, one of {@code l}, {@code e} or {@code h}.
   */
  public enum HiResThreshold {

    LOW("Low (allows score 0)", 'l'), MEDIUM("Medium", 'e'), HIGH("High", 'h');

    private final String label;
    private final char letter;

    HiResThreshold(final String label, final char letter) {
      this.label = label;
      this.letter = letter;
    }

    public char getLetter() {
      return letter;
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
