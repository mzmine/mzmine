/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.tools.kovats;

import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.controlsfx.control.CheckListView;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffEvent;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureDragDiffHandler;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.framework.listener.DelayedDocumentListener;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.kovats.KovatsValues.KovatsIndex;
import io.github.mzmine.modules.tools.mzrangecalculator.MzRangeFormulaCalculatorModule;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICSumDataSet;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeComponent;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeComponent;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.color.Colors;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.TxtWriter;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class KovatsIndexExtractionDialog extends ParameterSetupDialog {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private static final ExtensionFilter csvFilter =
      new ExtensionFilter("Comma-separated values", "*.csv");

  private NumberFormat rtFormat = new DecimalFormat("0.###");
  private static final Stroke markerStroke = new BasicStroke(1.5f);
  public static final int MIN_MARKERS = 3;
  private static final Image iconNext = FxIconUtil.loadImageFromResources("icons/btnNext.png");
  private static final Image iconPrev = FxIconUtil.loadImageFromResources("icons/btnPrev.png");

  // button size < >
  private static final int SIZE = 25;

  private FlowPane newMainPanel;
  private BorderPane pnChart;
  private KovatsIndex[] selectedKovats;

  // accepts saved files
  private Consumer<File> saveFileListener;
  private TextField txtPeakPick;
  private TextField valuesComponent;
  private TICPlot chart;

  private String pickedValuesString;
  private TreeMap<KovatsIndex, Double> parsedValues;
  private double noiseLevel = 0;
  private double ratioEdge = 2;
  private ComboBox<RawDataFile> comboDataFileName;
  private ComboBox<RawDataFile> comboDataFileName2;
  private RawDataFile[] selectedDataFile;
  private IntegerComponent minc;
  private IntegerComponent maxc;
  private DelayedDocumentListener ddlKovats;
  private CheckListView<KovatsIndex> comboKovats;
  private List<ValueMarker> markers;
  private ValueMarker currentlyDraggedMarker;
  private CheckBox cbSecondRaw;
  private CheckBox cbCurrentAlkaneSubH;
  private Label lbCurrentAlkane;
  // for direct selection of mz by alkane buttons
  private KovatsIndex currentAlkane;


  public KovatsIndexExtractionDialog(ParameterSet parameters) {
    this(parameters, null);
  }

  /**
   *
   * @param parent
   * @param parameters
   * @param saveFileListener accepts saved files
   */
  public KovatsIndexExtractionDialog(ParameterSet parameters, Consumer<File> saveFileListener) {
    super(false, parameters);
    this.saveFileListener = saveFileListener;

    // paramsPane.getChildren().clear();
    // paramsPane.getParent().remove(paramsPane);

    ddlKovats = new DelayedDocumentListener(e -> updateKovatsList());
    DelayedDocumentListener ddlUpdateChart = new DelayedDocumentListener(e -> updateChart());

    newMainPanel = new FlowPane();
    mainPane.setBottom(newMainPanel);

    BorderPane pnCenter = new BorderPane();
    mainPane.setCenter(pnCenter);
    pnChart = new BorderPane();
    pnCenter.setCenter(pnChart);

    // Box sizedummy = new Box(BoxLayout.X_AXIS);
    // sizedummy.setMinimumSize(new Dimension(200, 450));
    // sizedummy.setPreferredSize(new Dimension(200, 450));
    // pnChart.add(sizedummy, BorderLayout.CENTER);

    // left: Kovats: min max and list
    BorderPane west = new BorderPane();
    newMainPanel.getChildren().add(west);

    // add min max
    FlowPane pnKovatsParam = new FlowPane();
    west.setTop(pnKovatsParam);
    minc = getComponentForParameter(KovatsIndexExtractionParameters.minKovats);
    maxc = getComponentForParameter(KovatsIndexExtractionParameters.maxKovats);
    // minc.addDocumentListener(ddlKovats);
    // maxc.addDocumentListener(ddlKovats);

    pnKovatsParam.getChildren().add(new Label("Min carbon:"));
    pnKovatsParam.getChildren().add(minc);
    pnKovatsParam.getChildren().add(new Label("Max carbon:"));
    pnKovatsParam.getChildren().add(maxc);

    // kovats list
    BorderPane pnKovatsSelect = new BorderPane();
    west.setCenter(pnKovatsSelect);
    comboKovats = getComponentForParameter(KovatsIndexExtractionParameters.kovats);
    // comboKovats.addValueChangeListener(() -> handleKovatsSelectionChange());
    pnKovatsSelect.setCenter(comboKovats);

    // center: Chart and parameters
    BorderPane center = new BorderPane();
    newMainPanel.getChildren().add(center);

    // all parameters on peak pick panel
    BorderPane pnSouth = new BorderPane();
    center.setBottom(pnSouth);

    FlowPane pnPeakPick = new FlowPane();
    pnSouth.setCenter(pnPeakPick);

    valuesComponent = getComponentForParameter(KovatsIndexExtractionParameters.pickedKovatsValues);
    MZRangeComponent mzc =
        (MZRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.mzRange);
    RTRangeComponent rtc =
        (RTRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.rtRange);
    DoubleComponent noisec = getComponentForParameter(KovatsIndexExtractionParameters.noiseLevel);
    DoubleComponent edgeRatioC =
        getComponentForParameter(KovatsIndexExtractionParameters.ratioTopEdge);

    // valuesComponent.addDocumentListener(new DelayedDocumentListener(e -> kovatsValuesChanged()));
    // valuesComponent.setLayout(new GridLayout(1, 1));
    pnCenter.setBottom(valuesComponent);

    BorderPane pnButtonFlow = new BorderPane();
    pnPeakPick.getChildren().add(pnButtonFlow);
    Button btnUpdateChart = new Button("Update chart");
    btnUpdateChart.setOnAction(e -> updateChart());
    pnButtonFlow.getChildren().add(btnUpdateChart);
    Button btnPickRT = new Button("Pick peaks");
    btnPickRT.setOnAction(e -> pickRetentionTimes());
    pnButtonFlow.getChildren().add(btnPickRT);
    Button btnSaveFile = new Button("Save to file");
    btnSaveFile.setTooltip(new Tooltip("Save Kovats index file"));
    btnSaveFile.setOnAction(e -> saveToFile());
    pnButtonFlow.getChildren().add(btnSaveFile);
    Button btnLoad = new Button("Load");
    btnLoad.setTooltip(new Tooltip("Load Kovats index file"));
    btnLoad.setOnAction(e -> loadFile());
    pnButtonFlow.getChildren().add(btnLoad);
    Button btnCombineFiles = new Button("Combine files");
    btnCombineFiles
        .setTooltip(new Tooltip("Select multiple Kovats index files to be combined into one"));
    btnCombineFiles.setOnAction(e -> combineFiles());
    pnButtonFlow.getChildren().add(btnCombineFiles);

    // add combo for raw data file

    comboDataFileName = new ComboBox<RawDataFile>(
        MZmineCore.getProjectManager().getCurrentProject().getRawDataFiles());
    comboDataFileName2 = new ComboBox<RawDataFile>(
        MZmineCore.getProjectManager().getCurrentProject().getRawDataFiles());
    cbSecondRaw = new CheckBox();
    initRawDataFileSelection();

    pnPeakPick.getChildren().add(new Label("Raw data file(s)"));
    pnPeakPick.getChildren().add(comboDataFileName);
    cbSecondRaw.setOnAction(e -> useSecondDataFile(cbSecondRaw.isSelected()));
    pnPeakPick.getChildren().add(cbSecondRaw);
    pnPeakPick.getChildren().add(comboDataFileName2);
    // direct alkane selection < CxH2x+1 >
    FlowPane pnAlkaneSelect = new FlowPane();
    // Dimension dim = new Dimension(SIZE, SIZE);
    Button btnPrevAlkane = new Button(null, new ImageView(iconPrev));
    btnPrevAlkane.setOnAction(e -> setMzRangeByAlkane(-1));
    // btnPrevAlkane.setPreferredSize(dim);
    // btnPrevAlkane.setMaximumSize(dim);
    Button btnNextAlkane = new Button(null, new ImageView(iconNext));
    btnNextAlkane.setOnAction(e -> setMzRangeByAlkane(1));
    // btnNextAlkane.setPreferredSize(dim);
    // btnNextAlkane.setMaximumSize(dim);

    lbCurrentAlkane = new Label("");
    cbCurrentAlkaneSubH = new CheckBox("-H");
    cbCurrentAlkaneSubH.setSelected(true);
    cbCurrentAlkaneSubH.setOnAction(e -> setMzRangeByAlkane(0));

    pnAlkaneSelect.getChildren().addAll(btnPrevAlkane, lbCurrentAlkane, btnNextAlkane,
        cbCurrentAlkaneSubH);
    pnPeakPick.getChildren().add(pnAlkaneSelect);

    pnPeakPick.getChildren().add(new Label("m/z range"));
    pnPeakPick.getChildren().add(mzc);
    pnPeakPick.getChildren().add(new Label(KovatsIndexExtractionParameters.rtRange.getName()));
    pnPeakPick.getChildren().add(rtc);
    pnPeakPick.getChildren().add(new Label(KovatsIndexExtractionParameters.noiseLevel.getName()));
    pnPeakPick.getChildren().add(noisec);
    pnPeakPick.getChildren().add(new Label(KovatsIndexExtractionParameters.ratioTopEdge.getName()));
    pnPeakPick.getChildren().add(edgeRatioC);

    // add listeners
    comboDataFileName.setOnAction(e -> updateChart());
    // mzc.addDocumentListener(ddlUpdateChart);
    // rtc.addDocumentListener(ddlUpdateChart);

    updateChart();

  }

  private void setMzRangeByAlkane(int diff) {
    if (currentAlkane == null) {
      currentAlkane = KovatsIndex.C4;
    } else {
      int index = currentAlkane.ordinal() + diff;
      index = Math.min(Math.max(index, 0), KovatsIndex.values().length);
      currentAlkane = KovatsIndex.values()[index];
    }
    String formula = currentAlkane.getFormula(cbCurrentAlkaneSubH.isSelected());
    lbCurrentAlkane.setText(formula);
    // set mz
    Range<Double> mzRange = MzRangeFormulaCalculatorModule.getMzRangeFromFormula(formula,
        IonizationType.POSITIVE, new MZTolerance(0.75, 0), 1);
    ((MZRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.mzRange))
        .setValue(mzRange);
  }

  private void useSecondDataFile(boolean selected) {
    updateChart();
  }

  /**
   * Init raw data selection to last used raw data file or "kovats" or "dro"
   */
  private void initRawDataFileSelection() {

    var dataFiles = MZmineCore.getProjectManager().getCurrentProject().getRawDataFiles();

    if (dataFiles != null && dataFiles.size() <= 0)
      return;

    RawDataFilesSelection select =
        parameterSet.getParameter(KovatsIndexExtractionParameters.dataFiles).getValue();
    RawDataFile[] raw = null;
    // set to parameters files - if they exist in this project
    if (select != null && select.getMatchingRawDataFiles().length > 0) {
      RawDataFile[] exists = Arrays.stream(select.getMatchingRawDataFiles())
          .filter(r -> dataFiles.stream().anyMatch(d -> r.getName().equals(d.getName())))
          .toArray(RawDataFile[]::new);
      if (exists.length > 0)
        raw = exists;
    }

    if (raw == null) {
      // find kovats or dro file
      // first use all kovats named files and then dro (max 2)
      RawDataFile[] kovats = dataFiles.stream()
          .filter(d -> d.getName().toLowerCase().contains("kovats")).toArray(RawDataFile[]::new);
      RawDataFile[] dro = dataFiles.stream().filter(d -> d.getName().toLowerCase().contains("dro"))
          .toArray(RawDataFile[]::new);

      // maximum of two files are chosen (0,1,2)
      int size = Math.min(2, kovats.length + dro.length);
      raw = new RawDataFile[size];
      for (int i = 0; i < raw.length; i++) {
        if (i < kovats.length)
          raw[i] = kovats[i];
        else
          raw[i] = dro[i - kovats.length];
      }
    }

    if (raw.length > 0) {
      selectedDataFile = raw;
      comboDataFileName.getSelectionModel().select(selectedDataFile[0]);
      if (raw.length > 1) {
        comboDataFileName2.getSelectionModel().select(selectedDataFile[1]);
      }
    }
  }

  private void setLastFile(File f) {
    getComponentForParameter(KovatsIndexExtractionParameters.lastSavedFile).setValue(f);
    parameterSet.getParameter(KovatsIndexExtractionParameters.lastSavedFile).setValue(f);
  }

  /**
   * replace markers
   */
  private void kovatsValuesChanged() {
    // parse values
    if (parseValues() && chart != null) {
      //
      chart.getChart().getXYPlot().clearDomainMarkers();
      if (markers == null)
        markers = new ArrayList<>();
      else
        markers.clear();

      for (Entry<KovatsIndex, Double> e : parsedValues.entrySet()) {
        ValueMarker marker = new ValueMarker(e.getValue(),
            MZmineCore.getConfiguration().getDefaultColorPalette()
                .getPositiveColorAWT(), markerStroke);

        marker.setLabelOffset(new RectangleInsets(10, 0, 0, 0));
        marker.setLabelFont(new Font("Arial", Font.PLAIN, 12));
        marker.setLabelBackgroundColor(Color.WHITE);
        marker.setLabel(e.getKey().getShortName());
        chart.getChart().getXYPlot().addDomainMarker(marker);
        markers.add(marker);
      }

      // revalidate();
      // repaint();
    }
  }

  private boolean parseValues() {
    updateParameterSetFromComponents();
    if (parsedValues == null)
      parsedValues = new TreeMap<>();
    parsedValues.clear();
    try {
      String[] entries = pickedValuesString.split(",");
      for (String s : entries) {
        if (s.isEmpty())
          continue;
        String[] e = s.split(":");
        parsedValues.put(KovatsIndex.getByShortName(e[0]), Double.parseDouble(e[1]));
      }
      return parsedValues.size() >= MIN_MARKERS;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Format of picked values is wrong");
      return false;
    }
  }

  /**
   * Set value with index in markers list / parsedValues
   *
   * @param index
   * @param value
   */
  private void setValue(int index, double value) {
    int i = 0;
    for (Entry<KovatsIndex, Double> e : parsedValues.entrySet()) {
      if (i == index) {
        e.setValue(value);
        break;
      }
      i++;
    }
    setValues(parsedValues);
  }

  private void setValues(TreeMap<KovatsIndex, Double> values) {
    setValues(values, true);
  }

  /**
   *
   * @param values
   * @param checkMinMaxSelectedChange check min max and selected Kovats for Kovats Index list
   */
  private void setValues(TreeMap<KovatsIndex, Double> values, boolean checkMinMaxSelectedChange) {
    if (values.size() < 2)
      return;

    parsedValues = values;
    StringBuilder s = new StringBuilder();
    int min = 100;
    int max = 0;
    for (Entry<KovatsIndex, Double> e : values.entrySet()) {
      s.append(e.getKey().getShortName() + ":" + rtFormat.format(e.getValue()));
      s.append(",");

      int c = e.getKey().getNumCarbon();
      min = Math.min(c, min);
      max = Math.max(c, min);
    }
    // set min max
    if (checkMinMaxSelectedChange) {
      ddlKovats.setActive(false);
      minc.setText(String.valueOf(min));
      maxc.setText(String.valueOf(max));
      ddlKovats.setActive(true);
      updateKovatsList();

      // set selected
      comboKovats.setItems(FXCollections.observableArrayList(values.keySet()));
    }

    // set values
    valuesComponent.setText(s.toString());
    kovatsValuesChanged();
  }

  /**
   * GNPS GC MS formatted table (comma-separated)
   *
   * @param values
   *
   * @return
   */
  private String getCsvTable(TreeMap<KovatsIndex, Double> values) {
    StringBuilder s = new StringBuilder();
    String nl = "\n";
    // header for GNPS
    // alkane name, num carbon(int), rt (seconds)
    s.append("Compound_Name,Carbon_Number,RT" + nl);
    DecimalFormat f = new DecimalFormat("0.##");

    for (Entry<KovatsIndex, Double> e : values.entrySet()) {
      s.append(e.getKey().getAlkaneName());
      s.append(",");
      s.append(String.valueOf(e.getKey().getNumCarbon()));
      s.append(",");
      // export rt in seconds for GNPS GC
      s.append(f.format(e.getValue() * 60.0));
      s.append(nl);
    }
    return s.toString();
  }

  private void updateChart() {
    updateParameterSetFromComponents();
    if (selectedDataFile == null)
      return;
    try {
      // old charts axes ranges
      org.jfree.data.Range domainZoom =
          chart == null ? null : chart.getChart().getXYPlot().getDomainAxis().getRange();
      org.jfree.data.Range rangeZoom =
          chart == null ? null : chart.getChart().getXYPlot().getRangeAxis().getRange();

      Range<Double> rangeMZ =
          parameterSet.getParameter(KovatsIndexExtractionParameters.mzRange).getValue();
      Range<Double> rangeRT =
          parameterSet.getParameter(KovatsIndexExtractionParameters.rtRange).getValue();
      if (rangeMZ == null) {
        // set range to specific alkane fragment
        rangeMZ = Range.closed(56.6, 57.5);
        ((MZRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.mzRange))
            .setValue(rangeMZ);
      }
      if (rangeRT == null) {
        rangeRT = selectedDataFile[0].getDataRTRange();
        ((RTRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.rtRange))
            .setValue(rangeRT);
      }

      // create dataset
      TICSumDataSet data =
          new TICSumDataSet(selectedDataFile, rangeRT, rangeMZ, null, TICPlotType.BASEPEAK);
      chart = new TICPlot();
      chart.addTICDataset(data);
      if (domainZoom != null)
        chart.getChart().getXYPlot().getDomainAxis().setRange(domainZoom);
      if (rangeZoom != null)
        chart.getChart().getXYPlot().getRangeAxis().setRange(rangeZoom);

      // add control for markers
      chart.getGestureAdapter().addGestureHandler(new ChartGestureDragDiffHandler(Entity.PLOT,
          GestureButton.BUTTON1, new Key[] {Key.NONE}, e -> handleMarkerDrag(e)));
      chart.getGestureAdapter().addGestureHandler(
          new ChartGestureHandler(new ChartGesture(Entity.PLOT, Event.RELEASED), e -> {
            if (chart != null)
              chart.setMouseZoomable(true);
            if (currentlyDraggedMarker != null) {
              // set value of current marker
              logger.info("Marker dragging ended at " + currentlyDraggedMarker.getValue());
              int index = markers.indexOf(currentlyDraggedMarker);
              double value = e.getCoordinates().getX();
              setValue(index, value);
              //
              currentlyDraggedMarker = null;
            }
          }));

      kovatsValuesChanged();
      pnChart.getChildren().clear();
      pnChart.setCenter(chart);
      // revalidate();
      // repaint();
    } catch (Exception e) {
      logger.log(Level.WARNING, "Peak picking parameters incorrect");
    }
  }

  /**
   * Drag marker positions
   *
   * @param e
   */
  private void handleMarkerDrag(ChartGestureDragDiffEvent e) {
    if (markers == null || markers.isEmpty())
      return;

    // dragged marker?
    if (currentlyDraggedMarker == null) {
      double start = e.getFirstEvent().getCoordinates().getX();
      double minDist = Double.MAX_VALUE;
      currentlyDraggedMarker = null;
      for (ValueMarker m : markers) {
        double d = Math.abs(m.getValue() - start);
        if (d < minDist) {
          minDist = d;
          currentlyDraggedMarker = m;
        }
      }
      if (minDist >= 0.03) {
        currentlyDraggedMarker = null;
        chart.setMouseZoomable(true);
      }
    }

    if (currentlyDraggedMarker != null) {
      chart.setMouseZoomable(false);
      currentlyDraggedMarker.setValue(e.getLatestEvent().getCoordinates().getX());
    }
  }

  @Override
  protected void updateParameterSetFromComponents() {
    super.updateParameterSetFromComponents();
    selectedKovats = parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).getValue();
    pickedValuesString =
        parameterSet.getParameter(KovatsIndexExtractionParameters.pickedKovatsValues).getValue();
    noiseLevel =
        parameterSet.getParameter(KovatsIndexExtractionParameters.noiseLevel).getValue() != null
            ? parameterSet.getParameter(KovatsIndexExtractionParameters.noiseLevel).getValue()
            : 0;
    ratioEdge =
        parameterSet.getParameter(KovatsIndexExtractionParameters.ratioTopEdge).getValue() != null
            ? parameterSet.getParameter(KovatsIndexExtractionParameters.ratioTopEdge).getValue()
            : 2;

    if (cbSecondRaw.isSelected()
        || comboDataFileName2.getSelectionModel().getSelectedItem() == null)
      selectedDataFile = new RawDataFile[] {comboDataFileName.getSelectionModel().getSelectedItem(),
          comboDataFileName2.getSelectionModel().getSelectedItem()};
    else
      selectedDataFile =
          new RawDataFile[] {comboDataFileName.getSelectionModel().getSelectedItem()};

    if (selectedDataFile != null && selectedDataFile.length > 0 && selectedDataFile[0] != null) {
      parameterSet.getParameter(KovatsIndexExtractionParameters.dataFiles)
          .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, selectedDataFile);
    }
  }

  /**
   * Update kuvats list min max
   */
  private void updateKovatsList() {
    updateParameterSetFromComponents();
    try {
      int min = parameterSet.getParameter(KovatsIndexExtractionParameters.minKovats).getValue();
      int max = parameterSet.getParameter(KovatsIndexExtractionParameters.maxKovats).getValue();
      KovatsIndex[] newValues = KovatsIndex.getRange(min, max);
      KovatsIndex[] newSelected = Stream.of(newValues)
          .filter(k -> ArrayUtils.contains(selectedKovats, k)).toArray(KovatsIndex[]::new);

      parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).setChoices(newValues);
      parameterSet.getParameter(KovatsIndexExtractionParameters.kovats).setValue(newSelected);
      CheckListView<KovatsIndex> kovatsc =
          getComponentForParameter(KovatsIndexExtractionParameters.kovats);
      ObservableList<KovatsIndex> choicesList =
          FXCollections.observableArrayList(Arrays.asList(newValues));

      kovatsc.setItems(choicesList);
      kovatsc.getSelectionModel().clearSelection();
      for (KovatsIndex i : newSelected) {
        kovatsc.getSelectionModel().select(i);
      }
      // revalidate();
      // repaint();
      handleKovatsSelectionChange();
      // update parameters again
      updateParameterSetFromComponents();
    } catch (Exception e) {
    }
  }

  /**
   * Kovats list selection has changed
   */
  private void handleKovatsSelectionChange() {
    updateParameterSetFromComponents();
    // keep rt values
    StringBuilder s = new StringBuilder();
    int i = 0;
    double lastRT = 0;
    for (KovatsIndex ki : selectedKovats) {
      Double rt = lastRT + 1;
      if (parsedValues != null)
        rt = parsedValues.getOrDefault(ki, rt);

      s.append(ki.name() + ":" + rtFormat.format(rt) + ",");
      i++;
      lastRT = rt;
    }
    valuesComponent.setText(s.toString());
    kovatsValuesChanged();
  }

  /**
   * Peak picking to define Kovats index retention times
   */
  private void pickRetentionTimes() {
    updateParameterSetFromComponents();
    XYDataset data = getData();
    if (data == null)
      return;

    int items = data.getItemCount(0);
    List<Double> results = new ArrayList<>();

    // auto set noiselevel to max/20 if 0
    if (Double.compare(noiseLevel, 0d) <= 0) {
      for (int i = 0; i < items; i++) {
        double intensity = data.getYValue(0, i);
        noiseLevel = Math.max(noiseLevel, intensity);
      }
      noiseLevel = noiseLevel / 20.0;
    }

    double max = 0;
    double startI = Double.MAX_VALUE;
    double maxRT = 0;

    for (int i = 0; i < items; i++) {
      double rt = data.getXValue(0, i);
      double intensity = data.getYValue(0, i);

      // find max
      if (intensity > max && intensity >= noiseLevel) {
        max = intensity;
        maxRT = rt;
      }

      // minimize startI above noiseLevel
      if (intensity >= noiseLevel && (intensity < startI)) {
        startI = intensity;
        max = 0;
        maxRT = rt;
      }

      // is peak?
      if (max / startI > ratioEdge && max / intensity > ratioEdge) {
        // add peak
        results.add(maxRT);

        max = 0;
        startI = intensity;
        maxRT = rt;
      }
    }

    // set rt values
    StringBuilder s = new StringBuilder();
    int i = 0;
    double lastRT = 1;
    for (KovatsIndex ki : selectedKovats) {
      double rt = i < results.size() ? results.get(i) : lastRT + 1;
      s.append(ki.name() + ":" + rtFormat.format(rt) + ",");
      i++;
      lastRT = rt;
    }
    valuesComponent.setText(s.toString());
    kovatsValuesChanged();
  }

  private XYDataset getData() {
    return chart == null ? null : chart.getXYPlot().getDataset();
  }

  /**
   *
   * @return number of loaded files
   */
  private int loadFile() {
    File lastFile =
        parameterSet.getParameter(KovatsIndexExtractionParameters.lastSavedFile).getValue();
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(csvFilter);
    if (lastFile != null)
      chooser.setInitialFileName(lastFile.getAbsolutePath());

    List<File> f = chooser.showOpenMultipleDialog(this.getScene().getWindow());

    if (f == null)
      return 0;

    TreeMap<KovatsIndex, Double> values = new TreeMap<>();
    // combine all
    for (File cf : f) {
      try {
        List<String> lines = Files.readLines(cf, StandardCharsets.UTF_8);

        for (String s : lines) {
          String[] value = s.split(",");
          try {
            double time = Double.parseDouble(value[1]);
            KovatsIndex ki = KovatsIndex.getByString(value[0]);
            // average if already inserted
            if (values.get(ki) != null) {
              time = (time + values.get(ki)) / 2.0;
              values.put(ki, time);
            } else {
              values.put(ki, time);
            }
          } catch (Exception e) {
            // this try catch only identifies value columns
          }
        }
        // set last file
        setLastFile(cf);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Cannot read lines of " + cf.getAbsolutePath(), e);
      }
    }
    // all files are combined
    // to values component
    setValues(values);
    return f.size();

  }

  private synchronized void saveToFile() {
    // need to parse
    if (!parseValues()) {
      logger.log(Level.WARNING,
          "Parsing of Kovats values failed (text box). Maybe you have to select more markers: "
              + MIN_MARKERS + " (at least)");
      return;
    }
    final TreeMap<KovatsIndex, Double> values = parsedValues;

    File lastFile =
        parameterSet.getParameter(KovatsIndexExtractionParameters.lastSavedFile).getValue();

    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(csvFilter);
    if (lastFile != null)
      chooser.setInitialFileName(lastFile.getAbsolutePath());

    File f = chooser.showSaveDialog(this.getScene().getWindow());

    if (f == null)
      return;

    // set last file
    setLastFile(f);
    f = FileAndPathUtil.getRealFilePath(f, "csv");
    try {
      // save to file in GNPS GC format
      String exp = getCsvTable(values);
      if (TxtWriter.write(exp, f, false))
        saveFileListener.accept(f);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error while saving Kovats file to " + f.getAbsolutePath(), e);
    }

  }

  /**
   *
   */
  private void combineFiles() {
    // load success?
    if (loadFile() > 1) {
      // save file
      saveToFile();
    } else {
      DialogLoggerUtil.showMessageDialogForTime("Select multiple files",
          "Please select multiple files for combination", 3500);
    }
  }

}
