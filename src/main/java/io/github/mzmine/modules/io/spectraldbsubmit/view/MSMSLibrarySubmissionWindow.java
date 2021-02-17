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
package io.github.mzmine.modules.io.spectraldbsubmit.view;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.spectraldbsubmit.LibrarySubmitModule;
import io.github.mzmine.modules.io.spectraldbsubmit.LibrarySubmitTask;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.param.LibrarySubmitParameters;
import io.github.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.components.GridBagPanel;
import io.github.mzmine.util.scans.sorting.ScanSortMode;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import net.miginfocom.swing.MigLayout;

/**
 * Holds more charts for data reviewing
 *
 * @author Robin Schmid
 *
 */
public class MSMSLibrarySubmissionWindow extends JFrame implements ActionListener {

  private Logger log = Logger.getLogger(this.getClass().getName());
  protected Map<String, Node> parametersAndComponents;
  protected final LibrarySubmitParameters paramSubmit;
  protected final LibraryMetaDataParameters paramMeta = new LibraryMetaDataParameters();

  protected final URL helpURL;
  protected HelpWindow helpWindow = null;
  private JButton helpButton;

  // annotations for MSMS
  private List<AbstractMSMSIdentity> msmsAnnotations;
  // to flag annotations in spectra

  private boolean exchangeTolerance = true;
  private MZTolerance mzTolerance = new MZTolerance(0.0015, 2.5d);

  // MS 2
  private ChartGroup group;
  //
  private JPanel contentPane;
  private JPanel pnCharts;
  private boolean showTitle = true;
  private boolean showLegend = false;
  // click marker in all of the group
  private boolean showCrosshair = true;
  private JPanel pnSideMenu;

  private JPanel pnSettings;
  private JPanel pnButtons;

  private JLabel lblSettings;
  private JScrollPane scrollCharts;
  private final Color errorColor = Color.decode("#ff8080");
  private ScanSelectPanel[] pnScanSelect;

  private JScrollPane scrollMeta;
  private GridBagPanel pnMetaData;
  private GridBagPanel pnSubmitParam;
  private String helpID;

  //
  private boolean isFragmentScan = true;
  // data either rows or list of entries with 1 or multiple scans
  private FeatureListRow[] rows;
  private ObservableList<ObservableList<Scan>> scanList;
  private ResultsTextPane txtResults;

  /**
   * Create the frame.
   */
  public MSMSLibrarySubmissionWindow() {
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setBounds(100, 100, 854, 619);

    JPanel main = new JPanel(new BorderLayout());
    JSplitPane split = new JSplitPane();
    split.setOrientation(JSplitPane.VERTICAL_SPLIT);
    main.add(split);
    setContentPane(main);

    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    split.setLeftComponent(contentPane);

    txtResults = new ResultsTextPane();
    txtResults.setEditable(false);
    JScrollPane scrollResults = new JScrollPane(txtResults);
    scrollResults.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    split.setRightComponent(scrollResults);
    split.setResizeWeight(0.92);

    // load parameter
    paramSubmit = (LibrarySubmitParameters) MZmineCore.getConfiguration()
        .getModuleParameters(LibrarySubmitModule.class);

    pnSideMenu = new JPanel();
    contentPane.add(pnSideMenu, BorderLayout.EAST);
    pnSideMenu.setLayout(new BorderLayout(0, 0));

    pnSettings = new JPanel();
    pnSideMenu.add(pnSettings, BorderLayout.NORTH);
    pnSettings.setLayout(new MigLayout("", "[][grow]", "[][][][][]"));

    lblSettings = new JLabel("Masslist selection and preprocessing");
    lblSettings.setFont(new Font("Tahoma", Font.BOLD, 11));
    pnSettings.add(lblSettings, "cell 0 0");

    // buttons
    pnButtons = new JPanel();
    pnSideMenu.add(pnButtons, BorderLayout.SOUTH);

    this.helpURL = paramSubmit.getClass().getResource("help/help.html");

    if (helpURL != null) {
      helpButton = GUIUtils.addButton(pnButtons, "Help", null, this);
    }

    JButton btnCheck = new JButton("Check");
    btnCheck.addActionListener(e -> {
      if (checkParameters())
        DialogLoggerUtil.showMessageDialogForTime("Check OK", "All parameters are set", 1500);
    });
    pnButtons.add(btnCheck);

    JButton btnSubmit = new JButton("Submit");
    btnSubmit.addActionListener(e -> submitSpectra());
    pnButtons.add(btnSubmit);

    parametersAndComponents = new Hashtable<>();
    createSubmitParamPanel();
    createMetaDataPanel();

    // add listener
    addListener();

    scrollCharts = new JScrollPane();
    contentPane.add(scrollCharts, BorderLayout.CENTER);

    pnCharts = new JPanel();
    pnCharts.setLayout(new GridLayout(0, 1));
    scrollCharts.setViewportView(pnCharts);

    addMenu();
  }

  private void addListener() {
    // mc.addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));
    DoubleComponent nc = getNoiseLevelComponent();
    // nc.addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));
    IntegerComponent minc = getMinSignalComponent();
    // minc.addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));
    ComboBox<ScanSortMode> sortc = getComboSortMode();
    // sortc.addItemListener(e -> updateSortModeOnAllSelectors());

    IntegerComponent mslevel = getMSLevelComponent();
    // mslevel.addDocumentListener(new DelayedDocumentListener(e -> {
    // updateParameterSetFromComponents();
    // Integer level = paramMeta.getParameter(LibraryMetaDataParameters.MS_LEVEL).getValue();
    // setFragmentScan(level != null && level > 1);
    // }));
  }

  /**
   * Sort rows
   *
   * @param rows
   * @param sorting
   * @param direction
   */
  public void setData(FeatureListRow[] rows, SortingProperty sorting, SortingDirection direction,
      boolean isFragmentScan) {
    Arrays.sort(rows, new FeatureListRowSorter(sorting, direction));
    setData(rows, isFragmentScan);
  }

  /**
   * Create charts and show
   *
   * @param rows
   */
  public void setData(FeatureListRow[] rows, boolean isFragmentScan) {
    getMSLevelComponent().setText(isFragmentScan ? "2" : "1");
    scanList = null;
    this.rows = rows;
    this.pnScanSelect = new ScanSelectPanel[rows.length];

    setFragmentScan(isFragmentScan);
    // set rt
    double rt = Arrays.stream(rows).mapToDouble(FeatureListRow::getAverageRT).average().orElse(-1);
    setRetentionTimeToComponent(rt);
    updateAllChartSelectors();
  }

  /**
   * Set data as single scan
   *
   * @param scan
   */
  public void setData(Scan scan) {
    ObservableList<ObservableList<Scan>> newScanList = FXCollections.observableArrayList();
    newScanList.add(FXCollections.observableArrayList(scan));
    setData(newScanList);
  }

  /**
   * set data as set of scans of one entry
   *
   * @param scans
   */
  // remove? clashes with public void setData(ObservableList<ObservableList<Scan>> scanList)
  /*
  public void setData(ObservableList<Scan> scans) {
    scanList = FXCollections.observableArrayList();
    scanList.add(scans);
    setData(scanList);
  }
  */

  /**
   * Set data as set of entries with 1 or multiple scans
   *
   * @param scanList
   */
  public void setData(ObservableList<ObservableList<Scan>> scanList) {
    this.scanList = scanList;
    rows = null;
    this.pnScanSelect = new ScanSelectPanel[scanList.size()];

    double rt = scanList.stream().flatMap(ObservableList::stream).mapToDouble(Scan::getRetentionTime)
        .average().orElse(0d);

    // any scan matches MS level 1? --> set level to ms1
    int minMSLevel =
        scanList.stream().flatMap(ObservableList::stream).mapToInt(Scan::getMSLevel).min().orElse(1);
    getMSLevelComponent().setText(minMSLevel < 2 ? "1" : "" + minMSLevel);
    setFragmentScan(minMSLevel > 1);

    // set rt
    setRetentionTimeToComponent(rt);
    updateAllChartSelectors();
  }

  /**
   * Set whether this is a fragment scan or MS1 and enables some user parameters (e.g., precursor
   * mz, adduct, charge, GNPS submission)
   *
   * @param isFragmentScan
   */
  public void setFragmentScan(boolean isFragmentScan) {
    this.isFragmentScan = isFragmentScan;
    streamSelection().forEach(pn -> pn.setFragmentScan(isFragmentScan));
    // disable gnps
    if (!isFragmentScan) {
      paramSubmit.getParameter(LibrarySubmitParameters.SUBMIT_GNPS).setValue(false);
      getGnpsSubmitComponent().setSelected(false);
    }
    getGnpsSubmitComponent().setDisable(!isFragmentScan);
  }

  private void createSubmitParamPanel() {
    // Main panel which holds all the components in a grid
    pnSubmitParam = new GridBagPanel();
    pnSideMenu.add(pnSubmitParam, BorderLayout.NORTH);

    int rowCounter = 0;
    // Create labels and components for each parameter
    for (Parameter p : paramSubmit.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;

      Node comp = up.createEditingComponent();
      // comp.setToolTip(new Tooltip(up.getDescription()));

      // Set the initial value
      Object value = up.getValue();
      if (value != null)
        up.setValueToComponent(comp, value);

      // By calling this we make sure the components will never be resized
      // smaller than their optimal size
      // comp.setMinimumSize(comp.getPreferredSize());

      // add separator
      if (p.getName().equals(LibrarySubmitParameters.LOCALFILE.getName())) {
        pnSubmitParam.addSeparator(0, rowCounter, 2);
        rowCounter++;
      }

      Label label = new Label(p.getName());
      // pnSubmitParam.add(label, 0, rowCounter);
      // label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      ComboBox t = new ComboBox();
      // int comboh = t.getPreferredSize().height;
      // int comph = comp.getPreferredSize().height;

      // int verticalWeight = comph > 2 * comboh ? 1 : 0;
      // pnSubmitParam.add(comp, 1, rowCounter);
      rowCounter++;
    }
  }

  private void createMetaDataPanel() {
    // Main panel which holds all the components in a grid
    pnMetaData = new GridBagPanel();
    scrollMeta = new JScrollPane(pnMetaData);
    scrollMeta.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollMeta.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pnSideMenu.add(scrollMeta, BorderLayout.CENTER);

    int rowCounter = 0;
    int vertWeightSum = 0;

    // Create labels and components for each parameter
    for (Parameter p : paramMeta.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;

      Node comp = up.createEditingComponent();
      // comp.setToolTipText(up.getDescription());

      // Set the initial value
      Object value = up.getValue();
      if (value != null)
        up.setValueToComponent(comp, value);

      // By calling this we make sure the components will never be resized
      // smaller than their optimal size
      // comp.setMinimumSize(comp.getPreferredSize());

      JLabel label = new JLabel(p.getName());
      pnMetaData.add(label, 0, rowCounter);
      // label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      JComboBox t = new JComboBox();
      // int comboh = t.getPreferredSize().height;
      // int comph = comp.getPreferredSize().height;

      // Multiple selection will be expandable, other components not
      // int verticalWeight = comph > 2 * comboh ? 1 : 0;
      // vertWeightSum += verticalWeight;

      // pnMetaData.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight, GridBagConstraints.VERTICAL);
      rowCounter++;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void updateParameterSetFromComponents() {
    for (Parameter<?> p : paramSubmit.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      Node component = parametersAndComponents.get(p.getName());
      up.setValueFromComponent(component);
    }
    for (Parameter<?> p : paramMeta.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      Node component = parametersAndComponents.get(p.getName());
      up.setValueFromComponent(component);
    }
  }

  protected int getNumberOfParameters() {
    return paramSubmit.getParameters().length + paramMeta.getParameters().length;
  }

  protected boolean checkParameters() {
    // commit the changes to the parameter set
    updateParameterSetFromComponents();

    // check
    ArrayList<String> messages = new ArrayList<>();

    boolean checkIon = streamSelection().filter(ScanSelectPanel::isValidAndSelected)
        .filter(pn -> !pn.checkParameterValues(messages)).count() == 0;
    boolean checkSubmit = paramSubmit.checkParameterValues(messages);
    boolean checkMeta = paramMeta.checkParameterValues(messages);
    if (checkMeta && checkSubmit && checkIon) {
      return true;
    } else {
      String message = messages.stream().collect(Collectors.joining("\n"));
      MZmineCore.getDesktop().displayMessage(null, message);
      return false;
    }
  }

  private Stream<ScanSelectPanel> streamSelection() {
    if (pnScanSelect == null)
      return Stream.empty();
    return Arrays.stream(pnScanSelect).filter(Objects::nonNull);
  }

  private <ValType, Comp extends Node> Comp getComponentForParameter(
      UserParameter<ValType, Comp> p) {
    return (Comp) parametersAndComponents.get(p.getName());
  }

  /**
   * Submit. Checks parameters and adduct for each selected ion
   */
  private void submitSpectra() {
    if (checkParameters()) {
      // check number of ions
      int ions = countSelectedIons();
      // for ms level >1 (fragmentation scan MS/MS)
      int mslevel = paramMeta.getParameter(LibraryMetaDataParameters.MS_LEVEL).getValue();
      if (mslevel == 1)
        paramSubmit.getParameter(LibrarySubmitParameters.SUBMIT_GNPS).setValue(false);
      else {
        int adducts = countSelectedAdducts();
        // every valid selected ion needs an adduct for MS2
        if (ions != adducts) {
          MZmineCore.getDesktop()
              .displayErrorMessage(MessageFormat.format(
                  "Not all adducts are set: {0} ion spectra selected and only {1}  adducts set",
                  ions, adducts));
          return;
        }
      }

      //
      if (ions == 0) {
        log.info("No MS/MS spectrum selected or valid");
        DialogLoggerUtil.showMessageDialogForTime("Error", "No MS/MS spectrum selected or valid",
            1500);
      } else {
        String message = ions + " MS/MS spectra were selected. Submit?";
        if (mslevel > 1) {
          message +=
              " (" + streamSelection().filter(pn -> pn.isValidAndSelected() && pn.hasAdduct())
                  .map(ScanSelectPanel::getAdduct).collect(Collectors.joining(", ")) + ")";
        } else {
          message += " (MS1)";
        }
        // show accept dialog
        if (DialogLoggerUtil.showDialogYesNo("Submission?", message)) {
          // create library / submit to GNPS
          HashMap<LibrarySubmitIonParameters, DataPoint[]> map = new HashMap<>(ions);
          for (ScanSelectPanel ion : pnScanSelect) {
            if (ion.isValidAndSelected()) {
              // create ion param
              LibrarySubmitIonParameters ionParam =
                  createIonParameters(paramSubmit, paramMeta, ion);
              DataPoint[] dps = ion.getFilteredDataPoints();

              // submit and save locally
              map.put(ionParam, dps);
            }
          }
          // start task
          log.info(
              "Added task to export library entries: " + ions + " MS/MS spectra were selected");
          LibrarySubmitTask task = new LibrarySubmitTask(this, map);
          MZmineCore.getTaskController().addTask(task);
        }
      }
    }
  }

  public int countSelectedIons() {
    return (int) streamSelection().filter(ScanSelectPanel::isValidAndSelected).count();
  }

  public int countSelectedAdducts() {
    return (int) streamSelection().filter(pn -> pn.isValidAndSelected() && pn.hasAdduct()).count();
  }

  /**
   * The full set of parameters for the submission/creation of one library entry
   *
   * @param paramSubmit
   * @param paramMeta
   * @param ion
   * @return
   */
  private LibrarySubmitIonParameters createIonParameters(LibrarySubmitParameters paramSubmit,
      LibraryMetaDataParameters paramMeta, ScanSelectPanel ion) {
    LibrarySubmitIonParameters ionParam = new LibrarySubmitIonParameters();
    ionParam.getParameter(LibrarySubmitIonParameters.META_PARAM).setValue(paramMeta);
    ionParam.getParameter(LibrarySubmitIonParameters.SUBMIT_PARAM).setValue(paramSubmit);
    if (isFragmentScan) {
      String adduct = ion.getAdduct();
      double precursorMZ = ion.getPrecursorMZ();
      int charge = ion.getPrecursorCharge();
      ionParam.getParameter(LibrarySubmitIonParameters.ADDUCT)
          .setValue(adduct == null || adduct.isEmpty() ? null : adduct);
      ionParam.getParameter(LibrarySubmitIonParameters.CHARGE)
          .setValue(charge == 0 ? null : charge);
      ionParam.getParameter(LibrarySubmitIonParameters.MZ)
          .setValue(precursorMZ == 0d ? null : precursorMZ);
    } else {
      // MS1
      ionParam.getParameter(LibrarySubmitIonParameters.ADDUCT).setValue(null);
      ionParam.getParameter(LibrarySubmitIonParameters.CHARGE).setValue(null);
      ionParam.getParameter(LibrarySubmitIonParameters.MZ).setValue(null);
    }
    return (LibrarySubmitIonParameters) ionParam.cloneParameterSet();
  }

  private void updateSortModeOnAllSelectors() {
    ScanSortMode sort = getComboSortMode().getSelectionModel().getSelectedItem();
    if (pnScanSelect != null)
      for (ScanSelectPanel pn : pnScanSelect)
        pn.setSortMode(sort);
  }

  private void updateSettingsOnAllSelectors() {
    if (checkInput()) {
      Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
      Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
      if (pnScanSelect != null && minSignals != null && noiseLevel != null)
        for (ScanSelectPanel pn : pnScanSelect)
          pn.setFilter(noiseLevel, minSignals);
    }
  }

  private void addMenu() {
    JMenuBar menu = new JMenuBar();
    JMenu settings = new JMenu("Settings");
    menu.add(settings);
    JFrame thisframe = this;

    // reset zoom
    JMenuItem resetZoom = new JMenuItem("reset zoom");
    menu.add(resetZoom);
    resetZoom.addActionListener(e -> {
      if (group != null)
        group.resetZoom();
    });

    // JMenuItem setSize = new JMenuItem("chart size");
    // menu.add(setSize);
    // setSize.addActionListener(e -> {
    // Dimension dim = SizeSelectDialog.getSizeInput();
    // if (dim != null)
    // setChartSize(dim);
    // });

    //
    addCheckBox(settings, "show legend", showLegend,
        e -> setShowLegend(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show title", showTitle,
        e -> setShowTitle(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show crosshair", showCrosshair,
        e -> setShowCrosshair(((JCheckBoxMenuItem) e.getSource()).isSelected()));;

    this.setJMenuBar(menu);
  }

  private void setChartSize(Dimension dim) {
    if (pnScanSelect != null && dim != null)
      for (ScanSelectPanel pn : pnScanSelect)
        pn.setChartSize(dim);
  }

  public void setShowCrosshair(boolean showCrosshair) {
    this.showCrosshair = showCrosshair;
    if (group != null)
      group.setShowCrosshair(showCrosshair, showCrosshair);
  }

  public void setShowLegend(boolean showLegend) {
    this.showLegend = showLegend;
    forAllCharts(c -> c.getLegend().setVisible(showLegend));
  }

  public void setShowTitle(boolean showTitle) {
    this.showTitle = showTitle;
    forAllCharts(c -> c.getTitle().setVisible(showTitle));
  }

  public void setOnlyShowOneAxis(boolean onlyShowOneAxis) {
    int i = 0;
    forAllCharts(c -> {
      // show only the last domain axes
      ValueAxis axis = c.getXYPlot().getDomainAxis();
      axis.setVisible(!onlyShowOneAxis || i >= group.size());
    });
  }

  private void addCheckBox(JMenu menu, String title, boolean state, ItemListener il) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem(title);
    item.setSelected(state);
    item.addItemListener(il);
    menu.add(item);
  }

  private void setRetentionTimeToComponent(double rt) {
    OptionalParameterComponent<DoubleComponent> cb =
        (OptionalParameterComponent<DoubleComponent>) getComponentForParameter(
            LibraryMetaDataParameters.EXPORT_RT);
    cb.getEmbeddedComponent().setText(MZmineCore.getConfiguration().getRTFormat().format(rt));
  }

  /**
   * Create new scan selector panels
   */
  public void updateAllChartSelectors() {
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    pnCharts.removeAll();
    GridLayout layout = new GridLayout(0, 1);
    pnCharts.setLayout(layout);

    if (checkInput()) {
      Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
      Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
      if (minSignals != null && noiseLevel != null) {
        ScanSortMode sort = getComboSortMode().getSelectionModel().getSelectedItem();

        if (rows != null) {
          // TODO: ScanSelectPanel to JavaFX
          /*
          // create MS2 of all rows
          for (int i = 0; i < rows.length; i++) {
            FeatureListRow row = rows[i];
            ScanSelectPanel pn =
                new ScanSelectPanel(row, sort, noiseLevel, minSignals, massListName);
            pnScanSelect[i] = pn;
            pn.addChartChangedListener(chart -> regroupCharts());
            pnCharts.add(pn);

            // add to group
            EChartViewer c = pn.getChart();
            if (c != null) {
              group.add(new ChartViewWrapper(c));
            }
          }
          */
        } else if (scanList != null) {
          // all selectors of scanlist
          for (int i = 0; i < scanList.size(); i++) {
            ObservableList<Scan> scansEntry = scanList.get(i);
            ScanSelectPanel pn =
                new ScanSelectPanel(scansEntry, sort, noiseLevel, minSignals);
            pnScanSelect[i] = pn;
            pn.addChartChangedListener(chart -> regroupCharts());
            pnCharts.add(pn);

            // add to group
            EChartViewer c = pn.getChart();
            if (c != null) {
              group.add(new ChartViewWrapper(c));
            }
          }
        }
      }
      streamSelection().forEach(pn -> {
        pn.setFragmentScan(isFragmentScan);
        // only show exclude/check button if more than 1 entry
        pn.setShowExcludeButton(pnScanSelect.length > 1);
      });
    }

    pnCharts.revalidate();
    pnCharts.repaint();
  }

  private void regroupCharts() {
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    if (pnScanSelect != null) {
      for (ScanSelectPanel pn : pnScanSelect) {
        EChartViewer chart = pn.getChart();
        if (chart != null)
          group.add(new ChartViewWrapper(chart));
      }
    }
  }

  private boolean checkInput() {
    updateParameterSetFromComponents();
    Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
    /*
     * if (minSignals == null) getMinSignalComponent().getTextField().setBackground(errorColor);
     * else getMinSignalComponent().getTextField().setBackground(Color.white);
     *
     * Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
     * if (noiseLevel == null) getNoiseLevelComponent().getTextField().setBackground(errorColor);
     * else getNoiseLevelComponent().getTextField().setBackground(Color.white);
     *
     * return noiseLevel != null && minSignals != null;
     */
    return false;
  }

  // ANNOTATIONS
  public void addMSMSAnnotation(AbstractMSMSIdentity ann) {
    if (msmsAnnotations == null)
      msmsAnnotations = new ArrayList<>();
    msmsAnnotations.add(ann);

    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance)
      setMzTolerance(ann.getMzTolerance());

    // add to charts
    addAnnotationToCharts(ann);
  }

  public void addMSMSAnnotations(List<? extends AbstractMSMSIdentity> ann) {
    if (ann == null)
      return;
    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance)
      for (AbstractMSMSIdentity a : ann)
        if (a.getMzTolerance() != null) {
          setMzTolerance(a.getMzTolerance());
          break;
        }

    // add all
    for (AbstractMSMSIdentity a : ann)
      addMSMSAnnotation(a);
  }

  /**
   * To flag annotations in spectra
   *
   * @param mzTolerance
   */
  public void setMzTolerance(MZTolerance mzTolerance) {
    if (mzTolerance == null && this.mzTolerance == null)
      return;

    boolean changed =
        mzTolerance != this.mzTolerance || (this.mzTolerance == null && mzTolerance != null)
            || !this.mzTolerance.equals(mzTolerance);
    this.mzTolerance = mzTolerance;
    exchangeTolerance = false;

    if (changed)
      addAllAnnotationsToCharts();
  }

  private void addAllAnnotationsToCharts() {
    if (msmsAnnotations == null)
      return;

    removeAllAnnotationsFromCharts();

    for (AbstractMSMSIdentity a : msmsAnnotations)
      addAnnotationToCharts(a);
  }

  private void removeAllAnnotationsFromCharts() {
    forAllCharts(c -> {

    });
  }

  private void addAnnotationToCharts(AbstractMSMSIdentity ann) {
    if (mzTolerance != null)
      forAllCharts(c -> {
        PseudoSpectrumDataSet data = (PseudoSpectrumDataSet) c.getXYPlot().getDataset(0);
        data.addIdentity(mzTolerance, ann);
      });
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * all charts (ms1 and MS2)
   *
   * @param op
   */
  public void forAllCharts(Consumer<JFreeChart> op) {
    if (group != null)
      group.forAllCharts(op);
  }

  /**
   * only ms2 charts
   *
   * @param op
   */
  public void forAllMSMSCharts(Consumer<JFreeChart> op) {
    if (group == null || group.getList() == null)
      return;

    for (int i = 0; i < group.getList().size(); i++)
      op.accept(group.getList().get(i).getChart());
  }

  private OptionalModuleComponent getGnpsSubmitComponent() {
    return getComponentForParameter(LibrarySubmitParameters.SUBMIT_GNPS);
  }

  private IntegerComponent getMSLevelComponent() {
    return getComponentForParameter(LibraryMetaDataParameters.MS_LEVEL);
  }

  private IntegerComponent getMinSignalComponent() {
    return getComponentForParameter(LibrarySubmitParameters.minSignals);
  }

  private DoubleComponent getNoiseLevelComponent() {
    return getComponentForParameter(LibrarySubmitParameters.noiseLevel);
  }

  private ComboBox<ScanSortMode> getComboSortMode() {
    return getComponentForParameter(LibrarySubmitParameters.sorting);
  }

  public ResultsTextPane getTxtResults() {
    return txtResults;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    Object src = e.getSource();

    if (src == helpButton) {
      Platform.runLater(() -> {
        if (helpWindow != null) {
          helpWindow.show();
          helpWindow.toFront();
        } else {
          helpWindow = new HelpWindow(helpURL.toString());
          helpWindow.show();
        }
      });
    }

  }

}
