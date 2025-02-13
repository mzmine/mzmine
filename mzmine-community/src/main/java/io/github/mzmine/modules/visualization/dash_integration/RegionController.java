package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.IntegrationPlotController;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.IntegrationPlotViewBuilder;
import javafx.scene.layout.Region;

public record RegionController(IntegrationPlotController controller, Region region) {

}
