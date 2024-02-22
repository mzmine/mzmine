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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.PropertyComponent;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VolcanoPlotViewBuilder implements Builder<Region> {

  private final ObjectProperty<@Nullable RowSignificanceTest> test = new SimpleObjectProperty<>();

  private final VolcanoPlotModel model;
  private final int space = 5;

  public VolcanoPlotViewBuilder(VolcanoPlotModel model, Consumer<Runnable> onDatasetRequired) {
    this.model = model;
  }

  @Override
  public Region build() {

    final BorderPane mainPane = new BorderPane();
    final SimpleXYZScatterPlot<PlotXYZDataProvider> chart = new SimpleXYZScatterPlot<>(
        "Volcano plot");
    mainPane.setCenter(chart);

    final HBox metadataBox = createMetadataBox();
    final HBox abundanceBox = createAbundanceBox();
    final HBox featureListBox = createFeatureListBox();

    model.testProperty().bind(test);

    final FlowPane controls = createControlsPane(featureListBox, metadataBox, abundanceBox);
    mainPane.setBottom(controls);
    return mainPane;
  }

  @NotNull
  private HBox createFeatureListBox() {
    final HBox featureListBox = new HBox(space);
    final ComboBox<FeatureList> flistBox = new ComboBox<>();
    flistBox.itemsProperty().bindBidirectional(model.flistsProperty());
    model.selectedFlistProperty().bind(flistBox.getSelectionModel().selectedItemProperty());
    featureListBox.getChildren().addAll(new Label("Feature list:"), flistBox);
    if (!flistBox.getItems().isEmpty()) {
      flistBox.getSelectionModel().select(0);
    }
    return featureListBox;
  }

  @NotNull
  private FlowPane createControlsPane(Region... panes) {
    final FlowPane controls = new FlowPane(Orientation.HORIZONTAL);
    controls.setHgap(space);
    controls.setVgap(space);
    controls.getChildren().addAll(panes);
    controls.setPadding(new Insets(5));
    controls.setAlignment(Pos.CENTER);
    return controls;
  }

  @NotNull
  private HBox createMetadataBox() {
    final ComboBox<MetadataColumn<?>> metadataCombo = new ComboBox<>();
    metadataCombo.itemsProperty().bind(model.metadataColumnsProperty());
    model.selectedMetadataColumnProperty()
        .bind(metadataCombo.getSelectionModel().selectedItemProperty());

    final HBox metadataBox = new HBox(space);
    metadataBox.getChildren().addAll(new Label("Metadata column:"), metadataCombo);
    return metadataBox;
  }

  @NotNull
  private HBox createAbundanceBox() {
    final ComboBox<AbundanceMeasure> abundanceCombo = new ComboBox<>(
        FXCollections.observableList(List.of(AbundanceMeasure.values())));
    model.abundanceMeasureProperty()
        .bind(abundanceCombo.getSelectionModel().selectedItemProperty());

    final HBox abundanceBox = new HBox(space);
    abundanceBox.getChildren().addAll(new Label("Abundance measure:"), abundanceCombo);
    return abundanceBox;
  }

  private Node createTestParametersPane() {

    final ComboBox<RowSignificanceTestModule.TESTS> testComboBox = new ComboBox<>(
        FXCollections.observableList(List.of(RowSignificanceTestModule.TESTS.values())));
    testComboBox.getSelectionModel().selectFirst();

    HBox testConfigPane = new HBox(5);

    testComboBox.valueProperty().addListener((obs, o, n) -> {
      testConfigPane.getChildren().clear();
      final Label caption = new Label(n.getModule().getName());
      testConfigPane.getChildren().add(caption);

      final Region component = n.getModule().createConfigurationComponent();
      testConfigPane.getChildren().add(component);

      ((PropertyComponent<?>)component).valueProperty()
    });


  }
}
