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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FeatureListUtils;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link FeatureListUtils#copyCompoundList} — remapping member rows, dropping removed members
 * and empty compounds, re-picking a representative, and applying the compound-id filter.
 */
public class CompoundListCopyTest {

  private ModularFeatureList source;
  private ModularFeatureList target;

  private static ModularFeatureListRow row(final ModularFeatureList fl, final int id,
      final double mz) {
    final ModularFeatureListRow r = new ModularFeatureListRow(fl, id);
    r.set(MZType.class, mz);
    fl.addRow(r);
    return r;
  }

  @BeforeEach
  void setUp() {
    source = new ModularFeatureList("source", null,
        new RawDataFileImpl("sourceFile", null, null, Color.BLACK));
    target = new ModularFeatureList("target", null,
        new RawDataFileImpl("targetFile", null, null, Color.BLACK));
  }

  @Test
  void prunesRemovedMembers() {
    final ModularFeatureListRow s1 = row(source, 1, 100.0);
    final ModularFeatureListRow s2 = row(source, 2, 101.0);
    final ModularFeatureListRow s3 = row(source, 3, 102.0);

    final CompoundList scl = new CompoundList(source, null, 10);
    final ModularCompoundRow cr = new ModularCompoundRow(scl, 5, s1,
        List.of(new CompoundFeatureMember(s1, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(s2, CompoundMemberRole.CORRELATED, 0.5f),
            new CompoundFeatureMember(s3, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f, null);
    scl.setRows(List.of(cr));
    source.setCompoundList(scl);

    // target keeps rows 1 and 3, drops row 2
    final ModularFeatureListRow t1 = row(target, 1, 100.0);
    final ModularFeatureListRow t3 = row(target, 3, 102.0);
    final Map<FeatureListRow, ModularFeatureListRow> map = new IdentityHashMap<>();
    map.put(s1, t1);
    map.put(s3, t3);

    final CompoundList copy = FeatureListUtils.copyCompoundList(scl.getRowsCopy(), target, map::get,
        null, null);

    Assertions.assertNotNull(copy);
    Assertions.assertEquals(1, copy.getRows().size());
    final ModularCompoundRow copied = copy.getRows().getFirst();
    Assertions.assertEquals(5, copied.getCompoundId(), "compound id is preserved");
    Assertions.assertEquals(2, copied.getCompoundMembers().size(), "removed member is stripped");
    Assertions.assertSame(t1, copied.getPreferredRow(), "preferred remapped to the new row");
  }

  @Test
  void appliesCompoundIdFilter() {
    final ModularFeatureListRow s1 = row(source, 1, 100.0);
    final ModularFeatureListRow s2 = row(source, 2, 101.0);

    final CompoundList scl = new CompoundList(source, null, 10);
    final ModularCompoundRow cr5 = new ModularCompoundRow(scl, 5, s1,
        List.of(new CompoundFeatureMember(s1, CompoundMemberRole.REPRESENTATIVE, 1.0f)), 0.9f,
        null);
    final ModularCompoundRow cr7 = new ModularCompoundRow(scl, 7, s2,
        List.of(new CompoundFeatureMember(s2, CompoundMemberRole.REPRESENTATIVE, 1.0f)), 0.9f,
        null);
    scl.setRows(List.of(cr5, cr7));
    source.setCompoundList(scl);

    final ModularFeatureListRow t1 = row(target, 1, 100.0);
    final ModularFeatureListRow t2 = row(target, 2, 101.0);
    final Map<FeatureListRow, ModularFeatureListRow> map = new IdentityHashMap<>();
    map.put(s1, t1);
    map.put(s2, t2);

    final Predicate<ModularCompoundRow> keep5 = cr -> cr.getCompoundId() == 5;
    final CompoundList copy = FeatureListUtils.copyCompoundList(scl.getRowsCopy(), target, map::get,
        keep5, null);

    Assertions.assertNotNull(copy);
    Assertions.assertEquals(1, copy.getRows().size(), "only compound 5 is kept");
    Assertions.assertEquals(5, copy.getRows().getFirst().getCompoundId());
  }

  @Test
  void dropsEmptyCompoundReturnsNull() {
    final ModularFeatureListRow s1 = row(source, 1, 100.0);

    final CompoundList scl = new CompoundList(source, null, 10);
    final ModularCompoundRow cr = new ModularCompoundRow(scl, 5, s1,
        List.of(new CompoundFeatureMember(s1, CompoundMemberRole.REPRESENTATIVE, 1.0f)), 0.9f,
        null);
    scl.setRows(List.of(cr));
    source.setCompoundList(scl);

    // target keeps no rows of the compound
    final Map<FeatureListRow, ModularFeatureListRow> map = new IdentityHashMap<>();

    final CompoundList copy = FeatureListUtils.copyCompoundList(scl.getRowsCopy(), target, map::get,
        null, null);

    Assertions.assertNull(copy, "a compound with no surviving members is dropped; nothing remains");
  }

  @Test
  void repicksRepresentativeWhenPreferredRemoved() {
    final ModularFeatureListRow s1 = row(source, 1, 100.0);
    final ModularFeatureListRow s2 = row(source, 2, 101.0);

    final CompoundList scl = new CompoundList(source, null, 10);
    final ModularCompoundRow cr = new ModularCompoundRow(scl, 5, s1,
        List.of(new CompoundFeatureMember(s1, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(s2, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f, null);
    scl.setRows(List.of(cr));
    source.setCompoundList(scl);

    // drop the representative (row 1), keep row 2
    final ModularFeatureListRow t2 = row(target, 2, 101.0);
    final Map<FeatureListRow, ModularFeatureListRow> map = new IdentityHashMap<>();
    map.put(s2, t2);

    final CompoundList copy = FeatureListUtils.copyCompoundList(scl.getRowsCopy(), target, map::get,
        null, null);

    Assertions.assertNotNull(copy);
    final ModularCompoundRow copied = copy.getRows().getFirst();
    Assertions.assertEquals(1, copied.getCompoundMembers().size());
    Assertions.assertSame(t2, copied.getPreferredRow(),
        "a new representative is picked from the surviving members");
  }

  @Test
  void filtersNestedCompoundRows() {
    final ModularFeatureListRow s1 = row(source, 1, 100.0);
    final ModularFeatureListRow s2 = row(source, 2, 101.0);

    final CompoundList scl = new CompoundList(source, null, 10);
    // nested compound id 7 (member of the top-level compound id 5)
    final ModularCompoundRow nested = new ModularCompoundRow(scl, 7, s2,
        List.of(new CompoundFeatureMember(s2, CompoundMemberRole.REPRESENTATIVE, 1.0f)), 0.9f,
        null);
    final ModularCompoundRow top = new ModularCompoundRow(scl, 5, s1,
        List.of(new CompoundFeatureMember(s1, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(nested, CompoundMemberRole.CORRELATED, 0.5f)), 0.9f, null);
    scl.setRows(List.of(top));
    source.setCompoundList(scl);

    final ModularFeatureListRow t1 = row(target, 1, 100.0);
    final ModularFeatureListRow t2 = row(target, 2, 101.0);
    final Map<FeatureListRow, ModularFeatureListRow> map = new IdentityHashMap<>();
    map.put(s1, t1);
    map.put(s2, t2);

    // filter rejects compound id 7 — the nested compound must be dropped from its parent
    final Predicate<ModularCompoundRow> rejectNested = cr -> cr.getCompoundId() != 7;
    final CompoundList copy = FeatureListUtils.copyCompoundList(scl.getRowsCopy(), target, map::get,
        rejectNested, null);

    Assertions.assertNotNull(copy);
    Assertions.assertEquals(1, copy.getRows().size());
    final ModularCompoundRow copiedTop = copy.getRows().getFirst();
    Assertions.assertEquals(5, copiedTop.getCompoundId());
    Assertions.assertEquals(1, copiedTop.getCompoundMembers().size(),
        "nested compound 7 is filtered out, leaving only the feature-row member");
    Assertions.assertSame(t1, copiedTop.getPreferredRow());
  }
}
