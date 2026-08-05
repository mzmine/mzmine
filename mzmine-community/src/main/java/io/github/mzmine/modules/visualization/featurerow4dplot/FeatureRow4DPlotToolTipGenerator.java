package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Tool-tip generator for the 4D feature plot. Resolves the dataset item back to the source
 * {@link FeatureListRow} so the tooltip can display id, m/z, RT, intensity and the optional
 * preferred annotation.
 */
public class FeatureRow4DPlotToolTipGenerator implements XYToolTipGenerator {

  private final @NotNull FeatureRow4DPlotDataset dataset;

  public FeatureRow4DPlotToolTipGenerator(@NotNull final FeatureRow4DPlotDataset dataset) {
    this.dataset = dataset;
  }

  @Override
  public String generateToolTip(final XYDataset xyDataset, final int series, final int item) {
    final FeatureListRow row = dataset.getItemObject(item);
    if (row == null) {
      return null;
    }
    final NumberFormats fmt = ConfigService.getGuiFormats();
    final StringBuilder sb = new StringBuilder();
    sb.append("ID: ").append(row.getID());
    sb.append("\nm/z: ").append(fmt.mz(row.getAverageMZ()));
    if (row.getAverageRT() != null) {
      sb.append("\nRT: ").append(fmt.rt(row.getAverageRT()));
    }
    if (row.getAverageMobility() != null) {
      sb.append("\nMobility: ").append(fmt.mobility(row.getAverageMobility()));
    }
    if (row.getMaxHeight() != null) {
      sb.append("\nHeight: ").append(fmt.intensity(row.getMaxHeight()));
    }
    final String annotation = row.getPreferredAnnotationName();
    if (annotation != null) {
      sb.append('\n').append(annotation);
    }
    return sb.toString();
  }
}
