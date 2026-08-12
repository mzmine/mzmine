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

package io.github.mzmine.util.spectraldb.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpectralDBAnnotationTest {

  @Test
  void mapsDatabaseNameTypeToLibraryName() {
    final SpectralLibrary library = new SpectralLibrary(null, "MassBank", new File("massbank"));
    final SpectralDBEntry entry = new SpectralDBEntry(null, new double[]{100d},
        new double[]{1d}, Map.of(), library);
    final SpectralSimilarity similarity = new SpectralSimilarity("test", 0.95d, 1, 1d);
    final SpectralDBAnnotation annotation = new SpectralDBAnnotation(entry, similarity, null, null);

    assertEquals("MassBank", annotation.get(DatabaseNameType.class));
  }

  @Test
  void prefersExplicitDatabaseNameTypeOverride() {
    final SpectralLibrary library = new SpectralLibrary(null, "mzconnect", new File("mzconnect"));
    final SpectralDBEntry entry = new SpectralDBEntry(null, new double[]{100d},
        new double[]{1d}, Map.of(), library);
    final SpectralSimilarity similarity = new SpectralSimilarity("test", 0.95d, 1, 1d);
    final SpectralDBAnnotation annotation = new SpectralDBAnnotation(entry, similarity, null, null);
    annotation.set(DatabaseNameType.class, "mzconnect / MassBank");

    assertEquals("mzconnect / MassBank", annotation.get(DatabaseNameType.class));
  }
}
