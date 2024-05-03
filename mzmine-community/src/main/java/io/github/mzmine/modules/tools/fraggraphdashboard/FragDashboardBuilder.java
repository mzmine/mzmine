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

package io.github.mzmine.modules.tools.fraggraphdashboard;

import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.tools.fraggraphdashboard.nodetable.NodeTable;
import java.util.Comparator;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class FragDashboardBuilder extends FxViewBuilder<FragDashboardModel> {

  protected FragDashboardBuilder(FragDashboardModel model, @NotNull Region region) {
    super(model);
  }

  @Override
  public Region build() {

    NodeTable nodeTable = new NodeTable();
    nodeTable.itemsProperty().bind(Bindings.createObjectBinding(
        () -> FXCollections.observableArrayList(model.getAllNodes().values().stream()
            .sorted(Comparator.comparingDouble(sfm -> sfm.getPeakWithFormulae().peak().getMZ()))
            .toList()), model.allNodesProperty()));

    nodeTable.getSelectionModel().getSelectedIndices();

    model.selectedNodesProperty().bindBidirectional(Bindings.createObjectBinding(
        () -> FXCollections.observableMap(nodeTable.getSelectionModel().getSelectedItems().stream()
            .collect(Collectors.toMap(sfm -> sfm.getId(), sfm -> sfm))),
        nodeTable.getSelectionModel().getSelectedItems()));

    return null;
  }
}
