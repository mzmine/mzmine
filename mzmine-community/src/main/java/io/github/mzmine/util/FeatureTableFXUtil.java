/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.skin.VirtualFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureTableFXUtil {

  private static final Logger logger = Logger.getLogger(FeatureTableFX.class.getName());

  /**
   * Creates and shows a new FeatureTable. Should be called via
   * {@link Platform#runLater(Runnable)}.
   *
   * @param flist The feature list.
   */
  public static void addFeatureTableTab(FeatureList flist) {
    FeatureTableTab newTab = new FeatureTableTab(flist);
    MZmineCore.getDesktop().addTab(newTab);
  }


  /**
   * Scrolls to the row item if it is not visible and selects it.
   */
  public static void selectAndScrollTo(@Nullable TreeItem<ModularFeatureListRow> rowItem,
      @NotNull FeatureTableFX table) {
    if (rowItem == null) {
      return;
    }
    final int itemIndex = table.getRow(rowItem);
    if (itemIndex < 0) {
      // not expanded
      return;
    }
    scrollIntoViewIfNeeded(itemIndex, table);
    table.getSelectionModel().clearAndSelect(itemIndex);
  }

  /**
   * Scrolls to the row item if it is not visible. Does NOT change the table selection — use when
   * the original selection target isn't visible and we only want to reveal a related (parent) row
   * without triggering another selection-change propagation round.
   */
  public static void scrollTo(@Nullable TreeItem<ModularFeatureListRow> rowItem,
      @NotNull FeatureTableFX table) {
    if (rowItem == null) {
      return;
    }
    final int itemIndex = table.getRow(rowItem);
    if (itemIndex < 0) {
      return;
    }
    scrollIntoViewIfNeeded(itemIndex, table);
  }

  private static void scrollIntoViewIfNeeded(int itemIndex, @NotNull FeatureTableFX table) {
    final VirtualFlow<?> flow = (VirtualFlow<?>) table.getTable().lookup(".virtual-flow");
    if (flow == null) {
      return;
    }
    final IndexedCell<?> firstCell = flow.getFirstVisibleCell();
    final IndexedCell<?> lastCell = flow.getLastVisibleCell();
    if (firstCell != null && lastCell != null && !(itemIndex >= firstCell.getIndex()
        && itemIndex <= lastCell.getIndex())) {
      table.scrollTo(itemIndex);
    }
  }

  /**
   * Resolve {@code row} to a TreeItem in {@code table} and select/scroll to it. Rules:
   * <ol>
   *   <li>Prefer a TreeItem that is currently visible: a top-level (filtered) row whose value
   *       equals {@code row}, or a child of an already-expanded top-level compound whose value
   *       equals {@code row}. No row is expanded by this method.</li>
   *   <li>If {@code row} is a {@link ModularCompoundRow} and no direct match exists, also accept a
   *       visible top-level flat row equal to its preferred row (covers a linked table viewing the
   *       same selection in ALL_MAJOR_IONS / ALL_ISOTOPES mode).</li>
   *   <li>Otherwise, if {@code row} has exactly one parent {@link ModularCompoundRow} and that
   *       parent is visible at top level, <b>scroll only</b> to that parent — the selection is
   *       intentionally not changed so the fallback does not trigger another round of
   *       selected-rows propagation. Multiple parents or a hidden parent → no-op.</li>
   * </ol>
   */
  public static void selectAndScrollTo(@Nullable FeatureListRow row,
      @NotNull FeatureTableFX table) {
    if (row == null) {
      return;
    }

    // 1 + 2: direct visible match (top-level or child of expanded compound)
    final TreeItem<ModularFeatureListRow> direct = findVisibleMatch(row, table);
    if (direct != null) {
      selectAndScrollTo(direct, table);
      return;
    }

    // 3: only non-compound rows fall back to their parent compound
    if (row instanceof ModularCompoundRow) {
      return;
    }
    final FeatureList flist = row.getFeatureList();
    final CompoundList compoundList = flist == null ? null : flist.getCompoundList();
    if (compoundList == null) {
      return;
    }
    final List<ModularCompoundRow> parents = compoundList.findCompoundsOf(row);
    // decision: only fall back when exactly one parent compound exists — multiple parents are
    // ambiguous and we don't pick arbitrarily.
    if (parents.size() != 1) {
      return;
    }
    final ModularCompoundRow parent = parents.getFirst();
    for (TreeItem<ModularFeatureListRow> item : table.getFilteredRowItems()) {
      if (Objects.equals(item.getValue(), parent)) {
        // decision: scroll only — don't select the parent. Selecting it would push a new value
        // through the selection-model listener and trigger another propagation cycle through the
        // FeatureTableLink graph, which would corrupt the originating selection.
        scrollTo(item, table);
        return;
      }
    }
  }

  /**
   * Search visible TreeItems for one whose value matches {@code target}. Visible = present in the
   * filtered top-level list, or a direct child of a top-level item that is currently expanded.
   * Never expands a row.
   */
  private static @Nullable TreeItem<ModularFeatureListRow> findVisibleMatch(
      @NotNull FeatureListRow target, @NotNull FeatureTableFX table) {
    final boolean targetIsCompound = target instanceof ModularCompoundRow;
    final FeatureListRow representative =
        targetIsCompound ? ((ModularCompoundRow) target).getPreferredRow() : null;

    final ObservableList<TreeItem<ModularFeatureListRow>> topLevel = table.getFilteredRowItems();
    for (TreeItem<ModularFeatureListRow> item : topLevel) {
      final ModularFeatureListRow value = item.getValue();
      if (Objects.equals(value, target)) {
        return item;
      }
      // Compound target viewed in a flat table: accept its preferred row at top level.
      if (targetIsCompound && !(value instanceof ModularCompoundRow) && Objects.equals(value,
          representative)) {
        return item;
      }
      // Children of expanded top-level compound rows are visible too.
      if (item.isExpanded() && !item.getChildren().isEmpty()) {
        for (TreeItem<ModularFeatureListRow> child : item.getChildren()) {
          if (Objects.equals(child.getValue(), target)) {
            return child;
          }
        }
      }
    }
    return null;
  }

  public static void updateCellsForFeatureList(FeatureList flist) {
    if (!DesktopService.isGUI()) {
      return;
    }

    final MZmineGUI desktop = (MZmineGUI) DesktopService.getDesktop();
    final List<FeatureTableFX> featureTables = desktop.getAllTabs().stream()
        .filter(FeatureTableTab.class::isInstance).map(FeatureTableTab.class::cast)
        .map(FeatureTableTab::getController).map(FxFeatureTableController::getFeatureTable)
        .filter(t -> t.getFeatureList() == flist).toList();

    FxThread.runLater(() -> {
      for (FeatureTableFX featureTable : featureTables) {
        featureTable.refresh();
      }
    });
  }
}
