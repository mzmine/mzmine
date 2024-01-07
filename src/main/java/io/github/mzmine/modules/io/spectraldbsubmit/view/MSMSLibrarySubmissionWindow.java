/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
package io.github.mzmine.modules.io.spectraldbsubmit.view;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
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
import io.github.mzmine.modules.visualization.spectra.spectra_stack.pseudospectra.PseudoSpectrumDataSet;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DialogLoggerUtil;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.sorting.ScanSortMode;
import java.awt.Dimension;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Instant;
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
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;

/**
 * Holds more charts for data reviewing
 *
 * @author Robin Schmid
 */
public class MSMSLibrarySubmissionWindow extends Stage {

  private static final Logger logger = Logger.getLogger(
      MSMSLibrarySubmissionWindow.class.getName());
  protected final LibrarySubmitParameters paramSubmit;
  protected final LibraryMetaDataParameters paramMeta = new LibraryMetaDataParameters();
  protected final URL helpURL;
  private final Color errorColor = Color.web("#ff8080");
  private final BorderPane main;
  protected Map<String, Node> parametersAndComponents;
  protected HelpWindow helpWindow = null;
  // to flag annotations in spectra
  private Button helpButton;
  // annotations for MSMS
  private List<AbstractMSMSIdentity> msmsAnnotations;
  private boolean exchangeTolerance = true;
  private MZTolerance mzTolerance = new MZTolerance(0.0015, 2.5d);
  // MS 2
  private ChartGroup group;
  //
  private final BorderPane contentPane;
  private final GridPane pnCharts;
  private boolean showTitle = true;
  private boolean showLegend = false;
  // click marker in all of the group
  private boolean showCrosshair = true;
  private final BorderPane pnSideMenu;
  private final GridPane pnSettings;
  private final FlowPane pnButtons;
  private final Label lblSettings;
  private final ScrollPane scrollCharts;
  private ScanSelectPanel[] pnScanSelect;
  private ScrollPane scrollMeta;
  private GridPane pnMetaData;
  private GridPane pnSubmitParam;
  private String helpID;
  //
  private boolean isFragmentScan = true;
  // data either rows or list of entries with 1 or multiple scans
  private FeatureListRow[] rows;
  private ObservableList<ObservableList<Scan>> scanList;
  private final ResultsTextPane txtResults;

  /**
   * Create the frame.
   */
  public MSMSLibrarySubmissionWindow() {

    main = new BorderPane();
    SplitPane split = new SplitPane();
    split.setOrientation(Orientation.HORIZONTAL);
//    main.setCenter(split);

    main.setMinSize(754, 519);

    final Scene scene = new Scene(main);
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(scene);

    contentPane = new BorderPane();
    contentPane.setPadding(new Insets(5));
    main.setCenter(contentPane);
//    split.getItems().add(contentPane);

    txtResults = new ResultsTextPane();
    txtResults.setEditable(false);
    SwingNode resultsNode = new SwingNode();
    resultsNode.setContent(txtResults);
    resultsNode.resize(300, 500);
    ScrollPane scrollResults = new ScrollPane(resultsNode);
    scrollResults.setFitToWidth(true);
    scrollResults.setVbarPolicy(ScrollBarPolicy.ALWAYS);
//    split.getItems().add(scrollResults);
//    split.setResizeWeight(0.92);

    // load parameter
    paramSubmit = (LibrarySubmitParameters) MZmineCore.getConfiguration()
        .getModuleParameters(LibrarySubmitModule.class);

    pnSideMenu = new BorderPane();
    contentPane.setRight(pnSideMenu);

    pnSettings = new GridPane();
    pnSideMenu.setTop(pnSettings);
//    pnSettings.setLayout(new MigLayout("", "[][grow]", "[][][][][]"));

    lblSettings = new Label("Masslist selection and preprocessing");
//    lblSettings.setFont(new Font("Tahoma", Font.BOLD, 11));
//    pnSettings.add(lblSettings, "cell 0 0");
    pnSettings.add(lblSettings, 0, 0);

    // buttons
    pnButtons = new FlowPane();
    pnSideMenu.setBottom(pnButtons);

    this.helpURL = paramSubmit.getClass().getResource("help/help.html");

    if (helpURL != null) {
      helpButton = new Button("Help");
      pnButtons.getChildren().add(helpButton);
      helpButton.setOnAction(e -> {
        if (helpWindow != null) {
          helpWindow.show();
          helpWindow.toFront();
        } else {
          helpWindow = new HelpWindow(helpURL.toString());
          helpWindow.show();
        }
      });
    }

    Button btnCheck = new Button("Check");
    btnCheck.setOnAction(e -> {
      if (checkParameters()) {
        DialogLoggerUtil.showMessageDialogForTime("Check OK", "All parameters are set", 1500);
      }
    });
    pnButtons.getChildren().add(btnCheck);

    Button btnSubmit = new Button("Submit");
    btnSubmit.setOnAction(e -> submitSpectra());
    pnButtons.getChildren().add(btnSubmit);

    parametersAndComponents = new Hashtable<>();
    createSubmitParamPanel();
    createMetaDataPanel();

    // add listener
    addListener();

    scrollCharts = new ScrollPane();
    scrollCharts.setFitToWidth(true);
    scrollCharts.setMinWidth(500);
    contentPane.setCenter(scrollCharts);

    pnCharts = new GridPane();
//    final ColumnConstraints constraints = new ColumnConstraints(400);
//    pnCharts.getColumnConstraints().add(constraints);
    scrollCharts.setContent(pnCharts);

    addMenu();
  }

  private void addListener() {
    // mc.addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));
    DoubleComponent nc = getNoiseLevelComponent();
    nc.getTextField().textProperty()
        .addListener(((observable, oldValue, newValue) -> updateAllChartSelectors()));
    IntegerComponent minc = getMinSignalComponent();
    minc.getTextField().textProperty()
        .addListener(((observable, oldValue, newValue) -> updateAllChartSelectors()));
    ComboBox<ScanSortMode> sortc = getComboSortMode();
    sortc.valueProperty()
        .addListener(((observable, oldValue, newValue) -> updateAllChartSelectors()));

    IntegerComponent mslevel = getMSLevelComponent();
    mslevel.getTextField().textProperty().addListener(((observable, oldValue, newValue) -> {
      updateParameterSetFromComponents();
      Integer level = paramMeta.getParameter(LibraryMetaDataParameters.MS_LEVEL).getValue();
      setFragmentScan(level != null && level > 1);
    }));
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

    double rt = scanList.stream().flatMap(ObservableList::stream)
        .mapToDouble(Scan::getRetentionTime).average().orElse(0d);

    // any scan matches MS level 1? --> set level to ms1
    int minMSLevel = scanList.stream().flatMap(ObservableList::stream).mapToInt(Scan::getMSLevel)
        .min().orElse(1);
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
    pnSubmitParam = new GridPane();
    pnSideMenu.setTop(pnSubmitParam);

    int rowCounter = 0;
    // Create labels and components for each parameter
    for (Parameter p : paramSubmit.getParameters()) {
      if (!(p instanceof UserParameter up)) {
        continue;
      }

      Node comp = up.createEditingComponent();
      Tooltip.install(comp, new Tooltip(up.getDescription()));

      // Set the initial value
      Object value = up.getValue();
      if (value != null) {
        up.setValueToComponent(comp, value);
      }

      // add separator
      if (p.getName().equals(LibrarySubmitParameters.LOCALFILE.getName())) {
        pnSubmitParam.add(new Separator(), 0, rowCounter, 2, 1);
        rowCounter++;
      }

      Label label = new Label(p.getName());
      pnSubmitParam.add(label, 0, rowCounter);
      label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      pnSubmitParam.add(comp, 1, rowCounter);
      rowCounter++;
    }
  }

  private void createMetaDataPanel() {
    // Main panel which holds all the components in a grid
    pnMetaData = new GridPane();
    scrollMeta = new ScrollPane(pnMetaData);
    scrollMeta.setVbarPolicy(ScrollBarPolicy.ALWAYS);
    scrollMeta.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    pnSideMenu.setCenter(scrollMeta);

    int rowCounter = 0;
    int vertWeightSum = 0;

    // Create labels and components for each parameter
    for (Parameter p : paramMeta.getParameters()) {
      if (!(p instanceof UserParameter up)) {
        continue;
      }

      Node comp = up.createEditingComponent();
      Tooltip.install(comp, new Tooltip(up.getDescription()));

      // Set the initial value
      Object value = up.getValue();
      if (value != null) {
        up.setValueToComponent(comp, value);
      }

      Label label = new Label(p.getName());
      pnMetaData.add(label, 0, rowCounter);
      parametersAndComponents.put(p.getName(), comp);

      pnMetaData.add(comp, 1, rowCounter);
      rowCounter++;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void updateParameterSetFromComponents() {
    for (Parameter<?> p : paramSubmit.getParameters()) {
      if (!(p instanceof UserParameter up)) {
        continue;
      }
      Node component = parametersAndComponents.get(p.getName());
      up.setValueFromComponent(component);
    }
    for (Parameter<?> p : paramMeta.getParameters()) {
      if (!(p instanceof UserParameter up)) {
        continue;
      }
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
    if (pnScanSelect == null) {
      return Stream.empty();
    }
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
      if (mslevel == 1) {
        paramSubmit.getParameter(LibrarySubmitParameters.SUBMIT_GNPS).setValue(false);
      } else {
        int adducts = countSelectedAdducts();
        // every valid selected ion needs an adduct for MS2
        if (ions != adducts) {
          MZmineCore.getDesktop().displayErrorMessage(MessageFormat.format(
              "Not all adducts are set: {0} ion spectra selected and only {1}  adducts set", ions,
              adducts));
          return;
        }
      }

      //
      if (ions == 0) {
        logger.info("No MS/MS spectrum selected or valid");
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
              LibrarySubmitIonParameters ionParam = createIonParameters(paramSubmit, paramMeta,
                  ion);
              DataPoint[] dps = ion.getFilteredDataPoints();

              // submit and save locally
              map.put(ionParam, dps);
            }
          }
          // start task
          logger.info(
              "Added task to export library entries: " + ions + " MS/MS spectra were selected");
          LibrarySubmitTask task = new LibrarySubmitTask(this, map, Instant.now());
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
    if (pnScanSelect != null) {
      for (ScanSelectPanel pn : pnScanSelect) {
        pn.setSortMode(sort);
      }
    }
  }

  private void updateSettingsOnAllSelectors() {
    if (checkInput()) {
      Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
      Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
      if (pnScanSelect != null && minSignals != null && noiseLevel != null) {
        for (ScanSelectPanel pn : pnScanSelect) {
          pn.setFilter(noiseLevel, minSignals);
        }
      }
    }
  }

  private void addMenu() {
    MenuBar menu = new MenuBar();
    Menu settings = new Menu("Settings");
    menu.getMenus().add(settings);
    Stage thisframe = this;

    // reset zoom
    MenuItem resetZoom = new MenuItem("reset zoom");
    settings.getItems().add(resetZoom);
    resetZoom.setOnAction(e -> {
      if (group != null) {
        group.resetZoom();
      }
    });

    // JMenuItem setSize = new JMenuItem("chart size");
    // menu.add(setSize);
    // setSize.addActionListener(e -> {
    // Dimension dim = SizeSelectDialog.getSizeInput();
    // if (dim != null)
    // setChartSize(dim);
    // });

    //
    addCheckBox(settings, "show legend", showLegend, this::setShowLegend);
    addCheckBox(settings, "show title", showTitle, this::setShowTitle);
    addCheckBox(settings, "show crosshair", showCrosshair, this::setShowCrosshair);

    main.setTop(menu);
  }

  private void setChartSize(Dimension dim) {
    if (pnScanSelect != null && dim != null) {
      for (ScanSelectPanel pn : pnScanSelect) {
        pn.setChartSize(dim);
      }
    }
  }

  public void setShowCrosshair(boolean showCrosshair) {
    this.showCrosshair = showCrosshair;
    if (group != null) {
      group.setShowCrosshair(showCrosshair, showCrosshair);
    }
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

  private void addCheckBox(Menu menu, String title, boolean state, Consumer<Boolean> c) {
    CheckMenuItem item = new CheckMenuItem(title);
    item.setSelected(state);
    item.selectedProperty().addListener(((observable, oldValue, newValue) -> c.accept(newValue)));
    menu.getItems().add(item);
  }

  private void setRetentionTimeToComponent(double rt) {
    OptionalParameterComponent<DoubleComponent> cb = (OptionalParameterComponent<DoubleComponent>) getComponentForParameter(
        LibraryMetaDataParameters.EXPORT_RT);
    cb.getEmbeddedComponent().setText(MZmineCore.getConfiguration().getRTFormat().format(rt));
  }

  /**
   * Create new scan selector panels
   */
  public void updateAllChartSelectors() {
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    pnCharts.getChildren().clear();
//    GridLayout layout = new GridLayout(0, 1);
//    pnCharts.setLayout(layout);

    if (checkInput()) {
      Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
      Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
      if (minSignals != null && noiseLevel != null) {
        ScanSortMode sort = getComboSortMode().getSelectionModel().getSelectedItem();

        if (rows != null) {
          // create MS2 of all rows
          for (int i = 0; i < rows.length; i++) {
            FeatureListRow row = rows[i];
            ScanSelectPanel pn = new ScanSelectPanel(row, sort, noiseLevel, minSignals);
            pnScanSelect[i] = pn;
            pn.addChartChangedListener(chart -> regroupCharts());
            pnCharts.add(pn, 0, i);

            // add to group
            EChartViewer c = pn.getChart();
            if (c != null) {
              group.add(new ChartViewWrapper(c));
            }
          }
        } else if (scanList != null) {
          // all selectors of scanlist
          for (int i = 0; i < scanList.size(); i++) {
            ObservableList<Scan> scansEntry = scanList.get(i);
            ScanSelectPanel pn = new ScanSelectPanel(scansEntry, sort, noiseLevel, minSignals);
            pnScanSelect[i] = pn;
            pn.addChartChangedListener(chart -> regroupCharts());
            pnCharts.add(pn, 0, i);

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

//    pnCharts.revalidate();
//    pnCharts.repaint();
  }

  private void regroupCharts() {
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    if (pnScanSelect != null) {
      for (ScanSelectPanel pn : pnScanSelect) {
        EChartViewer chart = pn.getChart();
        if (chart != null) {
          group.add(new ChartViewWrapper(chart));
        }
      }
    }
  }

  private boolean checkInput() {
    updateParameterSetFromComponents();
    Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
    if (minSignals == null) {
      getMinSignalComponent().getTextField()
          .setBackground(new Background(new BackgroundFill(errorColor, CornerRadii.EMPTY, null)));
    } else {
      getMinSignalComponent().getTextField()
          .setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, null)));
    }

    Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
    if (noiseLevel == null) {
      getNoiseLevelComponent().getTextField()
          .setBackground(new Background(new BackgroundFill(errorColor, CornerRadii.EMPTY, null)));
    } else {
      getNoiseLevelComponent().getTextField()
          .setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, null)));
    }

    return noiseLevel != null && minSignals != null;
  }

  // ANNOTATIONS
  public void addMSMSAnnotation(AbstractMSMSIdentity ann) {
    if (msmsAnnotations == null) {
      msmsAnnotations = new ArrayList<>();
    }
    msmsAnnotations.add(ann);

    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance) {
      setMzTolerance(ann.getMzTolerance());
    }

    // add to charts
    addAnnotationToCharts(ann);
  }

  public void addMSMSAnnotations(List<? extends AbstractMSMSIdentity> ann) {
    if (ann == null) {
      return;
    }
    // extract mz tolerance
    if (mzTolerance == null || exchangeTolerance) {
      for (AbstractMSMSIdentity a : ann) {
        if (a.getMzTolerance() != null) {
          setMzTolerance(a.getMzTolerance());
          break;
        }
      }
    }

    // add all
    for (AbstractMSMSIdentity a : ann) {
      addMSMSAnnotation(a);
    }
  }

  private void addAllAnnotationsToCharts() {
    if (msmsAnnotations == null) {
      return;
    }

    removeAllAnnotationsFromCharts();

    for (AbstractMSMSIdentity a : msmsAnnotations) {
      addAnnotationToCharts(a);
    }
  }

  private void removeAllAnnotationsFromCharts() {
    forAllCharts(c -> {

    });
  }

  private void addAnnotationToCharts(AbstractMSMSIdentity ann) {
    if (mzTolerance != null) {
      forAllCharts(c -> {
        PseudoSpectrumDataSet data = (PseudoSpectrumDataSet) c.getXYPlot().getDataset(0);
        data.addIdentity(mzTolerance, ann);
      });
    }
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * To flag annotations in spectra
   *
   * @param mzTolerance
   */
  public void setMzTolerance(MZTolerance mzTolerance) {
    if (mzTolerance == null && this.mzTolerance == null) {
      return;
    }

    boolean changed =
        mzTolerance != this.mzTolerance || (this.mzTolerance == null && mzTolerance != null)
            || !this.mzTolerance.equals(mzTolerance);
    this.mzTolerance = mzTolerance;
    exchangeTolerance = false;

    if (changed) {
      addAllAnnotationsToCharts();
    }
  }

  /**
   * all charts (ms1 and MS2)
   *
   * @param op
   */
  public void forAllCharts(Consumer<JFreeChart> op) {
    if (group != null) {
      group.forAllCharts(op);
    }
  }

  /**
   * only ms2 charts
   *
   * @param op
   */
  public void forAllMSMSCharts(Consumer<JFreeChart> op) {
    if (group == null || group.getList() == null) {
      return;
    }

    for (int i = 0; i < group.getList().size(); i++) {
      op.accept(group.getList().get(i).getChart());
    }
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
}
