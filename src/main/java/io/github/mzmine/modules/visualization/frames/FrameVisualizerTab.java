/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.frames;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class FrameVisualizerTab extends MZmineTab {

  private final FrameVisualizerPane pane;

  private static final MZTolerance defaultTolerance = new MZTolerance(0.005, 20);

  private final ObservableList<Range<Double>> mobilogramRanges = FXCollections.observableArrayList();

  private final ObjectProperty<IMSRawDataFile> file = new SimpleObjectProperty<>();

  public FrameVisualizerTab(String title, boolean showBinding) {
    super(title, showBinding, false);

    pane = new FrameVisualizerPane(defaultTolerance, 1E3, 300, 1, 50, null, new ScanSelection(1),
        mobilogramRanges);
    final BorderPane main = new BorderPane();

    final ComboBox<Frame> frameChooser = new ComboBox<>(FXCollections.observableArrayList());
    frameChooser.valueProperty().bindBidirectional(pane.selectedFrameProperty());

    file.addListener((obs, old, n) -> pane.setRawDataFile(n));
    file.addListener((obs, old, n) -> frameChooser.setItems(
        FXCollections.observableArrayList(file.get().getFrames())));

    main.setCenter(pane);
    main.setBottom(frameChooser);
    setContent(main);
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return pane.getRawDataFile() != null ? List.of(pane.getRawDataFile()) : List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if (!rawDataFiles.isEmpty()) {
      final RawDataFile file = rawDataFiles.stream().findFirst().get();
      if (file instanceof IMSRawDataFile imsRawDataFile) {
        this.file.set(imsRawDataFile);
      }
    }
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
