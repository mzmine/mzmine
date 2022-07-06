package io.github.mzmine.modules.visualization.massvoltammogram;

import javax.swing.JButton;
import javax.swing.JToggleButton;
import org.math.plot.components.PlotToolBar;

/**
 * Class used to extend the PlotToolBar so that the old functions can be accessed from the new
 * toolbar created in the MassvoltammogramTab.
 */
public class ExtendedPlotToolBar extends PlotToolBar {

  final JToggleButton moveButton = buttonCenter;
  final JToggleButton rotateButton = buttonRotate;
  final JButton resetPlotButton = buttonReset;

  public ExtendedPlotToolBar(ExtendedPlot3DPanel plot) {
    super(plot);
  }
}
