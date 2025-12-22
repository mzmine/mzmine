/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.FeatureShapeIonMobilityRetentionTimeHeatMapChart;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.ims_featurevisualizer.IMSFeatureVisualizerTab;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureShapeIonMobilityRetentionTimeHeatMapType extends LinkedGraphicalType {

  private static final Logger logger = Logger.getLogger(
      FeatureShapeIonMobilityRetentionTimeHeatMapType.class.getName());

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "feature_shape_ion_mobility_rt_heatmap";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Ion mobility trace";
  }

  /*@Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    final TreeTableColumn<ModularFeatureListRow, Object> column = super.createColumn(raw,
        parentType);
    column.setCellFactory(new CountingFeatureChartCellFactory(i -> new ImsHeatmapChartCell(i, raw)));
    column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue().getValue()));
    return column;
  }*/

  @Override
  public @Nullable Node createCellContent(ModularFeatureListRow row, Boolean cellData,
      RawDataFile raw, AtomicDouble progress) {
    if (row == null || (cellData != null && !cellData) || !(raw instanceof IMSRawDataFile)
        || row.getFeature(raw) == null || raw instanceof ImagingRawDataFile) {
      return null;
    }

    ModularFeature feature = row.getFeature(raw);
    if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN) {
      return null;
    }

    if (!(feature.getFeatureData() instanceof IonMobilogramTimeSeries)) {
      Label label = new Label("Processed with\nLC-MS workflow");
      label.setTextAlignment(TextAlignment.CENTER);
      return label;
    }

    var chart = new FeatureShapeIonMobilityRetentionTimeHeatMapChart(feature, progress);
    return chart;
  }

  @Override
  public double getColumnWidth() {
    return LARGE_GRAPHICAL_CELL_WIDTH;
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(final @Nullable FeatureTableFX table, @NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, DataType<?> superType, @Nullable final Object value) {
    return () -> FxThread.runLater(() -> MZmineCore.getDesktop()
        .addTab(new IMSFeatureVisualizerTab(row.getFeature(file.get(0)))));
  }
}
