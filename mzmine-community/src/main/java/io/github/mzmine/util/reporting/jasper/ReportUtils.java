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

package io.github.mzmine.util.reporting.jasper;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MsOtherCorrelationResultType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationResult;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.rowsboxplot.RowBoxPlotDataset;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponentAWT;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.spectra.matchedlipid.LipidSpectrumPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.ContinuousRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.PeakRenderer;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MirrorChartFactory;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.mzmine.reports.FeatureDetail;
import io.mzmine.reports.FeatureSummary;
import io.sirius.ms.sdk.model.LipidAnnotation;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.openscience.cdk.exception.CDKException;
import org.w3c.dom.DOMImplementation;

/**
 * Utils class to generate the charts for reports. Future will show how many other reports will be
 * able to use this class. May be a good class to refactor.
 */
public class ReportUtils {

  private final SimpleXYChart<PlotXYDataProvider> eicChart = new SimpleXYChart<>(
      ConfigService.getGuiFormats().unit("Retention time", "min"),
      ConfigService.getGuiFormats().unit("Intensity", "a.u."));

  private final SimpleXYChart<PlotXYDataProvider> uvMsOverlay = new SimpleXYChart<>(
      ConfigService.getGuiFormats().unit("Retention time", "min"), "Normalized intensity");

  private final SimpleXYChart<PlotXYDataProvider> mobilogramChart = new SimpleXYChart<>(
      MobilityType.OTHER.getAxisLabel(), ConfigService.getGuiFormats().unit("Intensity", "a.u."));

  private final SimpleXYChart<PlotXYDataProvider> ms1Chart = new SimpleXYChart<>("m/z",
      ConfigService.getGuiFormats().unit("Intensity", "a.u."));

  private final SpectraPlot ms2Chart = new SpectraPlot(false, true);

  private final ExportBoxplot boxPlot = new ExportBoxplot();
  private final Function<RawDataFile, Color> getSpectrumColor = RawDataFile::getColorAWT;
  private final MetadataColumn<?> groupingColumn;
  @NotNull
  private final EStandardChartTheme theme;
  private final AbundanceMeasure boxPlotAbundanceMeasure = AbundanceMeasure.Area;
  private final NumberFormats formats = ConfigService.getGuiFormats();
  /**
   * Bytes of the svg string or BufferedImage
   */
  private @Nullable Object structureImage = null;
  private EChartViewer mirrorChart = null; // must be regenerated for each match
  private LipidSpectrumPlot lipidChart = null;

  public ReportUtils(MetadataColumn<?> groupingColumn, @NotNull EStandardChartTheme theme) {
    this.groupingColumn = groupingColumn;
    this.theme = theme;

    initChart(mobilogramChart);
    mobilogramChart.setRangeAxisNumberFormatOverride(
        ConfigService.getGuiFormats().intensityFormat());
    initChart(eicChart);
    eicChart.setRangeAxisNumberFormatOverride(ConfigService.getGuiFormats().intensityFormat());
    initChart(uvMsOverlay);
    initChart(ms1Chart);
    ms1Chart.setRangeAxisNumberFormatOverride(ConfigService.getGuiFormats().intensityFormat());
    initChart(ms2Chart);
    ((NumberAxis) ms2Chart.getXYPlot().getRangeAxis()).setNumberFormatOverride(
        ConfigService.getGuiFormats().intensityFormat());
    initChart(boxPlot);
  }

  private static @NotNull String getCompoundSummary(@NotNull FeatureListRow row,
      Optional<FeatureAnnotation> annotation, NumberFormats format, String id, String mz, String rt,
      String ccs) {
    return annotation.map(a -> {
      final Double precursorMz = a.getPrecursorMZ();
      final Double deltaMz = precursorMz != null ? row.getAverageMZ() - precursorMz : null;
      final Double ppm =
          precursorMz != null ? MathUtils.getPpmDiff(precursorMz, row.getAverageMZ()) : null;

      final @Nullable String dbEntryId = switch (a) {
        case SpectralDBAnnotation db -> db.getEntry().getOrElse(DBEntryField.ENTRY_ID, null);
        case LipidAnnotation l -> "Rule-based annotation";
        case CompoundDBAnnotation c -> null;
        default -> null;
      };

      return """
          Exact mass: %s
          m/z error: %s Da (%s ppm)
          Name: %s
          Formula: %s
          Internal ID: %s
          CAS: %s
          Database: %s %s""".formatted(format.mz(precursorMz), format.mz(deltaMz), format.ppm(ppm),
          Objects.requireNonNullElse(a.getCompoundName(), "-"),
          Objects.requireNonNullElse(a.getFormula(), "-"),
          Objects.requireNonNullElse(a.getInternalId(), "-"),
          Objects.requireNonNullElse(a.getCAS(), "-"),
          Objects.requireNonNullElse(a.getDatabase(), "-"),
          Objects.requireNonNullElse(dbEntryId, ""));
    }).orElse("""
        ID: %s
        m/z: %s
        RT: %s
        CCS: %s
        Not annotated.""".formatted(id, mz, rt, ccs.isBlank() ? "-" : ccs));
  }

  /**
   * Used for rendering the structres
   */
  private static @NotNull SVGGraphics2D renderJComponentToSvgGraphics(JComponent comp,
      final int width, final int height) {
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
    svgGenerator.setSVGCanvasSize(new Dimension(width, height));
    comp.paint(svgGenerator);
    return svgGenerator;
  }

  private static @Nullable Range<Float> getExpandedRtRange(@NotNull FeatureListRow row) {
    Range<Float> rtRange = row.get(RTRangeType.class);
    if (rtRange == null) {
      return null;
    }
    final Float rtRangeLength = RangeUtils.rangeLength(rtRange);
    rtRange = Range.closed(rtRange.lowerEndpoint() - rtRangeLength,
        rtRange.upperEndpoint() + rtRangeLength);
    return rtRange;
  }

  public FeatureSummary getSummaryData(@NotNull FeatureListRow row) {
    final NumberFormats format = ConfigService.getGuiFormats();
    final Optional<FeatureAnnotation> annotation = CompoundAnnotationUtils.getBestFeatureAnnotation(
        row);

    final String mz = format.mz(row.getAverageMZ());
    final String rt = format.rt(row.getAverageRT());
    final String ccs = format.ccs(row.getAverageCCS());
    final String id = String.valueOf(row.getID());
    final String height = format.intensity(row.getMaxHeight());
    final String area = format.intensity(row.getMaxArea());
    final String name = annotation.map(FeatureAnnotation::getCompoundName).orElse(null);
    final String internalId = annotation.map(FeatureAnnotation::getInternalId).orElse(null);

    return new FeatureSummary(id, mz, rt, ccs, height, area, name, internalId, null);
  }

  public FeatureDetail getFeatureReportData(@NotNull final FeatureListRow row) {
    final NumberFormats format = ConfigService.getGuiFormats();

    final String mz = format.mz(row.getAverageMZ());
    final String rt = format.rt(row.getAverageRT());
    final String ccs = format.ccs(row.getAverageCCS());
    final String id = String.valueOf(row.getID());

    final String title =
        "Feature report for ID #%s with m/z %s at RT %s min".formatted(id, mz, rt) + (ccs.isBlank()
            ? "" : " and ccs %s".formatted(ccs));

    final Optional<FeatureAnnotation> annotation = CompoundAnnotationUtils.getBestFeatureAnnotation(
        row);
    final String compoundSummary = getCompoundSummary(row, annotation, format, id, mz, rt, ccs);

    final FigureCollection figures = new FigureCollection();
    int chartCounter = 1;
    try {
      if (updateEicChart(row)) {
        figures.addTwoFigureRowFigure(FigureAndCaption.asTwoColumnSvg(eicChart,
            "Figure %s.%d: EICs (line) and features (area).".formatted(id, chartCounter++)));
      }
      if (updateMobilogramChart(row)) {
        figures.addTwoFigureRowFigure(FigureAndCaption.asTwoColumnSvg(mobilogramChart,
            "Figure %s.%d: EIMs of feature %s.".formatted(id, chartCounter++, id)));
      }
      if (updateBoxPlot(row)) {
        figures.addTwoFigureRowFigure(FigureAndCaption.asTwoColumnPng(boxPlot,
            "Figure %s.%d: %s distribution grouped by %s.".formatted(id, chartCounter++,
                boxPlotAbundanceMeasure, groupingColumn.getTitle())));
      }
      if (updateMs1Chart(row)) {
        figures.addTwoFigureRowFigure(FigureAndCaption.asTwoColumnSvg(ms1Chart,
            "Figure %s.%d: MS1 spectrum of feature %s.".formatted(id, chartCounter++, id)));
      }
      if (updateMirrorChart(row)) {
        figures.addTwoFigureRowFigure(FigureAndCaption.asTwoColumnSvg(mirrorChart,
            "Figure %s.%d: Spectral match of feature %s.".formatted(id, chartCounter++, id)));
      }
      if (updateMs2Chart(row)) {
        figures.addTwoFigureRowFigure(FigureAndCaption.asTwoColumnSvg(ms2Chart,
            "Figure %s.%d: MS2 spectrum of feature %s.".formatted(id, chartCounter++, id)));
      }
      if (updateLipidSpectrum(row)) {
        figures.addTwoFigureRowFigure(FigureAndCaption.asTwoColumnSvg(lipidChart,
            "Figure %s.%d: Matched lipid signals for feature %s.".formatted(id, chartCounter++,
                id)));
      }
      if (updateUvMsChart(row)) {
        figures.addSingleFigureRow(FigureAndCaption.asSingleColumnSvg(uvMsOverlay,
            "Figure %s.%d: Correlated trace in file %s.".formatted(id, chartCounter++,
                row.getBestFeature().getRawDataFile().getName())));
      }
      updateStructure(row);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    final String additional = "This feature was detected in the sample(s): %s.".formatted(
        row.streamFeatures().map(Feature::getRawDataFile).filter(Objects::nonNull)
            .map(RawDataFile::getName).sorted(Comparator.comparing(String::toString))
            .collect(Collectors.joining(", ")));

    return new FeatureDetail(title, id, mz, rt, ccs, compoundSummary, structureImage, additional,
        figures.getTwoFigureRows(), figures.getSingleFigureRows());
  }

  private boolean updateEicChart(@NotNull FeatureListRow row) {
    eicChart.setLegendItemsVisible(false);

    final Range<Double> mzRange = row.get(MZRangeType.class);
    final Range<Float> rtRange = getExpandedRtRange(row);
    if (rtRange == null) {
      return false;
    }

    final List<DatasetAndRenderer> datasets = new ArrayList<>();

    for (ModularFeature feature : row.streamFeatures()
        .sorted(Comparator.comparingDouble(Feature::getHeight).reversed()).toList()) {
      final ColoredXYDataset ds = new ColoredXYDataset(new IonTimeSeriesToXYProvider(feature),
          RunOption.THIS_THREAD);
      final ColoredAreaShapeRenderer renderer = new ColoredAreaShapeRenderer();
      datasets.add(new DatasetAndRenderer(ds, renderer));

      if (rtRange != null) {
        final RawDataFile file = feature.getRawDataFile();
        final IonTimeSeries<Scan> chromatogram = IonTimeSeriesUtils.extractIonTimeSeries(file,
            row.getFeatureList().getSeletedScans(file), mzRange, rtRange, null);
        final ColoredXYDataset eic = new ColoredXYDataset(
            new IonTimeSeriesToXYProvider(chromatogram, file.getName() + " eic", file.getColor()),
            RunOption.THIS_THREAD);
        datasets.add(new DatasetAndRenderer(eic, new ColoredXYLineRenderer()));
      }
    }

    eicChart.applyWithNotifyChanges(false, () -> {
      eicChart.removeAllDatasets();
      for (DatasetAndRenderer dataset : datasets) {
        eicChart.addDataset(dataset.dataset(), dataset.renderer());
      }
    });
    return true;
  }

  private boolean updateMobilogramChart(@NotNull FeatureListRow row) {

    final IMSRawDataFile imsFile = (IMSRawDataFile) row.getRawDataFiles().stream()
        .filter(file -> file instanceof IMSRawDataFile).findAny().orElse(null);
    mobilogramChart.removeAllDatasets();
    if (imsFile == null) {
      return false;
    }

    final List<ColoredXYDataset> datasets = new ArrayList<>();
    for (final ModularFeature f : row.getFeatures()) {
      IonTimeSeries<? extends Scan> series = f.getFeatureData();
      if (series instanceof IonMobilogramTimeSeries) {
        datasets.add(
            new ColoredXYDataset(new SummedMobilogramXYProvider(f), RunOption.THIS_THREAD));
      }
    }

    final ModularFeature bestFeature = (ModularFeature) row.getBestFeature();
    org.jfree.data.Range defaultRange = null;
    if (bestFeature != null && bestFeature.getRawDataFile() instanceof IMSRawDataFile imsRaw) {
      com.google.common.collect.Range<Float> mobilityRange = bestFeature.getMobilityRange();
      final Float mobility = bestFeature.getMobility();
      if (mobilityRange != null && mobility != null && !Float.isNaN(mobility)) {
        final Float length = RangeUtils.rangeLength(mobilityRange);
        defaultRange = new org.jfree.data.Range(
            Math.max(mobility - 3 * length, imsRaw.getDataMobilityRange().lowerEndpoint()),
            Math.min(mobility + 3 * length, imsRaw.getDataMobilityRange().upperEndpoint()));
      }
    }
    if (defaultRange == null) {
      defaultRange = new org.jfree.data.Range(0, 1);
    }

    final org.jfree.data.Range finalDefaultRange = defaultRange;
    mobilogramChart.applyWithNotifyChanges(false, () -> {
      final MobilityType mt = imsFile.getMobilityType();
      mobilogramChart.setRangeAxisNumberFormatOverride(
          ConfigService.getConfiguration().getIntensityFormat());
      mobilogramChart.setDomainAxisNumberFormatOverride(
          ConfigService.getConfiguration().getMobilityFormat());
      mobilogramChart.setLegendItemsVisible(false);
      mobilogramChart.setDomainAxisLabel(mt.getAxisLabel());

      mobilogramChart.addDatasets(datasets);
      try {
        mobilogramChart.getXYPlot().getDomainAxis().setRange(finalDefaultRange);
        mobilogramChart.getXYPlot().getDomainAxis().setDefaultAutoRange(finalDefaultRange);
        mobilogramChart.getXYPlot().getRangeAxis().setAutoRange(true);
      } catch (NullPointerException | NoSuchElementException e) {
        // error in jfreechart draw method
      }
    });

    return true;
  }

  private boolean updateMs2Chart(@NotNull FeatureListRow row) {
    if (isMs2OrMirror(row) == MirrorOrMs2.MIRROR) {
      return false;
    }

    final Scan mostIntenseFragmentScan = row.getMostIntenseFragmentScan();

    if (mostIntenseFragmentScan == null) {
      ms2Chart.removeAllDataSets();
      ms2Chart.getChart().getPlot().setNoDataMessage("No MS2 data available");
      return true;
    }

    ms2Chart.applyWithNotifyChanges(false, () -> {
      ms2Chart.removeAllDataSets();
      ms2Chart.getChart().getPlot().setNoDataMessage(null);
      ms2Chart.addDataSet(new ScanDataSet(mostIntenseFragmentScan),
          getSpectrumColor.apply(mostIntenseFragmentScan.getDataFile()), false, true, false);
    });
    return true;
  }

  private boolean updateMs1Chart(@NotNull FeatureListRow row) {
    final Feature f = row.getBestFeature();
    final IsotopePattern iso = f.getIsotopePattern();
    final Scan scan = f.getRepresentativeScan();

    if (iso == null && scan == null) {
      return false;
    }

    final Range<Double> mzRange;
    if (iso != null) {
      final double lower = iso.getMzValue(0) - 1.5;
      final double upper = iso.getMzValue(iso.getNumberOfDataPoints() - 1) + 1.5;
      mzRange = Range.closed(lower, upper);
    } else {
      mzRange = Range.closed(f.getMZ() - 1.5, f.getMZ() + 2.5);
    }
    final org.jfree.data.Range range = RangeUtils.guavaToJFree(mzRange);

    final XYDataset isoOrPeak = iso != null ? new IsotopesDataSet(iso, "Isotope pattern")
        : new ColoredXYDataset(new AnyXYProvider(Color.RED, "Feature", 1, _ -> f.getMZ(),
            _ -> f.getHeight().doubleValue()), RunOption.THIS_THREAD);

    ms1Chart.applyWithNotifyChanges(false, () -> {
      ms1Chart.removeAllDatasets();
      final Color color = scan != null ? getSpectrumColor.apply(scan.getDataFile()) : Color.BLACK;
      if (scan != null) {
        final ScanDataSet scanDataSet = new ScanDataSet(scan);
        ms1Chart.addDataset(scanDataSet,
            scan.getSpectrumType() == MassSpectrumType.PROFILE ? new ContinuousRenderer(color,
                false) : new PeakRenderer(color, false));
      }

      ms1Chart.addDataset(isoOrPeak, new PeakRenderer(
          ColorUtils.getContrastPaletteColorAWT(color, ConfigService.getDefaultColorPalette()),
          false));
      ms1Chart.getXYPlot().getDomainAxis().setRange(range);
      ms1Chart.getXYPlot().getDomainAxis().setDefaultAutoRange(range);
      ms1Chart.getXYPlot().getRangeAxis().setRange(0, 1.1 * Math.max(f.getHeight().doubleValue(),
          iso != null ? Objects.requireNonNullElse(iso.getBasePeakIntensity(), 0d) : 0d));
    });
    return true;
  }

  private boolean updateMirrorChart(@NotNull FeatureListRow row) {
    if (isMs2OrMirror(row) != MirrorOrMs2.MIRROR) {
      return false;
    }

    final List<SpectralDBAnnotation> matches = row.getSpectralLibraryMatches();
    if (matches.isEmpty()) {
      return false;
    }

    mirrorChart = MirrorChartFactory.createMirrorPlotFromSpectralDBPeakIdentity(matches.getFirst());
    mirrorChart.getChart().removeLegend();
    theme.apply(mirrorChart);
    CombinedDomainXYPlot xyPlot = (CombinedDomainXYPlot) mirrorChart.getChart().getXYPlot();
    xyPlot.setGap(1); // best match
    ((XYPlot) xyPlot.getSubplots().getFirst()).getRangeAxis().setLabel("");
    ((XYPlot) xyPlot.getSubplots().getLast()).getRangeAxis()
        .setLabel(formats.unit("           Relative intensity", "%"));
    xyPlot.getDomainAxis().setLabelInsets(new RectangleInsets(-10, 0, 0, 0));
    return true;
  }

  private boolean updateStructure(@NotNull FeatureListRow row) {
    final Optional<FeatureAnnotation> annotation = CompoundAnnotationUtils.getBestFeatureAnnotation(
        row);
    final MolecularStructure structure = annotation.map(FeatureAnnotation::getStructure)
        .orElse(null);
    if (structure == null) {
      this.structureImage = null;
      return false;
    }

    try {
      Structure2DComponentAWT comp = new Structure2DComponentAWT(structure.structure());
      comp.setSize(227 * 3, 130 * 3);

//      final SVGGraphics2D svgGenerator = renderJComponentToSvgGraphics(comp, 110, 70);
//      StringWriter stringWriter = new StringWriter();
//      svgGenerator.stream(stringWriter, true);
//      structureImage = stringWriter.toString().getBytes();

      // svg renderer does not produce nice structures
      structureImage = createBufferedImageFromComponent(comp, comp.getWidth(), comp.getHeight());

      return true;
    } catch (CDKException /*| SVGGraphics2DIOException*/ e) {
      structureImage = null;
      return false;
    }
  }

  private boolean updateLipidSpectrum(@NotNull FeatureListRow row) {
    final MatchedLipid lipid = CompoundAnnotationUtils.getBestFeatureAnnotation(row)
        .filter(MatchedLipid.class::isInstance).map(MatchedLipid.class::cast).orElse(null);
    if (lipid == null) {
      lipidChart = null;
      return false;
    }

    lipidChart = new LipidSpectrumPlot(lipid, false, RunOption.THIS_THREAD);
    theme.apply(lipidChart);
    return true;
  }

  private boolean updateBoxPlot(@NotNull FeatureListRow row) {
    final RowBoxPlotDataset ds = new RowBoxPlotDataset(row, groupingColumn,
        boxPlotAbundanceMeasure);
    if (ds.getColumnCount() == 0 || ds.getRowCount() == 0 || row.getRawDataFiles().size() <= 1) {
      return false;
    }
    boxPlot.setDataset(ds);
    boxPlot.getChart().getCategoryPlot().getDomainAxis().setLabel(groupingColumn.getTitle());
    return true;
  }

  private boolean updateUvMsChart(@NotNull FeatureListRow row) {

    final ModularFeature bestFeature = row.streamFeatures().filter(
            f -> f.get(MsOtherCorrelationResultType.class) != null && !f.get(
                MsOtherCorrelationResultType.class).isEmpty())
        .sorted(Comparator.comparingDouble(Feature::getHeight).reversed()).findFirst().orElse(null);

    if (bestFeature == null) {
      uvMsOverlay.removeAllDatasets();
      return false;
    }
    final Range<Float> rtRange = getExpandedRtRange(row);
    if (rtRange == null) {
      return false;
    }

    final List<MsOtherCorrelationResult> results = bestFeature.get(
        MsOtherCorrelationResultType.class);
    if (results == null || results.isEmpty()) {
      uvMsOverlay.removeAllDatasets();
      return false;
    }

    final MsOtherCorrelationResult correlation = results.getFirst();
    final OtherFeature correlatedFeature = correlation.otherFeature();
    final OtherFeature rawTrace = correlatedFeature.get(RawTraceType.class);
    final OtherFeature fullPreProcessed = rawTrace.getOtherDataFile().getOtherTimeSeriesData()
        .getPreProcessedFeatureForTrace(rawTrace);
    final OtherTimeSeries trimmedPreProcessed = fullPreProcessed.getFeatureData()
        .subSeries(null, rtRange.lowerEndpoint().floatValue(),
            rtRange.upperEndpoint().floatValue());

    final IonTimeSeries<Scan> msFeature = (IonTimeSeries<Scan>) bestFeature.getFeatureData();
    final RawDataFile bestFeatureFile = bestFeature.getRawDataFile();

    final List<? extends Scan> selectedScans = row.getFeatureList()
        .getSeletedScans(bestFeatureFile);
    final IonTimeSeries<Scan> msChrom = IonTimeSeriesUtils.extractIonTimeSeries(bestFeatureFile,
        selectedScans, row.getMZRange(), rtRange, null);

    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
    final Color uvColor = ColorUtils.getContrastPaletteColorAWT(bestFeatureFile.getColorAWT(),
        palette);

    uvMsOverlay.applyWithNotifyChanges(false, () -> {
      uvMsOverlay.removeAllDatasets();
      final float otherFeatureNormFactor = 1 / correlatedFeature.get(HeightType.class);
      final float msFeatureNormFactor = 1 / bestFeature.getHeight();

      uvMsOverlay.addDataset(new ColoredXYDataset(
          new OtherFeatureDataProvider(correlatedFeature, uvColor, otherFeatureNormFactor),
          RunOption.THIS_THREAD), new ColoredAreaShapeRenderer());
      uvMsOverlay.addDataset(new ColoredXYDataset(
          new IntensityTimeSeriesToXYProvider(trimmedPreProcessed, uvColor, null,
              otherFeatureNormFactor), RunOption.THIS_THREAD), new ColoredXYLineRenderer());

      uvMsOverlay.addDataset(new ColoredXYDataset(
          new IonTimeSeriesToXYProvider(msChrom, "EIC", bestFeatureFile.getColorAWT(),
              msFeatureNormFactor), RunOption.THIS_THREAD), new ColoredXYLineRenderer());
      uvMsOverlay.addDataset(new ColoredXYDataset(
          new IonTimeSeriesToXYProvider(msFeature, "MS feature", bestFeatureFile.getColorAWT(),
              msFeatureNormFactor), RunOption.THIS_THREAD), new ColoredXYLineRenderer());
    });

    return true;
  }

  private BufferedImage createBufferedImageFromComponent(JComponent comp, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();

    // Set rendering hints for quality and anti-aliasing
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
        RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

    // Paint the component
    comp.paint(g2);
    g2.dispose();
    return image;
  }


  private MirrorOrMs2 isMs2OrMirror(@NotNull FeatureListRow row) {
    return CompoundAnnotationUtils.getBestFeatureAnnotation(row)
        .filter(SpectralDBAnnotation.class::isInstance).isPresent() ? MirrorOrMs2.MIRROR
        : MirrorOrMs2.MS2;
  }

  private void initChart(@NotNull EChartViewer chart) {
    chart.getChart().setBackgroundPaint((new Color(0, 0, 0, 0)));
    chart.getChart().getPlot().setBackgroundPaint((new Color(0, 0, 0, 0)));
    theme.apply(chart);
  }

  public enum MirrorOrMs2 {
    MIRROR, MS2;
  }
}
