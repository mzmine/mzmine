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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.otherdectectors.MsOtherCorrelationResultType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationRowResult;
import java.util.List;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * ComboBox cell factory for the main column of {@link MsOtherCorrelationResultType}: lets the user
 * pick which correlated other-detector trace is preferred for an MS row. Selecting a trace moves it to
 * the front of the row's correlation list in the maps (first = preferred), which drives the derived
 * feature-level {@link io.github.mzmine.datamodel.features.types.otherdectectors.CorrelatedOtherFeatureType}
 * column. Modeled on {@link PreferredEditComboCellFactory}.
 */
public class CorrelatedTraceEditComboCellFactory implements
    Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>> {

  private static final Logger logger = Logger.getLogger(
      CorrelatedTraceEditComboCellFactory.class.getName());

  @Override
  public TreeTableCell<ModularFeatureListRow, Object> call(
      TreeTableColumn<ModularFeatureListRow, Object> param) {
    return new ComboBoxTreeTableCell<>() {

      final Label textValue = new Label();
      final VBox textWrapper = new VBox(textValue);

      {
        textValue.setWrapText(true);
        textWrapper.setMaxHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
        textWrapper.setPrefHeight(USE_COMPUTED_SIZE);
        textWrapper.setAlignment(Pos.CENTER);
        setMaxHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
        setWrapText(true);
      }

      @Override
      public void startEdit() {
        final ModularFeatureListRow row = getTableRow().getItem();
        if (row == null) {
          return;
        }
        final List<MsOtherCorrelationRowResult> correlated = row.get(
            MsOtherCorrelationResultType.class);
        if (correlated == null || correlated.isEmpty()) {
          return;
        }
        getItems().setAll(correlated);
        super.startEdit();
        if (isEditing() && getGraphic() instanceof ComboBox<?> combo) {
          // needs focus for proper working of esc/enter
          combo.requestFocus();
          combo.show();
        }
      }

      @Override
      public void commitEdit(Object newValue) {
        super.commitEdit(newValue);
        if (newValue instanceof MsOtherCorrelationRowResult selected) {
          final ModularFeatureListRow row = getTableRow().getItem();
          if (row.getFeatureList() instanceof ModularFeatureList flist) {
            logger.finest(() -> "Setting preferred correlated trace for row id %d to other-row %d"
                .formatted(row.getID(), selected.otherRowId()));
            // move the picked trace to the front of the row's correlation list (first = preferred)
            flist.getMsOtherCorrelationMaps()
                .setPreferredCorrelation(row.getID(), selected.otherRowId());
            getTreeTableView().refresh();
          }
        }
      }

      @Override
      public void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          final String formatted = item.toString();
          textValue.setText(formatted);
          setTooltip(new Tooltip(formatted));
          setText(formatted);
          setGraphic(null);
        }
        setAlignment(Pos.CENTER);
      }
    };
  }
}
