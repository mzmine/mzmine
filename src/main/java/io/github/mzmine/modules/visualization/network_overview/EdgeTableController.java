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

package io.github.mzmine.modules.visualization.network_overview;

import io.github.mzmine.modules.visualization.networking.visual.FilterableGraph;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeAtt;
import io.github.mzmine.util.GraphStreamUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn.CellDataFeatures;
import org.controlsfx.control.tableview2.FilteredTableColumn;
import org.controlsfx.control.tableview2.FilteredTableView;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;
import org.controlsfx.control.tableview2.filter.popupfilter.PopupFilter;
import org.controlsfx.control.tableview2.filter.popupfilter.PopupNumberFilter;
import org.controlsfx.control.tableview2.filter.popupfilter.PopupStringFilter;
import org.graphstream.graph.Edge;
import org.jetbrains.annotations.NotNull;

public class EdgeTableController {

  public FilteredTableView<Edge> edgeTable;

  public FilteredTableColumn<Edge, Integer> colId1;
  public FilteredTableColumn<Edge, Integer> colId2;
  public FilteredTableColumn<Edge, Integer> colMatchedSignals;
  public FilteredTableColumn<Edge, Integer> colNeighborDistance;
  public FilteredTableColumn<Edge, Double> colDeltaMz;
  public FilteredTableColumn<Edge, Float> colScore;
  public FilteredTableColumn<Edge, Float> colExplainedIntensity;
  public FilteredTableColumn<Edge, String> colType;
  public FilteredTableColumn<Edge, String> colLabel;

  @FXML
  public void initialize() {

    colLabel.setCellValueFactory(p -> getString(p, EdgeAtt.LABEL));
    colType.setCellValueFactory(p -> getString(p, EdgeAtt.TYPE));
    colDeltaMz.setCellValueFactory(p -> getDouble(p, EdgeAtt.DELTA_MZ));
    colScore.setCellValueFactory(p -> getFloat(p, EdgeAtt.SCORE));
    colExplainedIntensity.setCellValueFactory(p -> getFloat(p, EdgeAtt.EXPLAINED_INTENSITY));
    colId1.setCellValueFactory(p -> getInteger(p, EdgeAtt.ID1));
    colId2.setCellValueFactory(p -> getInteger(p, EdgeAtt.ID2));
    colNeighborDistance.setCellValueFactory(p -> getInteger(p, EdgeAtt.NEIGHBOR_DISTANCE));
    colMatchedSignals.setCellValueFactory(p -> getInteger(p, EdgeAtt.MATCHED_SIGNALS));

    edgeTable.setRowHeaderVisible(true);
    PopupFilter<Edge, String> popupFilter = new PopupStringFilter<>(colLabel);
    colLabel.setOnFilterAction(e -> popupFilter.showPopup());
    PopupFilter<Edge, String> popupFilter2 = new PopupStringFilter<>(colType);
    colType.setOnFilterAction(e -> popupFilter2.showPopup());
    addPopupNumberFilter(colId1);
    addPopupNumberFilter(colId2);
    addPopupNumberFilter(colDeltaMz);
    addPopupNumberFilter(colExplainedIntensity);
    addPopupNumberFilter(colScore);
    addPopupNumberFilter(colMatchedSignals);
    addPopupNumberFilter(colNeighborDistance);

    SouthFilter<Edge, String> southTypeFilter = new SouthFilter<>(colType, String.class);
    colType.setSouthNode(southTypeFilter);
  }

  private void addPopupNumberFilter(final FilteredTableColumn<Edge, ? extends Number> col) {
    var popup = new PopupNumberFilter<>(col);
    col.setOnFilterAction(e -> popup.showPopup());
  }

  @NotNull
  private static SimpleStringProperty getString(final CellDataFeatures<Edge, String> p,
      EdgeAtt att) {
    return new SimpleStringProperty(GraphStreamUtils.getStringOrElse(p.getValue(), att, null));
  }

  @NotNull
  private static SimpleObjectProperty<Integer> getInteger(final CellDataFeatures<Edge, Integer> p,
      EdgeAtt att) {
    return new SimpleObjectProperty<>(
        GraphStreamUtils.getIntegerValue(p.getValue(), att).orElse(null));
  }

  @NotNull
  private static SimpleObjectProperty<Double> getDouble(final CellDataFeatures<Edge, Double> p,
      EdgeAtt att) {
    return new SimpleObjectProperty<>(
        GraphStreamUtils.getDoubleValue(p.getValue(), att).orElse(null));
  }

  @NotNull
  private static SimpleObjectProperty<Float> getFloat(final CellDataFeatures<Edge, Float> p,
      EdgeAtt att) {
    return new SimpleObjectProperty<>(
        GraphStreamUtils.getFloatValue(p.getValue(), att).orElse(null));
  }

  public void setGraph(final FilterableGraph graph) {
    // add all edges and filter later
    FilteredList<Edge> filteredEdges = new FilteredList<>(
        FXCollections.observableArrayList(graph.getFullGraph().edges().toList()));
    SortedList<Edge> sortedFilteredEdges = new SortedList<>(filteredEdges);

    sortedFilteredEdges.comparatorProperty().bind(edgeTable.comparatorProperty());
    filteredEdges.predicateProperty().bind(edgeTable.predicateProperty());
    edgeTable.setItems(sortedFilteredEdges);

    // TODO how to filter for edges
//    graph.addGraphChangeListener(__ -> {
//      var edgesSet = graph.edges().collect(Collectors.toSet());
//      filteredEdges.setPredicate(edgesSet::contains);
//    });
  }

}