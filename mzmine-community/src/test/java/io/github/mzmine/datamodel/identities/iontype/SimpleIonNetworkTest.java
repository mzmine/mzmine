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

package io.github.mzmine.datamodel.identities.iontype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Invariants of the frozen {@link SimpleIonNetwork} and of the cached neutral mass that both
 * implementations keep.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimpleIonNetworkTest {

  @Mock
  RawDataFile raw;

  private final IonType protonated = IonType.create(IonParts.H);
  private final IonType sodiated = IonType.create(IonParts.NA);

  @Test
  void nodesAreSortedByRowIdRegardlessOfInsertionOrder() {
    final ModularFeatureList flist = newFeatureList();
    final ModularFeatureListRow r3 = row(flist, 3, 223.0);
    final ModularFeatureListRow r1 = row(flist, 1, 201.0);

    // inserted highest row id first
    final BuildingIonNetwork building = new BuildingIonNetwork(0);
    building.put(r3, new IonIdentity(sodiated));
    building.put(r1, new IonIdentity(protonated));

    final SimpleIonNetwork network = building.toSimple();
    assertEquals(List.of(1, 3), network.getRows().stream().map(FeatureListRow::getID).toList());
  }

  @Test
  void frozenNetworkExposesUnmodifiableLists() {
    final SimpleIonNetwork network = twoRowNetwork(7);

    assertThrows(UnsupportedOperationException.class, () -> network.getNodes().clear());
    assertThrows(UnsupportedOperationException.class, () -> network.getMolFormulas().clear());
  }

  @Test
  void withIdReturnsNewInstanceAndRepointsIons() {
    final SimpleIonNetwork network = twoRowNetwork(7);
    network.setNetworkToAllRows();

    final IonNetwork renumbered = network.withID(9);

    assertNotSame(network, renumbered, "renumbering must not mutate the frozen network");
    assertEquals(7, network.getID(), "the original keeps its id");
    assertEquals(9, renumbered.getID());
    // every ion now points at the renumbered network, which is what the equality checks rely on
    assertTrue(renumbered.streamIons().allMatch(ion -> ion.getNetwork() == renumbered));
    assertSame(network, network.withID(7), "same id may return the same instance");
  }

  @Test
  void cachedNeutralMassMatchesTheNodes() {
    final SimpleIonNetwork network = twoRowNetwork(0);
    assertEquals(IonNetwork.calcNeutralMass(network.getNodes()), network.getNeutralMass(), 1e-12);
  }

  @Test
  void buildingNetworkRefreshesNeutralMassOnEveryChange() {
    final ModularFeatureList flist = newFeatureList();
    final ModularFeatureListRow r1 = row(flist, 1, 200.0 + protonated.totalMass());
    final ModularFeatureListRow r2 = row(flist, 2, 200.0 + sodiated.totalMass());

    final BuildingIonNetwork building = new BuildingIonNetwork(0);
    assertEquals(0, building.getNeutralMass(), 1e-12, "empty network has no neutral mass");

    building.put(r1, new IonIdentity(protonated));
    assertEquals(200.0, building.getNeutralMass(), 1e-6);

    building.put(r2, new IonIdentity(sodiated));
    assertEquals(200.0, building.getNeutralMass(), 1e-6);
    assertEquals(IonNetwork.calcNeutralMass(building.getNodes()), building.getNeutralMass(), 1e-12);

    building.remove(r2);
    assertEquals(IonNetwork.calcNeutralMass(building.getNodes()), building.getNeutralMass(), 1e-12);

    building.clear();
    assertEquals(0, building.getNeutralMass(), 1e-12);
  }

  @Test
  void thawedNetworkStartsFromTheFrozenNeutralMass() {
    final SimpleIonNetwork frozen = twoRowNetwork(3);
    final BuildingIonNetwork thawed = new BuildingIonNetwork(frozen);

    assertEquals(frozen.getNeutralMass(), thawed.getNeutralMass(), 1e-12);
    assertEquals(frozen.getID(), thawed.getID());
    assertEquals(frozen.size(), thawed.size());
  }

  // ---- helpers ----

  private SimpleIonNetwork twoRowNetwork(final int id) {
    final ModularFeatureList flist = newFeatureList();
    final ModularFeatureListRow r1 = row(flist, 1, 200.0 + protonated.totalMass());
    final ModularFeatureListRow r2 = row(flist, 2, 200.0 + sodiated.totalMass());

    final BuildingIonNetwork building = new BuildingIonNetwork(id);
    building.put(r1, new IonIdentity(protonated));
    building.put(r2, new IonIdentity(sodiated));
    return building.toSimple();
  }

  private ModularFeatureList newFeatureList() {
    final ModularFeatureList flist = new ModularFeatureList("test", null, raw);
    flist.addRowType(new IDType());
    flist.addRowType(new RTType());
    flist.addRowType(new MZType());
    flist.addRowType(new HeightType());
    flist.addRowType(new IonIdentityListType());
    return flist;
  }

  private static ModularFeatureListRow row(final ModularFeatureList flist, final int id,
      final double mz) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    row.set(MZType.class, mz);
    row.set(RTType.class, 1.0f);
    row.set(HeightType.class, 1000f);
    flist.addRow(row);
    return row;
  }
}
