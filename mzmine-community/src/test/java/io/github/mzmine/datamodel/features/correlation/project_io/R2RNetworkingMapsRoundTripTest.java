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

package io.github.mzmine.datamodel.features.correlation.project_io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.OnlineReactionMatch;
import io.github.mzmine.datamodel.features.correlation.R2RMS2CosineSimilarityGNPS;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarity;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarityList;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SimpleRowsRelationship;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction;
import io.github.mzmine.util.FeatureListTestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class R2RNetworkingMapsRoundTripTest {

  @Test
  void roundTripsAllSupportedRelationshipTypes() throws Exception {
    final List<? extends RawDataFile> files = FeatureListTestUtils.createRawFiles(1, "raw",
        LocalDateTime.now(), Duration.ofMinutes(1));
    final ModularFeatureList flist = FeatureListTestUtils.createFeatureList("test", files, 4, 1.0f);
    final List<FeatureListRow> rows = flist.getRows();
    final FeatureListRow r1 = rows.get(0);
    final FeatureListRow r2 = rows.get(1);
    final FeatureListRow r3 = rows.get(2);
    final FeatureListRow r4 = rows.get(3);

    final R2RNetworkingMaps original = flist.getRowMaps();
    original.addRowsRelationship(r1, r2,
        new SimpleRowsRelationship(r1, r2, 0.5, "custom", "ann-1"));
    original.addRowsRelationship(r1, r3, new R2RSimpleSimilarity(r1, r3, Type.MS2Deepscore, 0.75f));
    original.addRowsRelationship(r2, r3, new R2RSpectralSimilarity(r2, r3, Type.MS2_COSINE_SIM,
        new SpectralSimilarity(0.81, 7, 10, 12, 0.9, 0.85)));
    original.addRowsRelationship(r2, r4,
        new R2RMS2CosineSimilarityGNPS(r2, r4, 0.91, "ann-gnps", "EDGE_X"));
    original.addRowsRelationship(r3, r4,
        new R2RSimpleCorrelationData(r3, r4, 0.92, 0.88, 0.7, 0.6, 0.8, 12.5));
    // educt = r4 (higher ID), product = r1 (lower ID) → isSwappedAB true
    original.addRowsRelationship(r1, r4, new OnlineReactionMatch(r4, r1,
        new OnlineReaction("rxn-1", "contains", "[C]", "[C]>>[C]", 1.5),
        OnlineReaction.Type.Product));

    final R2RSimpleSimilarityList simList = new R2RSimpleSimilarityList(r1, r2, Type.DREAMS);
    simList.addSimilarity(0.1);
    simList.addSimilarity(0.2);
    simList.addSimilarity(0.3);
    original.addRowsRelationship(r1, r2, simList);

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    R2RNetworkingMapsSaver.save(original, buffer);

    final R2RNetworkingMaps loaded;
    try (ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray())) {
      loaded = R2RNetworkingMapsLoader.load(in, flist);
    }

    assertEquals(original.getRowsMaps().keySet(), loaded.getRowsMaps().keySet(),
        "Same set of relationship types after round-trip");

    for (final String typeKey : original.getRowsMaps().keySet()) {
      final R2RMap<RowsRelationship> origMap = original.getRowsMaps().get(typeKey);
      final R2RMap<RowsRelationship> loadedMap = loaded.getRowsMaps().get(typeKey);
      assertNotNull(loadedMap, "Missing map for " + typeKey);
      assertEquals(origMap.size(), loadedMap.size(), "Edge count mismatch for " + typeKey);
      for (final Integer key : origMap.keySet()) {
        assertTrue(loadedMap.containsKey(key), "Missing edge key in " + typeKey);
        assertEquals(origMap.get(key).getClass(), loadedMap.get(key).getClass(),
            "Concrete class mismatch for " + typeKey);
        assertEquals(origMap.get(key).getScore(), loadedMap.get(key).getScore(), 1e-9,
            "Score mismatch for " + typeKey);
      }
    }

    final OnlineReactionMatch loadedMatch = (OnlineReactionMatch) loaded.getRowsMap(
        Type.ONLINE_REACTION.toString()).orElseThrow().values().iterator().next();
    assertEquals(r4.getID(), loadedMatch.getEductRow().getID(), "Educt should still be r4");
    assertEquals(r1.getID(), loadedMatch.getProductRow().getID(), "Product should still be r1");
    assertEquals(OnlineReaction.Type.Product, loadedMatch.getTypeOfThisRow());
    assertEquals("rxn-1", loadedMatch.getReaction().reactionName());
  }

  @Test
  void dropsEdgesWithMissingRows() throws Exception {
    final List<? extends RawDataFile> files = FeatureListTestUtils.createRawFiles(1, "raw",
        LocalDateTime.now(), Duration.ofMinutes(1));
    final ModularFeatureList flist = FeatureListTestUtils.createFeatureList("test", files, 3, 1.0f);
    final List<FeatureListRow> rows = flist.getRows();
    final FeatureListRow r1 = rows.get(0);
    final FeatureListRow r2 = rows.get(1);
    final FeatureListRow r3 = rows.get(2);
    flist.getRowMaps()
        .addRowsRelationship(r1, r2, new SimpleRowsRelationship(r1, r2, 0.5, "custom", "ann"));
    flist.getRowMaps()
        .addRowsRelationship(r1, r3, new SimpleRowsRelationship(r1, r3, 0.5, "custom", "ann"));

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    R2RNetworkingMapsSaver.save(flist.getRowMaps(), buffer);

    // remove one of the rows referenced by an edge — that edge must be dropped silently on load
    flist.removeRow(r3);

    final R2RNetworkingMaps loaded;
    try (ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray())) {
      loaded = R2RNetworkingMapsLoader.load(in, flist);
    }
    final R2RMap<RowsRelationship> map = loaded.getRowsMap("custom").orElseThrow();
    assertEquals(1, map.size(), "Edge referencing removed row should be dropped");
  }
}
