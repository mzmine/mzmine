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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.AnalogCompoundGroup.RowAnnotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalogCompoundGrouperTest {

  @Test
  void linksByInChIKey() {
    // two entries share an InChIKey → one group
    final RowAnnotation a = annotation("rowA", "Aspirin", "BSYNRYMUTXBXSQ-UHFFFAOYSA-N", null);
    final RowAnnotation b = annotation("rowB", "Acetylsalicylic acid",
        "BSYNRYMUTXBXSQ-UHFFFAOYSA-N", null);
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.group(List.of(a, b));
    assertEquals(1, groups.size());
    assertEquals(2, groups.getFirst().members().size());
  }

  @Test
  void transitiveAcrossIdentifiers() {
    // A↔B via InChIKey, B↔C via SMILES → {A, B, C}
    final RowAnnotation a = annotation("rowA", null, "KEYABCDEFGHIJKL-XXXXXXXXXX-N", null);
    final RowAnnotation b = annotation("rowB", "Compound B", "KEYABCDEFGHIJKL-XXXXXXXXXX-N", "CCO");
    final RowAnnotation c = annotation("rowC", "Compound C variant", null, "CCO");
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.group(List.of(a, b, c));
    assertEquals(1, groups.size(), "transitive link via InChIKey and SMILES should fuse all three");
    assertEquals(3, groups.getFirst().members().size());
  }

  @Test
  void stereoVariantsLinkByPrefix() {
    // 14-char prefix shared → one group even though the full InChIKey differs in stereo block
    final RowAnnotation a = annotation("rowA", "Glucose-α", "WQZGKKKJIJFFOK-DVKNGEFBSA-N", null);
    final RowAnnotation b = annotation("rowB", "Glucose-β", "WQZGKKKJIJFFOK-GASJEMHNSA-N", null);
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.group(List.of(a, b));
    assertEquals(1, groups.size());
    assertEquals(2, groups.getFirst().members().size());
  }

  @Test
  void unrelatedAnnotationsStaySeparate() {
    final RowAnnotation a = annotation("rowA", "Caffeine", "RYYVLZVUVIJVGH-UHFFFAOYSA-N", null);
    final RowAnnotation b = annotation("rowB", "Aspirin", "BSYNRYMUTXBXSQ-UHFFFAOYSA-N", null);
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.group(List.of(a, b));
    assertEquals(2, groups.size());
    assertEquals(1, groups.get(0).members().size());
    assertEquals(1, groups.get(1).members().size());
  }

  @Test
  void emptyInputProducesEmptyOutput() {
    assertTrue(AnalogCompoundGrouper.group(List.of()).isEmpty());
  }

  @Test
  void annotationWithNoIdentifiersIsItsOwnGroup() {
    // no name, no key, no SMILES → grouper has nothing to link on; result is a singleton group
    final RowAnnotation a = annotation("rowA", null, null, null);
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.group(List.of(a));
    assertEquals(1, groups.size());
    assertEquals(1, groups.getFirst().members().size());
  }

  // ----- helpers -----

  private static RowAnnotation annotation(final String rowTag, final String compoundName,
      final String inchiKey, final String smiles) {
    final Map<DBEntryField, Object> fields = new HashMap<>();
    if (compoundName != null) {
      fields.put(DBEntryField.NAME, compoundName);
    }
    if (inchiKey != null) {
      fields.put(DBEntryField.INCHIKEY, inchiKey);
    }
    if (smiles != null) {
      fields.put(DBEntryField.SMILES, smiles);
    }
    // SpectralLibraryEntryFactory.create accepts a fields-only entry (no scan / no real data
    // points) which is exactly what the grouper consumes via SpectralDBAnnotation.getNameIdentifier.
    final SpectralLibraryEntry entry = SpectralLibraryEntryFactory.create(null, fields,
        new DataPoint[0]);
    final SpectralSimilarity sim = new SpectralSimilarity("test", 0.8, 3, 1.0);
    final SpectralDBAnnotation db = new SpectralDBAnnotation(entry, sim, null, null, null, null,
        null);
    return new RowAnnotation(mock(FeatureListRow.class, rowTag), db);
  }
}
