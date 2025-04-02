/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.RegionSelectionWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import static io.github.mzmine.javafx.components.util.FxLayout.newFlowPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;
import static io.github.mzmine.javafx.components.util.FxLayout.newTitledPane;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.dataanalysis.utils.scaling.ScalingFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingComponent;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import org.controlsfx.control.CheckComboBox;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.ValueMarker;

public class PCAViewBuilder extends FxViewBuilder<PCAModel> {

  private final SimpleXYChart<?> scoresPlot = new SimpleXYChart<>("Scores", "PC1", "PC2");
  private final SimpleXYChart<?> loadingsPlot = new SimpleXYChart<>("Loadings", "PC1", "PC2");
  // add the wrapper to the other accordion and not directly around the chart
  private final Consumer<List<List<Point2D>>> onExtractRegionsPressed;

  public PCAViewBuilder(PCAModel model, Consumer<List<List<Point2D>>> onExtractRegionsPressed) {
    super(model);
    this.onExtractRegionsPressed = onExtractRegionsPressed;
  }

  @Override
  public Region build() {
    scoresPlot.setStickyZeroRangeAxis(false);
    loadingsPlot.setStickyZeroRangeAxis(false);
    final Color markerColor = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getNeutralColorAWT();
    scoresPlot.getXYPlot().addDomainMarker(
        new ValueMarker(0, markerColor, EStandardChartTheme.DEFAULT_MARKER_STROKE));
    scoresPlot.getXYPlot()
        .addRangeMarker(new ValueMarker(0, markerColor, EStandardChartTheme.DEFAULT_MARKER_STROKE));
    loadingsPlot.getXYPlot().addDomainMarker(
        new ValueMarker(0, markerColor, EStandardChartTheme.DEFAULT_MARKER_STROKE));
    loadingsPlot.getXYPlot()
        .addRangeMarker(new ValueMarker(0, markerColor, EStandardChartTheme.DEFAULT_MARKER_STROKE));

    final BorderPane pane = new BorderPane();
    final Accordion accordion = buildControlsAccordion();
    pane.setBottom(accordion);

    scoresPlot.setMinSize(200, 200);
    loadingsPlot.setMinSize(200, 200);

    // build the plot pane after the controls pane, bc the region wrapper automatically puts the
    // chart into it's center. to avoid the chart not showing up, set it to the plot pane afterward
    final GridPane plotPane = createPlotPane();

//    final FlowPane plots = new FlowPane(new BorderPane(scoresPlot), new BorderPane(loadingsPlot));
    pane.setCenter(plotPane);

    initDatasetListeners();
    initChartListeners();
    return pane;
  }

  private @NotNull Accordion buildControlsAccordion() {
    final HBox scaling = FxComboBox.createLabeledComboBox("Scaling",
        FXCollections.observableArrayList(ScalingFunctions.values()),
        model.scalingFunctionProperty());
    final HBox imputation = FxComboBox.createLabeledComboBox("Missing value imputation",
        FXCollections.observableArrayList(ImputationFunctions.values()),
        model.imputationFunctionProperty());
    final HBox domain = FxComboBox.createLabeledComboBox("Domain PC", model.getAvailablePCs(),
        model.domainPcProperty());
    final HBox range = FxComboBox.createLabeledComboBox("Range PC", model.getAvailablePCs(),
        model.rangePcProperty());
    final HBox coloring = createMetadataBox();
    final HBox abundance = FxComboBox.createLabeledComboBox("Abundance",
        FXCollections.observableArrayList(AbundanceMeasure.values()), model.abundanceProperty());
    final CheckComboBox<SampleType> sampleTypesBox = new CheckComboBox<>(
        FXCollections.observableArrayList(SampleType.values()));
    sampleTypesBox.getCheckModel().clearChecks();
    sampleTypesBox.getCheckModel().check(SampleType.SAMPLE);
    sampleTypesBox.getCheckModel().getCheckedItems().addListener(
        (ListChangeListener<SampleType>) c -> model.setSampleTypeFilter(
            SampleTypeFilter.of((List<SampleType>) c.getList())));
    final HBox sampleBox = newHBox(Insets.EMPTY, FxLabels.newLabel("Sample types"), sampleTypesBox);

    final TitledPane controls = new TitledPane("Controls",
        newFlowPane(scaling, imputation, domain, range, coloring, abundance, sampleBox));
    final RegionSelectionWrapper<? extends SimpleXYChart<?>> loadingsWrapper = new RegionSelectionWrapper<>(
        loadingsPlot, onExtractRegionsPressed);
    final Accordion accordion = new Accordion(
        newTitledPane("Regions of interest (ROI) selection from Loadings plot",
            loadingsWrapper.getControlPane()), controls);
    accordion.setExpandedPane(controls);
    return accordion;
  }

  @NotNull
  private GridPane createPlotPane() {
    GridPane plotPane = new GridPane();

    plotPane.add(scoresPlot, 0, 0);
    plotPane.add(loadingsPlot, 1, 0);
    plotPane.getColumnConstraints().addAll(
        new ColumnConstraints(300, GridPane.USE_COMPUTED_SIZE, GridPane.USE_COMPUTED_SIZE,
            Priority.ALWAYS, HPos.CENTER, true),
        new ColumnConstraints(300, GridPane.USE_COMPUTED_SIZE, GridPane.USE_COMPUTED_SIZE,
            Priority.ALWAYS, HPos.CENTER, true));
    plotPane.getRowConstraints().add(
        new RowConstraints(300, GridPane.USE_COMPUTED_SIZE, GridPane.USE_COMPUTED_SIZE,
            Priority.ALWAYS, VPos.CENTER, true));
    return plotPane;
  }

  private HBox createMetadataBox() {
    final Label coloringLabel = new Label("Coloring:");
    final MetadataGroupingComponent coloringSelection = new MetadataGroupingComponent();
    coloringSelection.valueProperty().bindBidirectional(model.metadataColumnProperty());
    return newHBox(Pos.CENTER_LEFT, coloringLabel, coloringSelection);
  }

  private void initDatasetListeners() {
    model.loadingsDatasetsProperty().addListener(((_, _, newValue) -> {
      loadingsPlot.applyWithNotifyChanges(false, () -> {
        setDatasets(loadingsPlot, newValue);
      });
    }));

    model.scoresDatasetsProperty().addListener(((_, _, newValue) -> {
      scoresPlot.applyWithNotifyChanges(false, () -> {
        setDatasets(scoresPlot, newValue);
      });
    }));
  }

  private void setDatasets(final SimpleXYChart<?> plot, final List<DatasetAndRenderer> newValue) {
    plot.removeAllDatasets();
    if (newValue == null || newValue.isEmpty()) {
      return;
    }

    LegendItemCollection collection = new LegendItemCollection();
    newValue.forEach(d -> {
      plot.addDataset(d.dataset(), d.renderer());
      collection.addAll(d.renderer().getLegendItems());
    });
    int domainPc = model.getDomainPc();
    int rangePc = model.getRangePc();
    int maxPc = Math.max(domainPc, rangePc);

    // show PC contribution on label - otherwise 0 before its calculated
    PCARowsResult results = model.getPcaResult();
    float[] contributions =
        results == null ? new float[maxPc] : results.pcaResult().getComponentContributions(maxPc);

    var percent = ConfigService.getGuiFormats().percentFormat();

    plot.getXYPlot().setFixedLegendItems(collection);
    plot.setDomainAxisLabel(
        "PC%d (%s)".formatted(domainPc, percent.format(contributions[domainPc - 1])));
    plot.setRangeAxisLabel(
        "PC%d (%s)".formatted(rangePc, percent.format(contributions[rangePc - 1])));
  }

  private void initChartListeners() {
    // todo listen the other way round
    loadingsPlot.cursorPositionProperty().addListener((_, _, n) -> {
      if (n == null || !(n.getDataset() instanceof ColoredXYZDataset zds)
          || !(zds.getValueProvider() instanceof XYItemObjectProvider<?> dataProvider)) {
        return;
      }
      final Object row = dataProvider.getItemObject(n.getValueIndex());
      if (row instanceof FeatureListRow r) {
        model.selectedRowsProperty().set(List.of(r));
      }
    });

    model.selectedRowsProperty().addListener((_, old, rows) -> {
      if (rows.isEmpty() || (old != null && old.equals(rows))) {
        return;
      }

      final FeatureListRow selectedRow = rows.getFirst();
      if (selectedRow == null) {
        return;
      }
      SimpleChartUtility.selectItemInChart(loadingsPlot, selectedRow, o -> o, FeatureListRow.class);
    });
  }
}
