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

package datamodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RStructureSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SimpleRowsRelationship;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkNode;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.datamodel.identities.iontype.SimpleIonNetwork;
import io.github.mzmine.modules.tools.molecular_similarity.tanimoto.FingerprintType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that copying a feature list also transfers everything that references rows directly: the
 * {@link R2RMap}s of {@link ModularFeatureList#getRowMaps()} and the {@link IonNetwork}s. Both have
 * to be recreated against the copied rows, see
 * {@link FeatureListUtils#transferRowRelationsAndIIN(FeatureList, ModularFeatureList, Function)}.
 */
public class RowRelationsCopyTest {

  private ModularFeatureList source;

  /**
   * {@link FeatureListUtils#copyRows} sorts by {@link FeatureListUtils#getDefaultRowSorter}, which
   * is RT here, so every row needs an RT for the copies to have a defined order.
   */
  private static ModularFeatureListRow row(final ModularFeatureList fl, final int id,
      final double mz, final float rt) {
    final ModularFeatureListRow r = new ModularFeatureListRow(fl, id);
    r.set(MZType.class, mz);
    r.set(RTType.class, rt);
    fl.addRow(r);
    return r;
  }

  /**
   * A row whose RT follows its m/z, so RT order and m/z order agree.
   */
  private static ModularFeatureListRow row(final ModularFeatureList fl, final int id,
      final double mz) {
    return row(fl, id, mz, (float) mz);
  }

  private static RowsRelationship cosine(final FeatureListRow a, final FeatureListRow b,
      final double score) {
    return new SimpleRowsRelationship(a, b, score, Type.MS2_COSINE_SIM.toString(), "cos");
  }

  /**
   * An ion identity network over the given rows, each row getting its own ion identity.
   */
  private static IonNetwork network(final int id, final FeatureListRow... rows) {
    final List<IonNetworkNode> nodes = new ArrayList<>();
    for (final FeatureListRow row : rows) {
      final IonIdentity ion = new IonIdentity(IonTypes.H.asIonType());
      row.setIonIdentities(List.of(ion));
      nodes.add(new IonNetworkNode(row, ion));
    }
    return new SimpleIonNetwork(id, nodes).setNetworkToAllRows();
  }

  @BeforeEach
  void setUp() {
    source = new ModularFeatureList("source", null,
        new RawDataFileImpl("sourceFile", null, null, Color.BLACK));
  }

  @Test
  void copyRemapsRelationshipsOntoTheCopiedRows() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);
    source.getRowMaps().addRowsRelationship(s1, s2, cosine(s1, s2, 0.9));

    final ModularFeatureList copy = FeatureListUtils.createCopy(source, "copy", null, true);

    final R2RMap<RowsRelationship> copied = copy.getRowMap(Type.MS2_COSINE_SIM).orElseThrow();
    assertEquals(1, copied.size());

    final FeatureListRow t1 = copy.findRowByID(1);
    final FeatureListRow t2 = copy.findRowByID(2);
    final RowsRelationship rel = copied.get(t1, t2);
    assertNotNull(rel, "the key is derived from the copied rows");
    assertSame(t1, rel.getRowA(), "the edge points at the copied row, not at the source row");
    assertSame(t2, rel.getRowB());
    assertEquals(0.9, rel.getScore());

    // the source keeps its own relationship untouched
    final RowsRelationship sourceRel = source.getRowMap(Type.MS2_COSINE_SIM).orElseThrow()
        .get(s1, s2);
    assertNotNull(sourceRel);
    assertNotSame(rel, sourceRel);
    assertSame(s1, sourceRel.getRowA());
  }

  @Test
  void relationshipsOfRowsThatWereNotCopiedAreDropped() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);
    final ModularFeatureListRow s3 = row(source, 3, 300d);
    source.getRowMaps().addRowsRelationship(s1, s2, cosine(s1, s2, 0.9));
    source.getRowMaps().addRowsRelationship(s1, s3, cosine(s1, s3, 0.5));

    // only rows 1 and 2 pass a filter, copied the way a filtering module does it
    final ModularFeatureList target = FeatureListUtils.createCopyWithoutRows(source, "filtered",
        null, null, null);
    final Map<FeatureListRow, ModularFeatureListRow> mapping = new IdentityHashMap<>();
    for (final ModularFeatureListRow src : List.of(s1, s2)) {
      final ModularFeatureListRow copy = new ModularFeatureListRow(target, src.getID(), src, true);
      target.addRow(copy);
      mapping.put(src, copy);
    }

    FeatureListUtils.transferRowRelationsAndIIN(source, target, mapping::get);

    final R2RMap<RowsRelationship> copied = target.getRowMap(Type.MS2_COSINE_SIM).orElseThrow();
    assertEquals(1, copied.size(), "the edge to the row that was not copied is dropped");
    assertNotNull(copied.get(target.findRowByID(1), target.findRowByID(2)));
    assertEquals(2, source.getRowMap(Type.MS2_COSINE_SIM).orElseThrow().size(),
        "the source keeps both edges");
  }

  /**
   * Renumbering can reverse the relative order of two row IDs.
   * {@link io.github.mzmine.datamodel.features.correlation.AbstractRowsRelationship} orders its
   * rows by ID, so payload that describes one of the two rows has to follow that row.
   */
  @Test
  void rowBoundPayloadFollowsItsRowWhenRenumberingReordersTheRows() {
    // RT order is the reverse of the ID order, and the copy is renumbered in RT order, so the two
    // rows swap their relative order
    final ModularFeatureListRow s5 = row(source, 5, 500d, 1f);
    final ModularFeatureListRow s3 = row(source, 3, 300d, 2f);
    // passed in ID order, so inchiA belongs to row 3 and inchiB to row 5
    source.getRowMaps().addRowsRelationship(s3, s5,
        new R2RStructureSimilarity(s3, s5, FingerprintType.ECFP4_2048, "inchi-3", "inchi-5", 0.7f));

    final ModularFeatureList copy = source.createCopy("copy", null, true);
    assertEquals(500d, copy.findRowByID(1).getAverageMZ(),
        "row 5 elutes first and therefore becomes ID 1");

    final R2RStructureSimilarity rel = (R2RStructureSimilarity) copy.getRowMap(
        Type.STRUCTURE_TANIMOTO).orElseThrow().values().iterator().next();

    // rowA is now the copy of source row 5, because it has the lower ID after renumbering
    assertEquals(500d, rel.getRowA().getAverageMZ());
    assertEquals("inchi-5", rel.getInchiA(), "the structure stays attached to its own row");
    assertEquals(300d, rel.getRowB().getAverageMZ());
    assertEquals("inchi-3", rel.getInchiB());
  }

  /**
   * Filtering in place reuses the row objects, but removing rows and renumbering their IDs
   * invalidates the map keys just the same.
   */
  @Test
  void inPlaceFilteringRekeysRelationshipsAndDropsFilteredRows() {
    final ModularFeatureListRow s1 = row(source, 10, 100d);
    final ModularFeatureListRow s2 = row(source, 20, 200d);
    final ModularFeatureListRow s3 = row(source, 30, 300d);
    source.getRowMaps().addRowsRelationship(s1, s2, cosine(s1, s2, 0.9));
    source.getRowMaps().addRowsRelationship(s2, s3, cosine(s2, s3, 0.5));
    final long staleKey = MathUtils.undirectedPairing(10, 20);

    // row 30 is filtered out and the survivors are renumbered, as the rows filter does in place
    source.setRowsApplySort(s1, s2);
    s1.set(IDType.class, 1);
    s2.set(IDType.class, 2);
    final Map<FeatureListRow, ModularFeatureListRow> mapping = new IdentityHashMap<>();
    mapping.put(s1, s1);
    mapping.put(s2, s2);

    FeatureListUtils.transferRowRelationsAndIIN(source, source, mapping::get);

    final R2RMap<RowsRelationship> maps = source.getRowMap(Type.MS2_COSINE_SIM).orElseThrow();
    assertEquals(1, maps.size(),
        "an edge needs both of its rows, so the one to the filtered row is dropped");
    final RowsRelationship rel = maps.get(s1, s2);
    assertNotNull(rel, "the remaining edge is re-keyed to the renumbered row IDs");
    assertEquals(0.9, rel.getScore());
    assertNull(maps.get(staleKey), "the key of the old row IDs is gone instead of kept alongside");
  }

  @Test
  void inPlaceFilteringShrinksIonNetworkToTheSurvivingRows() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);
    final ModularFeatureListRow s3 = row(source, 3, 300d);
    network(7, s1, s2, s3);

    source.setRowsApplySort(s1, s2);
    final Map<FeatureListRow, ModularFeatureListRow> mapping = new IdentityHashMap<>();
    mapping.put(s1, s1);
    mapping.put(s2, s2);

    FeatureListUtils.transferRowRelationsAndIIN(source, source, mapping::get);

    final IonNetwork net = s1.getBestIonIdentity().getNetwork();
    assertNotNull(net);
    assertEquals(List.of(s1, s2), net.getRows(), "the filtered row is no longer a member");
    assertSame(net, s2.getBestIonIdentity().getNetwork());
  }

  @Test
  void copyRemapsIonNetworksOntoTheCopiedRows() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);
    final IonNetwork sourceNet = network(7, s1, s2);

    final ModularFeatureList copy = FeatureListUtils.createCopy(source, "copy", null, true);

    final FeatureListRow t1 = copy.findRowByID(1);
    final FeatureListRow t2 = copy.findRowByID(2);
    final IonIdentity copiedIon = t1.getBestIonIdentity();
    assertNotNull(copiedIon);
    assertNotSame(s1.getBestIonIdentity(), copiedIon,
        "the copy must not share the mutable ion identity with its source row");
    assertEquals(s1.getBestIonIdentity().getIonType(), copiedIon.getIonType());

    final IonNetwork copiedNet = copiedIon.getNetwork();
    assertNotNull(copiedNet);
    assertNotSame(sourceNet, copiedNet);
    assertEquals(7, copiedNet.getID(), "the network ID is preserved");
    assertEquals(List.of(t1, t2), copiedNet.getRows(),
        "the network references the copied rows, not the source rows");
    assertSame(copiedNet, t2.getBestIonIdentity().getNetwork(),
        "both copied rows share one network");

    assertEquals(List.of(s1, s2), sourceNet.getRows(), "the source network is untouched");
  }

  /**
   * Filtering away the other members of a network must not take the ion annotation of the surviving
   * row with it: the adduct assignment stays useful on its own. Only a network that lost all of its
   * rows disappears.
   */
  @Test
  void ionNetworkOfASingleSurvivingRowIsKept() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);
    final IonNetwork sourceNet = network(7, s1, s2);
    final IonIdentity sourceIon = s1.getBestIonIdentity();

    // only row 1 passes the filter
    final ModularFeatureList target = FeatureListUtils.createCopyWithoutRows(source, "filtered",
        null, null, null);
    final ModularFeatureListRow t1 = new ModularFeatureListRow(target, s1.getID(), s1, true);
    target.addRow(t1);

    final Map<FeatureListRow, ModularFeatureListRow> mapping = new IdentityHashMap<>();
    mapping.put(s1, t1);
    FeatureListUtils.transferRowRelationsAndIIN(source, target, mapping::get);

    final IonIdentity keptIon = t1.getBestIonIdentity();
    assertNotNull(keptIon, "the ion annotation of the surviving row is kept");
    assertNotSame(sourceIon, keptIon, "but as a copy, not shared with the source row");
    assertEquals(sourceIon.getIonType(), keptIon.getIonType());

    final IonNetwork keptNet = keptIon.getNetwork();
    assertNotNull(keptNet);
    assertEquals(7, keptNet.getID(), "the network ID is preserved");
    assertEquals(List.of(t1), keptNet.getRows(), "the filtered row is no longer a member");

    assertEquals(List.of(s1, s2), sourceNet.getRows(), "the source network is untouched");
  }

  /**
   * In place, a network that lost no row must keep its {@link IonIdentity} instances: replacing
   * them would invalidate anything holding on to them, and there is nothing to redirect.
   */
  @Test
  void inPlaceFilteringLeavesUntouchedIonNetworksAlone() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);
    final ModularFeatureListRow s3 = row(source, 3, 300d);
    final IonNetwork keptNet = network(7, s1, s2);
    final IonNetwork shrinkingNet = network(8, s3);
    final IonIdentity keptIon = s1.getBestIonIdentity();

    // row 3 is filtered, so only its network changes
    source.setRowsApplySort(s1, s2);
    final Map<FeatureListRow, ModularFeatureListRow> mapping = new IdentityHashMap<>();
    mapping.put(s1, s1);
    mapping.put(s2, s2);

    FeatureListUtils.transferRowRelationsAndIIN(source, source, mapping::get);

    assertSame(keptIon, s1.getBestIonIdentity(), "the ion identity is not replaced");
    assertSame(keptNet, keptIon.getNetwork(), "nor is its network");
    assertEquals(List.of(s3), shrinkingNet.getRows(),
        "the network of the filtered row is left to be collected with it");
  }

  /**
   * A module may split one feature list into several, like the blank subtraction, and transfer from
   * the same source more than once. The source must stay untouched throughout.
   */
  @Test
  void createCopyWithRowsCanBeUsedSeveralTimesOnOneSource() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);
    final ModularFeatureListRow s3 = row(source, 3, 300d);
    source.getRowMaps().addRowsRelationship(s1, s2, cosine(s1, s2, 0.9));
    source.getRowMaps().addRowsRelationship(s1, s3, cosine(s1, s3, 0.4));
    final IonNetwork sourceNet = network(7, s1, s2);

    final ModularFeatureList first = FeatureListUtils.createCopyWithRows(source, null, "first",
        null, List.of(s1, s2), false);
    final ModularFeatureList second = FeatureListUtils.createCopyWithRows(source, null, "second",
        null, List.of(s1, s3), false);

    final R2RMap<RowsRelationship> firstMap = first.getRowMap(Type.MS2_COSINE_SIM).orElseThrow();
    assertEquals(1, firstMap.size());
    assertNotNull(firstMap.get(first.findRowByID(1), first.findRowByID(2)));

    final R2RMap<RowsRelationship> secondMap = second.getRowMap(Type.MS2_COSINE_SIM).orElseThrow();
    assertEquals(1, secondMap.size(), "the second transfer still sees the untouched source");
    assertNotNull(secondMap.get(second.findRowByID(1), second.findRowByID(3)));

    // each copy gets its own network over the rows it kept
    final IonNetwork firstNet = first.findRowByID(1).getBestIonIdentity().getNetwork();
    assertNotNull(firstNet);
    assertEquals(List.of(first.findRowByID(1), first.findRowByID(2)), firstNet.getRows());

    final IonNetwork secondNet = second.findRowByID(1).getBestIonIdentity().getNetwork();
    assertNotNull(secondNet, "the ion annotation is kept even though the partner row is gone");
    assertEquals(List.of(second.findRowByID(1)), secondNet.getRows());
    assertNotSame(firstNet, secondNet, "the two copies do not share a network");

    assertEquals(List.of(s1, s2), sourceNet.getRows(), "the source network is untouched");
    assertEquals(2, source.getRowMap(Type.MS2_COSINE_SIM).orElseThrow().size(),
        "the source keeps both edges");
  }

  @Test
  void createCopyWithRowsCopiesADuplicatedRowOnlyOnce() {
    final ModularFeatureListRow s1 = row(source, 1, 100d);
    final ModularFeatureListRow s2 = row(source, 2, 200d);

    // a row may be selected twice, e.g. by two regions or by two compounds
    final ModularFeatureList copy = FeatureListUtils.createCopyWithRows(source, null, "copy", null,
        List.of(s1, s2, s1), false);

    assertEquals(2, copy.getNumberOfRows());
  }

  @Test
  void selectedScansAreOnlyTransferredForTheFilesOfTheCopy() {
    final RawDataFile keep = new RawDataFileImpl("keep", null, null, Color.BLACK);
    final RawDataFile drop = new RawDataFileImpl("drop", null, null, Color.BLACK);
    final ModularFeatureList twoFiles = new ModularFeatureList("two files", null,
        List.of(keep, drop));
    twoFiles.setSelectedScans(keep, List.of());
    twoFiles.setSelectedScans(drop, List.of());

    // a copy that only covers a subset of the samples, e.g. split aligned feature list
    final ModularFeatureList copy = FeatureListUtils.createCopy(twoFiles, null, "subset", null,
        false, List.of(keep), false, null, null);

    assertNotNull(copy.getSeletedScans(keep));
    assertNull(copy.getSeletedScans(drop), "the copy does not contain this raw data file");
  }
}
