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

/**
 * The MSPepSearch search types exposed by this module.
 * <p>
 * The letter is the search type character of the leading MSPepSearch option token. Low resolution
 * types ({@code I}, {@code S}, {@code H}) work on unit mass EI spectra and use the NIST main and
 * replicate libraries; the high resolution type ({@code G}) works on accurate mass MS/MS spectra
 * and uses the tandem libraries.
 */
public enum NistSearchMode {

  /**
   * Low resolution identity search - the standard GC-EI workflow.
   */
  GC_EI_IDENTITY("GC-EI identity", 'I', false),
  /**
   * Low resolution simple similarity search, for compounds that are not in the library.
   */
  GC_EI_SIMILARITY("GC-EI similarity", 'S', false),
  /**
   * Low resolution hybrid similarity search. Needs the nominal molecular weight of the unknown
   * ({@code /MwForLoss}), which MSPepSearch only accepts as a single global value, so queries have
   * to be grouped by molecular weight.
   */
  GC_EI_HYBRID("GC-EI hybrid similarity", 'H', false),
  /**
   * High resolution generic MS/MS search - the standard LC-MS/MS workflow. "Generic" rather than
   * "peptide" so that no peptide peak annotation or weighting is applied.
   */
  MSMS_HIRES("High resolution MS/MS", 'G', true);

  private final String label;
  private final char searchTypeLetter;
  private final boolean highResolution;

  NistSearchMode(final String label, final char searchTypeLetter, final boolean highResolution) {
    this.label = label;
    this.searchTypeLetter = searchTypeLetter;
    this.highResolution = highResolution;
  }

  /**
   * @return the MSPepSearch search type character.
   */
  public char getSearchTypeLetter() {
    return searchTypeLetter;
  }

  /**
   * @return true for accurate mass MS/MS searches, false for unit mass EI searches. Decides which
   * option group applies and whether a precursor m/z is written to the query file.
   */
  public boolean isHighResolution() {
    return highResolution;
  }

  /**
   * @return true if this mode needs {@code /MwForLoss}, which forces queries to be grouped by
   * nominal molecular weight.
   */
  public boolean needsMolecularWeight() {
    return this == GC_EI_HYBRID;
  }

  @Override
  public String toString() {
    return label;
  }
}
