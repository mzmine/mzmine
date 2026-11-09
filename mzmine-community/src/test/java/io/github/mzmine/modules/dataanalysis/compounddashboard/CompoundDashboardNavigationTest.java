/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataanalysis.compounddashboard;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fxinitializer.InitJavaFX;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.HeadLessDesktop;
import java.util.List;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.LINUX)
class CompoundDashboardNavigationTest {

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
    DesktopService.setDesktop(new HeadLessDesktop());
  }

  @Test
  void secondaryAdductInCollapsedCompoundExpandsParentAndSelectsTheActualChild() {
    final NavigationFixture fixture = fixture();
    when(fixture.compounds.findCompoundsOf(fixture.secondaryAdduct))
        .thenReturn(List.of(fixture.compound));

    FeatureTableFXUtil.selectAndScrollTo(fixture.secondaryAdduct, fixture.table);

    assertTrue(fixture.compoundItem.isExpanded());
    verify(fixture.selection).clearAndSelect(1);
  }

  @Test
  void ambiguousSecondaryAdductRevealsAParentWithoutSelectingAnArbitraryChild() {
    final NavigationFixture fixture = fixture();
    final ModularCompoundRow otherCompound = mock(ModularCompoundRow.class);
    final TreeItem<ModularFeatureListRow> otherItem = new TreeItem<>(otherCompound);
    fixture.root.getChildren().add(otherItem);
    when(fixture.compounds.findCompoundsOf(fixture.secondaryAdduct))
        .thenReturn(List.of(fixture.compound, otherCompound));

    FeatureTableFXUtil.selectAndScrollTo(fixture.secondaryAdduct, fixture.table);

    assertTrue(fixture.compoundItem.isExpanded());
    verify(fixture.selection, never()).clearAndSelect(anyInt());
  }

  private static NavigationFixture fixture() {
    final FeatureTableFX table = mock(FeatureTableFX.class);
    final TreeTableView<ModularFeatureListRow> tree = mock(TreeTableView.class);
    final TreeTableView.TreeTableViewSelectionModel<ModularFeatureListRow> selection = mock(
        TreeTableView.TreeTableViewSelectionModel.class);
    final ModularFeatureList featureList = mock(ModularFeatureList.class);
    final CompoundList compounds = mock(CompoundList.class);
    final ModularCompoundRow compound = mock(ModularCompoundRow.class);
    final ModularFeatureListRow secondaryAdduct = mock(ModularFeatureListRow.class);
    final TreeItem<ModularFeatureListRow> compoundItem = new TreeItem<>(compound);
    compoundItem.getChildren().add(new TreeItem<>(secondaryAdduct));
    final TreeItem<ModularFeatureListRow> root = new TreeItem<>();
    root.getChildren().add(compoundItem);
    final FilteredList<TreeItem<ModularFeatureListRow>> items = new FilteredList<>(
        root.getChildren());
    when(table.getTable()).thenReturn(tree);
    when(table.getSelectionModel()).thenReturn(selection);
    when(table.getFilteredRowItems()).thenReturn(items);
    when(table.getRow(compoundItem)).thenReturn(0);
    when(table.getRow(compoundItem.getChildren().getFirst())).thenReturn(1);
    when(tree.getRoot()).thenReturn(root);
    when(secondaryAdduct.getFeatureList()).thenReturn(featureList);
    when(featureList.getCompoundList()).thenReturn(compounds);
    return new NavigationFixture(table, selection, compounds, compound, secondaryAdduct,
        compoundItem, root, items);
  }

  private record NavigationFixture(FeatureTableFX table,
      TreeTableView.TreeTableViewSelectionModel<ModularFeatureListRow> selection,
      CompoundList compounds, ModularCompoundRow compound, ModularFeatureListRow secondaryAdduct,
      TreeItem<ModularFeatureListRow> compoundItem, TreeItem<ModularFeatureListRow> root,
      FilteredList<TreeItem<ModularFeatureListRow>> items) {
  }
}
