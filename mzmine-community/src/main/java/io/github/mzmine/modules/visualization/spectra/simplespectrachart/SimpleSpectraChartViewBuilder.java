package io.github.mzmine.modules.visualization.spectra.simplespectrachart;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import javafx.collections.MapChangeListener;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * Builds the {@link SimpleSpectraChartController} view. Responsible for wiring the
 * {@link SimpleXYChart} to the model's observable state.
 */
public class SimpleSpectraChartViewBuilder extends FxViewBuilder<SimpleSpectraChartModel> {

  protected SimpleSpectraChartViewBuilder(@NotNull SimpleSpectraChartModel model) {
    super(model);
  }

  @Override
  public @NotNull Region build() {
    final BorderPane pane = new BorderPane();
    final SimpleXYChart<PlotXYDataProvider> chart = model.getChart();

    chart.setMinHeight(100);
    chart.setMinWidth(100);
    // enough space so that label is shown
    chart.getXYPlot().getRangeAxis().setUpperMargin(0.10);
    chart.setStickyZeroRangeAxis(true); // spectra start at 0 intensity
    pane.setCenter(chart);

    bindDatasets(chart);
    bindAxesAndTitle(chart);

    // Clear the drawn-label-bounds cache at the start of every chart draw pass. Using
    // ChartProgressEvent.DRAWING_STARTED (instead of PlotChangeListener) covers every cause of a
    // repaint — zoom, dataset add/remove, renderer swap AND window/SplitPane resize, the latter of
    // which does not fire a PlotChangeEvent because the plot's own state hasn't changed, only the
    // canvas size. Cached pixel bounds from the previous render would otherwise survive into the
    // new dataArea and block fresh labels.
    chart.getChart().addProgressListener(event -> {
      if (event.getType() == ChartProgressEvent.DRAWING_STARTED) {
        model.clearDrawnLabelBounds();
      }
    });

    return pane;
  }

  private void bindDatasets(@NotNull SimpleXYChart<PlotXYDataProvider> chart) {
    final SimpleSpectraItemLabelGenerator labelGenerator = new SimpleSpectraItemLabelGenerator(
        model);

    model.datasetRenderersProperty()
        .addListener((MapChangeListener<XYDataset, XYItemRenderer>) change -> {
          if (change.wasAdded()) {
            // Custom renderers added via addDataset(XYDataset, XYItemRenderer) bypass the chart's
            // defaultRenderer wiring, so the spectra-specific label generator and the chart's
            // tooltip generator are set here. The label generator is always overwritten so that
            // collision-aware spectra labeling wins over any pre-set generator.
            final XYItemRenderer r = change.getValueAdded();
            r.setDefaultItemLabelGenerator(labelGenerator);
            if (r.getDefaultToolTipGenerator() == null) {
              r.setDefaultToolTipGenerator(chart.getDefaultToolTipGenerator());
            }
            chart.addDataset(change.getKey(), r);
          }
          if (change.wasRemoved()) {
            if (change.getMap().isEmpty()) {
              chart.removeAllDatasets();
              return;
            }
            final XYDataset removedKey = change.getKey();
            chart.getAllDatasets().entrySet().stream() //
                .filter(e -> e.getValue() == removedKey) //
                .findFirst() //
                .ifPresent(e -> chart.removeDataSet(e.getKey()));
          }
        });
  }

  private void bindAxesAndTitle(@NotNull SimpleXYChart<PlotXYDataProvider> chart) {
    model.domainLabelProperty()
        .subscribe(label -> chart.getXYPlot().getDomainAxis().setLabel(label));
    model.rangeLabelProperty().subscribe(label -> chart.getXYPlot().getRangeAxis().setLabel(label));
    model.domainAxisFormatProperty().subscribe(format -> {
      if (format != null) {
        ((NumberAxis) chart.getXYPlot().getDomainAxis()).setNumberFormatOverride(format);
      }
    });
    model.rangeAxisFormatProperty().subscribe(format -> {
      if (format != null) {
        ((NumberAxis) chart.getXYPlot().getRangeAxis()).setNumberFormatOverride(format);
      }
    });
  }
}
