package io.github.mzmine.gui.chartbasics.simplechart.providers;

import java.awt.Color;
import javax.annotation.Nonnull;

public interface PieXYZDataProvider<T> extends PlotXYDataProvider, XYZValueProvider {

  /**
   *
   * @return The slice identifiers.
   */
  T[] getSliceIdentifiers();

  /**
   *
   * @param index The item index.
   * @return The summed value for the given index.
   */
  @Override
  double getZValue(int index);

  /**
   * @param item The item index.
   * @param slice The slice identifier.
   * @return The z value for the given slice.
   */
  double getZValue(int item, T slice);

  /**
   *
   * @param sliceIdentifier The index of the given slice.
   * @return The color.
   */
  @Nonnull
  Color getSliceColor(T sliceIdentifier);

  double getPieSize(int index);
}
