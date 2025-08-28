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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;

class RowTypeFilterPresetListCell extends ListCell<RowTypeFilterPreset> {

  public RowTypeFilterPresetListCell() {
    Node content = buildNode();
    graphicProperty().bind(
        Bindings.createObjectBinding(() -> !isEmpty() ? content : null, emptyProperty()));
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
  }

  private Node buildNode() {
    final ObservableValue<String> name = itemProperty().map(RowTypeFilterPreset::name);
    final ObservableValue<String> selectedType = itemProperty().map(
        p -> p == null ? "" : " " + p.filter().selectedType() + ": ");
    final ObservableValue<String> query = itemProperty().map(
        p -> p == null ? "" : " " + p.filter().matchingMode() + " " + p.filter().query());

    return FxTextFlows.newTextFlow( //
        FxLabels.newLabel(selectedType), //
        FxLabels.newLabel(Styles.BOLD, name), //
        FxLabels.newLabel(query) //
    );
  }
}
