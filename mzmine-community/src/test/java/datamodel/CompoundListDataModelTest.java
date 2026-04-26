package datamodel;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
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
}
