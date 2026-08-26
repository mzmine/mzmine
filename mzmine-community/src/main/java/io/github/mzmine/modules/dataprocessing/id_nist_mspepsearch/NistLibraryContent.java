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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

/**
 * What a NIST library holds, which decides for which search type it can be used.
 * <p>
 * Derived from the index files of the library directory rather than from its name, so that custom
 * libraries built with Lib2NIST are classified as well. See {@link NistLibrary}.
 */
public enum NistLibraryContent {

  /**
   * Electron ionization spectra - the unit mass GC-EI libraries such as mainlib and replib.
   */
  EI("EI"),
  /**
   * MS/MS spectra with a precursor m/z - the tandem libraries such as hr_msms_nist, lr_msms_nist
   * and apci_msms_nist.
   */
  MSMS("MS/MS"),
  /**
   * No spectra at all. NIST ships the retention index library nist_ri in the same format as the
   * tandem libraries, but it only holds names, structures and retention indices, so searching it
   * can never produce a hit.
   */
  NON_SPECTRAL("non spectral");

  private final String label;

  NistLibraryContent(final String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
