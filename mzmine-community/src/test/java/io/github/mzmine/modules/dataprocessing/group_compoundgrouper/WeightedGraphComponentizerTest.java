package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundContradiction;
import io.github.mzmine.datamodel.features.compoundlist.CompoundContradiction.ContradictionType;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundContradictionListType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.WeightedGraphComponentizer.Config;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link WeightedGraphComponentizer}: dense-core grouping, single-best loose-row
 * assignment with near-tie dual membership, annotation-forced single membership, the oversized-core
 * penalty, annotation-conflict splitting, and contradiction recording. Pure JUnit / Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WeightedGraphComponentizerTest {

  @Mock
  RawDataFile raw;

  private final IonType protonated = IonType.create(IonParts.H);
  private final IonType sodiated = IonType.create(IonParts.NA);

  private final MZTolerance mzTol = new MZTolerance(0.005, 10);
  private final RTTolerance rtTol = new RTTolerance(0.2f, Unit.MINUTES);

  // ---- config ----

  private Config config(final boolean splitOnAnnotationConflict) {
    return new Config(mzTol, rtTol, 2.0, 1.0, 0.5, 0.5, 0.5, 0.3, 0.5, 0.6, 0.1, 10, 0.3, 0.05, 3,
        0.5, splitOnAnnotationConflict);
  }

  private WeightedGraphComponentizer newComponentizer() {
    return new WeightedGraphComponentizer(config(true), new PreferredIonTypeRepresentativeSelector());
  }

  private WeightedGraphComponentizer newComponentizer(final boolean split) {
    return new WeightedGraphComponentizer(config(split),
        new PreferredIonTypeRepresentativeSelector());
  }

  // ---- helpers ----

  private ModularFeatureList newFeatureList(final String name) {
    final ModularFeatureList flist = new ModularFeatureList(name, null, raw);
    flist.addRowType(new IDType());
    flist.addRowType(new RTType());
    flist.addRowType(new MZType());
    flist.addRowType(new IonIdentityListType());
    flist.addRowType(new FeatureGroupType());
    return flist;
  }

  private ModularFeatureListRow row(final ModularFeatureList flist, final int id, final double mz,
      final float rt, final float height) {
    final ModularFeatureListRow base = new ModularFeatureListRow(flist, id);
    final ModularFeatureListRow s = spy(base);
    doReturn(mz).when(s).getAverageMZ();
    doReturn(rt).when(s).getAverageRT();
    doReturn(height).when(s).getMaxHeight();
    doReturn(height).when(s).getMaxDataPointIntensity();
    flist.addRow(s);
    return s;
  }

  private static IonNetwork buildNetwork(final int id, final Object[][] rowIonPairs) {
    final IonNetwork net = new IonNetwork(id);
    for (final Object[] pair : rowIonPairs) {
      final ModularFeatureListRow row = (ModularFeatureListRow) pair[0];
      final IonType ionType = (IonType) pair[1];
      final IonIdentity ion = new IonIdentity(ionType);
      net.put(row, ion);
      row.addIonIdentity(ion);
    }
    return net;
  }

  private static void addCorrelation(final ModularFeatureList flist, final ModularFeatureListRow a,
      final ModularFeatureListRow b, final float score) {
    flist.getRowMaps().addRowsRelationship(a, b,
        new R2RSimpleSimilarity(a, b, Type.MS1_FEATURE_CORR, score));
  }

  private static SpectralDBAnnotation libMatch(final String inchiKey) {
    final SpectralDBAnnotation m = mock(SpectralDBAnnotation.class);
    doReturn(inchiKey).when(m).getInChIKey();
    return m;
  }

  private CompoundList newTargetList(final ModularFeatureList flist) {
    return new CompoundList(flist, null, 16);
  }

  // ---------- Test 1: IIN net + 13C isotopologue attached via correlation → 1 compound ----------

  @Test
  void test1_singleNetworkWithIsotopologue() {
    final ModularFeatureList flist = newFeatureList("t1");
    final double baseMz = 200.0;
    final ModularFeatureListRow rH = row(flist, 1, baseMz + protonated.totalMass(), 5.0f, 1000f);
    final ModularFeatureListRow rNa = row(flist, 2, baseMz + sodiated.totalMass(), 5.0f, 500f);
    final ModularFeatureListRow rIso = row(flist, 3, baseMz + protonated.totalMass() + 1.003355,
        5.0f, 800f);
    buildNetwork(1, new Object[][]{{rH, protonated}, {rNa, sodiated}});
    addCorrelation(flist, rH, rIso, 0.9f);

    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist,
        newTargetList(flist));

    assertEquals(1, compounds.size());
    final ModularCompoundRow cr = compounds.getFirst();
    assertEquals(3, cr.compoundSize());
    assertEquals(rH.getID(), cr.getPreferredRow().getID());
    final Set<CompoundMemberRole> roles = rolesOf(cr);
    assertTrue(roles.contains(CompoundMemberRole.REPRESENTATIVE));
    assertTrue(roles.contains(CompoundMemberRole.ADDUCT));
    assertTrue(roles.contains(CompoundMemberRole.ISOTOPOLOGUE));
  }

  // ---------- Test 2: two disjoint IIN nets → 2 compounds ----------

  @Test
  void test2_twoDisjointNetworks() {
    final ModularFeatureList flist = newFeatureList("t2");
    final ModularFeatureListRow a1 = row(flist, 1, 200.0 + protonated.totalMass(), 3.0f, 1000f);
    final ModularFeatureListRow a2 = row(flist, 2, 200.0 + sodiated.totalMass(), 3.0f, 500f);
    buildNetwork(1, new Object[][]{{a1, protonated}, {a2, sodiated}});
    final ModularFeatureListRow b1 = row(flist, 3, 400.0 + protonated.totalMass(), 8.0f, 2000f);
    final ModularFeatureListRow b2 = row(flist, 4, 400.0 + sodiated.totalMass(), 8.0f, 700f);
    buildNetwork(2, new Object[][]{{b1, protonated}, {b2, sodiated}});

    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist,
        newTargetList(flist));

    assertEquals(2, compounds.size());
  }

  // ---------- Test 3: dense correlation triangle, no IIN → 1 compound ----------

  @Test
  void test3_correlationOnlyTriangle() {
    final ModularFeatureList flist = newFeatureList("t3");
    final ModularFeatureListRow r1 = row(flist, 1, 100.0, 2.0f, 1000f);
    final ModularFeatureListRow r2 = row(flist, 2, 150.0, 2.0f, 500f);
    final ModularFeatureListRow r3 = row(flist, 3, 200.0, 2.0f, 200f);
    addCorrelation(flist, r1, r2, 0.9f);
    addCorrelation(flist, r2, r3, 0.9f);
    addCorrelation(flist, r1, r3, 0.9f);

    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist,
        newTargetList(flist));

    assertEquals(1, compounds.size());
    assertEquals(3, compounds.getFirst().compoundSize());
  }

  // ---------- Test 4: bridge row near-tie → dual membership ----------

  @Test
  void test4_nearTieDualMembership() {
    final ModularFeatureList flist = newFeatureList("t4");
    final ModularFeatureListRow a1 = row(flist, 1, 200.0 + protonated.totalMass(), 3.0f, 1000f);
    final ModularFeatureListRow a2 = row(flist, 2, 200.0 + sodiated.totalMass(), 3.0f, 500f);
    buildNetwork(1, new Object[][]{{a1, protonated}, {a2, sodiated}});
    final ModularFeatureListRow b1 = row(flist, 3, 400.0 + protonated.totalMass(), 3.0f, 2000f);
    final ModularFeatureListRow b2 = row(flist, 4, 400.0 + sodiated.totalMass(), 3.0f, 700f);
    buildNetwork(2, new Object[][]{{b1, protonated}, {b2, sodiated}});
    // bridge row, no IIN, no annotation — equally correlated to both IINs at the same RT
    final ModularFeatureListRow bridge = row(flist, 5, 300.0, 3.0f, 100f);
    addCorrelation(flist, bridge, a1, 0.8f);
    addCorrelation(flist, bridge, b1, 0.8f);

    final CompoundList target = newTargetList(flist);
    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist, target);
    target.setRows(compounds);

    assertEquals(2, compounds.size());
    assertEquals(2, target.findCompoundsOf(bridge).size(),
        "Bridge row must be a member of both compounds on a near-tie");
  }

  // ---------- Test 5: annotation forces single membership despite a near-tie ----------

  @Test
  void test5_annotationForcesSingle() {
    final ModularFeatureList flist = newFeatureList("t5");
    final ModularFeatureListRow a1 = row(flist, 1, 200.0 + protonated.totalMass(), 3.0f, 1000f);
    final ModularFeatureListRow a2 = row(flist, 2, 200.0 + sodiated.totalMass(), 3.0f, 500f);
    buildNetwork(1, new Object[][]{{a1, protonated}, {a2, sodiated}});
    final ModularFeatureListRow b1 = row(flist, 3, 400.0 + protonated.totalMass(), 3.0f, 2000f);
    final ModularFeatureListRow b2 = row(flist, 4, 400.0 + sodiated.totalMass(), 3.0f, 700f);
    buildNetwork(2, new Object[][]{{b1, protonated}, {b2, sodiated}});

    final String keyA = "AAAAAAAAAAAAAA-BBBBBBBBBB-C";
    final SpectralDBAnnotation matchA = libMatch(keyA);
    doReturn(List.of(matchA)).when(a1).getSpectralLibraryMatches();

    // loose row equally correlated to both IINs, but its annotation matches core A's representative
    final ModularFeatureListRow loose = row(flist, 5, 300.0, 3.0f, 100f);
    doReturn(List.of(matchA)).when(loose).getSpectralLibraryMatches();
    doReturn(matchA).when(loose).getPreferredAnnotation();
    addCorrelation(flist, loose, a1, 0.8f);
    addCorrelation(flist, loose, b1, 0.8f);

    final CompoundList target = newTargetList(flist);
    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist, target);
    target.setRows(compounds);

    final List<ModularCompoundRow> withLoose = target.findCompoundsOf(loose);
    assertEquals(1, withLoose.size(), "Annotated loose row must join a single compound");
    assertTrue(withLoose.getFirst().getMemberRows().stream().anyMatch(r -> r.getID() == a1.getID()),
        "Loose row must join core A (matching annotation), not core B");
  }

  // ---------- Test 6: conflicting MS2 matches → contradiction recorded (split off) ----------

  @Test
  void test6_contradictionRecorded() {
    final ModularFeatureList flist = newFeatureList("t6");
    final ModularFeatureListRow rH = row(flist, 1, 200.0 + protonated.totalMass(), 5.0f, 1000f);
    final ModularFeatureListRow rNa = row(flist, 2, 200.0 + sodiated.totalMass(), 5.0f, 500f);
    buildNetwork(1, new Object[][]{{rH, protonated}, {rNa, sodiated}});
    doReturn(List.of(libMatch("KEYONE00000000-XXXXXXXXXX-A"))).when(rH).getSpectralLibraryMatches();
    doReturn(List.of(libMatch("KEYTWO00000000-YYYYYYYYYY-B"))).when(rNa).getSpectralLibraryMatches();

    final List<ModularCompoundRow> compounds = newComponentizer(false).componentize(flist,
        newTargetList(flist));

    assertEquals(1, compounds.size());
    final ModularCompoundRow cr = compounds.getFirst();
    final List<CompoundContradiction> rollup = cr.get(CompoundContradictionListType.class);
    assertNotNull(rollup);
    assertFalse(rollup.isEmpty());
    assertTrue(rollup.stream().anyMatch(c -> c.type() == ContradictionType.MS2_ANNOTATION_CONFLICT));
    // recorded on the involved member rows as well
    final List<CompoundContradiction> onRow = rH.get(CompoundContradictionListType.class);
    assertNotNull(onRow);
    assertTrue(onRow.stream().anyMatch(c -> c.type() == ContradictionType.MS2_ANNOTATION_CONFLICT));
  }

  // ---------- Test 7: split on annotation conflict → 2 compounds ----------

  @Test
  void test7_splitOnAnnotationConflict() {
    final ModularFeatureList flist = newFeatureList("t7");
    final ModularFeatureListRow rH = row(flist, 1, 200.0 + protonated.totalMass(), 5.0f, 1000f);
    final ModularFeatureListRow rNa = row(flist, 2, 200.0 + sodiated.totalMass(), 5.0f, 500f);
    buildNetwork(1, new Object[][]{{rH, protonated}, {rNa, sodiated}});
    doReturn(List.of(libMatch("KEYONE00000000-XXXXXXXXXX-A"))).when(rH).getSpectralLibraryMatches();
    doReturn(List.of(libMatch("KEYTWO00000000-YYYYYYYYYY-B"))).when(rNa).getSpectralLibraryMatches();

    final List<ModularCompoundRow> compounds = newComponentizer(true).componentize(flist,
        newTargetList(flist));

    assertEquals(2, compounds.size(), "Conflicting MS2 structures must split into two compounds");
  }

  // ---------- Test 8: validateInputs parity ----------

  @Test
  void test8_validateInputs() {
    final ModularFeatureList empty = newFeatureList("t8a");
    assertNotNull(newComponentizer().validateInputs(empty));

    final ModularFeatureList noEvidence = newFeatureList("t8b");
    row(noEvidence, 1, 100.0, 1.0f, 100f);
    row(noEvidence, 2, 200.0, 2.0f, 200f);
    assertNotNull(newComponentizer().validateInputs(noEvidence));
  }

  // ---------- Test 9: oversized-core penalty steers a loose row to the smaller core ----------

  @Test
  void test9_sizePenalty() {
    final ModularFeatureList flist = newFeatureList("t9");
    // big IIN core: 11 members (all at the same RT)
    final Object[][] bigPairs = new Object[11][];
    ModularFeatureListRow bigFirst = null;
    for (int i = 0; i < 11; i++) {
      final ModularFeatureListRow r = row(flist, 100 + i, 300.0 + i, 4.0f, 1000f - i);
      if (i == 0) {
        bigFirst = r;
      }
      bigPairs[i] = new Object[]{r, protonated};
    }
    buildNetwork(1, bigPairs);

    // small IIN core: 3 members
    final ModularFeatureListRow s1 = row(flist, 1, 500.0 + protonated.totalMass(), 4.0f, 2000f);
    final ModularFeatureListRow s2 = row(flist, 2, 500.0 + sodiated.totalMass(), 4.0f, 900f);
    final ModularFeatureListRow s3 = row(flist, 3, 520.0, 4.0f, 800f);
    buildNetwork(2, new Object[][]{{s1, protonated}, {s2, sodiated}});
    addCorrelation(flist, s1, s3, 0.9f);

    // loose row equally correlated to one member of each core
    final ModularFeatureListRow loose = row(flist, 4, 700.0, 4.0f, 100f);
    addCorrelation(flist, loose, bigFirst, 0.8f);
    addCorrelation(flist, loose, s1, 0.8f);

    final CompoundList target = newTargetList(flist);
    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist, target);
    target.setRows(compounds);

    final List<ModularCompoundRow> withLoose = target.findCompoundsOf(loose);
    assertEquals(1, withLoose.size(), "Loose row should go to a single (smaller) core");
    assertTrue(withLoose.getFirst().getMemberRows().stream().anyMatch(r -> r.getID() == s1.getID()),
        "Loose row should be steered to the smaller core by the size penalty");
  }

  // ---- helpers ----

  private static Set<CompoundMemberRole> rolesOf(final ModularCompoundRow cr) {
    return cr.getCompoundMembers().stream().map(m -> m.role())
        .collect(java.util.stream.Collectors.toSet());
  }
}
