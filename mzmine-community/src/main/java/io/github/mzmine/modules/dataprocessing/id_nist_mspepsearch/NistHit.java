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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One hit row of an MSPepSearch {@code /OUTTAB} table.
 * <p>
 * Everything except the two mapping keys is nullable: the columns MSPepSearch emits depend on the
 * search mode and on which {@code /Out...} switches were used.
 *
 * @param specNum       1-based ordinal of the search spectrum ({@code Num} column, from
 *                      {@code /OutSpecNum 1}).
 * @param unknownName   the {@code Name:} of the submitted MSP entry, echoed verbatim in the
 *                      {@code Unknown} column. The secondary mapping key.
 * @param rank          hit rank within the spectrum's hit list.
 * @param name          compound name.
 * @param libraryName   the library the hit came from ({@code Library} column).
 * @param entryId       library entry id ({@code Id} column).
 * @param nistNumber    NIST registry number.
 * @param cas           CAS registry number, unformatted and already normalised so that NIST's
 *                      {@code 0} placeholder becomes null.
 * @param inChIKey      InChIKey of the hit.
 * @param formula       molecular formula.
 * @param exactMass     the hit compound's exact mass ({@code Mass} column).
 * @param nominalMw     nominal molecular weight ({@code Lib MW} column).
 * @param matchFactor   the NIST match factor, 0-999: {@code MF} for low resolution searches and
 *                      {@code Score} for high resolution ones. This is what {@code /MinMF} filters
 *                      on, so it is the score mzmine reports.
 * @param revMatchFactor reverse match factor ({@code R.Match} / {@code Rev-Dot}).
 * @param dotProduct    the plain dot product ({@code DotProd} / {@code Dot Product}).
 * @param probability   NIST's hit probability in percent.
 * @param numMatchedPeaks number of matched signals ({@code NumMP}), the real overlap count.
 * @param numPeaks      number of peaks in the library spectrum.
 * @param precursorMz   the library entry's precursor m/z ({@code Lib Precursor m/z}).
 * @param precursorType the library entry's adduct, e.g. {@code [M+H]+}.
 * @param charge        the library entry's charge.
 * @param collisionEnergy collision energy as free text, e.g. {@code 30} or {@code NCE=65% 34eV}.
 * @param instrumentType instrument type, e.g. {@code Q-TOF} or {@code HCD}.
 * @param retentionIndex the {@code RI} column as MSPepSearch formats it, e.g. {@code 2480-S}.
 */
public record NistHit(int specNum, @Nullable String unknownName, int rank, @NotNull String name,
                      @Nullable String libraryName, @Nullable String entryId,
                      @Nullable String nistNumber, @Nullable String cas, @Nullable String inChIKey,
                      @Nullable String formula, @Nullable Double exactMass,
                      @Nullable Integer nominalMw, @Nullable Integer matchFactor,
                      @Nullable Integer revMatchFactor, @Nullable Integer dotProduct,
                      @Nullable Double probability, @Nullable Integer numMatchedPeaks,
                      @Nullable Integer numPeaks, @Nullable Double precursorMz,
                      @Nullable String precursorType, @Nullable Integer charge,
                      @Nullable String collisionEnergy, @Nullable String instrumentType,
                      @Nullable String retentionIndex) {

  /**
   * The match factor scaled to mzmine's 0-1 similarity score.
   *
   * @return the score, or {@link Double#NaN} if MSPepSearch reported no match factor.
   */
  public double score0to1() {
    return matchFactor == null ? Double.NaN : matchFactor / 1000.0;
  }
}
