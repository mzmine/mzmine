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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.BuildingIonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.util.FormulaUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Files that {@link IonNetworksSaver} wrote in the past must keep loading. Projects out there hold
 * exactly these files, and an ion identity that comes back with the wrong ion type is silent data
 * corruption, so every change to the format or to anything it depends on - the ion library JSON,
 * the ion type or ion part definitions, any sorting - has to be checked against them rather than
 * against a file the current code just wrote.
 * <p>
 * All reference files describe the same fixture, see
 * {@link #referenceNetworks(ModularFeatureList)}, and
 * {@link #assertFixture(ModularFeatureList, List)} states what loading them has to produce. Add a
 * new one whenever the written format changes, with {@link #regenerateReferenceFile()}.
 */
class IonNetworkFormatCompatibilityTest {

  private static final String RESOURCE_DIR = "/io/github/mzmine/datamodel/identities/iontype/project_io/";
  /**
   * The file the current code writes for the fixture. Regenerate it only together with a deliberate
   * format change, and keep the older files.
   */
  private static final String CURRENT_FORMAT_FILE = "ion_networks_v1.xml";

  private static final int NET_A = 10;
  private static final int NET_B = 20;
  private static final String CONSENSUS_FORMULA = "C10H15N";
  private static final String ION_FORMULA = "C10H16N";

  private static final IonType PROTONATED = IonType.create(IonParts.H);
  private static final IonType SODIATED = IonType.create(IonParts.NA);
  private static final IonType POTASSIATED = IonType.create(IonParts.K);

  /**
   * Every reference file of every format version the saver ever wrote.
   */
  @ParameterizedTest
  @ValueSource(strings = {CURRENT_FORMAT_FILE, "ion_networks_v1_unsorted_library.xml"})
  void referenceFilesStillLoad(final String fileName) throws Exception {
    final ModularFeatureList flist = fixtureFeatureList();
    final List<IonNetwork> loaded;
    try (InputStream in = open(fileName)) {
      loaded = IonNetworksLoader.load(in, flist);
    }
    assertFixture(flist, loaded);
  }

  /**
   * The ion type an ion references is the position in the library of its own file. This file holds
   * the very same networks as {@link #CURRENT_FORMAT_FILE} but with the library ion types in a
   * different order and the references renumbered to match, so it only loads correctly if the
   * indices are taken from the file instead of being re-derived from a sorting - which is free to
   * change and would otherwise silently repoint every ion of every old project.
   */
  @Test
  void ionTypeIndicesComeFromTheFileNotFromASorting() throws Exception {
    final ModularFeatureList sorted = fixtureFeatureList();
    final ModularFeatureList unsorted = fixtureFeatureList();
    try (InputStream in = open(CURRENT_FORMAT_FILE)) {
      IonNetworksLoader.load(in, sorted);
    }
    try (InputStream in = open("ion_networks_v1_unsorted_library.xml")) {
      IonNetworksLoader.load(in, unsorted);
    }

    for (final int rowId : List.of(1, 2, 3)) {
      assertEquals(ionTypes(sorted, rowId), ionTypes(unsorted, rowId),
          "row %d must get the same ion types no matter how the library of the file is ordered".formatted(
              rowId));
    }
  }

  /**
   * What the current code writes has to load back the same way, otherwise the reference files are
   * testing something the saver no longer produces.
   */
  @Test
  void currentFormatMatchesItsReferenceFile() throws Exception {
    final String written = writeFixture();
    final String reference = new String(open(CURRENT_FORMAT_FILE).readAllBytes(),
        StandardCharsets.UTF_8);

    assertEquals(normalize(withoutLibraryHeader(reference)),
        normalize(withoutLibraryHeader(written)),
        "the saver writes a different file than %s. If that is intended, add a new reference file with regenerateReferenceFile() and keep this one.".formatted(
            CURRENT_FORMAT_FILE));
  }

  /**
   * Writes the fixture as the current code saves it. Enable, run, and commit the result as a new
   * reference file whenever the format changes deliberately - never overwrite an existing one, the
   * point of those files is that they were written by an older version.
   */
  @Test
  @Disabled("run manually to add a reference file for a new format version")
  void regenerateReferenceFile() throws Exception {
    // gradle runs tests with the module directory as the working directory
    final Path file = Path.of("src/test/resources", RESOURCE_DIR.substring(1), CURRENT_FORMAT_FILE);
    Files.createDirectories(file.getParent());
    Files.writeString(file, writeFixture(), StandardCharsets.UTF_8);
  }

  // ---- the fixture ----

  /**
   * Two overlapping networks, so that a row carries more than one ion identity and the order of
   * those decides its best ion: network A has r1 [M+H] and r2 [M+Na], network B the competing r2
   * [M+H] together with r3 [M+K]. Formulas sit both on a network and on a single ion.
   */
  private static List<IonNetwork> referenceNetworks(final ModularFeatureList flist) {
    final FeatureListRow r1 = flist.findRowByID(1);
    final FeatureListRow r2 = flist.findRowByID(2);
    final FeatureListRow r3 = flist.findRowByID(3);

    final BuildingIonNetwork buildingA = new BuildingIonNetwork(NET_A);
    final IonIdentity r1H = buildingA.put(r1, new IonIdentity(PROTONATED));
    final IonIdentity r2Na = buildingA.put(r2, new IonIdentity(SODIATED));

    final BuildingIonNetwork buildingB = new BuildingIonNetwork(NET_B);
    final IonIdentity r2H = buildingB.put(r2, new IonIdentity(PROTONATED));
    final IonIdentity r3K = buildingB.put(r3, new IonIdentity(POTASSIATED));

    final IonNetwork netA = buildingA.toSimple();
    final IonNetwork netB = buildingB.toSimple();

    r1.setIonIdentities(List.of(r1H));
    // the M+H hypothesis of network B is the best ion of r2
    r2.setIonIdentities(List.of(r2H, r2Na));
    r3.setIonIdentities(List.of(r3K));

    netA.addMolFormulas(List.of(resultFormula(CONSENSUS_FORMULA, netA.getNeutralMass())));
    r1H.addMolFormulas(List.of(resultFormula(ION_FORMULA, r1.getAverageMZ())));

    return List.of(netA, netB);
  }

  /**
   * Everything a reference file has to restore.
   */
  private static void assertFixture(final ModularFeatureList flist, final List<IonNetwork> loaded) {
    assertEquals(2, loaded.size(), "both networks");
    final IonNetwork netA = byId(loaded, NET_A);
    final IonNetwork netB = byId(loaded, NET_B);

    assertEquals(List.of(1, 2), rowIds(netA));
    assertEquals(List.of(2, 3), rowIds(netB));

    assertEquals(PROTONATED, netA.get(flist.findRowByID(1)).getIonType());
    assertEquals(SODIATED, netA.get(flist.findRowByID(2)).getIonType());
    assertEquals(PROTONATED, netB.get(flist.findRowByID(2)).getIonType());
    assertEquals(POTASSIATED, netB.get(flist.findRowByID(3)).getIonType());

    // the order within a row, which is what makes one of its ions the best one
    final FeatureListRow r2 = flist.findRowByID(2);
    assertEquals(List.of(PROTONATED, SODIATED), ionTypes(flist, 2));
    final IonIdentity bestOfR2 = r2.getBestIonIdentity();
    assertNotNull(bestOfR2);
    assertEquals(PROTONATED, bestOfR2.getIonType());
    assertEquals(NET_B, bestOfR2.getNetwork().getID());

    assertEquals(List.of(CONSENSUS_FORMULA), formulaStrings(netA.getMolFormulas()));
    assertEquals(List.of(ION_FORMULA),
        formulaStrings(netA.get(flist.findRowByID(1)).getMolFormulas()));
  }

  // ---- helpers ----

  private static String writeFixture() throws Exception {
    final ModularFeatureList flist = fixtureFeatureList();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    IonNetworksSaver.save(referenceNetworks(flist), out);
    return out.toString(StandardCharsets.UTF_8);
  }

  private static InputStream open(final String fileName) {
    final InputStream in = IonNetworkFormatCompatibilityTest.class.getResourceAsStream(
        RESOURCE_DIR + fileName);
    assertNotNull(in, "missing reference file " + RESOURCE_DIR + fileName);
    return in;
  }

  /**
   * The library carries a fresh id and save date on every write, so those cannot be compared.
   */
  private static String withoutLibraryHeader(final String xml) {
    return xml.replaceAll("\"id\":\"[^\"]*\",\"origin\"", "\"id\":\"<id>\",\"origin\"")
        .replaceAll("\"savedDate\":\\[[^]]*]", "\"savedDate\":[]")
        .replaceAll("\"lastUpdatedDate\":\\[[^]]*]", "\"lastUpdatedDate\":[]");
  }

  private static String normalize(final String xml) {
    return xml.replace("\r\n", "\n").strip();
  }

  private static List<IonType> ionTypes(final ModularFeatureList flist, final int rowId) {
    return flist.findRowByID(rowId).getIonIdentities().stream().map(IonIdentity::getIonType)
        .toList();
  }

  private static List<String> formulaStrings(final List<ResultFormula> formulas) {
    return formulas.stream().map(ResultFormula::getFormulaAsString).toList();
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

  /**
   * Three rows with the ids and m/z the reference files were written from.
   */
  private static ModularFeatureList fixtureFeatureList() {
    final ModularFeatureList flist = new ModularFeatureList("fixture", null, List.of());
    flist.addRowType(new IDType());
    flist.addRowType(new RTType());
    flist.addRowType(new MZType());
    flist.addRowType(new HeightType());
    flist.addRowType(new IonIdentityListType());

    row(flist, 1, 200.0 + PROTONATED.totalMass());
    row(flist, 2, 200.0 + SODIATED.totalMass());
    row(flist, 3, 400.0 + POTASSIATED.totalMass());
    return flist;
  }

  private static void row(final ModularFeatureList flist, final int id, final double mz) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    row.set(MZType.class, mz);
    row.set(RTType.class, 1.0f);
    row.set(HeightType.class, 1000f);
    flist.addRow(row);
  }
}
