package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.features.FeatureListRow;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * One legend item under the dashboard charts: a member {@link FeatureListRow} together with the
 * color used to draw it in the EIC / mobilogram / MS1 plots.
 */
public record CompoundDashboardLegendEntry(@NotNull FeatureListRow row, @NotNull Color color) {

}
