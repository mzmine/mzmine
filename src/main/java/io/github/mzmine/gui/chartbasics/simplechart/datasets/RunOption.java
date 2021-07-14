package io.github.mzmine.gui.chartbasics.simplechart.datasets;

import javafx.beans.property.SimpleObjectProperty;

public enum RunOption {
  /**
   * Directly executes this dataset's calculations on the current thread. This may not be the FX
   * application thread.
   *
   * @see ColoredXYDataset#run()
   * @see io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider#computeValues
   */
  THIS_THREAD,
  /**
   * Directly executes this dataset calculations on a new thread.
   *
   * @see ColoredXYDataset#run()
   * @see io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider#computeValues(SimpleObjectProperty)
   */
  NEW_THREAD,
  /**
   * Does not run the dataset calculations. To be started individually by the programmer.
   *
   * @see ColoredXYDataset#run()
   * @see io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider#computeValues
   */
  DO_NOT_RUN;
}
