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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.AnalogSpectralLibraryMatchesType;
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
    // A↔B via InChIKey, B↔C via SMILES → {A, B, C}.
    final String ethanolInChIKey = "LFQSCWFLJHTTHZ-UHFFFAOYSA-N";
    final RowAnnotation a = annotation("rowA", null, ethanolInChIKey, null);
    final RowAnnotation b = annotation("rowB", "Compound B", ethanolInChIKey, "CCO");
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

  @Test
  void directMatchAttachesToExistingAnalogGroup() {
    // analog hit creates the cluster; direct hit shares InChIKey -> folded into the same cluster
    final RowAnnotation analog = analogAnnotation("rowAnalog", "Aspirin",
        "BSYNRYMUTXBXSQ-UHFFFAOYSA-N", null);
    final RowAnnotation direct = directAnnotation("rowDirect", "Acetylsalicylic acid",
        "BSYNRYMUTXBXSQ-UHFFFAOYSA-N", null);
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.groupWithDirectMatches(
        List.of(analog), List.of(direct));
    assertEquals(1, groups.size());
    assertEquals(2, groups.getFirst().members().size());
    // representative stays an analog annotation; direct match never overrides identity
    assertTrue(groups.getFirst().representative().isAnalogMatch());
    // the direct match is present as a member regardless
    final boolean directPresent = groups.getFirst().members().stream()
        .anyMatch(m -> !m.annotation().isAnalogMatch());
    assertTrue(directPresent, "direct match should be folded into the analog cluster");
  }

  @Test
  void directOnlyMatchDoesNotCreateGroup() {
    // no analog matches -> no cluster, even if a direct match has identifiers
    final RowAnnotation direct = directAnnotation("rowDirect", "Caffeine",
        "RYYVLZVUVIJVGH-UHFFFAOYSA-N", null);
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.groupWithDirectMatches(List.of(),
        List.of(direct));
    assertTrue(groups.isEmpty(), "direct matches must never form new clusters on their own");
  }

  @Test
  void directMatchUnrelatedToAnyGroupIsDropped() {
    // analog cluster on Aspirin; direct match is for Caffeine -> can't attach, gets dropped
    final RowAnnotation analog = analogAnnotation("rowAnalog", "Aspirin",
        "BSYNRYMUTXBXSQ-UHFFFAOYSA-N", null);
    final RowAnnotation direct = directAnnotation("rowDirect", "Caffeine",
        "RYYVLZVUVIJVGH-UHFFFAOYSA-N", null);
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.groupWithDirectMatches(
        List.of(analog), List.of(direct));
    assertEquals(1, groups.size());
    assertEquals(1, groups.getFirst().members().size(), "unrelated direct match should be dropped");
    assertFalse(
        groups.getFirst().members().stream().anyMatch(m -> !m.annotation().isAnalogMatch()));
  }

  @Test
  void directMatchAssignedToFirstMatchingGroupOnly() {
    // Two analog clusters; direct match has BOTH a SMILES that matches cluster 0 AND an InChIKey
    // that matches cluster 1. Deterministic assignment: first match in input order wins.
    final RowAnnotation analogA = analogAnnotation("rowA", "Compound A", null, "CCO");
    final RowAnnotation analogB = analogAnnotation("rowB", "Compound B",
        "ZZZZZZZZZZZZZZZZZZZZZZZZ-N", null);
    final RowAnnotation direct = directAnnotation("rowDirect", "Crossover",
        "ZZZZZZZZZZZZZZZZZZZZZZZZ-N", "CCO");
    final List<AnalogCompoundGroup> groups = AnalogCompoundGrouper.groupWithDirectMatches(
        List.of(analogA, analogB), List.of(direct));
    assertEquals(2, groups.size());
    final int sizeA = groups.get(0).members().size();
    final int sizeB = groups.get(1).members().size();
    // direct match lands in exactly one group, never both
    assertEquals(3, sizeA + sizeB);
    assertTrue((sizeA == 2 && sizeB == 1) || (sizeA == 1 && sizeB == 2));
  }

  // ----- helpers -----

  // Default factory used by the legacy tests; dataTypeClass = SpectralLibraryMatchesType (the
  // grouper itself doesn't care about analog vs direct for clustering, only identifier match).
  private static RowAnnotation annotation(final String rowTag, final String compoundName,
      final String inchiKey, final String smiles) {
    return buildRowAnnotation(rowTag, compoundName, inchiKey, smiles, false);
  }

  // Explicit analog-tagged annotation for the cross-type-dedup tests
  private static RowAnnotation analogAnnotation(final String rowTag, final String compoundName,
      final String inchiKey, final String smiles) {
    return buildRowAnnotation(rowTag, compoundName, inchiKey, smiles, true);
  }

  // Explicit direct (non-analog) annotation for the cross-type-dedup tests
  private static RowAnnotation directAnnotation(final String rowTag, final String compoundName,
      final String inchiKey, final String smiles) {
    return buildRowAnnotation(rowTag, compoundName, inchiKey, smiles, false);
  }

  private static RowAnnotation buildRowAnnotation(final String rowTag, final String compoundName,
      final String inchiKey, final String smiles, final boolean analog) {
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
    final SpectralDBAnnotation db;
    if (analog) {
      db = new SpectralDBAnnotation(entry, sim, null, null, null, null, null,
          AnalogSpectralLibraryMatchesType.class);
    } else {
      // default constructor -> SpectralLibraryMatchesType (non-analog)
      db = new SpectralDBAnnotation(entry, sim, null, null, null, null, null);
    }
    return new RowAnnotation(mock(FeatureListRow.class, rowTag), db);
  }
}
