package io.github.mzmine.modules.visualization.masst;

import io.github.mzmine.gui.mainwindow.SimpleTab;

public class MasstVisualizerTab extends SimpleTab {

  public MasstVisualizerTab() {
    super("MASST");

    MasstVisualizerController controller = new MasstVisualizerController();
    setContent(controller.getViewBuilder().build());
  }
}
