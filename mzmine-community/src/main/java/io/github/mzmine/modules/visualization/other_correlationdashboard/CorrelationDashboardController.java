/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.other_correlationdashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.otherdectectors.MsOtherCorrelationResultType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureDataProvider;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;

public class CorrelationDashboardController extends FxController<CorrelationDashboardModel> {

  private final CorrelationDashboardViewBuilder correlationDashboardViewBuilder = new CorrelationDashboardViewBuilder(
      model);

  public CorrelationDashboardController() {
    super(new CorrelationDashboardModel());

    // make sure the correlation type is present
    model.featureListProperty().addListener((_, _, fl) -> {
      if (fl == null) {
        return;
      }
      if (!fl.getFeatureTypes().contains(DataTypes.get(MsOtherCorrelationResultType.class))) {
        fl.addFeatureType(DataTypes.get(MsOtherCorrelationResultType.class));
      }
    });

    // update the raw data file according to the currently selected trace
    model.selectedOtherPreprocessedTraceProperty().addListener((_, _, preprocessedTrace) -> {
      if (preprocessedTrace == null) {
        return;
      }
      final RawDataFile file = preprocessedTrace.getRawDataFile();
      model.setSelectedRawDataFile(file);
    });

    // listener to determine the selected feature in the uv plot
    model.getUvPlotController().cursorPositionProperty().addListener((_, _, pos) -> {
      if (pos == null) {
        return;
      }

      // the dataset may be the actual feature or the full trace dataset.
      if (pos.getDataset() != null && pos.getDataset() instanceof ColoredXYDataset cds
          && cds.getValueProvider() instanceof OtherFeatureDataProvider ofdp
          && ofdp.getFeature() != model.getSelectedOtherPreprocessedTrace()) {
        // if it is not the full trace, simply update the selected feature
        model.selectedOtherFeatureProperty().set(ofdp.getFeature());
        return;
      }
      // otherwise, do our own search and set the selected feature
      final Optional<OtherFeatureDataProvider> selected = model.getUvPlotController()
          .getDatasetRenderers().keySet().stream().<ColoredXYDataset>mapMulti((ds, c) -> {
            if (ds instanceof ColoredXYDataset cds
                && cds.getValueProvider() instanceof OtherFeatureDataProvider ofdp
                && ofdp.getFeature() != model.getSelectedOtherPreprocessedTrace()) {
              c.accept(cds);
            }
          }).filter(cds -> cds.getDomainValueRange().contains(pos.getDomainValue()))
          .map(cds -> (OtherFeatureDataProvider) cds.getValueProvider()).findFirst();
      if (selected.isPresent()) {
        model.setSelectedOtherFeature(selected.get().getFeature());
      } else {
        model.setSelectedOtherFeature(null);
      }
    });
  }

  @Override
  protected @NotNull FxViewBuilder<CorrelationDashboardModel> getViewBuilder() {
    return correlationDashboardViewBuilder;
  }

  public ObjectProperty<ModularFeatureList> featureListProperty() {
    return model.featureListProperty();
  }
}
