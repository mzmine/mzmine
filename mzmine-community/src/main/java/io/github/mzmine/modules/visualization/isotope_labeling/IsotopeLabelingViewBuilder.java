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

package io.github.mzmine.modules.visualization.isotope_labeling;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;

/**
 * View builder for isotope labeling visualization with grouped bar charts
 */
public class IsotopeLabelingViewBuilder extends FxViewBuilder<IsotopeLabelingModel> {

  private static final Logger logger = Logger.getLogger(IsotopeLabelingViewBuilder.class.getName());
  private static final int SPACING = 5;
  private static final DecimalFormat MZ_FORMAT = new DecimalFormat("0.0000");
  private static final DecimalFormat RT_FORMAT = new DecimalFormat("0.00");

  private SwingNode chartNode;
  private ChartPanel chartPanel;
  private javafx.scene.control.TableView<ClusterTableEntry> clusterTableView;
  private Label statusLabel;
  private ComboBox<String> visTypeCombo;
  private IsotopologueDetailPanel detailPanel;
  private Map<Integer, List<FeatureListRow>> allClusters = new HashMap<>();
  private boolean updatingSelection = false;

  public IsotopeLabelingViewBuilder(@NotNull IsotopeLabelingModel model) {
    super(model);
  }

  @Override
  public Region build() {
    // Main layout is a border pane
    BorderPane mainPane = new BorderPane();

    // Create a split pane with cluster list on the left and chart on the right
    SplitPane splitPane = new SplitPane();

    // Create the cluster selection panel
    VBox selectionPanel = createClusterSelectionPanel();

    // Create the chart panel
    BorderPane chartPanel = new BorderPane();

    // Create a SwingNode to hold the JFreeChart
    chartNode = new SwingNode();
    chartPanel.setCenter(chartNode);

    // Initialize with an empty chart panel
    createEmptyChartPanel();

    // Add controls below the chart
    chartPanel.setBottom(createChartControlsPane());

    // Add both panels to the split pane
    splitPane.getItems().addAll(selectionPanel, chartPanel);
    splitPane.setDividerPositions(0.25);

    // Set the split pane as the main content
    mainPane.setCenter(splitPane);

    // Set up listeners for chart updates
    setupChartListeners();

    return mainPane;
  }

  /**
   * Create an empty chart panel when no chart is available
   */
  private void createEmptyChartPanel() {
    SwingUtilities.invokeLater(() -> {
      JFreeChart emptyChart = new JFreeChart("No data", JFreeChart.DEFAULT_TITLE_FONT,
          new org.jfree.chart.plot.XYPlot(), false);
      emptyChart.addSubtitle(new TextTitle("Select clusters to visualize"));

      chartPanel = new ChartPanel(emptyChart);
      chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
      chartPanel.setMinimumDrawWidth(200);
      chartPanel.setMinimumDrawHeight(200);
      chartPanel.setMaximumDrawWidth(4000);
      chartPanel.setMaximumDrawHeight(4000);

      chartNode.setContent(chartPanel);
    });
  }

  /**
   * Create the panel with the sortable table of available isotope clusters
   */
  @SuppressWarnings("unchecked")
  private VBox createClusterSelectionPanel() {
    VBox panel = new VBox(SPACING);
    panel.setPadding(new Insets(SPACING));

    Label titleLabel = new Label("Isotope Clusters");
    titleLabel.setStyle("-fx-font-weight: bold;");

    clusterTableView = new javafx.scene.control.TableView<>();
    clusterTableView.setPlaceholder(new Label("No isotope clusters available"));
    clusterTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    clusterTableView.setColumnResizePolicy(
        javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    // Cluster ID column
    javafx.scene.control.TableColumn<ClusterTableEntry, Integer> idCol =
        new javafx.scene.control.TableColumn<>("Cluster");
    idCol.setCellValueFactory(
        c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getClusterId()).asObject());
    idCol.setMaxWidth(60);

    // m/z column
    javafx.scene.control.TableColumn<ClusterTableEntry, String> mzCol =
        new javafx.scene.control.TableColumn<>("m/z");
    mzCol.setCellValueFactory(
        c -> new javafx.beans.property.SimpleStringProperty(MZ_FORMAT.format(c.getValue().getMz())));
    mzCol.setComparator((a, b) -> Double.compare(Double.parseDouble(a), Double.parseDouble(b)));

    // RT column
    javafx.scene.control.TableColumn<ClusterTableEntry, String> rtCol =
        new javafx.scene.control.TableColumn<>("RT (min)");
    rtCol.setCellValueFactory(
        c -> new javafx.beans.property.SimpleStringProperty(
            RT_FORMAT.format(c.getValue().getRtMin())));
    rtCol.setComparator((a, b) -> Double.compare(Double.parseDouble(a), Double.parseDouble(b)));
    rtCol.setMaxWidth(70);

    // # isotopologues column
    javafx.scene.control.TableColumn<ClusterTableEntry, Integer> nCol =
        new javafx.scene.control.TableColumn<>("#M+n");
    nCol.setCellValueFactory(
        c -> new javafx.beans.property.SimpleIntegerProperty(
            c.getValue().getIsotopologueCount()).asObject());
    nCol.setMaxWidth(55);

    // Fractional contribution column
    javafx.scene.control.TableColumn<ClusterTableEntry, String> fcCol =
        new javafx.scene.control.TableColumn<>("FC (%)");
    fcCol.setCellValueFactory(c -> {
      double fc = c.getValue().getFractionalContribution();
      String text = Double.isNaN(fc) ? "—" : String.format("%.1f", fc * 100);
      return new javafx.beans.property.SimpleStringProperty(text);
    });
    fcCol.setComparator((a, b) -> {
      if (a.equals("—")) return -1;
      if (b.equals("—")) return 1;
      return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
    });
    fcCol.setMaxWidth(60);

    clusterTableView.getColumns().addAll(idCol, mzCol, rtCol, nCol, fcCol);

    clusterTableView.getSelectionModel().getSelectedItems()
        .addListener((javafx.collections.ListChangeListener<ClusterTableEntry>) change -> {
          if (updatingSelection) return;
          List<Integer> selectedIds = clusterTableView.getSelectionModel().getSelectedItems()
              .stream().map(ClusterTableEntry::getClusterId).toList();
          model.setSelectedClusters(selectedIds);
          List<FeatureListRow> rows = new ArrayList<>();
          for (Integer id : selectedIds) {
            List<FeatureListRow> cr = allClusters.get(id);
            if (cr != null) rows.addAll(cr);
          }
          model.setSelectedRows(rows);
          updateStatusLabel();
        });

    VBox.setVgrow(clusterTableView, Priority.ALWAYS);
    clusterTableView.setTooltip(
        new Tooltip("Select clusters to visualize. Hold Ctrl/Cmd to select multiple."));

    HBox buttonBox = new HBox(SPACING);
    buttonBox.setAlignment(Pos.CENTER);
    Button selectAllButton = new Button("Select All");
    selectAllButton.setOnAction(e -> {
      clusterTableView.getSelectionModel().selectAll();
      updateStatusLabel();
    });
    Button clearSelectionButton = new Button("Clear Selection");
    clearSelectionButton.setOnAction(e -> {
      clusterTableView.getSelectionModel().clearSelection();
      model.setSelectedClusters(List.of());
      updateStatusLabel();
    });
    buttonBox.getChildren().addAll(selectAllButton, clearSelectionButton);

    statusLabel = new Label("No clusters selected");
    updateStatusLabel();

    panel.getChildren().addAll(titleLabel, clusterTableView, buttonBox, statusLabel);
    return panel;
  }

  /**
   * Create the control panel for chart settings
   */
  private Region createChartControlsPane() {
    Accordion accordion = new Accordion();

    TitledPane visualizationPane = new TitledPane("Visualization Settings",
        createVisualizationControlsPane());

    detailPanel = new IsotopologueDetailPanel();
    TitledPane detailPane = new TitledPane("Isotopologue Details", detailPanel);

    TitledPane appearancePane = new TitledPane("Export Options", createExportControlsPane());

    accordion.getPanes().addAll(visualizationPane, detailPane, appearancePane);
    accordion.setExpandedPane(visualizationPane);

    return accordion;
  }

  /**
   * Create controls for visualization settings
   */
  private Region createVisualizationControlsPane() {
    FlowPane controls = new FlowPane(Orientation.HORIZONTAL);
    controls.setHgap(SPACING);
    controls.setVgap(SPACING);
    controls.setPadding(new Insets(SPACING));
    controls.setAlignment(Pos.TOP_LEFT);

    // Visualization type selector
    HBox visTypeBox = new HBox(SPACING);
    Label visTypeLabel = new Label("Visualization:");
    visTypeCombo = new ComboBox<>(FXCollections.observableList(
        Arrays.asList("Relative intensities", "Absolute intensities")));

    // Set initial value and listen for changes
    visTypeCombo.setValue(model.getVisualizationType());
    visTypeCombo.setOnAction(e -> {
      String selectedType = visTypeCombo.getValue();
      if (selectedType != null && !selectedType.equals(model.getVisualizationType())) {
        model.setVisualizationType(selectedType);
      }
    });

    visTypeBox.getChildren().addAll(visTypeLabel, visTypeCombo);

    // Normalization option: "None" = fraction of total, "M+0"/"M+1"/... = ratio to that isotopologue
    HBox normalizeBox = new HBox(SPACING);
    Label normalizeLabel = new Label("Normalize to:");
    List<String> normOptions = new ArrayList<>();
    normOptions.add("None (fraction of total)");
    for (int k = 0; k <= model.getMaxIsotopologues(); k++) {
      normOptions.add("M+" + k);
    }
    ComboBox<String> normalizeCombo = new ComboBox<>(FXCollections.observableList(normOptions));
    int initRank = model.getNormalizationRank();
    normalizeCombo.setValue(initRank < 0 ? "None (fraction of total)" : "M+" + initRank);
    normalizeCombo.setOnAction(e -> {
      String sel = normalizeCombo.getValue();
      int rank = (sel == null || sel.startsWith("None")) ? -1
          : Integer.parseInt(sel.substring(2));
      if (rank != model.getNormalizationRank()) {
        model.setNormalizationRank(rank);
      }
    });
    model.normalizationRankProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        String expected = newVal.intValue() < 0 ? "None (fraction of total)"
            : "M+" + newVal.intValue();
        if (!expected.equals(normalizeCombo.getValue())) {
          normalizeCombo.setValue(expected);
        }
      }
    });
    // Disable normalization option if not using relative intensities
    normalizeBox.disableProperty()
        .bind(visTypeCombo.valueProperty().isNotEqualTo("Relative intensities"));
    normalizeBox.getChildren().addAll(normalizeLabel, normalizeCombo);

    // Max isotopologues spinner
    HBox maxIsotopologuesBox = new HBox(SPACING);
    Label maxIsotopologuesLabel = new Label("Max isotopologues:");
    Spinner<Integer> maxIsotopologuesSpinner = new Spinner<>(2, 30, model.getMaxIsotopologues(), 1);

    // Set up two-way binding
    ChangeListener<Number> spinnerListener = (obs, oldVal, newVal) -> {
      if (newVal != null && newVal.intValue() != model.getMaxIsotopologues()) {
        model.setMaxIsotopologues(newVal.intValue());
      }
    };

    maxIsotopologuesSpinner.valueProperty().addListener(spinnerListener);

    // Set the spinner value when model changes
    model.maxIsotopologuesProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && newVal.intValue() != maxIsotopologuesSpinner.getValue()) {
        maxIsotopologuesSpinner.getValueFactory().setValue(newVal.intValue());
      }
    });

    maxIsotopologuesBox.getChildren().addAll(maxIsotopologuesLabel, maxIsotopologuesSpinner);

    // Significance markers toggle
    CheckBox sigMarkersCheck = new CheckBox("Show significance markers (*, **, ***)");
    sigMarkersCheck.setSelected(model.getShowSignificanceMarkers());
    sigMarkersCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && newVal != model.getShowSignificanceMarkers()) {
        model.setShowSignificanceMarkers(newVal);
      }
    });
    model.showSignificanceMarkersProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && newVal != sigMarkersCheck.isSelected()) {
        sigMarkersCheck.setSelected(newVal);
      }
    });

    // Add all controls to the flow pane
    // Metadata grouping dropdown
    HBox groupingBox = new HBox(SPACING);
    Label groupingLabel = new Label("Group by:");
    List<String> columnNames = new ArrayList<>();
    columnNames.add("(default — from labeling task)");
    ProjectService.getMetadata().getColumns().stream()
        .map(MetadataColumn::getTitle).forEach(columnNames::add);
    ComboBox<String> groupingCombo = new ComboBox<>(
        FXCollections.observableList(columnNames));
    String currentGrouping = model.getGroupingColumnName();
    groupingCombo.setValue(
        (currentGrouping == null || currentGrouping.isBlank())
            ? columnNames.get(0) : currentGrouping);
    groupingCombo.setOnAction(e -> {
      String sel = groupingCombo.getValue();
      String newCol = (sel == null || sel.startsWith("(default")) ? null : sel;
      if (!java.util.Objects.equals(newCol, model.getGroupingColumnName())) {
        model.setGroupingColumnName(newCol);
      }
    });
    groupingBox.getChildren().addAll(groupingLabel, groupingCombo);

    // Chart layout: stacked vs grouped
    HBox chartTypeBox = new HBox(SPACING);
    Label chartTypeLabel = new Label("Chart type:");
    ComboBox<String> chartTypeCombo = new ComboBox<>(
        FXCollections.observableList(Arrays.asList("Stacked", "Grouped")));
    chartTypeCombo.setValue(model.isStackedBars() ? "Stacked" : "Grouped");
    chartTypeCombo.setOnAction(e -> {
      boolean stacked = "Stacked".equals(chartTypeCombo.getValue());
      if (stacked != model.isStackedBars()) {
        model.setStackedBars(stacked);
      }
    });
    model.stackedBarsProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        String expected = newVal ? "Stacked" : "Grouped";
        if (!expected.equals(chartTypeCombo.getValue())) {
          chartTypeCombo.setValue(expected);
        }
      }
    });
    chartTypeBox.getChildren().addAll(chartTypeLabel, chartTypeCombo);

    controls.getChildren().addAll(visTypeBox, normalizeBox, maxIsotopologuesBox, sigMarkersCheck,
        groupingBox, chartTypeBox);

    return controls;
  }

  /**
   * Create controls for exporting the chart
   */
  private Region createExportControlsPane() {
    FlowPane controls = new FlowPane(Orientation.HORIZONTAL);
    controls.setHgap(SPACING);
    controls.setVgap(SPACING);
    controls.setPadding(new Insets(SPACING));
    controls.setAlignment(Pos.TOP_LEFT);

    // Export options
    Button exportButton = new Button("Export Chart as PNG");
    exportButton.setOnAction(e -> {
      // Save the chart as PNG
      saveChartAsImage();
    });

    // Copy to clipboard button
    Button copyButton = new Button("Copy to Clipboard");
    copyButton.setOnAction(e -> {
      // Copy the chart to the clipboard using JavaFX's system clipboard
      copyChartToClipboard();
    });

    // Print button
    Button printButton = new Button("Print Chart");
    printButton.setOnAction(e -> {
      // Print the chart
      printChart();
    });

    // Add all controls to the flow pane
    HBox buttonBox = new HBox(SPACING);
    buttonBox.setAlignment(Pos.CENTER);
    buttonBox.getChildren().addAll(exportButton, copyButton, printButton);
    controls.getChildren().addAll(buttonBox);

    return controls;
  }

  /**
   * Save the chart as an image file
   */
  private void saveChartAsImage() {
    try {
      JFreeChart chart = model.getChart();
      if (chart == null) {
        MZmineCore.getDesktop().displayMessage("No chart available to export");
        return;
      }

      // Create a file chooser dialog
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Save Chart As PNG");
      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Images", "*.png"));
      File file = fileChooser.showSaveDialog(chartNode.getScene().getWindow());

      // Save the chart if a file was selected
      if (file != null) {
        ChartUtils.saveChartAsPNG(file, chart, 800, 600);
        MZmineCore.getDesktop().displayMessage("Chart saved to " + file.getName());
      }
    } catch (Exception e) {
      logger.warning("Error saving chart: " + e.getMessage());
      MZmineCore.getDesktop().displayMessage("Error saving chart: " + e.getMessage());
    }
  }

  /**
   * Copy the chart to the clipboard
   */
  private void copyChartToClipboard() {
    try {
      if (chartPanel == null || chartPanel.getChart() == null) {
        MZmineCore.getDesktop().displayMessage("No chart available to copy");
        return;
      }

      // Take a screenshot of the chart
      java.awt.image.BufferedImage image = chartPanel.getChart().createBufferedImage(800, 600);

      // Convert to JavaFX image
      javafx.scene.image.WritableImage fxImage = new javafx.scene.image.WritableImage(800, 600);
      for (int x = 0; x < 800; x++) {
        for (int y = 0; y < 600; y++) {
          fxImage.getPixelWriter().setArgb(x, y, image.getRGB(x, y));
        }
      }

      // Use JavaFX's system clipboard
      Clipboard clipboard = Clipboard.getSystemClipboard();
      ClipboardContent content = new ClipboardContent();
      content.putImage(fxImage);
      clipboard.setContent(content);

      // Provide feedback to user
      MZmineCore.getDesktop().displayMessage("Chart copied to clipboard");
    } catch (Exception e) {
      logger.warning("Error copying chart to clipboard: " + e.getMessage());
      MZmineCore.getDesktop().displayMessage("Error copying chart: " + e.getMessage());
    }
  }

  /**
   * Print the chart
   */
  private void printChart() {
    try {
      if (chartPanel == null) {
        MZmineCore.getDesktop().displayMessage("No chart available to print");
        return;
      }

      SwingUtilities.invokeLater(() -> {
        try {
          chartPanel.createChartPrintJob();
        } catch (Exception e) {
          logger.warning("Error printing chart: " + e.getMessage());
          Platform.runLater(() -> {
            MZmineCore.getDesktop().displayMessage("Error printing chart: " + e.getMessage());
          });
        }
      });
    } catch (Exception e) {
      logger.warning("Error starting print job: " + e.getMessage());
      MZmineCore.getDesktop().displayMessage("Error printing chart: " + e.getMessage());
    }
  }

  /**
   * Set up listeners for chart updates
   */
  private void setupChartListeners() {
    // Listen for chart changes in the model
    model.chartProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        updateChartPanel(newVal);
      }
    });

    // Listen for changes in selected clusters (status label + reverse-sync + detail panel refresh)
    model.selectedClustersProperty().addListener((obs, oldVal, newVal) -> {
      Platform.runLater(() -> {
        updateStatusLabel();
        if (!updatingSelection) {
          updatingSelection = true;
          try {
            syncClusterListToModel(newVal);
          } finally {
            updatingSelection = false;
          }
        }
        refreshDetailPanel(newVal);
      });
    });

    // Refresh FC column in cluster table when fractions are updated after a task run
    model.labeledFractionsProperty().addListener((obs, oldVal, newVal) -> {
      Platform.runLater(() -> updateClusterList(allClusters));
    });

    // Listen for visualization type changes
    model.visualizationTypeProperty().addListener((obs, oldVal, newVal) -> {
      Platform.runLater(() -> {
        // Update dropdown if needed (to keep in sync)
        if (newVal != null && !newVal.equals(visTypeCombo.getValue())) {
          visTypeCombo.setValue(newVal);
        }
      });
    });
  }

  /**
   * Update the chart panel with a new chart
   */
  private void updateChartPanel(JFreeChart chart) {
    SwingUtilities.invokeLater(() -> {
      // Create a new chart panel if needed
      if (chartPanel == null) {
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(600, 400));
        chartPanel.setMinimumDrawWidth(200);
        chartPanel.setMinimumDrawHeight(200);
        chartPanel.setMaximumDrawWidth(4000);
        chartPanel.setMaximumDrawHeight(4000);
        chartNode.setContent(chartPanel);
      } else {
        // Just update the existing chart panel
        chartPanel.setChart(chart);
      }

      // Refresh the display
      chartPanel.repaint();
    });
  }

  /**
   * Update the cluster table with available clusters. Fractions are read from the model if already
   * computed (i.e. after a visualization task has run); they show as "—" otherwise.
   */
  public void updateClusterList(Map<Integer, List<FeatureListRow>> clusters) {
    this.allClusters = clusters != null ? clusters : new HashMap<>();

    if (clusters == null || clusters.isEmpty()) {
      clusterTableView.setItems(FXCollections.observableArrayList());
      return;
    }

    try {
      Map<Integer, Double> fractions = model.getLabeledFractions();
      List<ClusterTableEntry> entries = clusters.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(e -> {
            int id = e.getKey();
            List<FeatureListRow> rows = e.getValue();
            FeatureListRow base = IsotopeLabelingModel.findBasePeak(rows);
            double mz = base != null ? base.getAverageMZ() : 0;
            double rt = base != null ? base.getAverageRT() : 0;
            double fc = fractions != null ? fractions.getOrDefault(id, Double.NaN) : Double.NaN;
            return new ClusterTableEntry(id, mz, rt, rows.size(), fc, rows);
          })
          .toList();

      clusterTableView.setItems(FXCollections.observableArrayList(entries));

      // Restore or initialize selection
      List<Integer> selectedClusters = model.getSelectedClusters();
      clusterTableView.getSelectionModel().clearSelection();
      if (selectedClusters != null && !selectedClusters.isEmpty()) {
        for (ClusterTableEntry entry : clusterTableView.getItems()) {
          if (selectedClusters.contains(entry.getClusterId())) {
            clusterTableView.getSelectionModel().select(entry);
          }
        }
      } else if (!entries.isEmpty()) {
        clusterTableView.getSelectionModel().select(0);
        model.setSelectedClusters(List.of(entries.get(0).getClusterId()));
      }

      updateStatusLabel();
    } catch (Exception e) {
      logger.warning("Error updating cluster table: " + e.getMessage());
    }
  }

  /**
   * Sync the cluster table selection to match the given list of cluster IDs (e.g. when selection
   * is driven externally from the feature table).
   */
  private void syncClusterListToModel(List<Integer> selectedClusters) {
    clusterTableView.getSelectionModel().clearSelection();
    if (selectedClusters == null || selectedClusters.isEmpty()) {
      return;
    }
    for (ClusterTableEntry entry : clusterTableView.getItems()) {
      if (selectedClusters.contains(entry.getClusterId())) {
        clusterTableView.getSelectionModel().select(entry);
      }
    }
  }

  /**
   * Update the status label with information about selected clusters
   */
  private void updateStatusLabel() {
    List<Integer> selectedClusters = model.getSelectedClusters();

    if (selectedClusters == null || selectedClusters.isEmpty()) {
      statusLabel.setText("No clusters selected");
    } else {
      statusLabel.setText(selectedClusters.size() + " cluster(s) selected");
    }
  }

  /**
   * Refresh the isotopologue detail panel for the first selected cluster.
   */
  private void refreshDetailPanel(List<Integer> selectedClusterIds) {
    if (detailPanel == null) {
      return;
    }
    if (selectedClusterIds == null || selectedClusterIds.isEmpty()) {
      detailPanel.clear();
      return;
    }
    List<FeatureListRow> rows = allClusters.get(selectedClusterIds.get(0));
    if (rows == null || rows.isEmpty()) {
      detailPanel.clear();
    } else {
      detailPanel.update(rows, model.getTracerMassDiff());
    }
  }
}