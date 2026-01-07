/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.image;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.gui.framework.fx.features.AbstractFeatureListRowsPane;
import io.github.mzmine.gui.framework.fx.features.ParentFeatureListPaneGroup;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColocatedImageVisualizerPane extends AbstractFeatureListRowsPane {

  private final SplitPane mainPane = new SplitPane();
  private final SplitPane imageSpectrumPane = new SplitPane();
  /**
   * contains the image + correlated spectrum of the selected row. Wraps them to show a "no content"
   * message in case nothing is selected or row has no image.
   */
  private final StackPane selectedWrapper = new StackPane();
  private final Label noContentLabel = new Label("Selected row has no image feature");
  private ColocatedImagePane colocatedImagePane = new ColocatedImagePane(null, null, null);
  private ScrollPane colocatedScroll = new ScrollPane(colocatedImagePane);
  @Nullable
  private FeatureListRow row = null;

  public ColocatedImageVisualizerPane(ParentFeatureListPaneGroup parentGroup) {
    super(parentGroup);

    imageSpectrumPane.setOrientation(Orientation.HORIZONTAL);

    colocatedScroll.setFitToWidth(true);
    colocatedScroll.setFitToHeight(true);

    selectedWrapper.getChildren().add(imageSpectrumPane);
    mainPane.setOrientation(Orientation.VERTICAL);
    mainPane.getItems().add(selectedWrapper);
    mainPane.getItems().add(colocatedScroll);
    setCenter(mainPane);
    onSelectedRowsChanged(parentGroup.getSelectedRows());
  }

  private static PseudoSpectrum extractCorrelatedFeaturesToPseudoSpectrum(
      FeatureListRow selectedRow, List<RowsRelationship> sortedRelationships,
      ModularFeature bestFeature) {
    final RawDataFile file = bestFeature.getRawDataFile();

    final List<DataPoint> dataPoints = sortedRelationships.stream().map(r -> {
      final FeatureListRow relatedRow = r.getOtherRow(selectedRow);
      final Feature relatedFeature = relatedRow.getFeature(file);
      if (relatedFeature == null) {
        return null;
      }
      return (DataPoint) new SimpleDataPoint(relatedFeature.getMZ(), relatedFeature.getHeight());
    }).filter(Objects::nonNull).sorted(DataPointSorter.DEFAULT_MZ_ASCENDING).toList();

    final double[][] mzIntensities = DataPointUtils.getDataPointsAsDoubleArray(dataPoints);
    return new SimplePseudoSpectrum(file, 1, bestFeature.getRT(), null, mzIntensities[0],
        mzIntensities[1], bestFeature.getRepresentativeScan().getPolarity(), "Correlated features",
        PseudoSpectrumType.MALDI_IMAGING);
  }

  @Override
  public void onRowsChanged(@NotNull List<? extends FeatureListRow> rows) {
    super.onRowsChanged(rows);
  }

  private void updateContent(FeatureListRow selectedRow, List<FeatureListRow> rows) {
    final Optional<ModularFeature> optBestFeature = selectedRow.streamFeatures()
        .filter(f -> f.getRawDataFile() instanceof ImagingRawDataFile)
        .max(Comparator.comparingDouble(Feature::getHeight));
    imageSpectrumPane.getItems().clear();
    selectedWrapper.getChildren().remove(noContentLabel);
    final FeatureList flist = selectedRow.getFeatureList();
    var opt = flist.getRowMap(Type.MS1_FEATURE_CORR);

    if (optBestFeature.isEmpty() || opt.isEmpty()) {
      selectedWrapper.getChildren().add(noContentLabel);
      colocatedImagePane.updateContent(null, null, null);
      return;
    }

    final ModularFeature bestFeature = optBestFeature.get();

    final R2RMap<RowsRelationship> rowsRelationshipR2RMap = opt.get();
    final List<RowsRelationship> sortedRelationships = rowsRelationshipR2RMap.streamAllCorrelatedRows(
            selectedRow, flist.getRows())
        .sorted(Comparator.comparingDouble(RowsRelationship::getScore).reversed()).toList();

    if (sortedRelationships.isEmpty()) {
      colocatedImagePane.updateContent(null, null, null);
    } else {
      colocatedImagePane.updateContent(sortedRelationships, selectedRow,
          bestFeature.getRawDataFile());
    }
    // main pane must be updated afterwards, so that the main image and the correlated images are
    // in the same chart group.
    updateMainImageAndPseudoSpectrum(selectedRow, bestFeature, sortedRelationships);
  }

  private void updateMainImageAndPseudoSpectrum(FeatureListRow selectedRow,
      ModularFeature bestFeature, List<RowsRelationship> sortedRelationships) {
    final BorderPane selectedImage = colocatedImagePane.createImagePane(selectedRow, bestFeature,
        1.0d);
    final PseudoSpectrum correlatedSpectrum = extractCorrelatedFeaturesToPseudoSpectrum(selectedRow,
        sortedRelationships, bestFeature);
    final Pane newSpectrumPlot = getNewSpectrumPlot(correlatedSpectrum, bestFeature);
    imageSpectrumPane.getItems().addAll(selectedImage, newSpectrumPlot);
  }

  @Override
  public void onSelectedRowsChanged(@NotNull List<? extends FeatureListRow> selectedRows) {
    super.onSelectedRowsChanged(selectedRows);

    if (selectedRows.isEmpty()) {
      return;
    }

    updateContent(selectedRows.get(0), getParentGroup().getRows());
  }

  @Override
  public boolean hasContent() {
    return false;
  }

  private Pane getNewSpectrumPlot(PseudoSpectrum scan, Feature selectedFeature) {
    SpectraVisualizerTab spectraVisualizerTab = new SpectraVisualizerTab(scan.getDataFile(), scan,
        false, true);
    spectraVisualizerTab.setText(scan.getScanDefinition());
    spectraVisualizerTab.loadRawData(scan);

    final PseudoSpectrum pseudoMSSelectedFeature = new SimplePseudoSpectrum(
        selectedFeature.getRawDataFile(), 1, selectedFeature.getRT(), null,
        new double[]{selectedFeature.getMZ()}, new double[]{selectedFeature.getHeight()},
        selectedFeature.getRepresentativeScan().getPolarity(), "Selected Feature",
        PseudoSpectrumType.MALDI_IMAGING);
    spectraVisualizerTab.addDataSet(new ScanDataSet(pseudoMSSelectedFeature),
        MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT(), true);
    return spectraVisualizerTab.getMainPane();
  }
}
