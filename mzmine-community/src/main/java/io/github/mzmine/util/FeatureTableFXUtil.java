/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXMLTabAnchorPaneController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import java.util.Optional;
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
   * @return The {@link FeatureTableFXMLTabAnchorPaneController} of the window or null if failed to
   * initialise.
   */
  @Nullable
  public static void addFeatureTableTab(FeatureList flist) {
    FeatureTableTab newTab = new FeatureTableTab(flist);
    MZmineCore.getDesktop().addTab(newTab);
  }


  /**
   * Scrolls to the selected row item if it is not visible and selects the row.
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
    VirtualFlow<?> flow = (VirtualFlow<?>) table.lookup(".virtual-flow");
    if (flow != null) {
      final IndexedCell<?> firstCell = flow.getFirstVisibleCell();
      final IndexedCell<?> lastCell = flow.getLastVisibleCell();
      if (!(itemIndex >= firstCell.getIndex() && itemIndex <= lastCell.getIndex())) {
        table.scrollTo(table.getRoot().getChildren().indexOf(rowItem));
      }
    }
    table.getSelectionModel().clearAndSelect(table.getRoot().getChildren().indexOf(rowItem));
  }

  public static void selectAndScrollTo(@Nullable FeatureListRow row,
      @NotNull FeatureTableFX table) {
    final ObservableList<TreeItem<ModularFeatureListRow>> children = table.getRoot().getChildren();
    final Optional<TreeItem<ModularFeatureListRow>> selected = children.stream()
        .filter(item -> item.getValue().equals(row)).findAny();
    selectAndScrollTo(selected.orElse(null), table);
  }
}
