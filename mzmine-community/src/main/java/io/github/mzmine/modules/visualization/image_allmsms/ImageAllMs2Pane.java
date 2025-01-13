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

package io.github.mzmine.modules.visualization.image_allmsms;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MobilityScanMobilogramProvider;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.visualization.image.ImageVisualizerModule;
import io.github.mzmine.modules.visualization.image.ImageVisualizerParameters;
import io.github.mzmine.modules.visualization.image.ImagingPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerTab;
import io.github.mzmine.util.ImagingUtils;
import io.github.mzmine.util.IonMobilityUtils.MobilogramType;
import io.github.mzmine.util.collections.StreamUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.xy.XYDataset;

public class ImageAllMs2Pane extends BorderPane {

  private static final Logger logger = Logger.getLogger(ImageAllMs2Pane.class.getName());
  private static final Comparator<Scan> msmsCollisionEnergySorter = Comparator.comparingDouble(
      msms -> {
        final MsMsInfo msMsInfo = msms.getMsMsInfo();
        if (msMsInfo == null) {
          return 0d;
        }
        return Objects.requireNonNullElse(msMsInfo.getActivationEnergy(), 0f).doubleValue();
      });

  private static final boolean normalizeMobilograms = false;
  protected final SplitPane mainSplit = new SplitPane();
  protected final BorderPane mainContent = new BorderPane();
  protected final ScrollPane msmsScroll = new ScrollPane();
  protected final VBox msmsContent = new VBox();
  protected final VBox spectrumContentWrapper = new VBox();
  protected final VBox ms1Content = new VBox();
  protected final SpectraVisualizerTab tab;
  protected final ImagingPlot imagePlot;
  protected final ObjectProperty<Feature> featureProperty = new SimpleObjectProperty<>();
  private final NumberFormats format = MZmineCore.getConfiguration().getGuiFormats();
  private final ImageNormalization imageNormalization;

  private final Font markerFont = MZmineCore.getConfiguration().getDefaultChartTheme()
      .getRegularFont();

  public ImageAllMs2Pane(@Nullable final Feature feature) {
    super();

    imageNormalization = MZmineCore.getConfiguration().getImageNormalization();

    setCenter(mainSplit);
    mainSplit.getItems().add(mainContent);
    mainSplit.getItems().add(spectrumContentWrapper);

    tab = new SpectraVisualizerTab(feature.getRawDataFile());
    final SpectraPlot spectrumPlot = tab.getSpectrumPlot();
    spectrumPlot.setLabelColorMatch(true);
    spectrumPlot.minHeightProperty().bind(mainSplit.heightProperty().divide(3));
    ms1Content.getChildren().add(spectrumPlot);

    spectrumContentWrapper.getChildren().add(ms1Content);
    spectrumContentWrapper.getChildren().add(new Separator(Orientation.HORIZONTAL));
    spectrumContentWrapper.getChildren().add(msmsScroll);
    msmsScroll.setContent(msmsContent);

    ms1Content.fillWidthProperty().set(true);
    spectrumContentWrapper.fillWidthProperty().set(true);
    msmsScroll.fitToWidthProperty().set(true);
    msmsScroll.fitToHeightProperty().set(true);
    msmsContent.fillWidthProperty().set(true);

    imagePlot = new ImagingPlot((ImageVisualizerParameters) MZmineCore.getConfiguration()
        .getModuleParameters(ImageVisualizerModule.class));
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
    if (feature.getFeatureData().getNumberOfValues() == 1) { // maldi spot measurement
      mainSplit.setDividerPosition(0, 0.01);
      tab.loadRawData(feature.getRepresentativeScan());
    }
  }

  private void featureChanged(final Feature feature) {

    imagePlot.getChart().removeAllDatasets();
    imagePlot.getChart().getChart().getXYPlot().clearAnnotations();
    msmsContent.getChildren().clear();

    if (feature == null || feature.getFeatureData() == null || feature.getFeatureData().getSpectra()
        .isEmpty() || !(feature.getFeatureData().getSpectra().get(0) instanceof ImagingScan)) {
      return;
    }

    imagePlot.getChart().applyWithNotifyChanges(false, () -> {
      imagePlot.setData(new ColoredXYZDataset(new FeatureImageProvider<>(feature,
          (List<ImagingScan>) feature.getFeatureList().getSeletedScans(feature.getRawDataFile()),
          imageNormalization)));

      final Map<Float, Color> ceColor = new HashMap<>();
      final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette()
          .clone();
      final BasicStroke stroke = new BasicStroke(3.0f);

      for (Scan scan : feature.getAllMS2FragmentScans()) {
        Map<MaldiSpotInfo, MsMsInfo> infoMap = ImagingUtils.getMsMsSpotInfosFromScan(scan);
        if (infoMap.isEmpty()) {
          continue;
        }

        for (var entry : infoMap.entrySet()) {
          // transform the coordinates back to the original file coordinates
          final double[] ms2Coord = ImagingUtils.transformCoordinates(entry.getKey(),
              (ImagingRawDataFile) feature.getRawDataFile());
          if (ms2Coord != null) {
            final Float ce = entry.getValue().getActivationEnergy();
            final Color clr = ceColor.computeIfAbsent(ce, energy -> palette.getNextColorAWT());

            XYPointerAnnotation msMsMarker = new XYPointerAnnotation(String.format("%.0f eV", ce),
                ms2Coord[0], ms2Coord[1], 315);
            msMsMarker.setArrowPaint(clr);
            msMsMarker.setArrowWidth(3d);
            msMsMarker.setArrowStroke(stroke);
            msMsMarker.setLabelOffset(10);
            msMsMarker.setFont(markerFont);

            imagePlot.getChart().getChart().getXYPlot().addAnnotation(msMsMarker, false);
          }
        }
      }
    });

    final MobilogramChart mobilogramChart = new MobilogramChart(feature, normalizeMobilograms);
    msmsContent.getChildren().add(mobilogramChart);
    final List<PlotXYDataProvider> mobilogramProviders = feature.getAllMS2FragmentScans().stream()
        .filter(msms -> msms instanceof MergedMsMsSpectrum).sorted(msmsCollisionEnergySorter).map(
            msms -> new MobilityScanMobilogramProvider(MobilogramType.TIC,
                ((List<MobilityScan>) (List<? extends MassSpectrum>) ((MergedMsMsSpectrum) msms).getSourceSpectra()),
                normalizeMobilograms)).map(provider -> (PlotXYDataProvider) provider).toList();
    mobilogramChart.addProviders(mobilogramProviders);

    // have all spectra in the same range so they are easier to compare
    final double minMz = feature.getAllMS2FragmentScans().stream().map(Scan::getDataPointMZRange)
                             .filter(Objects::nonNull).mapToDouble(Range::lowerEndpoint).min()
                             .orElse(0) * 0.8;
    final double maxMz = feature.getAllMS2FragmentScans().stream().map(Scan::getDataPointMZRange)
                             .filter(Objects::nonNull).mapToDouble(Range::upperEndpoint).max()
                             .orElse(feature.getMZ() + 20d) * 1.2;

    final ChartGroup ms2Group = new ChartGroup(false, false, true, false);
    // add spectra sorted by collision energy
    feature.getAllMS2FragmentScans().stream().sorted(msmsCollisionEnergySorter).map(msms -> {
      SpectraVisualizerTab ms2Tab = new SpectraVisualizerTab(msms.getDataFile());
      SpectraPlot spectrumPlot = ms2Tab.getSpectrumPlot();
      final ValueAxis domainAxis = spectrumPlot.getXYPlot().getDomainAxis();

      domainAxis.setDefaultAutoRange(new org.jfree.data.Range(minMz, maxMz));
      ms2Group.add(new ChartViewWrapper(spectrumPlot));

      ms2Tab.loadRawData(msms);
      var infoMap = ImagingUtils.getMsMsSpotInfosFromScan(msms);
      if (!infoMap.isEmpty()) {
        final String spotstr =
            infoMap.size() == 1 ? infoMap.keySet().stream().findFirst().get().spotName()
                : "%d spots".formatted(infoMap.size());

        // energies as CE: [30, 50]
        String energies = ScanUtils.extractCollisionEnergies(msms).stream().filter(Objects::nonNull)
            .sorted().collect(StreamUtils.joining(", ", ", CE: [", "]"));

        var title = "MS2 of m/z %s at %s%s".formatted(format.mz(msms.getPrecursorMz()), spotstr,
            energies);
        spectrumPlot.setTitle(title, "");
      }
      spectrumPlot.setMinHeight(250);
      spectrumPlot.setLabelColorMatch(true);
      domainAxis.setRange(minMz, maxMz);
      return spectrumPlot;
    }).forEachOrdered(plot -> msmsContent.getChildren().add(plot));
  }

}
