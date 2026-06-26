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

package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import static io.github.mzmine.modules.dataprocessing.group_compoundgrouper.WeightedGraphComponentizerTest.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link SimpleSeederComponentizer} role assignment and componentization.
 * <p>
 * Pure JUnit / Mockito — no JavaFX bootstrap needed. Built around the post-IIN-rework API
 * (IonType / IonPart / IonNetwork) and {@code featureList.getMs1CorrelationMap()} as the
 * correlation source.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SimpleSeederComponentizerTest {

  @Mock
  RawDataFile raw;

  private final IonType protonated = IonType.create(IonParts.H);
  private final IonType sodiated = IonType.create(IonParts.NA);
  private final IonType potassiated = IonType.create(IonParts.K);

  private final MZTolerance mzTol = new MZTolerance(0.005, 10);
  private final RTTolerance rtTol = new RTTolerance(0.2f, Unit.MINUTES);
  private final double minDensity = 0.3;

  // ---- helpers ----

  private @NotNull ModularFeatureList newFeatureList(
      final String name) {
    final ModularFeatureList flist = new ModularFeatureList(name, null, raw);
    flist.addRowType(new IDType());
    flist.addRowType(new RTType());
    flist.addRowType(new MZType());
    flist.addRowType(new IonIdentityListType());
    return flist;
  }

  /**
   * Build an {@link IonNetwork} that links the given (row, ion) pairs. Also attaches the
   * IonIdentity to each row so {@code IonNetworkLogic.streamNetworks(...)} can discover the
   * network through {@code row.getIonIdentities()}.
   */
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

  /**
   * Add an MS1 correlation edge between two rows.
   */
  private static void addCorrelation(final ModularFeatureList flist,
      final ModularFeatureListRow a, final ModularFeatureListRow b, final float score) {
    flist.getRowMaps().addRowsRelationship(a, b,
        new R2RSimpleSimilarity(a, b, Type.MS1_FEATURE_CORR, score));
  }

  private CompoundList newTargetList(final ModularFeatureList flist) {
    return new CompoundList(flist, null, 16);
  }

  private SimpleSeederComponentizer newComponentizer() {
    return new SimpleSeederComponentizer(mzTol, rtTol, minDensity,
        new PreferredIonTypeRepresentativeSelector());
  }

  // ---------- Test 1: 1 IonNetwork with M+H, M+Na + 13C isotopologue via correlation edge ----------

  @Test
  void test1_singleNetworkWithIsotopologue() {
    final ModularFeatureList flist = newFeatureList("test1");
    final double baseMz = 200.0;
    final ModularFeatureListRow rH = row(flist, 1, baseMz + protonated.totalMass(), 5.0f, 1000f);
    final ModularFeatureListRow rNa = row(flist, 2, baseMz + sodiated.totalMass(), 5.0f, 500f);
    // 13C isotopologue of M+H, ~1.003 m/z higher, same RT — not in IIN, attached via correlation
    final ModularFeatureListRow rIso = row(flist, 3, baseMz + protonated.totalMass() + 1.003355,
        5.0f, 800f);

    buildNetwork(1, new Object[][]{{rH, protonated}, {rNa, sodiated}});
    addCorrelation(flist, rH, rIso, 0.9f);

    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist,
        newTargetList(flist));

    assertEquals(1, compounds.size(), "Expected single compound");
    final ModularCompoundRow cr = compounds.get(0);
    assertEquals(3, cr.compoundSize());

    // Representative is the M+H row (highest tier in positive polarity)
    assertEquals(rH.getID(), cr.getPreferredRow().getID());

    final Set<CompoundMemberRole> roles = collectRoles(cr);
    assertTrue(roles.contains(CompoundMemberRole.REPRESENTATIVE));
    assertTrue(roles.contains(CompoundMemberRole.ADDUCT));
    assertTrue(roles.contains(CompoundMemberRole.ISOTOPOLOGUE));
  }

  // ---------- Test 2: 2 disjoint IonNetworks → 2 compounds ----------

  @Test
  void test2_twoDisjointNetworks() {
    final ModularFeatureList flist = newFeatureList("test2");
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
    final ModularFeatureList flist = newFeatureList("test3");
    final ModularFeatureListRow r1 = row(flist, 1, 100.0, 2.0f, 1000f);
    final ModularFeatureListRow r2 = row(flist, 2, 150.0, 2.0f, 500f);
    final ModularFeatureListRow r3 = row(flist, 3, 200.0, 2.0f, 200f);
    // pairwise correlated → density 1.0 → committed as one compound
    addCorrelation(flist, r1, r2, 0.9f);
    addCorrelation(flist, r2, r3, 0.9f);
    addCorrelation(flist, r1, r3, 0.9f);

    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist,
        newTargetList(flist));

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

  // ---------- Test 4: bridge row joins only its best-connected seed ----------

  @Test
  void test4_bridgeRowPrefersStrongerSeed() {
    final ModularFeatureList flist = newFeatureList("test4");
    final ModularFeatureListRow a1 = row(flist, 1, 200.0 + protonated.totalMass(), 3.0f, 1000f);
    final ModularFeatureListRow a2 = row(flist, 2, 200.0 + sodiated.totalMass(), 3.0f, 500f);
    buildNetwork(1, new Object[][]{{a1, protonated}, {a2, sodiated}});

    final ModularFeatureListRow b1 = row(flist, 3, 400.0 + protonated.totalMass(), 3.05f, 2000f);
    final ModularFeatureListRow b2 = row(flist, 4, 400.0 + sodiated.totalMass(), 3.05f, 700f);
    buildNetwork(2, new Object[][]{{b1, protonated}, {b2, sodiated}});

    // bridge row, no IIN — correlated more strongly to IIN A (a1) than IIN B (b1)
    final ModularFeatureListRow bridge = row(flist, 5, 300.0, 3.02f, 100f);
    addCorrelation(flist, bridge, a1, 0.85f);
    addCorrelation(flist, bridge, b1, 0.80f);

    final CompoundList target = newTargetList(flist);
    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist, target);
    target.setRows(compounds);

    // 2 compounds (one per IIN); bridge joins only the more strongly correlated seed A
    assertEquals(2, compounds.size(), "IIN seeds remain separate compounds");
    final List<ModularCompoundRow> withBridge = target.findCompoundsOf(bridge);
    assertEquals(1, withBridge.size(), "Bridge row joins only its single best-connected seed");
    assertTrue(withBridge.get(0).getMemberRows().stream().anyMatch(r -> r.getID() == a1.getID()),
        "Bridge row must join the more strongly correlated seed A");
  }

  // ---------- Test 4b: genuinely ambiguous bridge keeps dual membership ----------

  @Test
  void test4b_bridgeRowTieDualMembership() {
    final ModularFeatureList flist = newFeatureList("test4b");
    final ModularFeatureListRow a1 = row(flist, 1, 200.0 + protonated.totalMass(), 3.0f, 1000f);
    final ModularFeatureListRow a2 = row(flist, 2, 200.0 + sodiated.totalMass(), 3.0f, 500f);
    buildNetwork(1, new Object[][]{{a1, protonated}, {a2, sodiated}});

    final ModularFeatureListRow b1 = row(flist, 3, 400.0 + protonated.totalMass(), 3.05f, 2000f);
    final ModularFeatureListRow b2 = row(flist, 4, 400.0 + sodiated.totalMass(), 3.05f, 700f);
    buildNetwork(2, new Object[][]{{b1, protonated}, {b2, sodiated}});

    // bridge correlated equally to both IINs → genuinely ambiguous → dual membership
    final ModularFeatureListRow bridge = row(flist, 5, 300.0, 3.02f, 100f);
    addCorrelation(flist, bridge, a1, 0.80f);
    addCorrelation(flist, bridge, b1, 0.80f);

    final CompoundList target = newTargetList(flist);
    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist, target);
    target.setRows(compounds);

    assertEquals(2, compounds.size(), "IIN seeds remain separate compounds");
    assertEquals(2, target.findCompoundsOf(bridge).size(),
        "Equal correlation to both seeds is genuinely ambiguous → dual membership");
  }

  // ---------- Test 5: row in 2 IonNetworks → transitive merge into 1 compound ----------

  @Test
  void test5_rowInTwoNetworks() {
    final ModularFeatureList flist = newFeatureList("test5");
    final ModularFeatureListRow shared = row(flist, 1, 200.0 + protonated.totalMass(), 4.0f,
        1000f);
    final ModularFeatureListRow naRow = row(flist, 2, 200.0 + sodiated.totalMass(), 4.0f, 500f);
    final ModularFeatureListRow kRow = row(flist, 3, 200.0 + potassiated.totalMass(), 4.0f, 300f);

    buildNetwork(1, new Object[][]{{shared, protonated}, {naRow, sodiated}});
    buildNetwork(2, new Object[][]{{shared, protonated}, {kRow, potassiated}});

    final List<ModularCompoundRow> compounds = newComponentizer().componentize(flist,
        newTargetList(flist));

    assertEquals(2, compounds.size());
    assertEquals(2, compounds.get(0).compoundSize());
    assertEquals(1, compounds.get(1).compoundSize());
  }

  // ---------- Test 6: empty feature list → validateInputs returns error ----------

  @Test
  void test6_emptyFeatureList() {
    final ModularFeatureList flist = newFeatureList("test6");
    final String error = newComponentizer().validateInputs(flist);
    assertNotNull(error, "Empty feature list must yield a validation error");
  }

  // ---------- Test 7: no IIN and no correlation map → validateInputs returns error ----------

  @Test
  void test7_noIinNoCorrelation() {
    final ModularFeatureList flist = newFeatureList("test7");
    row(flist, 1, 100.0, 1.0f, 100f);
    row(flist, 2, 200.0, 2.0f, 200f);
    // no ion identities, no correlation edges

    final String error = newComponentizer().validateInputs(flist);
    assertNotNull(error, "Missing IIN and correlation map must yield a validation error");
  }

  // ---- helpers ----

  private static Set<CompoundMemberRole> collectRoles(final ModularCompoundRow cr) {
    final Set<CompoundMemberRole> roles = new HashSet<>();
    for (final CompoundFeatureMember m : cr.getCompoundMembers()) {
      roles.add(m.role());
    }
    return roles;
  }
}
