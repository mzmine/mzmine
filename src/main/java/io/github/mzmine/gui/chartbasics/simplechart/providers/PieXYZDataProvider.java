package io.github.mzmine.gui.chartbasics.simplechart.providers;

import java.awt.Color;
import javafx.beans.property.SimpleObjectProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.renderer.PaintScale;

public interface PieXYZDataProvider<T> extends PlotXYZDataProvider {

  /**
   * Returns the slice identifiers. In order to support JFreechart's legend generation, the array
   * has to be in the same order, every time this method is called. Therefore, streaming the entries
   * from an undordered collection to an array should not be executed every time this method is
   * called, but during calculation via {@link #computeValues(SimpleObjectProperty)} and stored
   * thereafter.
   *
   * @return The slice identifiers.
   */
  T[] getSliceIdentifiers();

  /**
   * @param item The item index.
   * @return The summed value for the given index.
   */
  @Override
  double getZValue(int item);

  double getZValue(int series, int item);

  /**
   * @param series The index of the given slice.
   * @return The color.
   */
  @Nonnull
  Color getSliceColor(int series);

  double getPieDiameter(int index);

  @Nullable
  @Override
  default Double getBoxHeight() {
    return null;
  }

  @Nullable
  @Override
  default Double getBoxWidth() {
    return null;
  }

   String getLabelForSeries(int series);

  @Nullable
  @Override
  default PaintScale getPaintScale() {
    return null;
  }
}
