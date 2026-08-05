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

package io.github.mzmine.modules.visualization.dash_lipidqc.summary;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.SelectableCategoryBarRenderer;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardComputationPane;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import javafx.beans.property.ObjectProperty;
import io.github.mzmine.modules.visualization.dash_lipidqc.DashboardFilterState;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.chart.axis.CategoryAnchor;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import javafx.scene.input.MouseButton;

/**
 * Dashboard panel that displays a bar chart summarising lipid annotations grouped by subclass,
 * main class, or category, with click-to-filter interaction linked to the other dashboard panes.
 */
public class LipidSummaryPane extends DashboardComputationPane {

  private final @NotNull DashboardFilterState filterState;
  private final @NotNull ComboBox<SummaryGroup> groupSelector = new ComboBox<>(
      FXCollections.observableArrayList(SummaryGroup.values()));
  private final @NotNull ComboBox<SummaryCountMode> countModeSelector = new ComboBox<>(
      FXCollections.observableArrayList(SummaryCountMode.values()));
  private final @NotNull Button clearFilterButton;

  private final @NotNull ObjectProperty<@NotNull ModularFeatureList> featureListProperty;
  private @Nullable String selectedGroup;
  private final @NotNull LinkedHashSet<String> selectedGroups = new LinkedHashSet<>();
  private final @NotNull Map<String, Set<Integer>> groupToRowIds = new TreeMap<>();
  private @Nullable Consumer<Set<Integer>> onGroupSelectedRowIds;

  public LipidSummaryPane(final @NotNull ObjectProperty<@NotNull ModularFeatureList> featureListProperty,
      final @NotNull DashboardFilterState filterState,
      final @NotNull ComboBox<?> preferredLevelCombo,
      final @NotNull FeatureTableFX featureTableFx) {
    super("Select a feature list with lipid annotations.");
    this.filterState = filterState;
    this.featureListProperty = featureListProperty;
    featureListProperty.subscribe(_ -> requestChartUpdate());

    groupSelector.getSelectionModel().select(SummaryGroup.LIPID_SUBCLASS);
    countModeSelector.getSelectionModel().select(SummaryCountMode.ROW_COUNT);
    groupSelector.valueProperty().addListener((_, _, _) -> requestChartUpdate());
    countModeSelector.valueProperty().addListener((_, _, _) -> requestChartUpdate());

    clearFilterButton = FxButtons.createButton("Clear filter", this::clearSummaryFilter);

    final HBox preferredLevelRow = new HBox(6, FxLabels.newLabel("Preferred level:"),
        preferredLevelCombo);
    preferredLevelRow.setAlignment(Pos.CENTER_LEFT);
    final HBox groupByRow = new HBox(6, FxLabels.newLabel("Group by:"), groupSelector);
    groupByRow.setAlignment(Pos.CENTER_LEFT);
    final HBox countModeRow = new HBox(6, FxLabels.newLabel("Count mode:"), countModeSelector);
    countModeRow.setAlignment(Pos.CENTER_LEFT);
    final HBox actionRow = new HBox(6, clearFilterButton);
    actionRow.setAlignment(Pos.CENTER_LEFT);

    final VBox filterControls = new VBox(6, preferredLevelRow, groupByRow, countModeRow,
        actionRow);
    filterControls.setAlignment(Pos.TOP_LEFT);
    final TitledPane filterPane = new TitledPane("Summary filters", filterControls);
    filterPane.setCollapsible(true);
    final Accordion filterAccordion = new Accordion(filterPane);
    filterAccordion.setExpandedPane(null);
    setBottom(filterAccordion);

    featureTableFx.sceneProperty().addListener((_, _, scene) -> {
      if (scene != null) {
        scene.getRoot().styleProperty().addListener((_, _, _) -> requestChartUpdate());
      }
    });
  }

  private void clearSummaryFilter() {
    selectedGroups.clear();
    selectedGroup = null;
    applySelectionToFilterState();
    notifyFilterChanged();
    requestChartUpdate();
  }

  public void requestChartUpdate() {
    final SummaryGroup grouping = groupSelector.getValue();
    final SummaryCountMode countMode = countModeSelector.getValue();
    selectedGroup = selectedGroups.stream().findFirst().orElse(null);
    scheduleUpdate(
        new SummaryComputationTask(this, featureListProperty.get(), grouping, countMode,
            selectedGroup));
  }

  void applySummaryResult(final @NotNull SummaryComputationResult result) {
    if (result.placeholderText() != null) {
      showPlaceholder(result.placeholderText());
      groupToRowIds.clear();
      selectedGroups.clear();
      selectedGroup = null;
      applySelectionToFilterState();
      notifyFilterChanged();
      return;
    }

    groupToRowIds.clear();
    groupToRowIds.putAll(result.groupToRowIds());
    selectedGroups.retainAll(groupToRowIds.keySet());
    selectedGroup = selectedGroups.stream().findFirst().orElse(null);
    applySelectionToFilterState();
    notifyFilterChanged();

    final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    result.groupToCount().forEach(
        (group, count) -> dataset.addValue(count, result.countMode().getSeriesLabel(), group));

    final JFreeChart chart = ChartFactory.createBarChart(null, result.grouping().getAxisLabel(),
        result.countMode().getRangeAxisLabel(), dataset, PlotOrientation.VERTICAL,
        false, true, false);
    chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
    final EChartViewer viewer = new EChartViewer(chart);
    final SelectableCategoryBarRenderer selectable = new SelectableCategoryBarRenderer();
    selectable.setSelectedCategoryColors(createSelectedGroupColors());
    selectable.setSelectedCategoryKey(selectedGroup);
    selectable.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
    selectable.setDefaultItemLabelsVisible(true);
    final Color textColor = summaryLabelColor();
    selectable.setDefaultItemLabelPaint(textColor);
    final double maxCount = Math.max(1d,
        result.groupToCount().values().stream().mapToInt(Integer::intValue).max().orElse(1));
    chart.getCategoryPlot().getRangeAxis().setUpperMargin(0.24d);
    if (dataset.getColumnCount() > 0) {
      final Comparable<?> rightCategory = dataset.getColumnKey(dataset.getColumnCount() - 1);
      final CategoryTextAnnotation totalAnnotation = new CategoryTextAnnotation(
          result.countMode().getTotalLabelPrefix() + result.totalCount(), rightCategory,
          maxCount * 1.12d);
      totalAnnotation.setCategoryAnchor(CategoryAnchor.END);
      totalAnnotation.setTextAnchor(TextAnchor.CENTER_RIGHT);
      totalAnnotation.setFont(new Font("SansSerif", Font.BOLD, 12));
      totalAnnotation.setPaint(textColor);
      chart.getCategoryPlot().addAnnotation(totalAnnotation);
    }
    selectable.setDefaultToolTipGenerator(
        (CategoryToolTipGenerator) (tipDataset, row, column) -> result.groupTooltip()
            .getOrDefault(Objects.toString(tipDataset.getColumnKey(column), ""),
                Objects.toString(tipDataset.getColumnKey(column), "")));
    viewer.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(final ChartMouseEventFX event) {
        if (event.getTrigger() == null || !event.getTrigger().isStillSincePress()
            || event.getTrigger().getButton() != MouseButton.PRIMARY) {
          return;
        }
        if (event.getEntity() instanceof CategoryItemEntity categoryEntity) {
          final String key = Objects.toString(categoryEntity.getColumnKey(), null);
          if (key != null && groupToRowIds.containsKey(key)) {
            final boolean wasSelected = selectedGroups.contains(key);
            if (selectedGroups.contains(key)) {
              selectedGroups.remove(key);
            } else {
              selectedGroups.add(key);
            }
            selectedGroup = selectedGroups.stream().findFirst().orElse(null);
            applySelectionToFilterState();
            notifyFilterChanged();
            if (!wasSelected) {
              notifyGroupSelectedRowIds(key);
            }
            requestChartUpdate();
          }
        }
      }

      @Override
      public void chartMouseMoved(final ChartMouseEventFX event) {
      }
    });
    ConfigService.getConfiguration().getDefaultChartTheme().apply(viewer);
    if (chart.getTitle() != null) {
      chart.getTitle().setPaint(textColor);
    }
    chart.getCategoryPlot().getDomainAxis().setLabelPaint(textColor);
    chart.getCategoryPlot().getDomainAxis().setTickLabelPaint(textColor);
    chart.getCategoryPlot().getRangeAxis().setLabelPaint(textColor);
    chart.getCategoryPlot().getRangeAxis().setTickLabelPaint(textColor);
    chart.getCategoryPlot().setRenderer(selectable);
    setCenter(viewer);
  }

  private static @NotNull Color summaryLabelColor() {
    return io.github.mzmine.main.ConfigService.getConfiguration().isDarkMode()
        ? new Color(230, 230, 230) : new Color(35, 35, 35);
  }

  private @NotNull Map<String, Color> createSelectedGroupColors() {
    final Map<String, Color> selectedGroupColors = new LinkedHashMap<>();
    final Set<Integer> usedColorCodes = new HashSet<>();
    final var palette = ConfigService.getConfiguration().getDefaultColorPalette();
    final int paletteSize = Math.max(0, palette.size());
    int colorIndex = 0;
    for (final String group : selectedGroups) {
      int overflowIndex = Math.max(0, colorIndex - paletteSize);
      Color color = colorIndex < paletteSize
          ? palette.getAWT(colorIndex)
          : generateDistinctOverflowColor(overflowIndex);
      while (usedColorCodes.contains(color.getRGB())) {
        color = generateDistinctOverflowColor(++overflowIndex);
      }
      usedColorCodes.add(color.getRGB());
      selectedGroupColors.put(group, color);
      colorIndex++;
    }
    return selectedGroupColors;
  }

  private static @NotNull Color generateDistinctOverflowColor(final int index) {
    // Low-discrepancy hue stepping keeps colors visually distinct for long selections.
    final double hue = (index * 0.6180339887498949d) % 1d;
    final float saturation = 0.72f;
    final float brightness = 0.88f;
    return Color.getHSBColor((float) hue, saturation, brightness);
  }

  private void applySelectionToFilterState() {
    final Map<String, Color> selectedGroupColors = createSelectedGroupColors();
    final Set<Integer> selectedRowIds = new HashSet<>();
    final Map<Integer, Color> selectedRowColors = new HashMap<>();
    for (final Map.Entry<String, Color> selectedGroupColor : selectedGroupColors.entrySet()) {
      final Set<Integer> rowIds = groupToRowIds.get(selectedGroupColor.getKey());
      if (rowIds == null || rowIds.isEmpty()) {
        continue;
      }
      selectedRowIds.addAll(rowIds);
      for (final Integer rowId : rowIds) {
        selectedRowColors.putIfAbsent(rowId, selectedGroupColor.getValue());
      }
    }
    filterState.setBarSelectedGroups(Set.copyOf(selectedGroups));
    filterState.setBarSelectedRowIds(Set.copyOf(selectedRowIds));
    filterState.setBarSelectedRowColors(Map.copyOf(selectedRowColors));
  }

  private void notifyFilterChanged() {
    final Runnable onChange = filterState.getOnChange();
    if (onChange != null) {
      onChange.run();
    }
  }

  public void setOnGroupSelectedRowIds(final @Nullable Consumer<Set<Integer>> onGroupSelectedRowIds) {
    this.onGroupSelectedRowIds = onGroupSelectedRowIds;
  }

  private void notifyGroupSelectedRowIds(final @NotNull String groupKey) {
    if (onGroupSelectedRowIds == null) {
      return;
    }
    final Set<Integer> rowIds = groupToRowIds.get(groupKey);
    if (rowIds == null || rowIds.isEmpty()) {
      return;
    }
    onGroupSelectedRowIds.accept(Set.copyOf(rowIds));
  }

}

