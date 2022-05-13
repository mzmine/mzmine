package io.github.mzmine.modules.visualization.massvoltammogram;

import javax.swing.JButton;
import javax.swing.JToggleButton;
import org.math.plot.components.PlotToolBar;


public class ExtendedPlotToolBar extends PlotToolBar {

final JToggleButton moveButton = buttonCenter;
final JToggleButton rotateButton = buttonRotate;
final JButton resetPlotButton = buttonReset;

  public ExtendedPlotToolBar(ExtendedPlot3DPanel plot) {
    super(plot);
  }
}
