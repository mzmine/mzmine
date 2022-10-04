/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.AddElementDialog;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.util.Callback;

/**
 * ComboBox cell factory for a DataType {@link ListDataType}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class EditComboCellFactory implements
    Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>> {

  private final SubColumnsFactory parentType;
  private final RawDataFile raw;
  private final DataType<?> type;
  private final int subcolumn;

  public EditComboCellFactory(RawDataFile raw, DataType<?> type, SubColumnsFactory parentType,
      int subcolumn) {
    this.type = type;
    this.raw = raw;
    this.parentType = parentType;
    this.subcolumn = subcolumn;
  }

  @Override
  public TreeTableCell<ModularFeatureListRow, Object> call(
      TreeTableColumn<ModularFeatureListRow, Object> param) {
    return new ComboBoxTreeTableCell<>() {
      @Override
      public void startEdit() {
        List list = getTypeList();
        getItems().clear();
        if (list != null) {
          getItems().addAll(list);
        }
        // create element that triggers the add element dialog on selection
        if (type instanceof AddElementDialog) {
          getItems().add(AddElementDialog.BUTTON_TEXT);
        }
        super.startEdit();
        if (isEditing() && getGraphic() instanceof ComboBox combo) {
          // needs focus for proper working of esc/enter
          combo.requestFocus();
          combo.show();
        }
      }

      @Override
      public void commitEdit(Object newValue) {
        super.commitEdit(newValue);
      }

      @Override
      public void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          // get list of this cell
          final List list;
          if (item instanceof List l) {
            // item is whole list
            list = l;
          } else {
            // item is single selected item
            List originalList = getTypeList();
            if (originalList != null) {
              // put object in first place of list
              list = new ArrayList<>(originalList);
              list.remove(item);
              list.add(0, item);
            } else {
              list = null;
            }
          }

          if (type instanceof GraphicalColumType graphType) {
            Node node = graphType.getCellNode(this, param, list, raw);
            getTableColumn().setMinWidth(graphType.getColumnWidth());
            setGraphic(node);
            setText(null);
            setTooltip(new Tooltip(type.getFormattedStringCheckType(list)));
          } else {
            String formatted = type.getFormattedStringCheckType(list);
            setTooltip(new Tooltip(formatted));
            setText(formatted);
            setGraphic(null);
          }
        }
        if (type instanceof NumberType) {
          setAlignment(Pos.CENTER_RIGHT);
        } else {
          setAlignment(Pos.CENTER);
        }
      }

      /**
       * Get the underlying list for this type in this cell
       *
       * @return
       */
      private List getTypeList() {
        ModularFeatureListRow row = getTreeTableRow().getItem();
        ModularDataModel model = raw == null ? row : row.getFeature(raw);
        final Object value;
        if (parentType instanceof DataType pt) {
          // SubColumnFactory parent type holds the value directly or in a subtype
          Object parentValue = model.get(pt);
          if (parentType.equals(type)) {
            value = parentValue;
          } else {
            value = parentType.getSubColValue(subcolumn, parentValue);
          }
        } else {
          value = model.get(type);
        }
        if (value instanceof List list) {
          return list;
        } else if (value == null) {
          return null;
        } else {
          throw new UnsupportedOperationException("Unhandled data type in edit combo CellFactory: "
                                                  + type.getHeaderString());
        }
      }
    };
  }
}
