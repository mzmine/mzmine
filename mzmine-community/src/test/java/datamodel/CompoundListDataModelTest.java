package datamodel;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowUtils;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the CompoundList data model (Sprint 1 tests #8, #10, #11).
 */
public class CompoundListDataModelTest {

  private RawDataFileImpl file;
  private ModularFeatureList flist;

  @BeforeEach
  void setUp() {
    file = new RawDataFileImpl("testfile", null, null, Color.BLACK);
    flist = new ModularFeatureList("testflist", null, file);
  }

  /**
   * Test #8: ModularCompoundRow.getAverageMZ() delegates to preferredRow's MZ.
   */
  @Test
  void testGetAverageMZDelegatesToPreferredRow() {
    final double expectedMZ = 345.6789;
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, 1);
    row.set(MZType.class, expectedMZ);
    flist.addRow(row);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow cr = new ModularCompoundRow(cl, 1, row,
        List.of(new CompoundFeatureMember(row, CompoundMemberRole.REPRESENTATIVE, 1.0f)),
        0.9f, null);

    Assertions.assertEquals(expectedMZ, cr.getAverageMZ(), 1e-9,
        "getAverageMZ() must delegate to preferredRow.getAverageMZ()");
  }

  /**
   * Test #10: addRow increments structuralVersion and makes CompoundList.isStale() true.
   */
  @Test
  void testAddRowMakesCompoundListStale() {
    final long v0 = flist.getStructuralVersion();
    final CompoundList cl = new CompoundList(flist, null, 10);
    Assertions.assertFalse(cl.isStale(), "freshly created CompoundList must not be stale");

    flist.addRow(new ModularFeatureListRow(flist, 1));

    Assertions.assertTrue(flist.getStructuralVersion() > v0,
        "structuralVersion must be incremented after addRow");
    Assertions.assertTrue(cl.isStale(),
        "CompoundList must be stale after a row is added to the feature list");
  }

  /**
   * Test #11: setCompoundList followed by addRow makes hasCompoundList() return false.
   */
  @Test
  void testHasCompoundListFalseAfterStructuralChange() {
    final CompoundList cl = new CompoundList(flist, null, 10);
    flist.setCompoundList(cl);

    Assertions.assertTrue(flist.hasCompoundList(),
        "hasCompoundList() must return true right after setCompoundList");

    flist.addRow(new ModularFeatureListRow(flist, 2));

    Assertions.assertFalse(flist.hasCompoundList(),
        "hasCompoundList() must return false after a structural change");
  }

  /**
   * Deleting a member row from the feature list must propagate into the CompoundList: the row is
   * stripped from its compound, the compound list stays alive (not disposed), and is not reported
   * as stale.
   */
  @Test
  void testRemoveMemberRowKeepsCompoundListAlive() {
    final ModularFeatureListRow rep = new ModularFeatureListRow(flist, 1);
    rep.set(MZType.class, 100.0);
    final ModularFeatureListRow member = new ModularFeatureListRow(flist, 2);
    member.set(MZType.class, 101.0);
    flist.addRow(rep);
    flist.addRow(member);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow cr = new ModularCompoundRow(cl, 1, rep,
        List.of(new CompoundFeatureMember(rep, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(member, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f, null);
    cl.setRows(List.of(cr));
    flist.setCompoundList(cl);

    Assertions.assertTrue(flist.hasCompoundList(), "precondition: compound list is attached");
    Assertions.assertEquals(2, cr.getCompoundMembers().size(), "precondition: 2 members");

    flist.removeRow(member);

    Assertions.assertTrue(flist.hasCompoundList(),
        "CompoundList must stay alive after a member row is removed");
    Assertions.assertSame(cl, flist.getCompoundList(),
        "CompoundList instance must be preserved across the deletion");
    Assertions.assertEquals(1, cl.getRows().size(), "compound is still present (rep remains)");
    Assertions.assertEquals(1, cl.getRows().getFirst().getCompoundMembers().size(),
        "member was stripped from the compound");
  }

  /**
   * If every member of a compound is deleted from the feature list, the compound itself is dropped
   * from the CompoundList — but the CompoundList stays alive.
   */
  @Test
  void testRemoveAllMembersDropsCompoundButKeepsList() {
    final ModularFeatureListRow rep = new ModularFeatureListRow(flist, 1);
    rep.set(MZType.class, 100.0);
    flist.addRow(rep);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow cr = new ModularCompoundRow(cl, 1, rep,
        List.of(new CompoundFeatureMember(rep, CompoundMemberRole.REPRESENTATIVE, 1.0f)), 0.9f,
        null);
    cl.setRows(List.of(cr));
    flist.setCompoundList(cl);

    flist.removeRow(rep);

    Assertions.assertTrue(flist.hasCompoundList(),
        "CompoundList must stay alive even after all its members are deleted");
    Assertions.assertEquals(0, cl.getRows().size(),
        "compound is dropped when it loses all its members");
  }

  /**
   * detachMemberRows must descend into nested compound rows. A leaf row that only lives under a
   * nested compound (not a direct member of the top-level compound) must still be stripped from the
   * nested compound; the parent compound's structure is preserved.
   */
  @Test
  void testDetachMemberRowsRecursesIntoNestedCompound() {
    final ModularFeatureListRow rep = new ModularFeatureListRow(flist, 1);
    final ModularFeatureListRow nestedRep = new ModularFeatureListRow(flist, 2);
    final ModularFeatureListRow nestedMember = new ModularFeatureListRow(flist, 3);
    flist.addRow(rep);
    flist.addRow(nestedRep);
    flist.addRow(nestedMember);

    final CompoundList cl = new CompoundList(flist, null, 10);
    // nested compound: id=2, holds nestedRep + nestedMember
    final ModularCompoundRow nestedCompound = new ModularCompoundRow(cl, 2, nestedRep,
        List.of(new CompoundFeatureMember(nestedRep, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(nestedMember, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f,
        null);
    // top compound: id=1, holds rep + nestedCompound
    final ModularCompoundRow top = new ModularCompoundRow(cl, 1, rep,
        List.of(new CompoundFeatureMember(rep, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(nestedCompound, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f,
        null);
    cl.setRows(List.of(top));
    flist.setCompoundList(cl);

    final boolean changed = CompoundRowUtils.detachMemberRows(cl, List.of(nestedMember));

    Assertions.assertTrue(changed, "detachMemberRows must report a change");
    Assertions.assertEquals(1, cl.getRows().size(), "top compound must still be present");
    // top still has rep + nested compound (nested compound retained, just trimmed)
    Assertions.assertEquals(2, top.getCompoundMembers().size(),
        "top compound's members must be unchanged in count");
    // nested compound still present, but with one member fewer
    Assertions.assertEquals(1, nestedCompound.getCompoundMembers().size(),
        "nested compound must have lost the detached member");
    Assertions.assertEquals(nestedRep.getID(),
        nestedCompound.getCompoundMembers().getFirst().row().getID(),
        "nested compound must still hold its representative");
  }

  /**
   * If a nested compound loses all its members through detachMemberRows, the nested compound is
   * dropped from its parent and the parent's representative is re-normalized.
   */
  @Test
  void testDetachMemberRowsDropsEmptyNestedCompound() {
    final ModularFeatureListRow topMember = new ModularFeatureListRow(flist, 1);
    final ModularFeatureListRow nestedOnly = new ModularFeatureListRow(flist, 2);
    flist.addRow(topMember);
    flist.addRow(nestedOnly);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow nested = new ModularCompoundRow(cl, 2, nestedOnly,
        List.of(new CompoundFeatureMember(nestedOnly, CompoundMemberRole.REPRESENTATIVE, 1.0f)),
        0.9f, null);
    final ModularCompoundRow top = new ModularCompoundRow(cl, 1, topMember,
        List.of(new CompoundFeatureMember(topMember, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(nested, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f, null);
    cl.setRows(List.of(top));
    flist.setCompoundList(cl);

    CompoundRowUtils.detachMemberRows(cl, List.of(nestedOnly));

    Assertions.assertEquals(1, cl.getRows().size(), "top compound stays at the top level");
    Assertions.assertEquals(1, top.getCompoundMembers().size(),
        "top must have dropped the now-empty nested compound");
    Assertions.assertEquals(topMember.getID(), top.getCompoundMembers().getFirst().row().getID(),
        "remaining member must be topMember");
  }

  /**
   * removeCompoundRows on a nested compound must also strip it from its parent's member list. If
   * the parent loses all members because of this, it cascades up — the parent is removed too.
   */
  @Test
  void testRemoveCompoundRowsCascadesUpThroughParents() {
    final ModularFeatureListRow nestedRep = new ModularFeatureListRow(flist, 1);
    flist.addRow(nestedRep);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow nested = new ModularCompoundRow(cl, 2, nestedRep,
        List.of(new CompoundFeatureMember(nestedRep, CompoundMemberRole.REPRESENTATIVE, 1.0f)),
        0.9f, null);
    // top compound's only member is the nested compound
    final ModularCompoundRow top = new ModularCompoundRow(cl, 1, nestedRep,
        List.of(new CompoundFeatureMember(nested, CompoundMemberRole.REPRESENTATIVE, 1.0f)), 0.9f,
        null);
    cl.setRows(List.of(top));
    flist.setCompoundList(cl);

    Assertions.assertEquals(1, cl.getRows().size(), "precondition: top compound present");

    final boolean changed = CompoundRowUtils.removeCompoundRows(cl, List.of(nested));

    Assertions.assertTrue(changed, "removeCompoundRows must report a change");
    Assertions.assertEquals(0, cl.getRows().size(),
        "cascade: parent must also be removed because it became empty");
  }

  /**
   * Removing a nested compound that has siblings strips it from the parent but the parent stays.
   */
  @Test
  void testRemoveCompoundRowsKeepsParentWithSiblings() {
    final ModularFeatureListRow rep = new ModularFeatureListRow(flist, 1);
    final ModularFeatureListRow nestedRep = new ModularFeatureListRow(flist, 2);
    flist.addRow(rep);
    flist.addRow(nestedRep);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow nested = new ModularCompoundRow(cl, 2, nestedRep,
        List.of(new CompoundFeatureMember(nestedRep, CompoundMemberRole.REPRESENTATIVE, 1.0f)),
        0.9f, null);
    final ModularCompoundRow top = new ModularCompoundRow(cl, 1, rep,
        List.of(new CompoundFeatureMember(rep, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(nested, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f, null);
    cl.setRows(List.of(top));
    flist.setCompoundList(cl);

    CompoundRowUtils.removeCompoundRows(cl, List.of(nested));

    Assertions.assertEquals(1, cl.getRows().size(), "top compound stays");
    Assertions.assertEquals(1, top.getCompoundMembers().size(),
        "nested compound must be stripped from top");
    Assertions.assertEquals(rep.getID(), top.getCompoundMembers().getFirst().row().getID(),
        "only rep remains as a member of the top compound");
  }

  @Test
  void testSetRepresentativeUsesTypedIdAndRefreshesNeutralMass() {
    final ModularFeatureListRow rep = new ModularFeatureListRow(flist, 1);
    final ModularFeatureListRow sharedIdLeaf = new ModularFeatureListRow(flist, 2);
    final ModularFeatureListRow nestedExtra = new ModularFeatureListRow(flist, 3);
    flist.addRow(rep);
    flist.addRow(sharedIdLeaf);
    flist.addRow(nestedExtra);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow nested = new ModularCompoundRow(cl, 200, sharedIdLeaf,
        List.of(new CompoundFeatureMember(sharedIdLeaf, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(nestedExtra, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f,
        null);
    final ModularCompoundRow top = new ModularCompoundRow(cl, 100, rep,
        List.of(new CompoundFeatureMember(rep, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(sharedIdLeaf, CompoundMemberRole.ADDUCT, 0.25f),
            new CompoundFeatureMember(nested, CompoundMemberRole.CORRELATED, 0.33f)), 0.9f, 123.4);
    cl.setRows(List.of(top));
    flist.setCompoundList(cl);

    final boolean changed = CompoundRowUtils.setRepresentative(top, nested);

    Assertions.assertTrue(changed, "changing representative to a nested compound must succeed");
    Assertions.assertSame(nested, top.getPreferredRow(),
        "preferred row must be set to the selected nested compound");

    final CompoundFeatureMember nestedMember = top.getCompoundMembers().stream()
        .filter(m -> m.row() == nested).findFirst().orElseThrow();
    Assertions.assertEquals(CompoundMemberRole.REPRESENTATIVE, nestedMember.role(),
        "selected nested row must be the sole representative");
    Assertions.assertEquals(1.0f, nestedMember.score(), 1e-6f,
        "new representative score must be normalized to 1.0");

    final CompoundFeatureMember leafMember = top.getCompoundMembers().stream()
        .filter(m -> m.row() == sharedIdLeaf).findFirst().orElseThrow();
    Assertions.assertEquals(CompoundMemberRole.ADDUCT, leafMember.role(),
        "member with colliding plain ID must keep its original role");
    Assertions.assertNull(top.get(NeutralMassType.class),
        "neutral mass must be recomputed from the new representative and cleared when unavailable");
  }

  @Test
  void testRoleLookupUsesTypedIdForNestedCompoundMembers() {
    final ModularFeatureListRow rep = new ModularFeatureListRow(flist, 1);
    final ModularFeatureListRow sharedIdLeaf = new ModularFeatureListRow(flist, 2);
    final ModularFeatureListRow nestedExtra = new ModularFeatureListRow(flist, 3);
    flist.addRow(rep);
    flist.addRow(sharedIdLeaf);
    flist.addRow(nestedExtra);

    final CompoundList cl = new CompoundList(flist, null, 10);
    final ModularCompoundRow nested = new ModularCompoundRow(cl, 201, sharedIdLeaf,
        List.of(new CompoundFeatureMember(sharedIdLeaf, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(nestedExtra, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f,
        null);
    final ModularCompoundRow top = new ModularCompoundRow(cl, 101, rep,
        List.of(new CompoundFeatureMember(rep, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(sharedIdLeaf, CompoundMemberRole.ADDUCT, 0.25f),
            new CompoundFeatureMember(nested, CompoundMemberRole.CORRELATED, 0.33f)), 0.9f, null);
    cl.setRows(List.of(top));
    flist.setCompoundList(cl);

    Assertions.assertEquals(CompoundMemberRole.CORRELATED, cl.getRoleOf(nested).orElseThrow(),
        "role lookup must resolve nested compound member by typed ID, not plain getID()");
  }
}
