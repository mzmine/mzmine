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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.javafx.MarkerDefinition;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxCheckBox;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.factories.TableColumns.ColumnAlignment;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceComponent;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.Layer;

class ChromatogramComparisonViewBuilder extends FxViewBuilder<ChromatogramComparisonModel> {

  /**
   * Opacity of the background markers: the other list has a signal in these scans, or a hole
   * between two signals that the other list does not fill either. Lines of single scans are thinner
   * and more opaque.
   */
  private static final float MISSING_SIGNAL_ALPHA = 0.2f;
  private static final float HOLE_ALPHA = 0.1f;
  private static final float SINGLE_SCAN_ALPHA_FACTOR = 1.75f;
  private static final BasicStroke SINGLE_SCAN_STROKE = new BasicStroke(3f);
  private static final BasicStroke REGION_OUTLINE_STROKE = new BasicStroke(1f);
  /**
   * Margin around the signals of a selected group, relative to their retention time span
   */
  private static final double RT_MARGIN_FACTOR = 0.15;
  private static final double MIN_RT_MARGIN = 0.05;
  private static final double INTENSITY_MARGIN_FACTOR = 0.05;

  private final NumberFormats formats = ConfigService.getGuiFormats();
  private final Color colorA;
  private final Color colorB;

  ChromatogramComparisonViewBuilder(@NotNull ChromatogramComparisonModel model) {
    super(model);
    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
    colorA = palette.get(0);
    colorB = palette.get(1);
  }

  @Override
  public Region build() {
    final SimpleXYChart<PlotXYDataProvider> chartA = createChart();
    final SimpleXYChart<PlotXYDataProvider> chartB = createChart();
    // synchronized zoom of both axes and a shared crosshair that follows the mouse
    final ChartGroup chartGroup = new ChartGroup(false, false, true, true);
    chartGroup.setUseMouseMovementCrosshair(true, false);
    chartGroup.add(new ChartViewWrapper(chartA));
    chartGroup.add(new ChartViewWrapper(chartB));

    // chart A shows both lists in overlay mode
    FxLayout.bindVisibleManaged(chartB, model.overlayProperty().not());
    final HBox charts = new HBox(FxLayout.DEFAULT_SPACE, chartA, chartB);
    HBox.setHgrow(chartA, Priority.ALWAYS);
    HBox.setHgrow(chartB, Priority.ALWAYS);

    PropertyUtils.onChange(() -> updateTitles(chartA, chartB), model.overlayProperty(),
        model.featureListAProperty(), model.featureListBProperty());
    updateTitles(chartA, chartB);
    // a new group zooms to its signals, switching the overlay keeps the zoom
    model.selectedGroupProperty().subscribe((_, _) -> updateCharts(chartA, chartB, true));
    model.overlayProperty().subscribe((_, _) -> updateCharts(chartA, chartB, false));

    final SplitPane split = new SplitPane(charts, createTable());
    split.setOrientation(Orientation.VERTICAL);
    split.setDividerPositions(0.55);

    final BorderPane main = new BorderPane(split);
    main.setTop(createToolbar());
    return main;
  }

  @NotNull
  private Node createToolbar() {
    final Label labelA = sideLabel(ComparisonSide.A);
    final Label labelB = sideLabel(ComparisonSide.B);
    PropertyUtils.onChange(() -> {
      labelA.setText(sideName(ComparisonSide.A));
      labelB.setText(sideName(ComparisonSide.B));
    }, model.featureListAProperty(), model.featureListBProperty());

    final MZToleranceComponent tolerance = new MZToleranceComponent();
    tolerance.setToolTipText("""
        Groups each chromatogram with the chromatogram of the other list with the closest m/z \
        within this tolerance. Default: the larger tolerance of both chromatogram builders.""");
    tolerance.valueProperty().bindBidirectional(model.mzToleranceProperty());

    final String signalTooltip = """
        Data points of at least this intensity are signals. Scans without data point where the \
        other list has a signal are missing. Gaps of up to %d scans between two signals are holes \
        if the mass list has a data point. Default: the larger minimum height of both \
        chromatogram builders.""".formatted(ChromatogramComparison.MAX_HOLE_SCANS);
    final var signal = FxTextFields.newNumberField(8, formats.intensityFormat(),
        model.minSignalIntensityProperty(), signalTooltip);

    return FxLayout.newFlowPane(labelA, labelB,
        FxButtons.createButton("Swap A and B", "Swap the compared feature lists",
            this::swapFeatureLists), //
        FxLayout.newHBox(Insets.EMPTY, new Label("m/z tolerance"), tolerance),
        FxLayout.newHBox(Insets.EMPTY, new Label("Signal intensity ≥"), signal), //
        FxCheckBox.newCheckBox("Overlay in one chart", model.overlayProperty()), //
        FxComboBox.createLabeledComboBox("Show",
            FXCollections.observableArrayList(GroupFilter.values()), model.groupFilterProperty()),
        FxLabels.newLabel(model.statusProperty()));
  }

  private void swapFeatureLists() {
    final FeatureList a = model.getFeatureListA();
    model.setFeatureListA(model.getFeatureListB());
    model.setFeatureListB(a);
  }

  @NotNull
  private Label sideLabel(@NotNull ComparisonSide side) {
    final Label label = new Label(sideName(side), new Rectangle(12, 12, sideColor(side)));
    label.setGraphicTextGap(FxLayout.DEFAULT_SPACE);
    return label;
  }

  @NotNull
  private String sideName(@NotNull ComparisonSide side) {
    final FeatureList flist =
        side == ComparisonSide.A ? model.getFeatureListA() : model.getFeatureListB();
    return side + ": " + (flist == null ? "" : flist.getName());
  }

  @NotNull
  private Color sideColor(@NotNull ComparisonSide side) {
    return side == ComparisonSide.A ? colorA : colorB;
  }

  @NotNull
  private SimpleXYChart<PlotXYDataProvider> createChart() {
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>(
        formats.unitFormat().format("Retention time", "min"), "Intensity");
    chart.setDomainAxisNumberFormatOverride(formats.rtFormat());
    chart.setRangeAxisNumberFormatOverride(formats.intensityFormat());
    chart.setStickyZeroRangeAxis(true);
    // shapes mark each data point, so single missing data points stand out
    final ColoredXYLineRenderer renderer = new ColoredXYLineRenderer();
    renderer.setDefaultShapesVisible(true);
    renderer.setDefaultShape(new Ellipse2D.Double(-2, -2, 4, 4));
    chart.setDefaultRenderer(renderer);
    return chart;
  }

  private void updateTitles(@NotNull SimpleXYChart<?> chartA, @NotNull SimpleXYChart<?> chartB) {
    final String nameA = sideName(ComparisonSide.A);
    final String nameB = sideName(ComparisonSide.B);
    chartA.getChart().setTitle(model.isOverlay() ? nameA + "   " + nameB : nameA);
    chartB.getChart().setTitle(nameB);
  }

  private void updateCharts(@NotNull SimpleXYChart<PlotXYDataProvider> chartA,
      @NotNull SimpleXYChart<PlotXYDataProvider> chartB, boolean zoomToSignals) {
    final ChromatogramGroup group = model.getSelectedGroup();
    final List<ColoredXYDataset> datasetsA = group == null ? List.of() : createDatasets(group.a());
    final List<ColoredXYDataset> datasetsB = group == null ? List.of() : createDatasets(group.b());
    final List<MarkerDefinition> markersA = group == null ? List.of() : createMarkers(group.a());
    final List<MarkerDefinition> markersB = group == null ? List.of() : createMarkers(group.b());

    if (model.isOverlay()) {
      setChartContent(chartA, Stream.concat(datasetsA.stream(), datasetsB.stream()).toList(),
          Stream.concat(markersA.stream(), markersB.stream()).toList());
      setChartContent(chartB, List.of(), List.of());
    } else {
      setChartContent(chartA, datasetsA, markersA);
      setChartContent(chartB, datasetsB, markersB);
    }

    if (zoomToSignals && group != null) {
      zoomToSignals(chartA, group);
    }
  }

  private static void setChartContent(@NotNull SimpleXYChart<PlotXYDataProvider> chart,
      @NotNull List<ColoredXYDataset> datasets, @NotNull List<MarkerDefinition> markers) {
    chart.applyWithNotifyChanges(false, () -> {
      if (datasets.isEmpty()) {
        chart.removeAllDatasets();
      } else {
        chart.setDatasets(datasets);
      }
      if (markers.isEmpty()) {
        chart.getXYPlot().clearDomainMarkers();
      } else {
        chart.getXYPlot().setAllDomainMarkers(markers.toArray(MarkerDefinition[]::new));
      }
    });
  }

  /**
   * Zooms to the signals of the group, the chart group applies the ranges to both charts. Not
   * {@link ChartGroup#resetZoom()}: an empty chart, e.g., of a group with only A, resets the
   * retention time range of the other chart and fails to find an intensity range.
   */
  private static void zoomToSignals(@NotNull SimpleXYChart<?> chart,
      @NotNull ChromatogramGroup group) {
    final Range<Float> signalRtRange = group.signalRtRange();
    if (signalRtRange == null) {
      return;
    }
    final double lower = signalRtRange.lowerEndpoint();
    final double upper = signalRtRange.upperEndpoint();
    final double margin = Math.max((upper - lower) * RT_MARGIN_FACTOR, MIN_RT_MARGIN);
    chart.getXYPlot().getDomainAxis().setRange(lower - margin, upper + margin);
    // the highest data point lies within the signals
    final double maxHeight = Math.max(Objects.requireNonNullElse(group.a().height(), 0d),
        Objects.requireNonNullElse(group.b().height(), 0d));
    if (maxHeight > 0) {
      chart.getXYPlot().getRangeAxis().setRange(0, maxHeight * (1 + INTENSITY_MARGIN_FACTOR));
    }
  }

  /**
   * @return one dataset per chromatogram, the most intense in the color of the side
   */
  @NotNull
  private List<ColoredXYDataset> createDatasets(@NotNull GroupSide side) {
    final Color sideColor = sideColor(side.side());
    final List<ComparedChromatogram> chromatograms = side.chromatograms();
    final List<ColoredXYDataset> datasets = new ArrayList<>(chromatograms.size());
    for (int i = 0; i < chromatograms.size(); i++) {
      final ComparedChromatogram chromatogram = chromatograms.get(i);
      final Feature feature = chromatogram.feature();
      if (feature == null) {
        continue;
      }
      final String seriesKey = "%s #%d %s".formatted(side.side(), chromatogram.rowId(),
          formats.mz(chromatogram.mz()));
      datasets.add(new ColoredXYDataset(
          new IonTimeSeriesToXYProvider(feature.getFeatureData(), seriesKey, shade(sideColor, i)),
          RunOption.THIS_THREAD));
    }
    return datasets;
  }

  /**
   * Other chromatograms of the same side shift the hue of the side color, alternating in both
   * directions.
   */
  @NotNull
  private static Color shade(@NotNull Color color, int index) {
    if (index == 0) {
      return color;
    }
    final int direction = index % 2 == 1 ? 1 : -1;
    return color.deriveColor(direction * 30d * ((index + 1) / 2), 1, 0.85, 1);
  }

  /**
   * Single missing scans are value markers, missing regions interval markers, both in the
   * background in a soft color of the side that misses the data points.
   */
  @NotNull
  private List<MarkerDefinition> createMarkers(@NotNull GroupSide side) {
    final java.awt.Color paint = FxColorUtil.fxColorToAWT(sideColor(side.side()));
    final List<MarkerDefinition> markers = new ArrayList<>();
    for (final MissingRegion region : side.missingRegions()) {
      final float alpha = region.otherSideHasSignal() ? MISSING_SIGNAL_ALPHA : HOLE_ALPHA;
      final Marker marker;
      if (region.isSingleScan()) {
        marker = new ValueMarker(region.rtFirst(), paint, SINGLE_SCAN_STROKE);
        marker.setAlpha(alpha * SINGLE_SCAN_ALPHA_FACTOR);
      } else {
        // the outline keeps narrow regions visible when zoomed out
        marker = new IntervalMarker(region.rtStart(), region.rtEnd(), paint, REGION_OUTLINE_STROKE,
            paint, REGION_OUTLINE_STROKE, alpha);
      }
      markers.add(new MarkerDefinition(0, marker, Layer.BACKGROUND));
    }
    return markers;
  }

  @NotNull
  private TableView<ChromatogramGroup> createTable() {
    final TableView<ChromatogramGroup> table = new TableView<>();
    table.setPlaceholder(new Label("No chromatograms to compare"));
    final FilteredList<ChromatogramGroup> filtered = new FilteredList<>(model.getGroups());
    filtered.predicateProperty().bind(
        Bindings.createObjectBinding(() -> model.getGroupFilter().predicate(),
            model.groupFilterProperty()));
    final SortedList<ChromatogramGroup> sorted = new SortedList<>(filtered);
    sorted.comparatorProperty().bind(table.comparatorProperty());
    table.setItems(sorted);

    final TableColumn<ChromatogramGroup, ChromatogramGroup> issuesColumn = createIssuesColumn();
    table.getColumns().addAll(List.of( //
        column("#", ChromatogramGroup::id), //
        numberColumn("m/z", formats.mzFormat(), ChromatogramGroup::mz), //
        numberColumn("RT", formats.rtFormat(), ChromatogramGroup::apexRt), //
        numberColumn("Δ m/z (ppm)", formats.ppmFormat(), ChromatogramGroup::deltaMzPpm), //
        issuesColumn, //
        column("Diff. points", group -> positiveOrNull(group.differentDataPoints())), //
        createSideColumn(ComparisonSide.A, table), //
        createSideColumn(ComparisonSide.B, table)));
    // decision: the most severe and intense issues first, the order of a review
    table.getSortOrder().add(issuesColumn);

    table.getSelectionModel().selectedItemProperty().subscribe(model::setSelectedGroup);
    // always show a group: new results or a filter can clear the selection. Platform.runLater
    // selects after the table processed the change, FxThread.runLater would run right away
    sorted.addListener((ListChangeListener<ChromatogramGroup>) _ -> Platform.runLater(() -> {
      if (table.getSelectionModel().getSelectedItem() == null && !sorted.isEmpty()) {
        table.getSelectionModel().selectFirst();
      }
    }));
    return table;
  }

  @NotNull
  private TableColumn<ChromatogramGroup, ChromatogramGroup> createIssuesColumn() {
    // sorts the most severe issues first, then the most intense groups
    final TableColumn<ChromatogramGroup, ChromatogramGroup> column = TableColumns.createColumn(
        "Issues", 160, 0, ColumnAlignment.LEFT, ChromatogramGroup.SEVERITY_ORDER,
        group -> new ReadOnlyObjectWrapper<>(group));
    column.setCellFactory(_ -> new TableCell<>() {
      @Override
      protected void updateItem(ChromatogramGroup group, boolean empty) {
        super.updateItem(group, empty);
        if (empty || group == null || !group.hasIssues()) {
          setText(null);
          setTooltip(null);
          return;
        }
        final Set<ChromatogramIssue> issues = group.issues();
        setText(issues.stream().map(ChromatogramIssue::getLabel).collect(Collectors.joining(", ")));
        setTooltip(new Tooltip(issues.stream().map(ChromatogramIssue::getDescription)
            .collect(Collectors.joining("\n"))));
      }
    });
    return column;
  }

  /**
   * Shows the chromatograms that took the data points a side misses. A double click selects the
   * group of the first chromatogram if the table shows it.
   */
  @NotNull
  private TableColumn<ChromatogramGroup, GroupSide> createTakenByColumn(
      @NotNull ComparisonSide side, @NotNull TableView<ChromatogramGroup> table) {
    final TableColumn<ChromatogramGroup, GroupSide> column = TableColumns.createColumn("Taken by",
        140, 0, ColumnAlignment.LEFT, null, group -> new ReadOnlyObjectWrapper<>(group.side(side)));
    column.setSortable(false);
    column.setCellFactory(_ -> {
      final TableCell<ChromatogramGroup, GroupSide> cell = new TableCell<>() {
        @Override
        protected void updateItem(GroupSide groupSide, boolean empty) {
          super.updateItem(groupSide, empty);
          if (empty || groupSide == null || groupSide.takenBy().isEmpty()) {
            setText(null);
            setTooltip(null);
            return;
          }
          setText(
              groupSide.takenBy().stream().map(this::shortText).collect(Collectors.joining(", ")));
          setTooltip(new Tooltip(takenByTooltip(groupSide)));
        }

        private String shortText(@NotNull DataPointOwner owner) {
          final ComparedChromatogram chromatogram = owner.chromatogram();
          return chromatogram == null ? owner.count() + " unused"
              : "%d in #%d (group %d)".formatted(owner.count(), chromatogram.rowId(),
                  owner.groupId());
        }
      };
      cell.setOnMouseClicked(event -> {
        final GroupSide groupSide = cell.getItem();
        if (event.getClickCount() == 2 && groupSide != null && !groupSide.takenBy().isEmpty()) {
          selectGroup(table, groupSide.takenBy().getFirst().groupId());
        }
      });
      return cell;
    });
    return column;
  }

  @NotNull
  private String takenByTooltip(@NotNull GroupSide groupSide) {
    final StringBuilder text = new StringBuilder("""
        The chromatograms of list %1$s with the data points that %1$s misses in this group: \
        signals of the other list in scans without own data point, data points of the mass lists \
        in holes, all signals of the other list if %1$s has no chromatogram here. Unused: in no \
        chromatogram of %1$s. Double click to show the first group.
        """.formatted(groupSide.side()));
    for (final DataPointOwner owner : groupSide.takenBy()) {
      final ComparedChromatogram chromatogram = owner.chromatogram();
      text.append("\n").append(chromatogram == null ? "unused: %d".formatted(owner.count())
          : "#%d %s in group %d: %d".formatted(chromatogram.rowId(), formats.mz(chromatogram.mz()),
              owner.groupId(), owner.count()));
    }
    return text.toString();
  }

  /**
   * Selects the group if the filter of the table shows it, the ids are the positions of the groups
   * in the model plus one.
   */
  private void selectGroup(@NotNull TableView<ChromatogramGroup> table, int groupId) {
    if (groupId < 1 || groupId > model.getGroups().size()) {
      return;
    }
    final ChromatogramGroup group = model.getGroups().get(groupId - 1);
    if (table.getItems().contains(group)) {
      table.getSelectionModel().select(group);
      table.scrollTo(group);
    }
  }

  @NotNull
  private TableColumn<ChromatogramGroup, ?> createSideColumn(@NotNull ComparisonSide side,
      @NotNull TableView<ChromatogramGroup> table) {
    final TableColumn<ChromatogramGroup, Object> column = new TableColumn<>();
    column.textProperty().bind(Bindings.createStringBinding(() -> sideName(side),
        side == ComparisonSide.A ? model.featureListAProperty() : model.featureListBProperty()));

    // empty cells for a side without chromatogram and for issue counts of 0
    final Function<ChromatogramGroup, GroupSide> sideOf = group -> group.side(side);
    column.getColumns().addAll(List.of( //
        TableColumns.createColumn("Chromatograms", 140, 0, ColumnAlignment.LEFT, null,
            group -> new ReadOnlyObjectWrapper<>(chromatogramsText(sideOf.apply(group)))), //
        numberColumn("Height", formats.intensityFormat(), group -> sideOf.apply(group).height()),
        column("Data points", group -> {
          final GroupSide groupSide = sideOf.apply(group);
          return groupSide.isEmpty() ? null : groupSide.dataPoints();
        }), //
        column("Missing", group -> positiveOrNull(sideOf.apply(group).missingDataPoints())),
        numberColumn("Max missed", formats.intensityFormat(),
            group -> positiveOrNull(sideOf.apply(group).maxMissedIntensity())), //
        column("Holes", group -> positiveOrNull(sideOf.apply(group).holeDataPoints())),
        createTakenByColumn(side, table)));
    return column;
  }

  private static @Nullable Integer positiveOrNull(int value) {
    return value > 0 ? value : null;
  }

  private static @Nullable Double positiveOrNull(double value) {
    return value > 0 ? value : null;
  }

  /**
   * @return row ids and m/z of all chromatograms of the side, most intense first
   */
  @NotNull
  private String chromatogramsText(@NotNull GroupSide side) {
    return side.chromatograms().stream().map(c -> "#%d %s".formatted(c.rowId(), formats.mz(c.mz())))
        .collect(Collectors.joining(", "));
  }

  @NotNull
  private static <V> TableColumn<ChromatogramGroup, V> column(@NotNull String name,
      @NotNull Function<ChromatogramGroup, V> value) {
    return TableColumns.createColumn(name, 0, 0, ColumnAlignment.RIGHT, null,
        group -> new ReadOnlyObjectWrapper<>(value.apply(group)));
  }

  @NotNull
  private static <V extends Number> TableColumn<ChromatogramGroup, V> numberColumn(
      @NotNull String name, @NotNull NumberFormat format,
      @NotNull Function<ChromatogramGroup, V> value) {
    return TableColumns.createColumn(name, 0, format, ColumnAlignment.RIGHT,
        group -> new ReadOnlyObjectWrapper<>(value.apply(group)));
  }
}
