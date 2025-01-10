package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import java.util.Collection;

public class IntegrationDashboardTab extends SimpleTab {

  final IntegrationDashboardController controller;

  public IntegrationDashboardTab() {
    super("Integration dashboard");
    controller = new IntegrationDashboardController();
    setContent(controller.buildView());
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    controller.setFeatureList(featureLists.isEmpty() ? null
        : (ModularFeatureList) featureLists.stream().toList().getFirst());
  }
}
