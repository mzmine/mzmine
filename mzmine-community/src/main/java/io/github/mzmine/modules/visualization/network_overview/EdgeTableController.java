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

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.visualization.networking.visual.FilterableGraph;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeAtt;
import io.github.mzmine.util.GraphStreamUtils;
import io.github.mzmine.util.StringUtils;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.textfield.TextFields;
import org.graphstream.graph.Edge;
import org.jetbrains.annotations.NotNull;

public class EdgeTableController {

  /**
   * The attributes rendered as columns, in column order. Also defines what the search field matches
   * against, so a search covers exactly what the user can see.
   */
  private static final List<EdgeAtt> SEARCHABLE_ATTRIBUTES = List.of(EdgeAtt.ID1, EdgeAtt.ID2,
      EdgeAtt.DELTA_MZ, EdgeAtt.SCORE, EdgeAtt.EXPLAINED_INTENSITY, EdgeAtt.MATCHED_SIGNALS,
      EdgeAtt.TYPE, EdgeAtt.LABEL, EdgeAtt.NEIGHBOR_DISTANCE);

  public BorderPane mainPane;
  public TableView<Edge> edgeTable;

  public TableColumn<Edge, Integer> colId1;
  public TableColumn<Edge, Integer> colId2;
  public TableColumn<Edge, Integer> colMatchedSignals;
  public TableColumn<Edge, Integer> colNeighborDistance;
  public TableColumn<Edge, Double> colDeltaMz;
  public TableColumn<Edge, Float> colScore;
  public TableColumn<Edge, Float> colExplainedIntensity;
  public TableColumn<Edge, String> colType;
  public TableColumn<Edge, String> colLabel;

  /**
   * Replaces the per-column popup filters of the previous ControlsFX FilteredTableView. Its
   * predicate is applied to the {@link FilteredList} created in {@link #setGraph(FilterableGraph)}.
   */
  private TextField searchField;

  @FXML
  public void initialize() {
    searchField = TextFields.createClearableTextField();
    searchField.setPromptText("Search edges...");
    searchField.setTooltip(new Tooltip("Enter text to search in all edge columns. "
        + "Multiple words are matched independently."));
    mainPane.setTop(FxLayout.newHBox(searchField));

    colLabel.setCellValueFactory(p -> getString(p, EdgeAtt.LABEL));
    colType.setCellValueFactory(p -> getString(p, EdgeAtt.TYPE));
    colDeltaMz.setCellValueFactory(p -> getDouble(p, EdgeAtt.DELTA_MZ));
    colScore.setCellValueFactory(p -> getFloat(p, EdgeAtt.SCORE));
    colExplainedIntensity.setCellValueFactory(p -> getFloat(p, EdgeAtt.EXPLAINED_INTENSITY));
    colId1.setCellValueFactory(p -> getInteger(p, EdgeAtt.ID1));
    colId2.setCellValueFactory(p -> getInteger(p, EdgeAtt.ID2));
    colNeighborDistance.setCellValueFactory(p -> getInteger(p, EdgeAtt.NEIGHBOR_DISTANCE));
    colMatchedSignals.setCellValueFactory(p -> getInteger(p, EdgeAtt.MATCHED_SIGNALS));
  }

  /**
   * The text a search query is matched against: the displayed value of every column, so searching
   * finds anything visible in the table.
   */
  private static @NotNull String rowText(final Edge edge) {
    final StringBuilder text = new StringBuilder();
    for (final EdgeAtt att : SEARCHABLE_ATTRIBUTES) {
      // "" and not null: getStringOrElse throws on a null default when the attribute is absent,
      // and not every edge type carries every attribute (e.g. MATCHED_SIGNALS on cosine edges only)
      final String value = GraphStreamUtils.getStringOrElse(edge, att, "");
      if (!value.isEmpty()) {
        text.append(value).append(' ');
      }
    }
    return text.toString();
  }

  @NotNull
  private static SimpleStringProperty getString(final CellDataFeatures<Edge, String> p,
      EdgeAtt att) {
    // "" and not null, see rowText: a null default makes getStringOrElse throw on a missing
    // attribute. Renders the same empty cell either way.
    return new SimpleStringProperty(GraphStreamUtils.getStringOrElse(p.getValue(), att, ""));
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
    // bound rather than a listener on the search field: the mapping only observes the text while
    // this FilteredList is alive, so calling setGraph again does not leave a stale listener behind.
    // A blank query yields an all-pass predicate, so the list starts unfiltered.
    filteredEdges.predicateProperty().bind(searchField.textProperty()
        .map(query -> StringUtils.<Edge>allWordsSubMatchPredicate(query,
            EdgeTableController::rowText)));
    edgeTable.setItems(sortedFilteredEdges);

    // TODO how to filter for edges
//    graph.addGraphChangeListener(__ -> {
//      var edgesSet = graph.edges().collect(Collectors.toSet());
//      filteredEdges.setPredicate(edgesSet::contains);
//    });
  }

}