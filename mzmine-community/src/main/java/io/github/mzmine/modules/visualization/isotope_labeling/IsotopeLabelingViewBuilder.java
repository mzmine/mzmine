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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import javafx.scene.control.ListView;
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
 * View builder for isotope labeling visualization with stacked bar charts
 */
public class IsotopeLabelingViewBuilder extends FxViewBuilder<IsotopeLabelingModel> {

  private static final Logger logger = Logger.getLogger(IsotopeLabelingViewBuilder.class.getName());
  private static final int SPACING = 5;
  // private static final Stroke ANNOTATION_STROKE = EStandardChartTheme.DEFAULT_MARKER_STROKE;
  // private static final DecimalFormat MZ_FORMAT = new DecimalFormat("0.0000");
  // private static final DecimalFormat RT_FORMAT = new DecimalFormat("0.00");

  private SwingNode chartNode;
  private ChartPanel chartPanel;
  private ListView<Integer> clusterListView;
  private Label statusLabel;
  private ComboBox<String> visTypeCombo;

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
   * Create the panel with the list of available isotope clusters
   */
  private VBox createClusterSelectionPanel() {
    VBox panel = new VBox(SPACING);
    panel.setPadding(new Insets(SPACING));

    // Title
    Label titleLabel = new Label("Isotope Clusters");
    titleLabel.setStyle("-fx-font-weight: bold;");

    // Create list view for clusters
    clusterListView = new ListView<>();
    clusterListView.setPlaceholder(new Label("No isotope clusters available"));

    // Enable multiple selection
    clusterListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    // Add selection listener
    clusterListView.getSelectionModel().selectedItemProperty()
        .addListener((obs, oldVal, newVal) -> {
          if (newVal != null) {
            // Get all selected clusters
            List<Integer> selectedClusters = new ArrayList<>(
                clusterListView.getSelectionModel().getSelectedItems());

            // Update model with selection
            model.setSelectedClusters(selectedClusters);

            // Update the status label
            updateStatusLabel();
          }
        });

    clusterListView.setMinWidth(200);
    VBox.setVgrow(clusterListView, Priority.ALWAYS);

    // Add tooltip to explain selection
    clusterListView.setTooltip(
        new Tooltip("Select clusters to visualize. Hold Ctrl/Cmd to select multiple clusters."));

    // Add selection buttons
    HBox buttonBox = new HBox(SPACING);
    buttonBox.setAlignment(Pos.CENTER);

    Button selectAllButton = new Button("Select All");
    selectAllButton.setOnAction(e -> {
      if (clusterListView.getItems() != null && !clusterListView.getItems().isEmpty()) {
        clusterListView.getSelectionModel().selectAll();

        // Update model with all clusters
        model.setSelectedClusters(new ArrayList<>(clusterListView.getItems()));

        // Update the status label
        updateStatusLabel();
      }
    });

    Button clearSelectionButton = new Button("Clear Selection");
    clearSelectionButton.setOnAction(e -> {
      clusterListView.getSelectionModel().clearSelection();
      model.setSelectedClusters(List.of());

      // Update the status label
      updateStatusLabel();
    });

    buttonBox.getChildren().addAll(selectAllButton, clearSelectionButton);

    // Add status label
    statusLabel = new Label("No clusters selected");
    updateStatusLabel();

    panel.getChildren().addAll(titleLabel, clusterListView, buttonBox, statusLabel);
    return panel;
  }

  /**
   * Create the control panel for chart settings
   */
  private Region createChartControlsPane() {
    // Create accordion for controls
    Accordion accordion = new Accordion();

    // Create visualization settings panel
    TitledPane visualizationPane = new TitledPane("Visualization Settings",
        createVisualizationControlsPane());

    // Create appearance settings panel
    TitledPane appearancePane = new TitledPane("Export Options", createExportControlsPane());

    // Add panes to accordion
    accordion.getPanes().addAll(visualizationPane, appearancePane);
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

    // Normalization option
    CheckBox normalizeCheck = new CheckBox("Normalize to base peak (M+0)");
    normalizeCheck.setSelected(model.getNormalizeToBaseIsotopologue());
    normalizeCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && newVal != model.getNormalizeToBaseIsotopologue()) {
        model.setNormalizeToBaseIsotopologue(newVal);
      }
    });

    // Update checkbox when model changes
    model.normalizeToBaseIsotopologueProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && newVal != normalizeCheck.isSelected()) {
        normalizeCheck.setSelected(newVal);
      }
    });

    // Disable normalization option if not using relative intensities
    normalizeCheck.disableProperty()
        .bind(visTypeCombo.valueProperty().isNotEqualTo("Relative intensities"));

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

    // Add all controls to the flow pane
    controls.getChildren().addAll(visTypeBox, normalizeCheck, maxIsotopologuesBox);

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

    // Listen for changes in selected clusters
    model.selectedClustersProperty().addListener((obs, oldVal, newVal) -> {
      Platform.runLater(() -> {
        updateStatusLabel();
      });
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
   * Update the cluster list with available clusters
   *
   * @param clusters Map of available isotope clusters
   */
  public void updateClusterList(Map<Integer, List<FeatureListRow>> clusters) {
    if (clusters == null || clusters.isEmpty()) {
      clusterListView.setItems(FXCollections.observableArrayList());
      return;
    }

    try {
      // Get list of cluster IDs sorted numerically
      List<Integer> clusterIds = clusters.keySet().stream().sorted().toList();

      // Update list view with cluster IDs
      clusterListView.setItems(FXCollections.observableArrayList(clusterIds));

      // Restore or initialize selection
      List<Integer> selectedClusters = model.getSelectedClusters();

      // Clear current selection
      clusterListView.getSelectionModel().clearSelection();

      if (selectedClusters != null && !selectedClusters.isEmpty()) {
        // Restore existing selection
        for (Integer clusterId : selectedClusters) {
          int index = clusterIds.indexOf(clusterId);
          if (index >= 0) {
            clusterListView.getSelectionModel().select(index);
          }
        }
      } else if (!clusterIds.isEmpty()) {
        // Select first cluster by default if none selected
        clusterListView.getSelectionModel().select(0);
        model.setSelectedClusters(List.of(clusterIds.get(0)));
      }

      // Update status
      updateStatusLabel();
    } catch (Exception e) {
      logger.warning("Error updating cluster list: " + e.getMessage());
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
}