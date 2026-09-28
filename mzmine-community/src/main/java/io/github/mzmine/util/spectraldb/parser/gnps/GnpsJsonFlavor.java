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

package io.github.mzmine.util.spectraldb.parser.gnps;

import org.jetbrains.annotations.NotNull;

/**
 * The shapes GNPS uses for its json libraries. Only the mapping of a single entry differs, so
 * {@link GNPSJsonParser} reads all of them.
 */
public enum GnpsJsonFlavor {

  /**
   * Classic GNPS export and the cleaned libraries. Every key sits on the entry itself and the
   * signals are json nested in a peaks_json string.
   */
  CLASSIC(GnpsLibraryEntry.class),

  /**
   * GNPS2. The metadata is a nested object with mgf style keys and the signals are a json array.
   */
  GNPS2(Gnps2LibraryEntry.class);

  private final Class<? extends GnpsEntry> entryClass;

  GnpsJsonFlavor(@NotNull final Class<? extends GnpsEntry> entryClass) {
    this.entryClass = entryClass;
  }

  @NotNull Class<? extends GnpsEntry> getEntryClass() {
    return entryClass;
  }
}
