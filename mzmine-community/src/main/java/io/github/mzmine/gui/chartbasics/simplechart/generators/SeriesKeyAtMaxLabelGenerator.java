package io.github.mzmine.gui.chartbasics.simplechart.generators;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import java.util.Arrays;
import java.util.WeakHashMap;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Renders the dataset's series key as an item label on the single point with the highest range
 * value (top of the curve). All other items return {@code null} so JFreeChart draws exactly one
 * label per series.
 *
 * <p>When an {@link XYLabelCollisionResolver} is supplied the label for the series-max point is
 * additionally filtered for screen-space overlap against all previously accepted labels — useful
 * when several chromatograms peak close to each other on the time axis.
 */
public class SeriesKeyAtMaxLabelGenerator implements XYItemLabelGenerator {

  // assumption: providers backed by IonTimeSeries are immutable after computeValues finishes,
  // so caching the max-item index by dataset reference is safe.
  private final WeakHashMap<XYDataset, int[]> maxItemPerSeries = new WeakHashMap<>();

  private final @Nullable XYLabelCollisionResolver collisionResolver;

  public SeriesKeyAtMaxLabelGenerator() {
    this(null);
  }

  public SeriesKeyAtMaxLabelGenerator(@Nullable XYLabelCollisionResolver collisionResolver) {
    this.collisionResolver = collisionResolver;
  }

  @Override
  public @Nullable String generateLabel(final XYDataset dataset, final int series, final int item) {
    if (!(dataset instanceof ColoredXYDataset coloredDataset)) {
      return null;
    }
    if (!coloredDataset.getValueProvider().isComputed()) {
      return null;
    }
    if (lookupMaxItem(coloredDataset, series) != item) {
      return null;
    }
    final Comparable<?> key = dataset.getSeriesKey(series);
    if (key == null) {
      return null;
    }
    final String label = key.toString();
    if (collisionResolver != null && !collisionResolver.tryAcceptLabel(dataset, series, item,
        label)) {
      return null;
    }
    return label;
  }

  private synchronized int lookupMaxItem(final ColoredXYDataset dataset, final int series) {
    final int seriesCount = dataset.getSeriesCount();
    int[] cached = maxItemPerSeries.get(dataset);
    if (cached == null || cached.length < seriesCount) {
      final int[] next = new int[seriesCount];
      Arrays.fill(next, -2); // -2 = not yet computed, -1 = computed but empty
      if (cached != null) {
        System.arraycopy(cached, 0, next, 0, cached.length);
      }
      cached = next;
      maxItemPerSeries.put(dataset, cached);
    }
    if (cached[series] == -2) {
      cached[series] = computeMaxItem(dataset, series);
    }
    return cached[series];
  }

  private static int computeMaxItem(final ColoredXYDataset dataset, final int series) {
    final int count = dataset.getItemCount(series);
    int maxItem = -1;
    double maxValue = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < count; i++) {
      final double y = dataset.getYValue(series, i);
      if (y > maxValue) {
        maxValue = y;
        maxItem = i;
      }
    }
    return maxItem;
  }
}
