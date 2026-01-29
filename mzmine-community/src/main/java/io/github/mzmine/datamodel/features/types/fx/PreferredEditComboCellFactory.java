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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.PreferredAnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
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
 * ComboBox cell factory for a DataType {@link ListDataType}
 *
 */
public class PreferredEditComboCellFactory implements
    Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>> {

  private static final Logger logger = Logger.getLogger(
      PreferredEditComboCellFactory.class.getName());

  private final SubColumnsFactory parentType;
  private final RawDataFile raw;
  private final DataType<?> type;
  private final int subcolumn;

  public PreferredEditComboCellFactory(RawDataFile raw, DataType<?> type,
      SubColumnsFactory parentType, int subcolumn) {
    this.type = type;
    this.raw = raw;
    this.parentType = parentType;
    this.subcolumn = subcolumn;
  }

  @Override
  public TreeTableCell<ModularFeatureListRow, Object> call(
      TreeTableColumn<ModularFeatureListRow, Object> param) {
    return new ComboBoxTreeTableCell<>() {

      Label textValue = new Label();
      VBox textWrapper = new VBox(textValue);

      {
        textValue.setWrapText(true);
        textWrapper.setMaxHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
        textWrapper.setPrefHeight(USE_COMPUTED_SIZE);
        textWrapper.setAlignment(Pos.CENTER);
        setPrefWidth(type.getPrefColumnWidth());
        textValue.setPrefWidth(type.getPrefColumnWidth());
        setMaxHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);
        setWrapText(true);
      }

      @Override
      public void startEdit() {
        List<FeatureAnnotation> list = CompoundAnnotationUtils.getAllFeatureAnnotationsByDescendingConfidence(
            getTableRow().getItem());
        getItems().setAll(list);
        super.startEdit();
        if (isEditing() && getGraphic() instanceof ComboBox<?> combo) {
          // needs focus for proper working of esc/enter
          combo.requestFocus();
          combo.show();
        }
      }

      @Override
      public void commitEdit(Object newValue) {
        // sometimes this method seems to be called with a wrapped value
        // (ArrayList with the selected value). Did not find a way to resolve this here or before,
        // so this is handled in DataType#createStandardColumn
        super.commitEdit(newValue);
        if (newValue instanceof FeatureAnnotation newAnnotation) {
          logger.finest("Changing preferred annotation for row id %d to %s".formatted(
              getTableRow().getItem().getID(), newAnnotation.toString()));
          getTableRow().getItem().set(PreferredAnnotationType.class, newAnnotation);
          getTreeTableView().refresh();
        }
      }

      @Override
      public void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          String formatted = type.getFormattedStringCheckType(item);
          if (item instanceof FeatureAnnotation a) {
            formatted += " (" + a.getAnnotationMethodName() + ")";
          }
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
