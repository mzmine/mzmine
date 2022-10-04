/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
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

  protected final SplitPane mainSplit = new SplitPane();
  protected final BorderPane mainContent = new BorderPane();
  protected final ScrollPane msmsScroll = new ScrollPane();
  protected final VBox msmsContent = new VBox();
  protected final VBox spectrumContentWrapper = new VBox();
  protected final VBox ms1Content = new VBox();

  protected final SpectraVisualizerTab tab;

  protected final ImagingPlot imagePlot;

  protected final ObjectProperty<Feature> featureProperty = new SimpleObjectProperty<>();

  public ImageAllMsMsPane(@Nullable final Feature feature) {
    super();

    setCenter(mainSplit);
    mainSplit.getItems().add(mainContent);
    mainSplit.getItems().add(msmsScroll);

    msmsScroll.setContent(spectrumContentWrapper);

    tab = new SpectraVisualizerTab(feature.getRawDataFile());
    final SpectraPlot spectrumPlot = tab.getSpectrumPlot();
    spectrumPlot.setMinHeight(300);
    ms1Content.getChildren().add(spectrumPlot);

    spectrumContentWrapper.getChildren().add(ms1Content);
    spectrumContentWrapper.getChildren().add(new Separator(Orientation.HORIZONTAL));
    spectrumContentWrapper.getChildren().add(msmsContent);

    ms1Content.fillWidthProperty().set(true);
    spectrumContentWrapper.fillWidthProperty().set(true);
    msmsScroll.fitToWidthProperty().set(true);
    msmsScroll.fitToHeightProperty().set(true);
    msmsContent.fillWidthProperty().set(true);

    imagePlot = new ImagingPlot(
        MZmineCore.getConfiguration().getModuleParameters(ImageVisualizerModule.class));
    mainContent.setCenter(imagePlot);

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
    imagePlot.getChart().getChart().getXYPlot().clearAnnotations();
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
            XYPointerAnnotation msMsMarker = new XYPointerAnnotation(
                String.format("%d, %d", info.xIndexPos(), info.yIndexPos()), info.xIndexPos(),
                info.yIndexPos(), -45.0);
            msMsMarker.setBaseRadius(50);
            msMsMarker.setTipRadius(10);
            msMsMarker.setArrowPaint(markerColor);
            msMsMarker.setOutlinePaint(outlineColor);
//            XYBoxAnnotation msMsMarker = new XYBoxAnnotation(info.xIndexPos(), info.yIndexPos(),
//                info.xIndexPos() + 50, info.yIndexPos() + 50, new BasicStroke(1.0f), Color.green,
//                Color.yellow);
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
