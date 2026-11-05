package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class;

import io.github.mzmine.modules.visualization.spectra.matchedlipid.StyledLipidSpectrumBarRenderer;
import java.awt.Paint;
import java.util.function.DoublePredicate;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Lipid spectrum bars that draws selected peaks in a highlight color, e.g., the fragments of the
 * rule selected in the custom lipid class editor.
 */
class HighlightingLipidBarRenderer extends StyledLipidSpectrumBarRenderer {

  private final @NotNull DoublePredicate isHighlighted;
  private final @NotNull Paint highlightPaint;

  /**
   * @param isHighlighted  tests the m/z of a bar
   * @param highlightPaint color of highlighted bars
   */
  HighlightingLipidBarRenderer(boolean isTransparent, @NotNull Paint borderPaint,
      @NotNull DoublePredicate isHighlighted, @NotNull Paint highlightPaint) {
    super(isTransparent, borderPaint);
    this.isHighlighted = isHighlighted;
    this.highlightPaint = highlightPaint;
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    final XYPlot plot = getPlot();
    if (plot != null) {
      final XYDataset dataset = plot.getDataset(plot.getIndexOf(this));
      if (dataset != null && isHighlighted.test(dataset.getXValue(row, column))) {
        return highlightPaint;
      }
    }
    return super.getItemPaint(row, column);
  }
}
