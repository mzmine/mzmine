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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.util.List;
import java.util.stream.Collectors;

public class SpectralLibrarySelectionException extends IllegalArgumentException {

  public SpectralLibrarySelectionException() {
    this(
        "Some library files were not imported before spectral library matching. However, it is recommended to import spectral libraries during the initial data import with the MS data import or spectral library import. And maybe set the library selection to use all imported libraries.");
  }

  public SpectralLibrarySelectionException(final String s) {
    super(s);
  }

  public static SpectralLibrarySelectionException forNoLibraries() {
    return new SpectralLibrarySelectionException("No spectral libraries selected");
  }

  public static SpectralLibrarySelectionException forEmptyLibraries(
      final List<SpectralLibrary> libraries) {
    var librariesJoined = libraries.stream().map(SpectralLibrary::getNameWithSize)
        .collect(Collectors.joining(", "));
    return new SpectralLibrarySelectionException("""
        Spectral library matching but libraries are empty. This might indicate that the imported library files \
        are empty or incompatible with mzmine. If this is the case, please raise an issue on GitHub or reach out to our team.\
        Provide an example library file and the whole log file of mzmine. Currently selected libraries:
        %s""".formatted(librariesJoined));
  }
}
