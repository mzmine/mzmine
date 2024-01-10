/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_imagecorrelate;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.gui.framework.fx.features.AbstractFeatureListRowsPane;
import io.github.mzmine.gui.framework.fx.features.ParentFeatureListPaneGroup;
import io.github.mzmine.util.DataPointSorter;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CorrelatedImageVisualizerPane extends AbstractFeatureListRowsPane {

  private final SplitPane mainPane = new SplitPane();
  private final SplitPane imageSpectrumPane = new SplitPane();
  private ColocatedImagePane colocatedImagePane = new ColocatedImagePane();
  private ScrollPane colocatedScroll = new ScrollPane(colocatedImagePane);
  @Nullable
  private FeatureListRow row = null;

  public CorrelatedImageVisualizerPane(ParentFeatureListPaneGroup parentGroup) {
    super(parentGroup);

    imageSpectrumPane.setOrientation(Orientation.HORIZONTAL);

    colocatedScroll.setFitToWidth(true);
    colocatedScroll.setFitToHeight(true);

    imageSpectrumPane.getItems().add(spectraVisualizerTab.getMainPane());

    mainPane.setOrientation(Orientation.VERTICAL);
    mainPane.getItems().add(imageSpectrumPane);
    mainPane.getItems().add(colocatedScroll);
    setCenter(mainPane);
  }

  @Override
  public void onRowsChanged(@NotNull List<? extends FeatureListRow> rows) {
    super.onRowsChanged(rows);
  }

  private void updateContent(FeatureListRow selectedRow, List<FeatureListRow> rows) {
    final Optional<ModularFeature> optBestFeature = selectedRow.streamFeatures()
        .filter(f -> f.getRawDataFile() instanceof ImagingRawDataFile)
        .max(Comparator.comparingDouble(Feature::getHeight));

    if(optBestFeature.isEmpty()) {
      // todo add no data text
      return;
    }
    final ModularFeature bestFeature = optBestFeature.get();

    final FeatureList flist = selectedRow.getFeatureList();
    var opt = flist.getRowMap(Type.MS1_FEATURE_CORR);
    if (opt.isEmpty()) {
      return;
    }

    final R2RMap<RowsRelationship> rowsRelationshipR2RMap = opt.get();

    final List<RowsRelationship> sortedRelationships = rowsRelationshipR2RMap.streamAllCorrelatedRows(selectedRow,
            flist.getRows()).sorted(Comparator.comparingDouble(RowsRelationship::getScore).reversed())
        .toList();
    if (sortedRelationships.isEmpty()) {
//      MZmineCore.getDesktop().displayMessage("No co-located images found for selected image");
      return;
    }

    final List<DataPoint> dataPoints = sortedRelationships.stream()
        .map(r -> (DataPoint) new SimpleDataPoint(r.getAverageMZ(), r.getAverageHeight()))
        .sorted(DataPointSorter.DEFAULT_MZ_ASCENDING).toList();
    final List<FeatureListRow> correlatedRows = correlatedRowToScoreMap.entrySet().stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).map(Entry::getKey).toList();


  }

  @Override
  public void onSelectedRowsChanged(@NotNull List<? extends FeatureListRow> selectedRows) {
    super.onSelectedRowsChanged(selectedRows);

    if(selectedRows.isEmpty()) {
      return;
    }

    updateContent(selectedRows.get(0), getParentGroup().getRows());
  }

  @Override
  public boolean hasContent() {
    return false;
  }
}
