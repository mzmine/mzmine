/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.msn_tree;

import static io.github.mzmine.modules.visualization.spectra.msn_tree.IndividualScansOrMerged.INDIVIDUAL_SCANS;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectParameters.SelectInputScans;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.MassListDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.RelativeOption;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.ArrowRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.ArrowRenderer.ShapeType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.LabelOnlyRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.PeakRenderer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupPane;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import io.github.mzmine.util.scans.merging.SpectraMerger;
import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class MSnTreeTab extends SimpleTab {

  private final AtomicLong currentThread = new AtomicLong(0L);
  private final TreeView<PrecursorIonTreeNode> treeView;
  private final GridPane spectraPane;
  private final Label legendEnergies;
  private final CheckBox cbRelative;
  private final CheckBox cbDenoise;
  private final List<SpectraPlot> spectraPlots = new ArrayList<>(10);
  private final ComboBox<IndividualScansOrMerged> comboScanSelection;
  public Ellipse2D circle = new Ellipse2D.Double(-2.5d, 0, 5, 5);
  private final Spinner<Integer> sizeSpinner;
  private final ChartGroup chartGroup;
  private final ParameterSet treeParameters;
  // current shapes
  public Shape downArrow = new Polygon(new int[]{-3, 3, 0}, new int[]{0, 0, 3}, 3);
  public Shape upArrow = new Polygon(new int[]{-3, 3, 0}, new int[]{3, 3, 0}, 3);
  public Shape diamond = new Polygon(new int[]{0, -3, 0, 3}, new int[]{0, 3, 6, 3}, 4);
  private int numberUsedSpectraPlots = 0;
  private int lastSelectedItem = -1;
  private PrecursorIonTreeNode currentRoot = null;

  // only one may be selected
  private RawDataFile raw;
  private FeatureList featureList;
  private final Map<PrecursorIonTreeNode, javafx.scene.paint.Color> colorMap = new HashMap<>();


  public MSnTreeTab() {
    super("MSn Tree", true, false);

    treeParameters = MZmineCore.getConfiguration()
        .getModuleParameters(MSnTreeVisualizerModule.class);

    BorderPane main = new BorderPane();

    // add tree to the left
    // buttons over tree
    HBox buttons = new HBox(5, // add buttons
        createButton("Expand", e -> expandTreeView(true)),
        createButton("Collapse", e -> expandTreeView(false)));

    treeView = new TreeView<>();
    ScrollPane treeScroll = new ScrollPane(treeView);
    //    treeScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
    treeScroll.setFitToHeight(true);
    treeScroll.setFitToWidth(true);

    TreeItem<PrecursorIonTreeNode> root = new TreeItem<>();
    root.setExpanded(true);
    treeView.setRoot(root);
    treeView.setShowRoot(false);
    treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    treeView.getSelectionModel().selectedItemProperty().addListener(
        ((observable, oldValue, newValue) -> showSpectra(
            newValue == null ? null : newValue.getValue())));

    treeView.setCellFactory(tv -> new TreeCell<>() {
      @Override
      protected void updateItem(PrecursorIonTreeNode item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
        } else {
          List<Text> texts = item.streamParents().map(node -> {
            Text text = new Text(node.getFormatted());
            text.setFill(colorMap.get(node));
            return text;
          }).toList();
          texts.get(0).setText("m/z " + texts.get(0).getText());
          for (int i = 1; i < texts.size(); i++) {
            texts.get(i).setText(" ↦ " + texts.get(i).getText());
          }

          setGraphic(new HBox(5, texts.toArray(Node[]::new)));
        }
      }
    });

    BorderPane left = new BorderPane();
    left.setTop(buttons);
    left.setCenter(treeScroll);

    // create spectra grid
    spectraPane = new GridPane();
    spectraPane.getColumnConstraints()
        .addAll(new ColumnConstraints(200, 350, -1, Priority.ALWAYS, HPos.LEFT, true));
    spectraPane.setGridLinesVisible(true);
    spectraPane.getRowConstraints()
        .add(new RowConstraints(100, -1, -1, Priority.ALWAYS, VPos.CENTER, true));
    // create first plot and initialize group for zooming etc
    chartGroup = new ChartGroup(false, false, true, false);
    chartGroup.setShowCrosshair(true, false);
    final SpectraPlot plot = new SpectraPlot(false, false);
    plot.getXYPlot().getRangeAxis().setLabel("MS2 intensity");
    spectraPlots.add(plot);
    chartGroup.add(new ChartViewWrapper(plot));
    spectraPane.add(new BorderPane(spectraPlots.get(0)), 0, 0);

    ScrollPane scrollSpectra = new ScrollPane(new BorderPane(spectraPane));
    scrollSpectra.setFitToHeight(true);
    scrollSpectra.setFitToWidth(true);
    scrollSpectra.setVbarPolicy(ScrollBarPolicy.ALWAYS);

    BorderPane center = new BorderPane(scrollSpectra);

    // add menu to spectra
    HBox spectraMenu = new HBox(8);
    spectraMenu.setAlignment(Pos.CENTER_LEFT);
    center.setTop(spectraMenu);

    sizeSpinner = new Spinner<>(1, 100, 3);
    sizeSpinner.getEditor().setPrefColumnCount(4);
    sizeSpinner.valueProperty().addListener((o, ov, nv) -> changeSymbolSize());

    cbRelative = new CheckBox("Relative");
    cbRelative.selectedProperty().addListener((o, ov, nv) -> changeRelative());

    cbDenoise = new CheckBox("Denoise");
    cbDenoise.selectedProperty().addListener((o, ov, nv) -> updateCurrentSpectra());

    legendEnergies = new Label("");

    var options = FXCollections.observableArrayList(IndividualScansOrMerged.values());
    comboScanSelection = new ComboBox<>(options);
    comboScanSelection.getSelectionModel().select(INDIVIDUAL_SCANS);
    var selectedScansProperty = comboScanSelection.getSelectionModel().selectedItemProperty();
    selectedScansProperty.addListener((obs, ov, nv) -> updateCurrentSpectra());

    legendEnergies.visibleProperty().bind(selectedScansProperty.isEqualTo(INDIVIDUAL_SCANS));

    // menu
    spectraMenu.getChildren().addAll( // menu
        createButton("Auto range", this::autoRange), //
        comboScanSelection, cbRelative, cbDenoise, new Label("Size"), sizeSpinner, legendEnergies);

    SplitPane splitPane = new SplitPane(left, center);
    splitPane.setDividerPositions(0.22);

    Accordion topParam = new Accordion(new TitledPane("Tree generation parameters",
        new ParameterSetupPane(true, true, treeParameters) {
          @Override
          protected void callOkButton() {
            regenerateTrees(treeParameters);
          }

          @Override
          protected void parametersChanged() {
            regenerateTrees(treeParameters);
          }
        }));

    // main pane
    main.setTop(topParam);
    main.setCenter(splitPane);

    // add main to tab
    main.getStyleClass().add("region-match-chart-bg");
    this.setContent(main);

    main.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.DOWN) {
        nextPrecursor();
        main.requestFocus();
        e.consume();
      } else if (e.getCode() == KeyCode.UP) {
        previousPrecursor();
        main.requestFocus();
        e.consume();
      }
    });
  }

  private synchronized void regenerateTrees(ParameterSet treeParameters) {
    lastSelectedItem = -1;
    treeView.getRoot().getChildren().clear();

    // parameters
    final MZTolerance mzTol = treeParameters.getValue(MSnTreeVisualizerParameters.mzTol);
    final RawDataFile finalraw = this.raw;
    final FeatureList finalFlist = this.featureList;
    // track current thread
    final long current = currentThread.incrementAndGet();
    Thread thread = new Thread(() -> {
      // run on different thread
      final List<PrecursorIonTree> trees;
      if (finalraw != null) {
        trees = ScanUtils.getMSnFragmentTrees(finalraw, mzTol);
      } else {
        trees = ScanUtils.getMSnFragmentTrees(finalFlist, mzTol);
      }

      // refresh colors
      final SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
      colorMap.clear();
      trees.forEach(tree -> addToColorMap(tree, colors));

      FxThread.runLater(() -> {
        if (current == currentThread.get()) {
          // add to tree
          treeView.getRoot().getChildren()
              .addAll(trees.stream().map(t -> createTreeItem(t.getRoot())).toList());

          expandTreeView(treeView.getRoot(), false);
        }
      });
    });
    thread.start();
  }

  /**
   * Defines the colors for each node (based on their MS level)
   */
  private void addToColorMap(final PrecursorIonTree tree, final SimpleColorPalette colors) {
    tree.groupByMsLevel().values().forEach(nodes -> {
      for (int i = 0; i < nodes.size(); i++) {
        colorMap.put(nodes.get(i), colors.get(i % colors.size()));
      }
    });
  }

  private void changeSymbolSize() {
    int size = sizeSpinner.getValue();

    downArrow = new Polygon(new int[]{-size, size, 0}, new int[]{0, 0, size}, 3);
    upArrow = new Polygon(new int[]{-size, size, 0}, new int[]{size, size, 0}, 3);
    diamond = new Polygon(new int[]{0, -size, 0, size}, new int[]{0, size, size * 2, size}, 4);

    size += 2;
    circle = new Ellipse2D.Double(-size / 2d, 0, size, size);

    for (var p : spectraPlots) {
      XYPlot xyPlot = p.getXYPlot();
      int numDatasets = JFreeChartUtils.getDatasetCountNullable(xyPlot);
      for (int i = 0; i < numDatasets; i++) {
        final var renderer = xyPlot.getRenderer(i);
        if (renderer instanceof ArrowRenderer arrowRenderer) {
          final ShapeType type = arrowRenderer.getShapeType();
          renderer.setDefaultShape(getShape(type));
        }
      }
      p.getChart().fireChartChanged();
    }
  }

  private Shape getShape(ShapeType type) {
    return switch (type) {
      case UP -> upArrow;
      case DOWN -> downArrow;
      case CIRCLE -> circle;
      case DIAMOND -> diamond;
      case LEFT -> null;
      case RIGHT -> null;
    };
  }

  private void changeRelative() {
    if (currentRoot != null) {
      final boolean normalize = cbRelative.isSelected();
      for (var p : spectraPlots) {
        int numDatasets = JFreeChartUtils.getDatasetCountNullable(p.getXYPlot());
        for (int i = 0; i < numDatasets; i++) {
          final XYDataset data = p.getXYPlot().getDataset(i);
          if (data instanceof RelativeOption op) {
            op.setRelative(normalize);
          }
        }

        applyIntensityFormatToAxis(p, normalize);

        p.getChart().fireChartChanged();
      }
      chartGroup.recalcMaxRanges();
      chartGroup.resetRangeZoom();
    }
  }

  private void updateCurrentSpectra() {
    if (currentRoot != null) {
      showSpectra(currentRoot);
    }
  }

  private void autoRange(ActionEvent actionEvent) {
    if (chartGroup != null) {
      chartGroup.resetZoom();
    }
  }

  private Button createButton(String title, EventHandler<ActionEvent> action) {
    final Button button = new Button(title);
    button.setOnAction(action);
    return button;
  }

  /**
   * Set raw data file and update tree
   *
   * @param raw update all views to this raw file
   */
  public synchronized void setRawDataFile(RawDataFile raw) {
    this.raw = raw;
    featureList = null;
    regenerateTrees(treeParameters);
  }

  public void setFeatureList(final FeatureList flist) {
    this.featureList = flist;
    this.raw = null;
    regenerateTrees(treeParameters);
  }

  private TreeItem<PrecursorIonTreeNode> createTreeItem(PrecursorIonTreeNode node) {
    final var item = new TreeItem<>(node);
    item.getChildren()
        .addAll(node.getChildPrecursors().stream().map(this::createTreeItem).toList());
    return item;
  }

  public void showSpectra(PrecursorIonTreeNode any) {
    spectraPane.getChildren().clear();
    spectraPane.getRowConstraints().clear();
    spectraPlots.forEach(SpectraPlot::removeAllDataSets);
    numberUsedSpectraPlots = 0;
    if (any == null) {
      return;
    }
    // add spectra
    PrecursorIonTreeNode prevRoot = currentRoot;
    currentRoot = any.getRoot();
    boolean rootHasChanged = !Objects.equals(prevRoot, currentRoot);

    switch (comboScanSelection.getSelectionModel().getSelectedItem()) {
      case INDIVIDUAL_SCANS -> showIndividualScans(currentRoot);
      case MERGED -> showMergedSpectra(currentRoot);
    }

    if (rootHasChanged) {
      chartGroup.applyAutoRange(true);
    }

    // update chart
    for (var spectraPlot : spectraPlots) {
      spectraPlot.setNotifyChange(true);
      spectraPlot.fireChangeEvent();
    }
  }

  private void showIndividualScans(final PrecursorIonTreeNode currentRoot) {
    if (currentRoot == null) {
      return;
    }
    SpectraPlot previousPlot = null;
    // distribute collision energies in three categories low, med, high
    final List<Float> collisionEnergies = currentRoot.getAllFragmentScans().stream()
        .map(Scan::getMsMsInfo).filter(Objects::nonNull).map(MsMsInfo::getActivationEnergy)
        .filter(Objects::nonNull).distinct().sorted().toList();

    float minEnergy = 0f;
    float maxEnergy = 0f;
    float medEnergy = 0f;
    if (!collisionEnergies.isEmpty()) {
      minEnergy = collisionEnergies.get(0);
      maxEnergy = collisionEnergies.get(collisionEnergies.size() - 1);
      medEnergy = collisionEnergies.get(collisionEnergies.size() / 2);
      // set legend
      if (minEnergy != maxEnergy) {
        if (minEnergy != medEnergy && maxEnergy != medEnergy) {
          legendEnergies.setText(
              String.format("Activation: ▽≈%.0f △≈%.0f ◇≈%.0f", minEnergy, medEnergy, maxEnergy));
        } else {
          legendEnergies.setText(String.format("Activation: ▽≈%.0f ◇≈%.0f", minEnergy, maxEnergy));
        }
      } else {
        legendEnergies.setText(String.format("Activation: ◇≈%.0f", maxEnergy));
      }
    }
    // relative intensities? and denoise?
    final boolean normalizeIntensities = cbRelative.isSelected();
    final boolean denoise = cbDenoise.isSelected();

    List<PrecursorIonTreeNode> levelPrecursors = List.of(currentRoot);
    int levelFromRoot = 0;

    // for each MS level
    do {
      int msLevel = levelFromRoot + 2;
      SpectraPlot spectraPlot = getSpectraPlot(msLevel);

      // relative or absolute
      applyIntensityFormatToAxis(spectraPlot, normalizeIntensities);

      // create one dataset for labels - otherwise there are too many labels
      Double2DoubleOpenHashMap combinedData = new Double2DoubleOpenHashMap();

      // create combined SpectraPlot for each MS level - multiple datasets for shapes and lines
      int c = 0;
      for (PrecursorIonTreeNode precursor : levelPrecursors) {
        final Color color = FxColorUtil.fxColorToAWT(
            colorMap.getOrDefault(precursor, javafx.scene.paint.Color.BLACK));
        final List<Scan> fragmentScans = precursor.getFragmentScans();
        for (final Scan scan : fragmentScans) {
          AbstractXYDataset data = ensureCentroidDataset(normalizeIntensities, denoise, scan);
          // add peak renderer to show centroids - no labels
          spectraPlot.addDataSet(data, color, false, new PeakRenderer(color, false), null, false,
              false);

          // add shapes dataset and renderer - no labels
          final ShapeType shapeType = getActivationEnergyShape(
              scan.getMsMsInfo().getActivationEnergy(), minEnergy, medEnergy, maxEnergy);
          spectraPlot.addDataSet(data, color, false,
              new ArrowRenderer(shapeType, getShape(shapeType), color), null, false, false);

          // combine all to one dataset for label
          combineDatasetsToOne(combinedData, data);
        }

        spectraPlot.getChart().getLegend().setVisible(false);
        // add precursor markers for each different precursor only once
        spectraPlot.addPrecursorMarkers(precursor.getFragmentScans().get(0), color, 0.25f);
        c++;
      }

      // add the combined dataset
      addCombinedDatasetForLabels(spectraPlot, combinedData);

      // hide x axis
      if (previousPlot != null) {
        previousPlot.getXYPlot().getDomainAxis().setVisible(false);
      }
      // add
      spectraPlot.getXYPlot().getDomainAxis().setVisible(true);
      spectraPane.getRowConstraints()
          .add(new RowConstraints(100, -1, -1, Priority.ALWAYS, VPos.CENTER, true));
      spectraPane.add(new BorderPane(spectraPlot), 0, levelFromRoot);
      previousPlot = spectraPlot;
      // next level
      levelFromRoot++;
      levelPrecursors = currentRoot.getPrecursors(levelFromRoot);

    } while (!levelPrecursors.isEmpty());
  }

  private void showMergedSpectra(final PrecursorIonTreeNode any) {
    if (any == null) {
      return;
    }
    final boolean normalizeIntensities = cbRelative.isSelected();
    final boolean denoise = cbDenoise.isSelected();
    final MZTolerance mzTol = treeParameters.getValue(MSnTreeVisualizerParameters.mzTol);
    var root = any.getRoot();
    // only get the merged spectrum on each level

    var scanTypes = List.of(MergedSpectraFinalSelectionTypes.ACROSS_SAMPLES,
        MergedSpectraFinalSelectionTypes.ACROSS_ENERGIES,
        MergedSpectraFinalSelectionTypes.MSN_TREE);
    var merger = new SpectraMerger(scanTypes, mzTol, IntensityMergingType.MAXIMUM);
    FragmentScanSelection selection = new FragmentScanSelection(null, SelectInputScans.NONE, merger,
        scanTypes);
    List<Scan> mergedSpectra = selection.getAllFragmentSpectra(root);

    // MS2 has two spectra - the merged MS2 and the spectrum of all MSn merged into it
    Map<Integer, List<Scan>> byMsLevel = mergedSpectra.stream()
        .sorted(Comparator.comparingInt(Scan::getMSLevel))
        .collect(Collectors.groupingBy(Scan::getMSLevel));

    final SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
    SpectraPlot previousPlot = null;
    int rowIndex = 0;
    for (var entry : byMsLevel.entrySet()) {
      Integer msLevel = entry.getKey();
      if (msLevel == null) {
        continue;
      }

      List<Scan> scans = entry.getValue();
      SpectraPlot spectraPlot = getSpectraPlot(msLevel);

      // relative or absolute
      applyIntensityFormatToAxis(spectraPlot, normalizeIntensities);

      for (int i = 0; i < scans.size(); i++) {
        final Scan spec = scans.get(i);
        // create combined SpectraPlot for each MS level
        AbstractXYDataset data = ensureCentroidDataset(normalizeIntensities, denoise, spec);
        // add peak renderer to show centroids - no labels
        Color color = colors.getAWT(i);
        spectraPlot.addDataSet(data, color, false, new PeakRenderer(color, false), null, false,
            false);

        spectraPlot.getChart().getLegend().setVisible(false);
        root.streamWholeTree().filter(node -> node.getMsLevel() == msLevel)
            .map(PrecursorIonTreeNode::getFragmentScans).map(list -> list.get(0)).findAny()
            .ifPresent(scan -> spectraPlot.addPrecursorMarkers(scan, color, 0.25f));

        spectraPlot.addDataSet(data, color, false,
            new ArrowRenderer(ShapeType.CIRCLE, getShape(ShapeType.CIRCLE), color), null, false,
            false);
      }
      // hide x axis
      if (previousPlot != null) {
        previousPlot.getXYPlot().getDomainAxis().setVisible(false);
      }
      // add
      spectraPlot.getXYPlot().getDomainAxis().setVisible(true);
      spectraPane.getRowConstraints()
          .add(new RowConstraints(100, -1, -1, Priority.ALWAYS, VPos.CENTER, true));
      spectraPane.add(new BorderPane(spectraPlot), 0, rowIndex);
      previousPlot = spectraPlot;
      rowIndex++;
    }
  }

  /**
   * @param msLevel only used for labelling
   * @return
   */
  private SpectraPlot getSpectraPlot(final int msLevel) {
    // create one spectra plot for each MS level
    if (numberUsedSpectraPlots >= spectraPlots.size()) {
      final SpectraPlot plot = new SpectraPlot();
      spectraPlots.add(plot);
      chartGroup.add(new ChartViewWrapper(plot));
    }
    SpectraPlot spectraPlot = spectraPlots.get(numberUsedSpectraPlots);
    spectraPlot.setNotifyChange(false);
    spectraPlot.getXYPlot().getRangeAxis().setLabel(String.format("MS%d intensity", msLevel));
    numberUsedSpectraPlots++;
    return spectraPlot;
  }

  private void addCombinedDatasetForLabels(SpectraPlot spectraPlot,
      Double2DoubleOpenHashMap combinedData) {
    final double[] mzs = combinedData.keySet().toDoubleArray();
    final double[] intensities = combinedData.values().toDoubleArray();
    MassListDataSet data = new MassListDataSet(mzs, intensities);

    final Color labelColor = MZmineCore.getConfiguration().getDefaultChartTheme()
        .getMasterFontColor();
    spectraPlot.addDataSet(data, labelColor, false, new LabelOnlyRenderer(), false, false);
  }

  /**
   * Uses the mz format to reduce the number of data points for labels
   */
  private void combineDatasetsToOne(Double2DoubleOpenHashMap combinedData, XYDataset data) {
    // reduce number of values based on mzformat
    final NumberFormat format = MZmineCore.getConfiguration().getMZFormat();
    for (int s = 0; s < data.getSeriesCount(); s++) {
      for (int i = 0; i < data.getItemCount(s); i++) {
        final double key = Double.parseDouble(format.format(data.getXValue(s, i)));
        final double intensity = data.getYValue(s, i);
        final double old = combinedData.getOrDefault(key, intensity);
        // maximize intensity for this entry
        if (Double.compare(old, intensity) <= 0) {
          combinedData.put(key, intensity);
        }
      }
    }
  }

  private void applyIntensityFormatToAxis(SpectraPlot spectraPlot, boolean normalizeIntensities) {
    if (spectraPlot.getXYPlot().getRangeAxis() instanceof NumberAxis va) {
      if (normalizeIntensities) {
        va.setNumberFormatOverride(new DecimalFormat("0.#"));
      } else {
        va.setNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
      }
    }
  }

  @NotNull
  private AbstractXYDataset ensureCentroidDataset(boolean normalizeIntensities, boolean denoise,
      Scan scan) {
    final double[][] masses;
    if (scan.getMassList() != null) {
      masses = new double[][]{scan.getMassList().getMzValues(new double[0]),
          scan.getMassList().getIntensityValues(new double[0])};
    } else if (!MassSpectrumType.PROFILE.equals(scan.getSpectrumType())) {
      masses = new double[][]{scan.getMzValues(new double[0]),
          scan.getIntensityValues(new double[0])};
    } else {
      // profile data run mass detection
      masses = ExactMassDetector.getMassValues(scan, 0);
    }
    List<DataPoint> dps = new ArrayList<>();
    if (denoise) {
      final double[] sortedIntensities = Arrays.stream(masses[1]).filter(v -> v > 0).sorted()
          .toArray();
      if (sortedIntensities.length > 0) {
        double min = sortedIntensities[0];
        // remove everything <2xmin
        for (int i = 0; i < masses[0].length; i++) {
          if (masses[1][i] > min * 2.5d) {
            dps.add(new SimpleDataPoint(masses[0][i], masses[1][i]));
          }
        }
      }
    } else {
      // filter zeros
      for (int i = 0; i < masses[0].length; i++) {
        if (masses[1][i] > 0) {
          dps.add(new SimpleDataPoint(masses[0][i], masses[1][i]));
        }
      }
    }
    return new DataPointsDataSet("", dps.toArray(DataPoint[]::new), normalizeIntensities);
  }

  /**
   * Three groups of activation energies close to min, median, max
   *
   * @param ae current activation energy
   * @return a shape from the {@link ArrowRenderer}
   */
  private ShapeType getActivationEnergyShape(Float ae, float minEnergy, float medEnergy,
      float maxEnergy) {
    if (ae == null) {
      return ShapeType.CIRCLE;
    }
    final float med = Math.abs(medEnergy - ae);
    return ae - minEnergy < med ? ShapeType.DOWN
        : (med < maxEnergy - ae ? ShapeType.UP : ShapeType.DIAMOND);
  }

  private void expandTreeView(boolean expanded) {
    expandTreeView(treeView.getRoot(), expanded);
  }

  private void expandTreeView(TreeItem<?> item, boolean expanded) {
    if (item != null && !item.isLeaf()) {
      item.setExpanded(expanded);
      for (TreeItem<?> child : item.getChildren()) {
        expandTreeView(child, expanded);
      }
    }
    treeView.getRoot().setExpanded(true);
  }

  public void previousPrecursor() {
    if (lastSelectedItem > 0) {
      lastSelectedItem--;
      treeView.getSelectionModel().select(getMS2Nodes().get(lastSelectedItem));
    }
  }

  private ObservableList<TreeItem<PrecursorIonTreeNode>> getMS2Nodes() {
    return treeView.getRoot().getChildren();
  }

  public void nextPrecursor() {
    if (lastSelectedItem + 1 < getMS2Nodes().size()) {
      lastSelectedItem++;
      treeView.getSelectionModel().select(treeView.getRoot().getChildren().get(lastSelectedItem));
    }
  }


  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if (rawDataFiles != null && rawDataFiles.size() > 0) {
      setRawDataFile(rawDataFiles.stream().findFirst().get());
      setSubTitle(raw.getName());
    }
  }

}
