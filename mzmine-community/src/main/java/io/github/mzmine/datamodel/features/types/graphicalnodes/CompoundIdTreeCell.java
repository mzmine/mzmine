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
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxControls;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.color.ColorsFX;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundIdTreeCell extends TreeTableCell<ModularFeatureListRow, Object> {

  final Region[] stripes = new Region[3];
  final ObjectProperty<RowState> rowState = new SimpleObjectProperty<>(null);
  private final ObservableValue<@NotNull String> tooltip;

  public CompoundIdTreeCell() {
    super();
    tooltip = rowState.map(RowState::toString);

    final HBox stripesPane = new HBox(0);
    final Color[] stripeColors = {ColorsFX.POSITIVE_MARKER_COLORBLIND, ColorsFX.MAGENTA,
        ColorsFX.NEUTRAL_MARKER};
    for (int i = 0; i < stripes.length; i++) {
      final Region stripe = new Region();
      final int width = 8;
      stripe.setPrefWidth(width);
      stripe.setMinWidth(width);
      stripe.setMaxWidth(width);
      final var height = stripesPane.heightProperty().subtract(5);
      stripe.prefHeightProperty().bind(height);
      stripe.minHeightProperty().bind(height);
      stripe.maxHeightProperty().bind(height);

      FxControls.addTooltip(stripe, tooltip);

      stripe.setStyle("-fx-background-color: " + ColorsFX.toHexString(stripeColors[i]) + ";");
      stripes[i] = stripe;
    }

    final Label label = FxLabels.newLabel(textProperty());
    final StackPane stackPane = FxLayout.newStackPane(stripesPane, label);
    StackPane.setAlignment(label, Pos.CENTER_RIGHT);
    StackPane.setAlignment(stripesPane, Pos.CENTER_LEFT);

    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    setGraphic(stackPane);

//    setPadding(Insets.EMPTY);

    PropertyUtils.onChange(() -> {
      if (isEmpty() || !(getTableRow().getItem() instanceof ModularFeatureListRow row)) {
        setText(null);
        return;
      }
      final Integer cid = row.get(CompoundIdType.class);
      setText(cid == null ? null : cid.toString());

      final int level = rowParentLevel(row);
      stripesPane.getChildren().setAll(stripes[level]);
    }, itemProperty(), emptyProperty());
  }

  private int rowParentLevel(ModularFeatureListRow row) {
    if (!isVisible()) {
      rowState.set(null);
      return 0;
    }

    final TreeItem<ModularFeatureListRow> item = getTableRow().getTreeItem();
    if (item == null) {
      rowState.set(null);
      return 0;
    }

    final TreeItem<ModularFeatureListRow> parent1 = item.getParent();
    if (parent1 != null && parent1.getValue() != null) {
      final TreeItem<ModularFeatureListRow> parent2 = parent1.getParent();
      if (parent2 != null && parent2.getValue() != null) {
        // has 2 parents so return level 2
        rowState.set(new RowState(parent2.getValue(), parent1.getValue(), item.getValue(), 2));
        return 2;
      }
      rowState.set(new RowState(parent1.getValue(), null, item.getValue(), 1));
      return 1;
    }
    // only target as this is top level
    rowState.set(new RowState(null, null, item.getValue(), 0));
    return 0;
  }

  /**
   *
   * @param compoundRow from target this would be the 2nd level parent
   * @param ionParent 1st level parent if target is an isotope otherwise null
   * @param target the target row
   * @param level the level of target in tree
   */
  record RowState(@Nullable ModularFeatureListRow compoundRow,
                  @Nullable ModularFeatureListRow ionParent, @NotNull ModularFeatureListRow target,
                  int level) {

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
