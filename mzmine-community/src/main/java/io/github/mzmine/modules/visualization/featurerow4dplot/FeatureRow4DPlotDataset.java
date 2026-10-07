package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYZBubbleDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.AbstractXYZDataset;

/**
 * XYZ + bubble dataset for the 4D feature plot:
 * <ul>
 *   <li>x = average m/z</li>
 *   <li>y = average retention time</li>
 *   <li>z (color) = mobility if the source feature list has a {@link MobilityType} row binding,
 *       otherwise the retention time</li>
 *   <li>bubble size = {@code log10(maxHeight)}</li>
 * </ul>
 * Rows without m/z or RT are skipped at construction time so the index of each item lines up with
 * the cached {@code rows} array and {@link #getItemObject(int)} can return the row directly.
 */
public class FeatureRow4DPlotDataset extends AbstractXYZDataset implements XYZBubbleDataset,
    XYItemObjectProvider<FeatureListRow> {

  private final FeatureListRow[] rows;
  private final double[] xValues;
  private final double[] yValues;
  private final double[] zValues;
  private final double[] bubbleSizeValues;
  private final boolean colorByMobility;
  private final @NotNull String colorAxisLabel;

  public FeatureRow4DPlotDataset(@NotNull final FeatureList featureList) {
    this(featureList, featureList.getRows());
  }

  /**
   * Build the dataset from an explicit row list (e.g. derived from a
   * {@link io.github.mzmine.datamodel.features.compoundlist.CompoundList} via
   * {@code getRowsCopy(CompoundRowSelection)}). The {@code featureList} is still used for metadata
   * such as the {@link MobilityType} row binding that drives the colour axis.
   */
  public FeatureRow4DPlotDataset(@NotNull final FeatureList featureList,
      @NotNull final List<? extends FeatureListRow> sourceRows) {
    this.colorByMobility = featureList.hasRowType(MobilityType.class);
    this.colorAxisLabel = colorByMobility ? "Mobility" : "Retention time / min";

    final List<FeatureListRow> kept = new ArrayList<>(sourceRows.size());
    final List<Double> xs = new ArrayList<>(sourceRows.size());
    final List<Double> ys = new ArrayList<>(sourceRows.size());
    final List<Double> zs = new ArrayList<>(sourceRows.size());
    final List<Double> bubbles = new ArrayList<>(sourceRows.size());

    for (final FeatureListRow row : sourceRows) {
      final Double mz = row.getAverageMZ();
      final Float rt = row.getAverageRT();
      if (mz == null || rt == null || !Double.isFinite(mz) || !Float.isFinite(rt)) {
        continue;
      }
      final double colorValue;
      if (colorByMobility) {
        final Float mobility = row.getAverageMobility();
        // assumption: rows without a mobility on an IMS list are still worth plotting; 0.0 lets the
        // paint scale handle them at the low end without breaking quantile computation.
        colorValue = (mobility != null && Float.isFinite(mobility)) ? mobility : 0.0;
      } else {
        colorValue = rt;
      }
      // log10 of the apex intensity. Heights below 1 (including null) are clamped so log10 stays
      // finite at 0 — they will still render as the smallest bubble.
      final Float rawHeight = row.getMaxHeight();
      final double bubble = Math.log10(
          Math.max(1.0, rawHeight == null ? 0.0 : rawHeight.doubleValue()));

      kept.add(row);
      xs.add(mz);
      ys.add((double) rt);
      zs.add(colorValue);
      bubbles.add(bubble);
    }

    this.rows = kept.toArray(new FeatureListRow[0]);
    this.xValues = toArray(xs);
    this.yValues = toArray(ys);
    this.zValues = toArray(zs);
    this.bubbleSizeValues = toArray(bubbles);
  }

  private static double[] toArray(@NotNull final List<Double> list) {
    final double[] out = new double[list.size()];
    for (int i = 0; i < list.size(); i++) {
      out[i] = list.get(i);
    }
    return out;
  }

  public boolean isColorByMobility() {
    return colorByMobility;
  }

  public @NotNull String getColorAxisLabel() {
    return colorAxisLabel;
  }

  public double[] getZValues() {
    return zValues;
  }

  /**
   * Linear search for the dataset index that holds {@code row}. Returns {@code -1} when the row was
   * either not part of the source feature list or was skipped during construction (no m/z or RT).
   * Identity comparison — feature-list rows are unique instances.
   */
  public int indexOfRow(@Nullable final FeatureListRow row) {
    if (row == null) {
      return -1;
    }
    for (int i = 0; i < rows.length; i++) {
      if (rows[i] == row) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(final int series) {
    return "features";
  }

  @Override
  public int getItemCount(final int series) {
    return xValues.length;
  }

  @Override
  public Number getX(final int series, final int item) {
    return xValues[item];
  }

  @Override
  public Number getY(final int series, final int item) {
    return yValues[item];
  }

  @Override
  public Number getZ(final int series, final int item) {
    return zValues[item];
  }

  @Override
  public double getBubbleSizeValue(final int series, final int item) {
    return bubbleSizeValues[item];
  }

  @Override
  public double[] getBubbleSizeValues() {
    return bubbleSizeValues;
  }

  @Override
  public @Nullable FeatureListRow getItemObject(final int item) {
    if (item < 0 || item >= rows.length) {
      return null;
    }
    return rows[item];
  }
}
