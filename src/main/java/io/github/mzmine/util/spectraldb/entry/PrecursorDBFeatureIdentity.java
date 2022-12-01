/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import java.text.MessageFormat;

public class PrecursorDBFeatureIdentity extends SimpleFeatureIdentity {

  private final SpectralLibraryEntry entry;

  public PrecursorDBFeatureIdentity(SpectralLibraryEntry entry, String method) {
    super(MessageFormat.format("Precursor? {0} as {3} ({1}) {2}",
            entry.getField(DBEntryField.NAME).orElse("NONAME"), // Name
            entry.getField(DBEntryField.PRECURSOR_MZ).orElse(""), // precursor m/z
            entry.getField(DBEntryField.FORMULA).orElse(""), // molecular
            // formula
            entry.getField(DBEntryField.ION_TYPE).orElse("")), // Ion type
        entry.getField(DBEntryField.FORMULA).orElse("").toString(), method, "", "");
    this.entry = entry;
  }

  public SpectralLibraryEntry getEntry() {
    return entry;
  }

  public DataPoint[] getLibraryDataPoints() {
    return entry.getDataPoints();
  }

}
