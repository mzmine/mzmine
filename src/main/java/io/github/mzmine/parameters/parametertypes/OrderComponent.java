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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.util.javafx.DraggableListCell;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;

/**
 * A modified ListView that can reorder items by dragging with mouse
 *
 */
public class OrderComponent<ValueType> extends ListView<ValueType> {

  public OrderComponent() {

    // for some reason, plain JList does not have a border (at least on Mac)
    // Border border = BorderFactory.createEtchedBorder();
    // setBorder(border);

    setCellFactory(param -> new DraggableListCell<>() {
      @Override
      protected void updateItem(ValueType item, boolean empty) {
        super.updateItem(item, empty);
        if(!empty && item != null) {
          setText(item.toString());
        }
      }
    });

    // Adjust the size of the component
    setPrefWidth(150);
  }

  public void setValues(ValueType newValues[]) {
    setItems(FXCollections.observableArrayList(Arrays.asList(newValues)));
  }

  @SuppressWarnings("unchecked")
  public ValueType[] getValues() {
    return (ValueType[]) getItems().toArray();
  }


}
