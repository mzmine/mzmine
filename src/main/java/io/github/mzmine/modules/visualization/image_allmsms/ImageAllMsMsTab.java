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

package io.github.mzmine.modules.visualization.image_allmsms;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.Collection;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageAllMsMsTab extends MZmineTab {

  private final FeatureTableFX featureTable;

  public ImageAllMsMsTab(@Nullable FeatureTableFX featureTable, @Nullable Feature feature,
      String title, boolean showBinding, boolean defaultBindingState) {
    super(title, showBinding, defaultBindingState);

    this.featureTable = featureTable;
    final RawDataFile rawDataFile = feature.getRawDataFile();
    if (!(rawDataFile instanceof ImagingRawDataFile)) {
      MZmineCore.getDesktop().displayMessage("Not an imaging feature",
          "Selected feature does not belong to an imaging data file. Cannot display image visualizer.");
      return;
    }

    ImageAllMs2Pane pane = new ImageAllMs2Pane(feature);
    setContent(pane);

    var listener = new ChangeListener<TreeItem<ModularFeatureListRow>>() {
      @Override
      public void changed(ObservableValue<? extends TreeItem<ModularFeatureListRow>> observable,
          TreeItem<ModularFeatureListRow> oldValue, TreeItem<ModularFeatureListRow> newValue) {
        if (isUpdateOnSelection()) {
          pane.featureProperty.set(newValue.getValue().getFeature(rawDataFile));
        }
      }
    };

    if (featureTable != null) {
      featureTable.getSelectionModel().selectedItemProperty().addListener(listener);
      setOnClosed(
          e -> featureTable.getSelectionModel().selectedItemProperty().removeListener(listener));
    }
  }

  public static void addNewImageAllMsMsTab(@Nullable FeatureTableFX featureTable,
      @Nullable Feature f, boolean showBinding, boolean defaultBindingState) {
    MZmineCore.getDesktop().addTab(
        new ImageAllMsMsTab(featureTable, f, "Image All MS/MS", showBinding, defaultBindingState));
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return List.of(featureTable.getFeatureList());
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
