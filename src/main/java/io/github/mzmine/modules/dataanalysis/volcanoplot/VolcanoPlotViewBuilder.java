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
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYZDotRenderer;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestModule;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestModule.TESTS;
import io.github.mzmine.parameters.PropertyComponent;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
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

  private static final Logger logger = Logger.getLogger(VolcanoPlotViewBuilder.class.getName());

  private final ObjectProperty<@Nullable RowSignificanceTest> test = new SimpleObjectProperty<>();

  private final VolcanoPlotModel model;
  private final int space = 5;

  public VolcanoPlotViewBuilder(VolcanoPlotModel model, Consumer<Runnable> onDatasetRequired) {
    this.model = model;
  }

  @Override
  public Region build() {

    final BorderPane mainPane = new BorderPane();
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>("Volcano plot",
        "-log10(p-Value)", "log2(fold change)");
    mainPane.setCenter(chart);

    final HBox abundanceBox = createAbundanceBox();
    final HBox featureListBox = createFeatureListBox();
    final HBox testConfigPane = createTestParametersPane();

    final FlowPane controls = createControlsPane(featureListBox, abundanceBox, testConfigPane);
    mainPane.setBottom(controls);

    chart.setDefaultRenderer(new ColoredXYZDotRenderer());
    model.datasetsProperty().addListener((obs, o, n) -> {
      chart.applyWithNotifyChanges(false, () -> {
        chart.removeAllDatasets();
        chart.addDatasetProviders(n);
      });
    });

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
  private HBox createAbundanceBox() {
    final ComboBox<AbundanceMeasure> abundanceCombo = new ComboBox<>(
        FXCollections.observableList(List.of(AbundanceMeasure.values())));
    model.abundanceMeasureProperty()
        .bind(abundanceCombo.getSelectionModel().selectedItemProperty());

    final HBox abundanceBox = new HBox(space);
    abundanceBox.getChildren().addAll(new Label("Abundance measure:"), abundanceCombo);
    return abundanceBox;
  }

  private HBox createTestParametersPane() {

    final ComboBox<RowSignificanceTestModule.TESTS> testComboBox = new ComboBox<>(
        FXCollections.observableList(List.of(RowSignificanceTestModule.TESTS.values())));
    testComboBox.getSelectionModel().selectFirst();

    HBox testConfigPane = new HBox(5);

    // the combo box allows selection of the test and creates a component to configure the test
    testComboBox.valueProperty().addListener((obs, o, n) -> {
      testConfigPane.getChildren().clear();
      final Label caption = new Label(n.getModule().getName());
      testConfigPane.getChildren().add(caption);

      final Region component = n.getModule().createConfigurationComponent();
      testConfigPane.getChildren().add(component);

      ((PropertyComponent<?>) component).valueProperty()
          .addListener((ChangeListener<Object>) (observableValue, o1, t1) -> {
            try {
              // try to update the test
              final RowSignificanceTest instance = n.getModule()
                  .getInstance((PropertyComponent) component);
              test.set(instance);
            } catch (Exception e) {
              test.set(null);
              logger.log(Level.WARNING, e.getMessage(), e);
            }
          });
    });

    // listen to external changes of the test and update the combo box accordingly
    test.addListener((observableValue, rowSignificanceTest, newTest) -> {
      if (newTest == null) {
        return;
      }
      if (testComboBox.valueProperty().get().getTestClass().isInstance(newTest)) {
        return;
      }
      final TESTS newTestModule = testComboBox.getItems().stream()
          .filter(t -> t.getTestClass().isInstance(newTest)).findFirst().orElse(null);
      if (newTestModule != null) {
        testComboBox.setValue(newTestModule);
      } else {
        logger.warning("Selected test is invalid.");
      }
    });

    return testConfigPane;
  }
}
