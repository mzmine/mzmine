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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import static io.github.mzmine.javafx.components.util.FxLayout.newFlowPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;
import static io.github.mzmine.javafx.components.util.FxLayout.newTitledPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newVBox;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.RegionSelectionWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.statistics.TTestConfigurationComponent;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.math.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;

public class VolcanoPlotViewBuilder extends FxViewBuilder<VolcanoPlotModel> {

  private static final Logger logger = Logger.getLogger(VolcanoPlotViewBuilder.class.getName());

  private final Stroke annotationStroke = EStandardChartTheme.DEFAULT_MARKER_STROKE;
  private final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>("Volcano plot",
      "log2(fold change)", "-log10(p-Value)");
  // region wrapper not added directly to the gui because we want to combine the two accordions
  private final Consumer<List<List<Point2D>>> onExtractRegionsPressed;

  public VolcanoPlotViewBuilder(VolcanoPlotModel model,
      Consumer<List<List<Point2D>>> onExtractRegionsPressed) {
    super(model);
    this.onExtractRegionsPressed = onExtractRegionsPressed;
  }

  @Override
  public Region build() {

    final BorderPane mainPane = new BorderPane();
    final Accordion accordion = createControlsAccordion();

    // add the chart after the building the controls pane, bc the region wrapper automatically puts the
    // chart into it's center. to avoid the chart not showing up, set it to the plot pane afterward
    mainPane.setCenter(chart);
    mainPane.setBottom(accordion);

    chart.setDefaultRenderer(new ColoredXYShapeRenderer());
    model.datasetsProperty().addListener((_, _, n) -> {
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

        chart.getXYPlot().clearDomainMarkers(0);
        chart.getXYPlot().clearRangeMarkers(0);
        // p-Value line
        chart.getXYPlot().addRangeMarker(0,
            new ValueMarker(-Math.log10(model.getpValue()), neutralColor, annotationStroke),
            Layer.FOREGROUND);
        // annotation for fold change (a/b) = 0.5 (half)
        chart.getXYPlot().addDomainMarker(0,
            new ValueMarker(MathUtils.log(2, 0.5), neutralColor, annotationStroke),
            Layer.FOREGROUND);
        // annotation for fold change (a/b) = 2 (double) // log_2(2) = 1
        chart.getXYPlot().addDomainMarker(0, new ValueMarker(1, neutralColor, annotationStroke),
            Layer.FOREGROUND);
      });
    });

    addChartValueListener();
    initializeExternalSelectedRowListener();
    return mainPane;
  }

  private @NotNull Accordion createControlsAccordion() {
    final RegionSelectionWrapper<SimpleXYChart<?>> regionWrapper = new RegionSelectionWrapper<>(
        chart, onExtractRegionsPressed);

    final HBox pValueBox = createPValueBox();
    final HBox abundanceBox = createAbundanceBox();
    final HBox missingValueBox = createMissingValueBox();
    final VBox dataPreparationPane = newVBox(abundanceBox, missingValueBox);
    final Region testConfigPane = createTestParametersPane();

    final Button helpButton = FxButtons.createHelpButton(
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/statistics_dashboard/statistics_dashboard.html#volcano-plot");

    final VBox pValueAndButtonPane = newVBox(helpButton, pValueBox);

    final TitledPane controls = newTitledPane("Controls",
        newFlowPane(testConfigPane, dataPreparationPane, pValueAndButtonPane));
    final Accordion accordion = new Accordion(
        newTitledPane("Region of interest (ROI) selection", regionWrapper.getControlPane()),
        controls);
    accordion.setExpandedPane(controls);
    return accordion;
  }

  @NotNull
  private HBox createPValueBox() {
    Label label = new Label("p-Value:");
    final DoubleComponent pValueComponent = new DoubleComponent(100, 0.0, 1d,
        new DecimalFormat("0.###"), 0.05);
    Bindings.bindBidirectional(pValueComponent.getTextField().textProperty(),
        model.pValueProperty(), new DecimalFormat("0.###"));
    return newHBox(Insets.EMPTY, label, pValueComponent.getTextField());
  }

  @NotNull
  private HBox createAbundanceBox() {
    final ComboBox<AbundanceMeasure> abundanceCombo = new ComboBox<>(
        FXCollections.observableList(List.of(AbundanceMeasure.values())));
    abundanceCombo.setValue(model.getAbundanceMeasure());
    model.abundanceMeasureProperty().bindBidirectional(abundanceCombo.valueProperty());

    return newHBox(Insets.EMPTY, new Label("Abundance measure:"), abundanceCombo);
  }

  @NotNull
  private HBox createMissingValueBox() {
    final ComboBox<ImputationFunctions> missingValueCombo = new ComboBox<>(
        FXCollections.observableList(List.of(ImputationFunctions.valuesExcludeNone)));
    missingValueCombo.valueProperty().bindBidirectional(model.missingValueImputationProperty());

    return newHBox(Insets.EMPTY, new Label("Missing value imputation:"), missingValueCombo);
  }

  private Region createTestParametersPane() {
    final TTestConfigurationComponent ttestComponent = new TTestConfigurationComponent();
    ttestComponent.valueProperty().bindBidirectional(model.testProperty());
    return ttestComponent;
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

  private void initializeExternalSelectedRowListener() {
    model.selectedRowsProperty().addListener((_, old, rows) -> {
      if (rows.isEmpty() || ListUtils.isEqualList(old, rows)) {
        return;
      }
      final FeatureListRow selectedRow = rows.getFirst();
      if (selectedRow == null) {
        return;
      }
      SimpleChartUtility.selectItemInChart(chart, selectedRow, o1 -> {
        if (!(o1 instanceof RowSignificanceTestResult result)) {
          return null;
        }
        return result.row();
      }, RowSignificanceTestResult.class);
    });
  }

}
