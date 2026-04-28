package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.correlation.RowGroupFull;
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link SimpleSeederComponentizer} role assignment and componentization.
 * <p>
 * Mirrors scenarios #1–7 from the CompoundDashboard implementation plan. Pure JUnit / Mockito —
 * no JavaFX bootstrap needed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SimpleSeederComponentizerTest {

  @Mock
  RawDataFile raw;

  private final IonType protonated = new IonType(IonModification.H);
  private final IonType sodiated = new IonType(IonModification.NA);

  private final MZTolerance mzTol = new MZTolerance(0.005, 10);
  private final RTTolerance rtTol = new RTTolerance(0.2f, Unit.MINUTES);

  // ---- helpers ----

  private @org.jetbrains.annotations.NotNull ModularFeatureList newFeatureList(
      final String name) {
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

  private RowGroup newGroup(final ModularFeatureList flist, final int id,
      final ModularFeatureListRow... rows) {
    final RowGroup group = new RowGroupFull(List.of(raw), id);
    for (final ModularFeatureListRow r : rows) {
      group.add(r);
    }
    final List<RowGroup> groups = new ArrayList<>(
        flist.getGroups() == null ? List.of() : flist.getGroups());
    groups.add(group);
    flist.setGroups(groups);
    return group;
  }

  private CompoundList newTargetList(final ModularFeatureList flist) {
    return new CompoundList(flist, null, 16);
  }

  // ---------- Test 1: 1 IonNetwork with M+H, M+Na + 13C correlated row ----------

  @Test
  void test1_singleNetworkWithIsotopologue() {
    final ModularFeatureList flist = newFeatureList("test1");
    final double baseMz = 200.0;
    final ModularFeatureListRow rH = row(flist, 1, baseMz + protonated.getMassDifference(), 5.0f,
        1000f);
    final ModularFeatureListRow rNa = row(flist, 2, baseMz + sodiated.getMassDifference(), 5.0f,
        500f);
    // 13C isotopologue of M+H, ~1.003 m/z higher, same RT
    final ModularFeatureListRow rIso = row(flist, 3,
        baseMz + protonated.getMassDifference() + 1.003355, 5.0f, 800f);

    IonIdentity.addAdductIdentityToRow(mzTol, rH, protonated, rNa, sodiated);

    // RowGroup containing all three so the isotopologue is merged into the IIN component
    newGroup(flist, 0, rH, rNa, rIso);

    final List<ModularCompoundRow> compounds = new SimpleSeederComponentizer(mzTol, rtTol)
        .componentize(flist, newTargetList(flist));

    assertEquals(1, compounds.size(), "Expected single compound");
    final ModularCompoundRow cr = compounds.get(0);
    assertEquals(3, cr.compoundSize());

    // Representative is the M+H row
    assertEquals(rH.getID(), cr.getPreferredRow().getID());

    final Set<CompoundMemberRole> roles = collectRoles(cr);
    assertTrue(roles.contains(CompoundMemberRole.REPRESENTATIVE));
    assertTrue(roles.contains(CompoundMemberRole.ADDUCT));
    assertTrue(roles.contains(CompoundMemberRole.ISOTOPOLOGUE));
  }

  // ---------- Test 2: 2 disjoint IonNetworks ----------

  @Test
  void test2_twoDisjointNetworks() {
    final ModularFeatureList flist = newFeatureList("test2");
    final ModularFeatureListRow a1 = row(flist, 1, 200.0 + protonated.getMassDifference(), 3.0f,
        1000f);
    final ModularFeatureListRow a2 = row(flist, 2, 200.0 + sodiated.getMassDifference(), 3.0f,
        500f);
    IonIdentity.addAdductIdentityToRow(mzTol, a1, protonated, a2, sodiated);

    final ModularFeatureListRow b1 = row(flist, 3, 400.0 + protonated.getMassDifference(), 8.0f,
        2000f);
    final ModularFeatureListRow b2 = row(flist, 4, 400.0 + sodiated.getMassDifference(), 8.0f,
        700f);
    IonIdentity.addAdductIdentityToRow(mzTol, b1, protonated, b2, sodiated);

    final List<ModularCompoundRow> compounds = new SimpleSeederComponentizer(mzTol, rtTol)
        .componentize(flist, newTargetList(flist));

    // Two networks → two compounds; remaining rows (none here) would be singletons
    assertEquals(2, compounds.size());
  }

  // ---------- Test 3: RowGroup only, no IIN → all CORRELATED ----------

  @Test
  void test3_rowGroupOnly() {
    final ModularFeatureList flist = newFeatureList("test3");
    final ModularFeatureListRow r1 = row(flist, 1, 100.0, 2.0f, 1000f);
    final ModularFeatureListRow r2 = row(flist, 2, 150.0, 2.0f, 500f);
    final ModularFeatureListRow r3 = row(flist, 3, 200.0, 2.0f, 200f);
    newGroup(flist, 0, r1, r2, r3);

    final List<ModularCompoundRow> compounds = new SimpleSeederComponentizer(mzTol, rtTol)
        .componentize(flist, newTargetList(flist));

    assertEquals(1, compounds.size());
    final ModularCompoundRow cr = compounds.get(0);
    assertEquals(3, cr.compoundSize());

    long correlated = cr.getCompoundMembers().stream()
        .filter(m -> m.role() == CompoundMemberRole.CORRELATED).count();
    long rep = cr.getCompoundMembers().stream()
        .filter(m -> m.role() == CompoundMemberRole.REPRESENTATIVE).count();
    assertEquals(1, rep);
    assertEquals(2, correlated);
  }

  // ---------- Test 4: RowGroup spanning 2 distinct IINs → IIN-safety guard splits ----------

  @Test
  void test4_groupSpansDistinctIins() {
    final ModularFeatureList flist = newFeatureList("test4");
    final ModularFeatureListRow a1 = row(flist, 1, 200.0 + protonated.getMassDifference(), 3.0f,
        1000f);
    final ModularFeatureListRow a2 = row(flist, 2, 200.0 + sodiated.getMassDifference(), 3.0f,
        500f);
    IonIdentity.addAdductIdentityToRow(mzTol, a1, protonated, a2, sodiated);

    final ModularFeatureListRow b1 = row(flist, 3, 400.0 + protonated.getMassDifference(), 3.05f,
        2000f);
    final ModularFeatureListRow b2 = row(flist, 4, 400.0 + sodiated.getMassDifference(), 3.05f,
        700f);
    IonIdentity.addAdductIdentityToRow(mzTol, b1, protonated, b2, sodiated);

    // a single RowGroup that bridges both IIN networks — should be skipped by safety guard
    newGroup(flist, 0, a1, a2, b1, b2);

    final List<ModularCompoundRow> compounds = new SimpleSeederComponentizer(mzTol, rtTol)
        .componentize(flist, newTargetList(flist));

    assertEquals(2, compounds.size(), "IIN-safety guard should keep networks separate");
  }

  // ---------- Test 5: Row in 2 IonNetworks → transitive merge into 1 compound ----------

  @Test
  void test5_rowInTwoNetworks() {
    final ModularFeatureList flist = newFeatureList("test5");
    // shared row participates in two distinct adduct relationships → networks merge transitively
    final ModularFeatureListRow shared = row(flist, 1,
        200.0 + protonated.getMassDifference(), 4.0f, 1000f);
    final ModularFeatureListRow naRow = row(flist, 2,
        200.0 + sodiated.getMassDifference(), 4.0f, 500f);
    final ModularFeatureListRow kRow = row(flist, 3, 200.0 + 38.963158, 4.0f, 300f);

    IonIdentity.addAdductIdentityToRow(mzTol, shared, protonated, naRow, sodiated);
    IonIdentity.addAdductIdentityToRow(mzTol, shared, protonated, kRow,
        new IonType(IonModification.K));

    final List<ModularCompoundRow> compounds = new SimpleSeederComponentizer(mzTol, rtTol)
        .componentize(flist, newTargetList(flist));

    assertEquals(1, compounds.size());
    assertEquals(3, compounds.get(0).compoundSize());
  }

  // ---------- Test 6: Empty feature list → CompoundGrouperTask precondition fails ----------

  @Test
  void test6_emptyFeatureList() {
    final ModularFeatureList flist = newFeatureList("test6");
    final CompoundGrouperParameters params = new CompoundGrouperParameters();
    params.setParameter(CompoundGrouperParameters.MZ_TOLERANCE, mzTol);
    params.setParameter(CompoundGrouperParameters.RT_TOLERANCE, rtTol);

    final CompoundGrouperTask task = new CompoundGrouperTask(flist, params, null,
        java.time.Instant.now());
    task.run();
    assertEquals(io.github.mzmine.taskcontrol.TaskStatus.ERROR, task.getStatus());
    assertNotNull(task.getErrorMessage());
  }

  // ---------- Test 7: No IIN and no RowGroups → precondition error ----------

  @Test
  void test7_noIinNoGroups() {
    final ModularFeatureList flist = newFeatureList("test7");
    row(flist, 1, 100.0, 1.0f, 100f);
    row(flist, 2, 200.0, 2.0f, 200f);
    // no ion identities, no groups

    final CompoundGrouperParameters params = new CompoundGrouperParameters();
    params.setParameter(CompoundGrouperParameters.MZ_TOLERANCE, mzTol);
    params.setParameter(CompoundGrouperParameters.RT_TOLERANCE, rtTol);

    final CompoundGrouperTask task = new CompoundGrouperTask(flist, params, null,
        java.time.Instant.now());
    task.run();
    assertEquals(io.github.mzmine.taskcontrol.TaskStatus.ERROR, task.getStatus());
    assertNotNull(task.getErrorMessage());
  }

  // ---- helpers ----

  private static Set<CompoundMemberRole> collectRoles(final ModularCompoundRow cr) {
    final Set<CompoundMemberRole> roles = new java.util.HashSet<>();
    for (final CompoundFeatureMember m : cr.getCompoundMembers()) {
      roles.add(m.role());
    }
    return roles;
  }
}
