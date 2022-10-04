/**
 * MIT License
 * <p>
 * Copyright (c) 2006-2022 The MZmine Development Team
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.image_allmsms;

import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.visualization.image.ImageVisualizerModule;
import io.github.mzmine.modules.visualization.image.ImagingPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import java.awt.Color;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.data.xy.XYDataset;

public class ImageAllMsMsPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(ImageAllMsMsPane.class.getName());

  protected static final SplitPane mainSplit = new SplitPane();
  protected static final BorderPane mainContent = new BorderPane();
  protected static final ScrollPane msmsScroll = new ScrollPane();
  protected static final VBox msmsContent = new VBox();
  protected static final VBox spectrumContentWrapper = new VBox();
  protected static final VBox ms1Content = new VBox();

  protected final SpectraVisualizerTab tab;

  protected final ImagingPlot imagePlot;

  protected final ObjectProperty<Feature> featureProperty = new SimpleObjectProperty<>();

  public ImageAllMsMsPane(@Nullable final Feature feature) {
    super();

    setCenter(mainSplit);
    mainSplit.getItems().addAll(mainContent, msmsScroll);
    spectrumContentWrapper.getChildren()
        .addAll(ms1Content, new Separator(Orientation.HORIZONTAL), msmsContent);
    msmsScroll.setContent(spectrumContentWrapper);

    tab = new SpectraVisualizerTab(feature.getRawDataFile());
    ms1Content.getChildren().add(tab.getSpectrumPlot());

    imagePlot = new ImagingPlot(
        MZmineCore.getConfiguration().getModuleParameters(ImageVisualizerModule.class));

    imagePlot.getChart().cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      final XYDataset dataset = newValue.getDataset();

      if (dataset instanceof ColoredXYZDataset xyz
          && xyz.getXyzValueProvider() instanceof FeatureImageProvider fip) {
        final int valueIndex = newValue.getValueIndex();
        if (valueIndex >= 0 && valueIndex < fip.getValueCount()) {
          final ImagingScan spectrum = fip.getSpectrum(valueIndex);
          tab.loadRawData(spectrum);
        }
      }
    }));

    featureProperty.addListener(((observable, oldValue, newValue) -> featureChanged(newValue)));
    featureProperty.set(feature);

    mainSplit.setDividerPosition(0, 0.7);
  }

  private void featureChanged(final Feature feature) {

    final Color markerColor = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getPositiveColorAWT();
    final Color outlineColor = MZmineCore.getConfiguration().getDefaultColorPalette()
        .getNegativeColorAWT();

    imagePlot.getChart().removeAllDatasets();
    msmsContent.getChildren().clear();

    if (feature.getFeatureData() == null || feature.getFeatureData().getSpectra().isEmpty()
        || !(feature.getFeatureData().getSpectra().get(0) instanceof ImagingScan)) {
      return;
    }

    imagePlot.getChart().applyWithNotifyChanges(false, () -> {
      if (feature != null) {
        imagePlot.setData(new ColoredXYZDataset(new FeatureImageProvider<>(feature, true)));

        imagePlot.getChart().applyWithNotifyChanges(false, () -> {
          for (Scan scan : feature.getAllMS2FragmentScans()) {
            final MaldiSpotInfo info = getMsMsSpotInfo(scan);
            if (info == null) {
              continue;
            }
//            final XYBoxAnnotation msMsMarker = new XYBoxAnnotation(info.xIndexPos(),
//                info.yIndexPos(), info.xIndexPos() + 1, info.yIndexPos() + 1, new BasicStroke(0f),
//                markerColor, outlineColor);
            XYPointerAnnotation msMsMarker = new XYPointerAnnotation(
                String.format("%d, %d", info.xIndexPos(), info.yIndexPos()), info.xIndexPos(),
                info.yIndexPos(), 45.0);
            msMsMarker.setBaseRadius(6);
            msMsMarker.setTipRadius(3);
            msMsMarker.setArrowPaint(markerColor);
            msMsMarker.setOutlinePaint(outlineColor);
            imagePlot.getChart().getChart().getXYPlot().addAnnotation(msMsMarker, false);
          }
        });
      }
    });

    for (final Scan msms : feature.getAllMS2FragmentScans()) {
      msmsContent.getChildren().add(new MobilogramMsMsPane(msms, feature));
    }
  }

  @Nullable
  public static MaldiSpotInfo getMsMsSpotInfo(Scan scan) {
    if (scan instanceof MergedMsMsSpectrum merged) {
      final List<MassSpectrum> sourceSpectra = merged.getSourceSpectra();
      final MobilityScan mobilityScan = (MobilityScan) sourceSpectra.stream()
          .filter(s -> s instanceof MobilityScan).findFirst().orElse(null);
      if (mobilityScan != null && mobilityScan.getFrame() instanceof ImagingFrame imagingFrame) {
        return imagingFrame.getMaldiSpotInfo();
      }
    }
    return null;
  }
}
