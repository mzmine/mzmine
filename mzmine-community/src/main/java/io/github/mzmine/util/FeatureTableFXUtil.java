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
   * Collapse every top-level row in {@code table}'s filtered view except (optionally)
   * {@code keepExpanded}. Use after navigating to a target row to close compound rows that aren't
   * relevant to the current selection — leaves the user with a clean view focused on the row being
   * shown (and its enclosing compound row, when applicable).
   * <p>
   * If {@code keepExpanded} is a child rather than a top-level item itself, its enclosing top-level
   * ancestor is kept expanded so the child stays visible. Pass {@code null} to collapse every
   * top-level row unconditionally.
   *
   * @param keepExpanded a TreeItem whose top-level ancestor should remain in its current expanded
   *                     state; pass {@code null} to collapse all top-level rows.
   */
  public static void collapseAllTopLevelExcept(
      @Nullable TreeItem<ModularFeatureListRow> keepExpanded, @NotNull FeatureTableFX table) {
    final TreeItem<ModularFeatureListRow> keepTopLevel =
        keepExpanded == null ? null : topLevelAncestor(keepExpanded, table);
    for (TreeItem<ModularFeatureListRow> item : table.getFilteredRowItems()) {
      if (item != keepTopLevel && item.isExpanded()) {
        item.setExpanded(false);
      }
    }
  }

  /**
   * Walks up the TreeItem parent chain until reaching an item whose parent is the (invisible)
   * TreeTableView root. That item is the top-level ancestor of {@code item}; {@code item} itself is
   * returned when it is already at top level.
   */
  private static @Nullable TreeItem<ModularFeatureListRow> topLevelAncestor(
      @NotNull TreeItem<ModularFeatureListRow> item, @NotNull FeatureTableFX table) {
    final TreeItem<ModularFeatureListRow> root = table.getTable().getRoot();
    TreeItem<ModularFeatureListRow> current = item;
    while (current != null && current.getParent() != null && current.getParent() != root) {
      current = current.getParent();
    }
    return current;
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
   *       equals {@code row}. No row is expanded by this step.</li>
   *   <li>If {@code row} is a {@link ModularCompoundRow} and no direct match exists, also accept a
   *       visible top-level flat row equal to its preferred row (covers a linked table viewing the
   *       same selection in ALL_MAJOR_IONS / ALL_ISOTOPES mode).</li>
   *   <li>Otherwise look up {@code row}'s parent compounds in the {@link CompoundList}:
   *     <ul>
   *       <li><b>Exactly one</b> parent in the data model: find that parent at top level,
   *           {@code setExpanded(true)} it, then select + scroll to the child TreeItem inside.
   *           Selecting the child is safe because it equals the row that originated the call.</li>
   *       <li><b>More than one</b> parent in the data model: find the first such parent that is
   *           visible at top level, expand it, and scroll to that parent without selecting it.
   *           The child is left unselected because picking one parent arbitrarily would push an
   *           ambiguous {@code resolveCompoundRow} result through downstream bindings.</li>
   *       <li>No matching parent visible at top level → no-op.</li>
   *     </ul>
   *   </li>
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
      // Collapse first so the row index that scrollIntoViewIfNeeded computes is already against
      // the final tree state. If direct is a child of a compound, that compound is kept expanded;
      // if direct is top-level itself, nothing extra is kept (collapse all others).
      collapseAllTopLevelExcept(direct, table);
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
    if (parents.isEmpty()) {
      return;
    }

    if (parents.size() == 1) {
      expandSingleParentAndSelectChild(parents.getFirst(), row, table);
    } else {
      expandFirstVisibleParent(parents, table);
    }
  }

  /**
   * Single-parent fallback: locate {@code parent} at top level, expand it, then resolve the
   * matching child TreeItem and select + scroll to it. If the parent isn't visible (e.g. filtered
   * out), or its children list doesn't contain {@code row} for any reason, this falls back to
   * scrolling to the parent without selecting anything.
   */
  private static void expandSingleParentAndSelectChild(@NotNull ModularCompoundRow parent,
      @NotNull FeatureListRow row, @NotNull FeatureTableFX table) {
    final TreeItem<ModularFeatureListRow> parentItem = findTopLevelItem(parent, table);
    if (parentItem == null) {
      return;
    }
    parentItem.setExpanded(true);
    final TreeItem<ModularFeatureListRow> childItem = findChildOf(parentItem, row);
    // Collapse other compounds before scroll so the kept parent is the only thing expanded and
    // the target index is computed against the final tree state.
    collapseAllTopLevelExcept(parentItem, table);
    if (childItem != null) {
      // Safe to select: the child's value IS the row that initiated this scroll request, so any
      // selection-cascade that fires from this re-selects the same row downstream (no-op via the
      // existing equality guards in the dashboards).
      selectAndScrollTo(childItem, table);
    } else {
      // Defensive: parent claims this row but the TreeItem hierarchy doesn't expose it as a
      // direct child. Reveal the parent so the user gets the relevant context.
      scrollTo(parentItem, table);
    }
  }

  /**
   * Multi-parent fallback: iterate the table's top-level rows and expand the first one that matches
   * any of {@code parents}, scrolling to it without changing the selection. Selecting one of
   * several possible parents would push an arbitrary {@code resolveCompoundRow} result through the
   * downstream bindings.
   */
  private static void expandFirstVisibleParent(@NotNull List<ModularCompoundRow> parents,
      @NotNull FeatureTableFX table) {
    for (TreeItem<ModularFeatureListRow> item : table.getFilteredRowItems()) {
      if (item.getValue() instanceof ModularCompoundRow compound && parents.contains(compound)) {
        item.setExpanded(true);
        // Close every other top-level compound so the user only sees the candidate parent open.
        collapseAllTopLevelExcept(item, table);
        scrollTo(item, table);
        return;
      }
    }
  }

  /**
   * @return the top-level TreeItem whose value equals {@code row}, or {@code null} if no top-level
   * item matches (typically because filtering hides it).
   */
  private static @Nullable TreeItem<ModularFeatureListRow> findTopLevelItem(
      @NotNull FeatureListRow row, @NotNull FeatureTableFX table) {
    for (TreeItem<ModularFeatureListRow> item : table.getFilteredRowItems()) {
      if (Objects.equals(item.getValue(), row)) {
        return item;
      }
    }
    return null;
  }

  /**
   * @return the direct child TreeItem of {@code parent} whose value equals {@code target}, or
   * {@code null} if no such child exists.
   */
  private static @Nullable TreeItem<ModularFeatureListRow> findChildOf(
      @NotNull TreeItem<ModularFeatureListRow> parent, @NotNull FeatureListRow target) {
    for (TreeItem<ModularFeatureListRow> child : parent.getChildren()) {
      if (Objects.equals(child.getValue(), target)) {
        return child;
      }
    }
    return null;
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
