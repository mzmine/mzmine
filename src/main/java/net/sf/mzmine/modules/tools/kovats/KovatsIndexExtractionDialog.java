/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.tools.kovats;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.Window;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.gestures.ChartGesture;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Button;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Event;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Key;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffEvent;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.tools.kovats.KovatsValues.KovatsIndex;
import net.sf.mzmine.modules.tools.mzrangecalculator.MzRangeFormulaCalculatorModule;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICSumDataSet;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.DoubleComponent;
import net.sf.mzmine.parameters.parametertypes.IntegerComponent;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.StringComponent;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameComponent;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeComponent;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeComponent;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.ColorPalettes;
import net.sf.mzmine.util.DialogLoggerUtil;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.io.TxtWriter;

public class KovatsIndexExtractionDialog extends ParameterSetupDialog {
  private static final long serialVersionUID = 1L;
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private NumberFormat rtFormat = new DecimalFormat("0.###");
  private static final Stroke markerStroke = new BasicStroke(1.5f);
  public static final int MIN_MARKERS = 3;
  private static final Icon iconNext = createIcon("icons/btnNext.png");
  private static final Icon iconPrev = createIcon("icons/btnPrev.png");

  // button size < >
  private static final int SIZE = 25;

  private Window parent;
  private JPanel newMainPanel;
  private JPanel pnChart;
  private KovatsIndex[] selectedKovats;

  // accepts saved files
  private Consumer<File> saveFileListener;
  private JTextField txtPeakPick;
  private StringComponent valuesComponent;
  private TICPlot chart;


  private String pickedValuesString;
  private TreeMap<KovatsIndex, Double> parsedValues;
  private double noiseLevel = 0;
  private double ratioEdge = 2;
  private RawDataFile[] dataFiles;
  private JComboBox<RawDataFile> comboDataFileName;
  private JComboBox<RawDataFile> comboDataFileName2;
  private RawDataFile[] selectedDataFile;
  private IntegerComponent minc;
  private IntegerComponent maxc;
  private DelayedDocumentListener ddlKovats;
  private MultiChoiceComponent comboKovats;
  private List<ValueMarker> markers;
  private ValueMarker currentlyDraggedMarker;
  private JCheckBox cbSecondRaw;
  private JCheckBox cbCurrentAlkaneSubH;
  private JLabel lbCurrentAlkane;
  // for direct selection of mz by alkane buttons
  private KovatsIndex currentAlkane;

  /**
   * 
   * @param parent
   * @param parameters
   * @param saveFileListener accepts saved files
   */
  public KovatsIndexExtractionDialog(Window parent, ParameterSet parameters,
      Consumer<File> saveFileListener) {
    this(parent, parameters);
    this.saveFileListener = saveFileListener;
  }

  public KovatsIndexExtractionDialog(Window parent, ParameterSet parameters) {
    super(parent, false, parameters);
    this.parent = parent;
  }



  @Override
  protected void addDialogComponents() {
    super.addDialogComponents();
    mainPanel.removeAll();
    mainPanel.getParent().remove(mainPanel);

    ddlKovats = new DelayedDocumentListener(e -> updateKovatsList());
    DelayedDocumentListener ddlUpdateChart = new DelayedDocumentListener(e -> updateChart());

    newMainPanel = new JPanel(new MigLayout("fill", "[right][grow,fill]", ""));
    getContentPane().add(newMainPanel, BorderLayout.SOUTH);

    JPanel pnCenter = new JPanel(new BorderLayout());
    getContentPane().add(pnCenter, BorderLayout.CENTER);
    pnChart = new JPanel(new BorderLayout());
    pnCenter.add(pnChart, BorderLayout.CENTER);

    Box sizedummy = new Box(BoxLayout.X_AXIS);
    sizedummy.setMinimumSize(new Dimension(200, 450));
    sizedummy.setPreferredSize(new Dimension(200, 450));
    pnChart.add(sizedummy, BorderLayout.CENTER);

    // left: Kovats: min max and list
    JPanel west = new JPanel(new BorderLayout());
    newMainPanel.add(west);

    // add min max
    JPanel pnKovatsParam = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    west.add(pnKovatsParam, BorderLayout.NORTH);
    minc = (IntegerComponent) getComponentForParameter(KovatsIndexExtractionParameters.minKovats);
    maxc = (IntegerComponent) getComponentForParameter(KovatsIndexExtractionParameters.maxKovats);
    minc.addDocumentListener(ddlKovats);
    maxc.addDocumentListener(ddlKovats);

    pnKovatsParam.add(new JLabel("Min carbon:"));
    pnKovatsParam.add(minc);
    pnKovatsParam.add(new JLabel("Max carbon:"));
    pnKovatsParam.add(maxc);

    // kovats list
    JPanel pnKovatsSelect = new JPanel(new BorderLayout());
    west.add(pnKovatsSelect, BorderLayout.CENTER);
    comboKovats =
        (MultiChoiceComponent) getComponentForParameter(KovatsIndexExtractionParameters.kovats);
    comboKovats.addValueChangeListener(() -> handleKovatsSelectionChange());
    pnKovatsSelect.add(comboKovats, BorderLayout.CENTER);

    // center: Chart and parameters
    JPanel center = new JPanel(new BorderLayout());
    newMainPanel.add(center);

    // all parameters on peak pick panel
    JPanel pnSouth = new JPanel(new BorderLayout());
    center.add(pnSouth, BorderLayout.SOUTH);

    JPanel pnPeakPick = new JPanel(new MigLayout("", "[right][]", ""));
    pnSouth.add(pnPeakPick, BorderLayout.CENTER);

    valuesComponent = (StringComponent) getComponentForParameter(
        KovatsIndexExtractionParameters.pickedKovatsValues);
    MZRangeComponent mzc =
        (MZRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.mzRange);
    RTRangeComponent rtc =
        (RTRangeComponent) getComponentForParameter(KovatsIndexExtractionParameters.rtRange);
    DoubleComponent noisec =
        (DoubleComponent) getComponentForParameter(KovatsIndexExtractionParameters.noiseLevel);
    DoubleComponent edgeRatioC =
        (DoubleComponent) getComponentForParameter(KovatsIndexExtractionParameters.ratioTopEdge);

    valuesComponent.addDocumentListener(new DelayedDocumentListener(e -> kovatsValuesChanged()));
    valuesComponent.setLayout(new GridLayout(1, 1));
    pnCenter.add(valuesComponent, BorderLayout.SOUTH);

    JPanel pnButtonFlow = new JPanel();
    pnPeakPick.add(pnButtonFlow, "cell 0 0 2 1");
    JButton btnUpdateChart = new JButton("Update chart");
    btnUpdateChart.addActionListener(e -> updateChart());
    pnButtonFlow.add(btnUpdateChart);
    JButton btnPickRT = new JButton("Pick peaks");
    btnPickRT.addActionListener(e -> pickRetentionTimes());
    pnButtonFlow.add(btnPickRT);
    JButton btnSaveFile = new JButton("Save to file");
    btnSaveFile.setToolTipText("Save Kovats index file");
    btnSaveFile.addActionListener(e -> saveToFile());
    pnButtonFlow.add(btnSaveFile);
    JButton btnLoad = new JButton("Load");
    btnLoad.setToolTipText("Load Kovats index file");
    btnLoad.addActionListener(e -> loadFile());
    pnButtonFlow.add(btnLoad);
    JButton btnCombineFiles = new JButton("Combine files");
    btnCombineFiles.setToolTipText("Select multiple Kovats index files to be combined into one");
    btnCombineFiles.addActionListener(e -> combineFiles());
    pnButtonFlow.add(btnCombineFiles);

    // add combo for raw data file
    dataFiles = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();
    comboDataFileName = new JComboBox<RawDataFile>(dataFiles);
    comboDataFileName2 = new JComboBox<RawDataFile>(dataFiles);
    cbSecondRaw = new JCheckBox();
    initRawDataFileSelection();

    pnPeakPick.add(new JLabel("Raw data file(s)"), "cell 0 1");
    pnPeakPick.add(comboDataFileName);
    cbSecondRaw.addItemListener(e -> useSecondDataFile(cbSecondRaw.isSelected()));
    pnPeakPick.add(cbSecondRaw, "cell 0 2");
    pnPeakPick.add(comboDataFileName2);
    // direct alkane selection < CxH2x+1 >
    JPanel pnAlkaneSelect = new JPanel();
    Dimension dim = new Dimension(SIZE, SIZE);
    JButton btnPrevAlkane = new JButton(iconPrev);
    btnPrevAlkane.addActionListener(e -> setMzRangeByAlkane(-1));
    btnPrevAlkane.setPreferredSize(dim);
    btnPrevAlkane.setMaximumSize(dim);
    JButton btnNextAlkane = new JButton(iconNext);
    btnNextAlkane.addActionListener(e -> setMzRangeByAlkane(1));
    btnNextAlkane.setPreferredSize(dim);
    btnNextAlkane.setMaximumSize(dim);

    lbCurrentAlkane = new JLabel("");
    cbCurrentAlkaneSubH = new JCheckBox("-H");
    cbCurrentAlkaneSubH.setSelected(true);
    cbCurrentAlkaneSubH.addItemListener(e -> setMzRangeByAlkane(0));

    pnAlkaneSelect.add(btnPrevAlkane);
    pnAlkaneSelect.add(lbCurrentAlkane);
    pnAlkaneSelect.add(btnNextAlkane);
    pnAlkaneSelect.add(cbCurrentAlkaneSubH);
    pnPeakPick.add(pnAlkaneSelect, "cell 1 3");


    pnPeakPick.add(new JLabel("m/z range"), "cell 0 4");
    pnPeakPick.add(mzc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.rtRange.getName()), "cell 0 5");
    pnPeakPick.add(rtc);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.noiseLevel.getName()), "cell 0 6");
    pnPeakPick.add(noisec);
    pnPeakPick.add(new JLabel(KovatsIndexExtractionParameters.ratioTopEdge.getName()), "cell 0 7");
    pnPeakPick.add(edgeRatioC);

    // add listeners
    comboDataFileName.addItemListener(e -> updateChart());
    mzc.addDocumentListener(ddlUpdateChart);
    rtc.addDocumentListener(ddlUpdateChart);

    // show
    revalidate();
    updateMinimumSize();
    pack();
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
    if (dataFiles != null && dataFiles.length <= 0)
      return;

    RawDataFilesSelection select =
        parameterSet.getParameter(KovatsIndexExtractionParameters.dataFiles).getValue();
    RawDataFile[] raw = null;
    // set to parameters files - if they exist in this project
    if (select != null && select.getMatchingRawDataFiles().length > 0) {
      RawDataFile[] exists = Arrays.stream(select.getMatchingRawDataFiles())
          .filter(r -> Arrays.stream(dataFiles).anyMatch(d -> r.getName().equals(d.getName())))
          .toArray(RawDataFile[]::new);
      if (exists.length > 0)
        raw = exists;
    }

    if (raw == null) {
      // find kovats or dro file
      // first use all kovats named files and then dro (max 2)
      RawDataFile[] kovats = Arrays.stream(dataFiles)
          .filter(d -> d.getName().toLowerCase().contains("kovats")).toArray(RawDataFile[]::new);
      RawDataFile[] dro = Arrays.stream(dataFiles)
          .filter(d -> d.getName().toLowerCase().contains("dro")).toArray(RawDataFile[]::new);

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
      comboDataFileName.setSelectedItem(selectedDataFile[0]);
      if (raw.length > 1) {
        comboDataFileName2.setSelectedItem(selectedDataFile[1]);
      }
    }
  }

  private void setLastFile(File f) {
    ((FileNameComponent) getComponentForParameter(KovatsIndexExtractionParameters.lastSavedFile))
        .setValue(f);
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
            ColorPalettes.getPositiveColor(MZmineCore.getConfiguration().getColorVision()),
            markerStroke);
        marker.setLabelOffset(new RectangleInsets(10, 0, 0, 0));
        marker.setLabelFont(new Font("Arial", Font.PLAIN, 12));
        marker.setLabelBackgroundColor(Color.WHITE);
        marker.setLabel(e.getKey().getShortName());
        chart.getChart().getXYPlot().addDomainMarker(marker);
        markers.add(marker);
      }

      revalidate();
      repaint();
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
      comboKovats.setValue(values.keySet().toArray(KovatsIndex[]::new));
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
      chart = new TICPlot(this);
      chart.addTICDataset(data);
      if (domainZoom != null)
        chart.getChart().getXYPlot().getDomainAxis().setRange(domainZoom);
      if (rangeZoom != null)
        chart.getChart().getXYPlot().getRangeAxis().setRange(rangeZoom);

      // add control for markers
      chart.getGestureAdapter().addGestureHandler(new ChartGestureDragDiffHandler(Entity.PLOT,
          Button.BUTTON1, new Key[] {Key.NONE}, e -> handleMarkerDrag(e)));
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
      pnChart.removeAll();
      pnChart.add(chart, BorderLayout.CENTER);
      revalidate();
      repaint();
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


    if (cbSecondRaw.isSelected() || comboDataFileName2.getSelectedItem() == null)
      selectedDataFile = new RawDataFile[] {(RawDataFile) comboDataFileName.getSelectedItem(),
          (RawDataFile) comboDataFileName2.getSelectedItem()};
    else
      selectedDataFile = new RawDataFile[] {(RawDataFile) comboDataFileName.getSelectedItem()};

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
      MultiChoiceComponent kovatsc =
          (MultiChoiceComponent) getComponentForParameter(KovatsIndexExtractionParameters.kovats);
      kovatsc.setChoices(newValues);
      kovatsc.setValue(newSelected);
      revalidate();
      repaint();
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
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter ff = new FileNameExtensionFilter("Comma-separated values", "csv");
    chooser.addChoosableFileFilter(ff);
    chooser.setFileFilter(ff);
    if (lastFile != null)
      chooser.setSelectedFile(lastFile);

    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(true);

    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      File[] f = chooser.getSelectedFiles();

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
      return f.length;
    }
    return 0;
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
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter ff = new FileNameExtensionFilter("Comma-separated values", "csv");
    chooser.addChoosableFileFilter(ff);
    chooser.setFileFilter(ff);
    if (lastFile != null)
      chooser.setSelectedFile(lastFile);

    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
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
      DialogLoggerUtil.showMessageDialogForTime(MZmineCore.getDesktop().getMainWindow(),
          "Select multiple files", "Please select multiple files for combination", 3500);
    }
  }


  private static Icon createIcon(String path) {
    return new ImageIcon(
        new ImageIcon(path).getImage().getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH));
  }
}
