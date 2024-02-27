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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYValueProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.framework.fx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestModules;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
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
import org.apache.commons.math.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.data.xy.XYDataset;

public class VolcanoPlotViewBuilder extends FxViewBuilder<VolcanoPlotModel> {

  private static final Logger logger = Logger.getLogger(VolcanoPlotViewBuilder.class.getName());

  private final int space = 5;
  private final Stroke annotationStroke = new BasicStroke(1.0f);
  private SimpleXYChart<PlotXYDataProvider> chart;

  public VolcanoPlotViewBuilder(VolcanoPlotModel model) {
    super(model);
  }

  @Override
  public Region build() {

    final BorderPane mainPane = new BorderPane();
    chart = new SimpleXYChart<>("Volcano plot", "log2(fold change)", "-log10(p-Value)");
    mainPane.setCenter(chart);

    final HBox pValueBox = createPValueBox();
    final HBox abundanceBox = createAbundanceBox();
    final Region testConfigPane = createTestParametersPane();

    final FlowPane controls = createControlsPane(pValueBox, abundanceBox, testConfigPane);
    mainPane.setBottom(controls);

    chart.setDefaultRenderer(new ColoredXYShapeRenderer());
    model.datasetsProperty().addListener((obs, o, n) -> {
      chart.applyWithNotifyChanges(false, () -> {
        final var neutralColor = MZmineCore.getConfiguration().getDefaultColorPalette()
            .getNeutralColorAWT();
        chart.removeAllDatasets();
        chart.getXYPlot().clearAnnotations();
        if (n == null) {
          return;
        }
        n.forEach(datasetAndRenderer -> chart.addDataset(datasetAndRenderer.dataset(),
            datasetAndRenderer.renderer()));

        // p-Value line
        final XYLineAnnotation pValueLine = new XYLineAnnotation(-1000d,
            -Math.log10(model.getpValue()), 1000d, -Math.log10(model.getpValue()), annotationStroke,
            neutralColor);
        // annotation for fold change (a/b) = 0.5 (half)
        final XYLineAnnotation leftFoldChange = new XYLineAnnotation(MathUtils.log(2, 0.5), 1000,
            MathUtils.log(2, 0.5), -1000, annotationStroke, neutralColor);
        // annotation for fold change (a/b) = 2 (double)
        final XYLineAnnotation rightFoldChange = new XYLineAnnotation(1, 1000, 1, -1000,
            annotationStroke, neutralColor);
        chart.getXYPlot().addAnnotation(pValueLine);
        chart.getXYPlot().addAnnotation(leftFoldChange);
        chart.getXYPlot().addAnnotation(rightFoldChange);
      });
    });

    addChartValueListener();

    return mainPane;
  }

  @NotNull
  private HBox createPValueBox() {
    final HBox pValueBox = new HBox(space);
    Label label = new Label("p-Value:");
    final DoubleComponent pValueComponent = new DoubleComponent(100, 0.0, 1d,
        new DecimalFormat("0.###"), 0.05);
    Bindings.bindBidirectional(pValueComponent.getTextField().textProperty(),
        model.pValueProperty(), new DecimalFormat("0.###"));
    pValueBox.getChildren().addAll(label, pValueComponent.getTextField());
    return pValueBox;
  }

  @NotNull
  private FlowPane createControlsPane(Region... panes) {
    final FlowPane controls = new FlowPane(Orientation.HORIZONTAL);
    controls.setHgap(space);
    controls.setVgap(space);
    controls.getChildren().addAll(panes);
    controls.setPadding(new Insets(space));
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

    FlowPane controls = new FlowPane(Orientation.HORIZONTAL, space, space);
    controls.setRowValignment(VPos.CENTER);
    controls.getChildren().addAll(new Label("Test type:"), testComboBox);

    HBox testConfigPane = new HBox(5);
    controls.getChildren().add(testConfigPane);

    // the combo box allows selection of the test and creates a component to configure the test
    testComboBox.valueProperty().addListener((obs, o, n) -> {
      testConfigPane.getChildren().clear();
      final Region component = n.getModule().createConfigurationComponent();
      testConfigPane.getChildren().add(component);

      ((ValuePropertyComponent<?>) component).valueProperty()
          .addListener((ChangeListener<Object>) (observableValue, o1, t1) -> {
            try {
              // try to update the test
              final RowSignificanceTest instance = n.getModule()
                  .getInstance((ValuePropertyComponent) component);
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

  private void addChartValueListener() {
    chart.cursorPositionProperty().addListener((obs, o, pos) -> {
      final XYDataset dataset = pos.getDataset();
      if (!(dataset instanceof ColoredXYDataset cds)) {
        return;
      }
      if (!(cds.getValueProvider() instanceof XYItemObjectProvider<?> provider)) {
        return;
      }
      final int index = pos.getValueIndex();
      final Object item = provider.getItemObject(index);
      if (item instanceof RowSignificanceTestResult testResult) {
        model.selectedRowsProperty().set(List.of(testResult.row()));
      }
    });
  }

  // todo: enable as soon as we merge the pr with mvci row listener interfaces.
  private void initializeExternalSelectedRowListener() {
    model.selectedRowsProperty().addListener((obs, o, rows) -> {
      // todo: this listener should be executed on a seperate thread to not stop the gui from
      //  working while looping through the values
      //  is it best practice to do this searching from the controller?
      //  then the controller needs to access this view's chart.
      //  Otherwise this view needs to be able to start a task on the controller.
      if (rows.isEmpty()) {
        return;
      }

      final FeatureListRow selectedRow = rows.get(0);
      final LinkedHashMap<Integer, XYDataset> datasets = chart.getAllDatasets();
      for (XYDataset dataset : datasets.values()) {
        if (!(dataset instanceof ColoredXYDataset cds)) {
          continue;
        }
        final XYValueProvider valueProvider = cds.getValueProvider();
        if (!(valueProvider instanceof XYItemObjectProvider<?> provider)) {
          return;
        }
        for (int i = 0; i < valueProvider.getValueCount(); i++) {
          final Object itemObject = provider.getItemObject(i);
          if (!(itemObject instanceof RowSignificanceTestResult result)) {
            continue;
          }
          if (result.row().equals(selectedRow)) {
            logger.finest(
                STR."Selecting row id \{selectedRow.getID()} in dataset \{cds.getSeriesKey()}.");

            int finalI = i;
            // todo: only this should be done on the fx thread
            chart.applyWithNotifyChanges(false, () -> {
              chart.setCursorPosition(new PlotCursorPosition(valueProvider.getDomainValue(finalI),
                  valueProvider.getRangeValue(finalI), finalI, dataset));
              chart.getXYPlot().setDomainCrosshairValue(valueProvider.getDomainValue(finalI));
              chart.getXYPlot().setRangeCrosshairValue(valueProvider.getRangeValue(finalI));
            });
            return;
          }
        }
      }
    });
  }
}
