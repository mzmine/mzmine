package io.github.mzmine.modules.visualization.spectra.simplespectrachart;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.XYLabelCollisionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Spectra-specific label generator. Delegates screen-space collision detection to
 * {@link XYLabelCollisionResolver}, configured with the shared drawn-label-bounds cache and bottom
 * margin from {@link SimpleSpectraChartModel}.
 *
 * <p>The bounds cache is cleared by a {@code ChartProgressEvent.DRAWING_STARTED} listener
 * registered in {@link SimpleSpectraChartViewBuilder}.
 */
public class SimpleSpectraItemLabelGenerator implements XYItemLabelGenerator {

  private final @NotNull XYLabelCollisionResolver resolver;

  public SimpleSpectraItemLabelGenerator(final @NotNull SimpleSpectraChartModel model) {
    this.resolver = new XYLabelCollisionResolver(model::getXYPlot, model::getDrawnLabelBounds,
        model::getBottomLabelMargin);
  }

  @Override
  public @Nullable String generateLabel(final @NotNull XYDataset dataset, final int series,
      final int item) {
    if (!(dataset instanceof ColoredXYDataset cxy)) {
      return null;
    }

    final String label = cxy.getLabel(item);
    if (label == null || label.isBlank()) {
      return null;
    }

    return resolver.tryAcceptLabel(dataset, series, item, label) ? label : null;
  }
}
