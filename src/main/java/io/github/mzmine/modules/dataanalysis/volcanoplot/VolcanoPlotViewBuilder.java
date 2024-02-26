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
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.framework.fx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestModules;
import io.github.mzmine.parameters.PropertyComponent;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.annotations.XYLineAnnotation;

public class VolcanoPlotViewBuilder extends FxViewBuilder<VolcanoPlotModel> {

  private static final Logger logger = Logger.getLogger(VolcanoPlotViewBuilder.class.getName());

  private final int space = 5;
  private final Stroke annotationStroke = new BasicStroke(1.0f);

  public VolcanoPlotViewBuilder(VolcanoPlotModel model, Consumer<Runnable> onDatasetRequired) {
    super(model);
  }

  @Override
  public Region build() {

    final BorderPane mainPane = new BorderPane();
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>("Volcano plot",
        "log2(fold change)", "-log10(p-Value)");
    mainPane.setCenter(chart);

    final HBox abundanceBox = createAbundanceBox();
    final HBox featureListBox = createFeatureListBox();
    final Region testConfigPane = createTestParametersPane();

    final FlowPane controls = createControlsPane(featureListBox, abundanceBox, testConfigPane);
    mainPane.setBottom(controls);

    chart.setDefaultRenderer(new ColoredXYShapeRenderer());
    model.datasetsProperty().addListener((obs, o, n) -> {
      chart.applyWithNotifyChanges(false, () -> {
        final var neutralColor = MZmineCore.getConfiguration().getDefaultColorPalette()
            .getNeutralColorAWT();
        chart.removeAllDatasets();
        chart.getXYPlot().clearAnnotations();
        n.forEach(chart::addDataset);

        final XYLineAnnotation pValueLine = new XYLineAnnotation(-1000d, 1.301029996, 1000d,
            1.301029996, annotationStroke, neutralColor);
        final XYLineAnnotation leftFoldChange = new XYLineAnnotation(-1, 1000, -1, -1000,
            annotationStroke, neutralColor);
        final XYLineAnnotation rightFoldChange = new XYLineAnnotation(1, 1000, 1, -1000,
            annotationStroke, neutralColor);
        chart.getXYPlot().addAnnotation(pValueLine);
        chart.getXYPlot().addAnnotation(leftFoldChange);
        chart.getXYPlot().addAnnotation(rightFoldChange);
      });
    });

    return mainPane;
  }

  @NotNull
  private HBox createFeatureListBox() {
    final HBox featureListBox = new HBox(space);
    final ComboBox<FeatureList> flistBox = new ComboBox<>();
    flistBox.itemsProperty().bindBidirectional(model.flistsProperty());
    flistBox.valueProperty().bindBidirectional(model.selectedFlistProperty());
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
    abundanceCombo.setValue(model.getAbundanceMeasure());
    model.abundanceMeasureProperty().bindBidirectional(abundanceCombo.valueProperty());

    final HBox abundanceBox = new HBox(space);
    abundanceBox.getChildren().addAll(new Label("Abundance measure:"), abundanceCombo);
    return abundanceBox;
  }

  private Region createTestParametersPane() {

    final ComboBox<RowSignificanceTestModules> testComboBox = new ComboBox<>(
        FXCollections.observableList(RowSignificanceTestModules.TWO_GROUP_TESTS));

    FlowPane controls = new FlowPane(Orientation.HORIZONTAL, 5, 5);
    controls.setRowValignment(VPos.CENTER);
    controls.getChildren().addAll(new Label("Test type:"), testComboBox);

    HBox testConfigPane = new HBox(5);
    controls.getChildren().add(testConfigPane);

    // the combo box allows selection of the test and creates a component to configure the test
    testComboBox.valueProperty().addListener((obs, o, n) -> {
      testConfigPane.getChildren().clear();
      final Region component = n.getModule().createConfigurationComponent();
      testConfigPane.getChildren().add(component);

      ((PropertyComponent<?>) component).valueProperty()
          .addListener((ChangeListener<Object>) (observableValue, o1, t1) -> {
            try {
              // try to update the test
              final RowSignificanceTest instance = n.getModule()
                  .getInstance((PropertyComponent) component);
              model.setTest(instance);
            } catch (Exception e) {
              model.setTest(null);
              logger.log(Level.WARNING, e.getMessage(), e);
            }
          });
    });

    // listen to external changes of the test and update the combo box accordingly
    model.testProperty().addListener((observableValue, rowSignificanceTest, newTest) -> {
      if (newTest == null) {
        return;
      }
      final RowSignificanceTestModules newTestModule = testComboBox.getItems().stream()
          .filter(t -> t.getTestClass().isInstance(newTest)).findFirst().orElse(null);
      if (newTestModule != null) {
        testComboBox.setValue(newTestModule);
      } else {
        logger.warning("Selected test is invalid.");
      }
    });

    testComboBox.getSelectionModel().selectFirst();
    return controls;
  }
}
