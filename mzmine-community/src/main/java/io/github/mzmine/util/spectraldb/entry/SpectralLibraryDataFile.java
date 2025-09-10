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

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * wraps a spectral library to a data file for processing
 */
public class SpectralLibraryDataFile extends RawDataFileImpl {

  private static final Logger logger = Logger.getLogger(SpectralLibraryDataFile.class.getName());
  @NotNull
  private final SpectralLibrary library;

  public SpectralLibraryDataFile(@NotNull SpectralLibrary library) {
    // name used to be name with size - but this does not match to real raw data file names
    // also this creates issues with the USI and {@link ScanUtil#extractScanIdString} that the size is in there
    // just use library.getName() instead
    super(library.getName(), library.getPath().getAbsolutePath(), library.getStorage());
    this.library = library;

    var entries = library.getEntries();
    List<Scan> scans = IntStream.range(0, entries.size())
        .mapToObj(i -> (Scan) new LibraryEntryWrappedScan(this, entries.get(i), i))
        // need to be sorted by RT even if no RT set
        .sorted(Comparator.comparingDouble(Scan::getRetentionTime)).toList();
    for (final Scan scan : scans) {
      addScan(scan);
    }
  }


  @NotNull
  public SpectralLibrary getLibrary() {
    return library;
  }

  @NotNull
  public Stream<LibraryEntryWrappedScan> streamLibraryScan() {
    return stream().map(scan -> (LibraryEntryWrappedScan) scan);
  }
}
