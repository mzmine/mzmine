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

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundIdType;
import io.github.mzmine.javafx.components.util.FxControls;
import io.github.mzmine.javafx.properties.PropertyUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders the compound id integer as text. The colored hierarchy stripe is no longer drawn inside
 * this cell - see {@link CompoundHierarchyTreeTableRow}, which paints it in the tree's disclosure
 * area instead. The tooltip describing the row's place in the compound hierarchy is kept here.
 */
public class CompoundIdTreeCell extends TreeTableCell<ModularFeatureListRow, Object> {

  private final ObjectProperty<RowState> rowState = new SimpleObjectProperty<>(null);
  private final ObservableValue<@NotNull String> tooltip;

  public CompoundIdTreeCell() {
    super();
    tooltip = rowState.map(RowState::toString);
    setTooltip(FxControls.newTooltip(tooltip));

    PropertyUtils.onChange(() -> {
      if (isEmpty() || !(getTableRow().getItem() instanceof ModularFeatureListRow row)) {
        setText(null);
        rowState.set(null);
        return;
      }
      final Integer cid = row.get(CompoundIdType.class);
      setText(cid == null ? null : cid.toString());
      updateRowState();
    }, itemProperty(), emptyProperty());
  }

  private void updateRowState() {
    if (!isVisible()) {
      rowState.set(null);
      return;
    }

    final TreeItem<ModularFeatureListRow> item = getTableRow().getTreeItem();
    if (item == null) {
      rowState.set(null);
      return;
    }

    final TreeItem<ModularFeatureListRow> parent1 = item.getParent();
    if (parent1 != null && parent1.getValue() != null) {
      final TreeItem<ModularFeatureListRow> parent2 = parent1.getParent();
      if (parent2 != null && parent2.getValue() != null) {
        // has 2 parents so return level 2
        rowState.set(new RowState(parent2.getValue(), parent1.getValue(), item.getValue(), 2));
        return;
      }
      rowState.set(new RowState(parent1.getValue(), null, item.getValue(), 1));
      return;
    }
    // only target as this is top level
    rowState.set(new RowState(null, null, item.getValue(), 0));
  }

  /**
   * @param compoundRow from target this would be the 2nd level parent
   * @param ionParent   1st level parent if target is an isotope otherwise null
   * @param target      the target row
   * @param level       the level of target in tree
   */
  record RowState(@Nullable ModularFeatureListRow compoundRow,
                  @Nullable ModularFeatureListRow ionParent,
                  @NotNull ModularFeatureListRow target, int level) {

    @Override
    public @NotNull String toString() {
      final String targetStr;
      if (target instanceof CompoundRow compoundRow) {
        targetStr = "Compound row CID%d".formatted(compoundRow.getCompoundId());
      } else {
        targetStr = "Feature row ID%d".formatted(target.getID());
      }

      final String lvl = switch (level) {
        case 0 -> "Compound row";
        case 1 -> "Compound child (level 2: ions)";
        case 2 -> "Compound child (level 3: isotopes)";
        default -> "";
      };
      if (ionParent != null && compoundRow != null) {
        return """
            %s
            %s is listed as an isotope of compound row CID%d,
            which is a major member of compound row CID%d""".formatted(lvl, targetStr,
            ionParent.get(CompoundIdType.class), compoundRow.get(CompoundIdType.class));
      } else if (compoundRow != null) {
        return """
            %s
            %s is listed as a major member of compound row CID%d""".formatted(lvl, targetStr,
            compoundRow.get(CompoundIdType.class));
      }
      // is compound row itself
      return lvl;
    }
  }

}
