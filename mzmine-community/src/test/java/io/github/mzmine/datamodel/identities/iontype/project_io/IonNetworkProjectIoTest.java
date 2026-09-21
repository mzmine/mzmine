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

package io.github.mzmine.datamodel.identities.iontype.project_io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.github.mzmine.datamodel.identities.io.IonLibraryIO;
import io.github.mzmine.datamodel.identities.iontype.BuildingIonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.util.FormulaUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Round trip of {@link IonNetworksSaver} and {@link IonNetworksLoader}: ion networks and the ion
 * identities of their rows must survive a save and load unchanged, including the order of the ion
 * identities within a row, which is what makes one of them the best ion.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IonNetworkProjectIoTest {

  private static final int NET_A = 10;
  private static final int NET_B = 20;

  @Mock
  RawDataFile raw;

  private final IonType protonated = IonType.create(IonParts.H);
  private final IonType sodiated = IonType.create(IonParts.NA);
  private final IonType potassiated = IonType.create(IonParts.K);

  @Test
  void networksAndIonOrderSurviveRoundTrip() throws Exception {
    final ModularFeatureList saved = newFeatureList("saved");
    final ModularFeatureListRow r1 = row(saved, 1, 200.0 + protonated.totalMass());
    final ModularFeatureListRow r2 = row(saved, 2, 200.0 + sodiated.totalMass());
    final ModularFeatureListRow r3 = row(saved, 3, 400.0 + potassiated.totalMass());

    // network A: r1 [M+H], r2 [M+Na]
    final BuildingIonNetwork buildingA = new BuildingIonNetwork(NET_A);
    final IonIdentity r1H = buildingA.put(r1, new IonIdentity(protonated));
    final IonIdentity r2Na = buildingA.put(r2, new IonIdentity(sodiated));

    // network B: a competing hypothesis for r2 [M+H] together with r3 [M+K]
    final BuildingIonNetwork buildingB = new BuildingIonNetwork(NET_B);
    final IonIdentity r2H = buildingB.put(r2, new IonIdentity(protonated));
    final IonIdentity r3K = buildingB.put(r3, new IonIdentity(potassiated));

    final IonNetwork netA = buildingA.toSimple();
    final IonNetwork netB = buildingB.toSimple();

    // r2 is part of both networks, its M+H hypothesis from network B is the best one
    r1.setIonIdentities(List.of(r1H));
    r2.setIonIdentities(List.of(r2H, r2Na));
    r3.setIonIdentities(List.of(r3K));

    // formulas on the network (consensus) and on a single ion
    final ResultFormula consensus = resultFormula("C10H15N", netA.getNeutralMass());
    netA.addMolFormulas(List.of(consensus));
    final ResultFormula ionFormula = resultFormula("C10H16N", r1.getAverageMZ());
    r1H.addMolFormulas(List.of(ionFormula));

    // ---- round trip into a fresh feature list with the same row ids but no ion identities ----
    final ModularFeatureList loadedFlist = newFeatureList("loaded");
    row(loadedFlist, 1, r1.getAverageMZ());
    row(loadedFlist, 2, r2.getAverageMZ());
    row(loadedFlist, 3, r3.getAverageMZ());

    final List<IonNetwork> loaded = roundTrip(List.of(netA, netB), loadedFlist);

    assertEquals(2, loaded.size(), "both networks should be restored");
    final IonNetwork loadedA = byId(loaded, NET_A);
    final IonNetwork loadedB = byId(loaded, NET_B);

    // membership, in the row id order that SimpleIonNetwork guarantees
    assertEquals(List.of(1, 2), rowIds(loadedA));
    assertEquals(List.of(2, 3), rowIds(loadedB));

    // ion types per row
    assertEquals(protonated, loadedA.get(loadedFlist.findRowByID(1)).getIonType());
    assertEquals(sodiated, loadedA.get(loadedFlist.findRowByID(2)).getIonType());
    assertEquals(protonated, loadedB.get(loadedFlist.findRowByID(2)).getIonType());
    assertEquals(potassiated, loadedB.get(loadedFlist.findRowByID(3)).getIonType());

    // the saved order decides the best ion of a row: M+H of network B, not M+Na of network A
    final FeatureListRow loadedR2 = loadedFlist.findRowByID(2);
    assertEquals(2, loadedR2.getIonIdentities().size());
    final IonIdentity bestOfR2 = loadedR2.getBestIonIdentity();
    assertNotNull(bestOfR2);
    assertEquals(protonated, bestOfR2.getIonType());
    assertEquals(NET_B, bestOfR2.getNetwork().getID(), "best ion must point back to its network");
    assertEquals(sodiated, loadedR2.getIonIdentities().get(1).getIonType());

    // derived values are recomputed from the restored rows
    assertEquals(netA.getNeutralMass(), loadedA.getNeutralMass(), 1e-9);
    assertEquals(netB.getNeutralMass(), loadedB.getNeutralMass(), 1e-9);

    // formulas of the network and of the single ion
    assertEquals(List.of(consensus.getFormulaAsString()),
        loadedA.getMolFormulas().stream().map(ResultFormula::getFormulaAsString).toList());
    final IonIdentity loadedR1H = loadedA.get(loadedFlist.findRowByID(1));
    assertEquals(List.of(ionFormula.getFormulaAsString()),
        loadedR1H.getMolFormulas().stream().map(ResultFormula::getFormulaAsString).toList());
  }

  @Test
  void nodesOfMissingRowsAreDropped() throws Exception {
    final ModularFeatureList saved = newFeatureList("saved");
    final ModularFeatureListRow r1 = row(saved, 1, 200.0 + protonated.totalMass());
    final ModularFeatureListRow r2 = row(saved, 2, 200.0 + sodiated.totalMass());

    final BuildingIonNetwork building = new BuildingIonNetwork(NET_A);
    r1.setIonIdentities(List.of(building.put(r1, new IonIdentity(protonated))));
    r2.setIonIdentities(List.of(building.put(r2, new IonIdentity(sodiated))));
    final IonNetwork net = building.toSimple();

    // row 2 is gone from the target feature list
    final ModularFeatureList loadedFlist = newFeatureList("loaded");
    row(loadedFlist, 1, r1.getAverageMZ());

    final List<IonNetwork> loaded = roundTrip(List.of(net), loadedFlist);

    assertEquals(1, loaded.size());
    assertEquals(List.of(1), rowIds(loaded.getFirst()));
    assertTrue(loadedFlist.findRowByID(1).hasIonIdentity());
  }

  @Test
  void emptyInputWritesReadableFile() throws Exception {
    final ModularFeatureList loadedFlist = newFeatureList("loaded");
    assertTrue(roundTrip(List.of(), loadedFlist).isEmpty());
  }

  @Test
  void everyIonTypeIsWrittenOnceAndReferencedByIndex() throws Exception {
    final ModularFeatureList flist = newFeatureList("saved");
    final ModularFeatureListRow r1 = row(flist, 1, 200.0 + protonated.totalMass());
    final ModularFeatureListRow r2 = row(flist, 2, 200.0 + sodiated.totalMass());
    final ModularFeatureListRow r3 = row(flist, 3, 400.0 + potassiated.totalMass());

    // M+H is used twice, by r1 in network A and by r2 in network B
    final BuildingIonNetwork buildingA = new BuildingIonNetwork(NET_A);
    r1.setIonIdentities(List.of(buildingA.put(r1, new IonIdentity(protonated))));
    final IonIdentity r2Na = buildingA.put(r2, new IonIdentity(sodiated));
    final BuildingIonNetwork buildingB = new BuildingIonNetwork(NET_B);
    final IonIdentity r2H = buildingB.put(r2, new IonIdentity(protonated));
    r3.setIonIdentities(List.of(buildingB.put(r3, new IonIdentity(potassiated))));
    r2.setIonIdentities(List.of(r2H, r2Na));

    final String xml = write(List.of(buildingA.toSimple(), buildingB.toSimple()));

    // 4 ions, but each of the 3 distinct ion types is stored once in the library
    assertEquals(4, countOccurrences(xml, "<ion "), "every ion references its type by index");
    assertTrue(xml.indexOf("<ionlibrary>") < xml.indexOf("<row "),
        "the library must come before the rows that reference it");

    final IonLibrary library = IonLibraryIO.loadFromJson(libraryJson(xml)).library();
    assertEquals(3, library.ions().size(), "each distinct ion type is stored once");
    // and each ion part definition only once, no matter how many ion types use it
    assertEquals(List.of("H", "K", "Na"),
        library.ions().stream().flatMap(type -> type.parts().stream()).map(IonPart::name).distinct()
            .sorted().toList());
  }

  @Test
  void savingTwiceProducesTheSameStructure() throws Exception {
    final ModularFeatureList flist = newFeatureList("saved");
    final ModularFeatureListRow r1 = row(flist, 1, 200.0 + protonated.totalMass());
    final ModularFeatureListRow r2 = row(flist, 2, 200.0 + sodiated.totalMass());
    final ModularFeatureListRow r3 = row(flist, 3, 400.0 + potassiated.totalMass());

    final BuildingIonNetwork buildingA = new BuildingIonNetwork(NET_A);
    r1.setIonIdentities(List.of(buildingA.put(r1, new IonIdentity(protonated))));
    final IonIdentity r2Na = buildingA.put(r2, new IonIdentity(sodiated));
    final BuildingIonNetwork buildingB = new BuildingIonNetwork(NET_B);
    final IonIdentity r2H = buildingB.put(r2, new IonIdentity(protonated));
    r3.setIonIdentities(List.of(buildingB.put(r3, new IonIdentity(potassiated))));
    r2.setIonIdentities(List.of(r2H, r2Na));

    // networks handed over in the opposite order must still produce the same structure
    final List<IonNetwork> networks = List.of(buildingA.toSimple(), buildingB.toSimple());
    final String first = write(networks);
    final String second = write(networks.reversed());

    // the library carries a fresh id and save date every time, that part is allowed to differ
    assertEquals(withoutLibrary(first), withoutLibrary(second));
    assertEquals(IonLibraryIO.loadFromJson(libraryJson(first)).library().ions(),
        IonLibraryIO.loadFromJson(libraryJson(second)).library().ions(),
        "the library content must not depend on the order the networks are handed over");
  }

  // ---- helpers ----

  private static List<IonNetwork> roundTrip(final List<IonNetwork> networks,
      final ModularFeatureList target) throws Exception {
    final byte[] xml = write(networks).getBytes(StandardCharsets.UTF_8);
    return IonNetworksLoader.load(new ByteArrayInputStream(xml), target);
  }

  private static String write(final List<IonNetwork> networks) throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonNetworksSaver.save(networks, out);
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Everything but the library, whose id and save date are new on every write.
   */
  private static String withoutLibrary(final String xml) {
    return xml.substring(0, xml.indexOf("<ionlibrary>")) + xml.substring(
        xml.indexOf("</ionlibrary>"));
  }

  /**
   * The library element holds the StorableIonLibrary as JSON.
   */
  private static String libraryJson(final String xml) {
    final int start = xml.indexOf("<ionlibrary>") + "<ionlibrary>".length();
    final int end = xml.indexOf("</ionlibrary>");
    assertTrue(start > 0 && end > start, "no ion library in the written file");
    return xml.substring(start, end).trim();
  }

  private static int countOccurrences(final String text, final String needle) {
    int count = 0;
    for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
      count++;
    }
    return count;
  }

  private static IonNetwork byId(final List<IonNetwork> networks, final int id) {
    return networks.stream().filter(net -> net.getID() == id).findFirst()
        .orElseThrow(() -> new AssertionError("no network with id " + id));
  }

  private static List<Integer> rowIds(final IonNetwork network) {
    return network.getRows().stream().map(FeatureListRow::getID).sorted(Comparator.naturalOrder())
        .toList();
  }

  private static ResultFormula resultFormula(final String formula, final double searchedMass) {
    return new ResultFormula(FormulaUtils.createMajorIsotopeMolFormulaWithCharge(formula), null,
        null, null, null, searchedMass);
  }

  private ModularFeatureList newFeatureList(final String name) {
    final ModularFeatureList flist = new ModularFeatureList(name, null, raw);
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
