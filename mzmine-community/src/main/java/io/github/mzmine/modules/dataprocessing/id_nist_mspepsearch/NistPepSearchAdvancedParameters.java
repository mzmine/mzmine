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
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;

/**
 * Rarely needed MSPepSearch options.
 */
public class NistPepSearchAdvancedParameters extends SimpleParameterSet {

  public static final ComboParameter<Presearch> presearch = new ComboParameter<>("Presearch", """
      How MSPepSearch narrows down the library before the actual comparison.
      "Default" and "Precursor m/z window" match the defaults of the NIST MS Search user interface; \
      "Off" compares against every library spectrum and is dramatically slower.""",
      Presearch.values(), Presearch.DEFAULT);

  public static final BooleanParameter reverseSearch = new BooleanParameter("Reverse search", """
      MSPepSearch r: ignore query peaks that are absent from the library spectrum. Useful for \
      spectra of impure peaks.""", false);

  public static final BooleanParameter librariesInMemory = new BooleanParameter(
      "Load libraries in memory", """
      MSPepSearch /LibInMem: load the libraries into memory. Much faster for many query spectra but \
      needs enough free RAM to hold the libraries.""", false);

  public static final BooleanParameter elevatedPriority = new BooleanParameter("Elevated priority",
      "MSPepSearch /HiPri: run the search process at above normal priority.", false);

  public static final IntegerParameter timeout = new IntegerParameter("Timeout (minutes)", """
      Abort the search if MSPepSearch has not finished within this time.""", 60, 1,
      Integer.MAX_VALUE);

  public static final StringParameter extraArguments = new StringParameter("Extra arguments", """
      Additional MSPepSearch arguments, separated by spaces, appended to the generated command line.
      For experts: nothing here is validated, and options that change the output columns may stop \
      mzmine from reading the results.""", "", false);

  public NistPepSearchAdvancedParameters() {
    super(presearch, reverseSearch, librariesInMemory, elevatedPriority, timeout, extraArguments);
  }

  /**
   * The MSPepSearch presearch modes, with the correspondence to the NIST MS Search user interface
   * that the MSPepSearch help documents.
   */
  public enum Presearch {

    /**
     * {@code d} for low resolution searches, {@code m} for high resolution ones - together these are
     * the "Presearch Default" of the MS Search user interface.
     */
    DEFAULT("Default", 'd', 'm'),
    FAST("Fast", 'f', 'f'),
    OFF("Off (compare all library spectra)", 's', 's'),
    INCHIKEY("First InChIKey segment", 'k', 'k');

    private final String label;
    private final char loResLetter;
    private final char hiResLetter;

    Presearch(final String label, final char loResLetter, final char hiResLetter) {
      this.label = label;
      this.loResLetter = loResLetter;
      this.hiResLetter = hiResLetter;
    }

    /**
     * @param highResolution see {@link NistSearchMode#isHighResolution()}.
     * @return the presearch letter of the leading MSPepSearch option token.
     */
    public char getLetter(final boolean highResolution) {
      return highResolution ? hiResLetter : loResLetter;
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
