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
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.sorting.ScanSortMode;
import io.github.mzmine.util.swing.IconUtil;
import java.awt.Dimension;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScanSelectPanel extends BorderPane {

  private static final Logger logger = Logger.getLogger(ScanSelectPanel.class.getName());
  // icons
  static final Image iconTIC = FxIconUtil.loadImageFromResources("icons/btnTIC.png");
  static final Image iconTICFalse = FxIconUtil.loadImageFromResources("icons/btnTIC_grey.png");
  static final Image iconSignals = FxIconUtil.loadImageFromResources("icons/btnSignals.png");
  static final Image iconSignalsFalse = FxIconUtil.loadImageFromResources(
      "icons/btnSignals_grey.png");
  static final Image iconAccept = FxIconUtil.loadImageFromResources("icons/btnAccept.png");
  static final Image iconCross = FxIconUtil.loadImageFromResources("icons/btnCross.png");
  static final Image iconNext = FxIconUtil.loadImageFromResources("icons/btnNext.png");
  static final Image iconPrev = FxIconUtil.loadImageFromResources("icons/btnPrev.png");
  static final Image iconNextGrey = FxIconUtil.loadImageFromResources("icons/btnNext_grey.png");
  static final Image iconPrevGrey = FxIconUtil.loadImageFromResources("icons/btnPrev_grey.png");


  private static final int SIZE = 40;
  public final Color colorRemovedData;
  public final Color colorUsedData;
  private final Color errorColor = Color.web("#ffb3b3");
  private ToggleButton btnToggleUse;
  private TextField txtAdduct;
  private ScanSortMode sort;
  // noise level to cut off signals
  private double noiseLevel;
  // minimum of 1
  private int minNumberOfSignals;
  private Label lbMassListError;

  //
  private List<Scan> scans;
  private int selectedScanI;

  private BorderPane pnChart;

  private ToggleButton btnSignals;
  private ToggleButton btnMaxTic;
  private SpectraPlot spectrumPlot;

  private Consumer<SpectraPlot> listener;
  private Label lblTic;
  private Label lblSignals;
  private Label lbTIC;
  private Label lbSignals;
  private Dimension chartSize = new Dimension(500, 320);
  private boolean validSelection = false;
  private TextField txtCharge;
  private TextField txtPrecursorMZ;
  private Label lblChargeMz;
  private Button btnFromScan;

  private boolean showRemovedData = true;
  private boolean showLegend = true;
  // MS1 or MS2
  private boolean isFragmentScan = true;

  // data either row or scans
  private FeatureListRow row;
  private ObservableList<Scan> scansEntry;
  private Label lblAdduct;
  private GridPane pnData;
  private Button btnPrev;
  private Button btnNext;

  /**
   * Create the panel.
   */
  public ScanSelectPanel(FeatureListRow row, ScanSortMode sort, double noiseLevel,
      int minNumberOfSignals) {
    this(sort, noiseLevel, minNumberOfSignals);
    this.row = row;
    // create chart with current sort mode
    setSortMode(sort);
    createChart();
    setMZandChargeFromScan();
  }

  public ScanSelectPanel(ObservableList<Scan> scansEntry, ScanSortMode sort, double noiseLevel,
      int minNumberOfSignals) {
    this(sort, noiseLevel, minNumberOfSignals);
    this.scansEntry = scansEntry;
    // create chart with current sort mode
    setSortMode(sort);
    createChart();
    setMZandChargeFromScan();
  }

  public ScanSelectPanel(ScanSortMode sort, double noiseLevel, int minNumberOfSignals) {
//    setMinSize(400, 300);

    // get colors for vision
    SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();
    colorUsedData = palette.getPositiveColor();
    colorRemovedData = palette.getNegativeColor();

//    setBorder(new LineBorder(UIManager.getColor("textHighlight")));
    this.sort = sort;
    this.noiseLevel = noiseLevel;
    setMinNumberOfSignals(minNumberOfSignals);

    pnChart = new BorderPane();
    setCenter(pnChart);

    BorderPane pnMenu = new BorderPane();
    setRight(pnMenu);

    GridPane pnButtons = new GridPane();
    pnMenu.setLeft(pnButtons);
//    pnButtons.setLayout(new MigLayout("", "[40px]", "[grow][40px][40px][40px][40px][40px][grow]"));

    // TODO: uncomment all and change to JavaFX
    btnToggleUse = new ToggleButton(null, IconUtil.scaledImageView(iconCross, SIZE));
    //btnToggleUse.setSelectedIcon(iconAccept);
    btnToggleUse.setTooltip(new Tooltip(
        "Export this entry (checked) or exclude from export (X). Useful when multiple ions (adducts) of the same compound are exported at once."));
    pnButtons.add(btnToggleUse, 0, 1);
    btnToggleUse.setSelected(true);
    btnToggleUse.selectedProperty().addListener((o, ol, ne) -> applySelectionState());

    btnNext = new Button(null, IconUtil.scaledImageView(iconNext, SIZE));
    //btnNext.setDisabledIcon(iconNextGrey);
    btnNext.setTooltip(new Tooltip("Next spectrum (in respect to sorting)"));
    btnNext.setOnAction(e -> nextScan());
    pnButtons.add(btnNext, 0, 2);

    btnPrev = new Button(null, IconUtil.scaledImageView(iconPrev, SIZE));
    //btnPrev.setDisabledIcon(iconPrevGrey);
    btnPrev.setTooltip(new Tooltip("Previous spectrum (in respect to sorting)"));
    btnPrev.setOnAction(a -> prevScan());
    pnButtons.add(btnPrev, 0, 3);

    btnMaxTic = new ToggleButton(null, IconUtil.scaledImageView(iconTICFalse, SIZE));
    btnMaxTic.setTooltip(new Tooltip("Change sorting to max TIC"));
    //btnMaxTic.setSelectedIcon(iconTIC);
    btnMaxTic.selectedProperty().addListener((o, ol, ne) -> {
      if (ne) {
        setSortMode(ScanSortMode.MAX_TIC);
      }
    });
    pnButtons.add(btnMaxTic, 0, 4);

    btnSignals = new ToggleButton(null, IconUtil.scaledImageView(iconSignalsFalse, SIZE));
    btnSignals.setTooltip(new Tooltip("Change sorting to max number of signals"));
//    btnSignals.setSelectedIcon(iconSignals);
    btnSignals.selectedProperty().addListener((o, ol, ne) -> {
      if (ne) {
        setSortMode(ScanSortMode.NUMBER_OF_SIGNALS);
      }
    });
    pnButtons.add(btnSignals, 0, 5);

    ToggleGroup group = new ToggleGroup();
    group.getToggles().add(btnSignals);
    group.getToggles().add(btnMaxTic);

    pnData = new GridPane();
    pnMenu.setCenter(pnData);
//    pnData.setLayout(new MigLayout("", "[grow][][]", "[][][][][][][][]"));

    lblAdduct = new Label("Adduct:");
//    pnData.add(lblAdduct, "cell 0 0 3 1");
    pnData.add(lblAdduct, 0, 0, 3, 1);

    txtAdduct = new TextField();
    /*Document doc = txtAdduct.getDocument();
    if (doc instanceof AbstractDocument) {
      ((AbstractDocument) doc).setDocumentFilter(new DocumentSizeFilter(20));
    }*/
    txtAdduct.setTooltip(new Tooltip(
        "Insert adduct in this format: M+H, M-H2O+H, 2M+Na, M+2H+2 (for doubly charged)"));
//    pnData.add(txtAdduct, "cell 0 1 3 1,growx");
    pnData.add(txtAdduct, 0, 1, 3, 1);
    txtAdduct.setPrefColumnCount(10);

    lblChargeMz = new Label("Charge; m/z");
//    pnData.add(lblChargeMz, "cell 0 2");
    pnData.add(lblChargeMz, 0, 2);

    txtCharge = new TextField();
    txtCharge.setTooltip(new Tooltip("Charge (numeric, integer)"));
    txtCharge.setText("1");
//    pnData.add(txtCharge, "cell 0 3,growx,aligny top");
    pnData.add(txtCharge, 0, 3);
    txtCharge.setPrefColumnCount(4);

    txtPrecursorMZ = new TextField();
    txtPrecursorMZ.setTooltip(new Tooltip("Exact (ideally calculated) precursor m/z of this ion"));
//    pnData.add(txtPrecursorMZ, "cell 0 4,growx,aligny top");
    pnData.add(txtPrecursorMZ, 0, 4);
    txtPrecursorMZ.setPrefColumnCount(9);

    btnFromScan = new Button("From scan");
    btnFromScan.setTooltip(new Tooltip("Precursor m/z and charge from scan or feature"));
    btnFromScan.setOnAction(e -> setMZandChargeFromScan());
//    pnData.add(btnFromScan, "cell 0 5,growx");
    pnData.add(btnFromScan, 0, 5);

    lblTic = new Label("TIC=");
//    pnData.add(lblTic, "cell 0 6,alignx trailing");
    pnData.add(lblTic, 0, 6);

    lbTIC = new Label("0");
//    pnData.add(lbTIC, "cell 1 6");
    pnData.add(lbTIC, 1, 6);

    lblSignals = new Label("Signals: ");
//    pnData.add(lblSignals, "cell 0 7,alignx trailing");
    pnData.add(lblSignals, 0, 7);

    lbSignals = new Label("0");
//    pnData.add(lbSignals, "cell 1 7");
    pnData.add(lbSignals, 1, 7);

    lbMassListError = new Label("ERROR with masslist selection: Wrong name or no masslist");
//    lbMassListError.setFont(new Font("Tahoma", Font.BOLD, 13));
    lbMassListError.setAlignment(Pos.CENTER);
    lbMassListError.setTextFill(
        new Color(220 / (double) 255, 20 / (double) 255, 60 / (double) 255, 1d));
    lbMassListError.setVisible(false);
//    add(lbMassListError, BorderLayout.NORTH);
    setTop(lbMassListError);
  }

  /**
   * Fragment scan with adduct, precursor mz and charge or MS1
   *
   * @param isFragmentScan
   */
  public void setFragmentScan(boolean isFragmentScan) {
    this.isFragmentScan = isFragmentScan;

    lblChargeMz.setVisible(isFragmentScan);
    lblAdduct.setVisible(isFragmentScan);
    txtAdduct.setVisible(isFragmentScan);
    txtCharge.setVisible(isFragmentScan);
    txtPrecursorMZ.setVisible(isFragmentScan);
    btnFromScan.setVisible(isFragmentScan);
//    pnData.revalidate();
//    pnData.repaint();

    // if data is from rows - get new list of scans
    if (row != null) {
      createSortedScanList();
      setMZandChargeFromScan();
    }
  }

  /**
   *
   */
  public void setMZandChargeFromScan() {
    // MS1
    if (!isFragmentScan) {
      return;
    }

    if (scans != null && !scans.isEmpty()) {
      Scan scan = scans.get(selectedScanI);
      double mz = scan.getPrecursorMz() != null ? scan.getPrecursorMz() : 0;
      if (mz == 0) {
        if (row != null) {
          mz = row.getAverageMZ();
        }
      }
      int charge = scan.getPrecursorCharge() != null ? scan.getPrecursorCharge() : 0;
      if (charge == 0 && row != null) {
        charge = row.getRowCharge();
      }

      if (charge == 0) {
        charge = 1;
      }

      // set as text
      txtCharge.setText(String.valueOf(charge));
      txtPrecursorMZ.setText(MZmineCore.getConfiguration().getMZFormat().format(mz));
    }
  }

  /**
   * Show the exclude from export button (check button). This button is usually hidden when only one
   * ScanSelectPanel is shown in the {@link MSMSLibrarySubmissionWindow}
   *
   * @param state linked to the visibility of the exclude button
   */
  public void setShowExcludeButton(boolean state) {
    btnToggleUse.setVisible(state);
  }

  public FeatureListRow getRow() {
    return row;
  }

  public double getPrecursorMZ() {
    try {
      double c = Double.parseDouble(txtPrecursorMZ.getText());
      txtPrecursorMZ.setBorder(new Border(
          new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
              new BorderWidths(1d))));
      return c;
    } catch (Exception e) {
      txtPrecursorMZ.setBorder(new Border(
          new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
              new BorderWidths(1d))));
      return 0;
    }
  }

  public int getPrecursorCharge() {
    try {
      int c = Integer.parseInt(txtCharge.getText());
      txtCharge.setBorder(new Border(
          new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
              new BorderWidths(1d))));
      return c;
    } catch (Exception e) {
      txtCharge.setBorder(new Border(
          new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
              new BorderWidths(1d))));
      return 0;
    }
  }

  public boolean checkParameterValues(List<String> messages) {
    // no parameters for MS1 scan
    if (!isFragmentScan) {
      return true;
    }

    // for MS/MS scans:
    String adduct = getAdduct();
    if (adduct.isEmpty()) {
      messages.add("Adduct is not set properly: " + txtAdduct.getText());
    }

    int charge = getPrecursorCharge();
    if (charge <= 0) {
      messages.add("Charge is not set properly: " + txtCharge.getText());
    }

    double mz = getPrecursorCharge();
    if (mz <= 0) {
      messages.add("Precursor m/z is not set properly: " + txtPrecursorMZ.getText());
    }

    return !(adduct.isEmpty() || charge <= 0 || mz <= 0);
  }

  /**
   * minimum is >=1
   *
   * @param minNumberOfSignals
   */
  public void setMinNumberOfSignals(int minNumberOfSignals) {
    this.minNumberOfSignals = Math.min(minNumberOfSignals, 1);
  }

  private void applySelectionState() {
    boolean selected = btnToggleUse.isSelected();
    SpectraPlot chart = getChart();
    if (chart != null) {
      chart.getChart().getXYPlot().setBackgroundPaint(
          selected ? FxColorUtil.fxColorToAWT(Color.WHITE) : FxColorUtil.fxColorToAWT(errorColor));
    }
  }

  public void setSortMode(ScanSortMode sort) {
    if (this.sort.equals(sort)) {
      return;
    }

    this.sort = sort;
    switch (sort) {
      case NUMBER_OF_SIGNALS:
        btnSignals.setSelected(true);
        break;
      case MAX_TIC:
        btnMaxTic.setSelected(true);
        break;
    }
    createSortedScanList();
  }

  public void setFilter(double noiseLevel, int minNumberOfSignals) {
    this.noiseLevel = noiseLevel;
    this.minNumberOfSignals = minNumberOfSignals;
    createSortedScanList();
  }

  /**
   * Creates a sorted list of all scans that match the minimum criteria
   */
  private void createSortedScanList() {
    if (row == null && scansEntry == null) {
      return;
    }
    // get all scans that match filter criteria
    try {
      if (row != null) {
        if (isFragmentScan) {
          // first entry is the best fragmentation scan
          scans = ScanUtils.listAllFragmentScans(row, noiseLevel, minNumberOfSignals, sort);
        } else {
          // get most representative MS 1 scans of all features
          scans = ScanUtils.listAllMS1Scans(row, noiseLevel, minNumberOfSignals, sort);
        }
      } else if (scansEntry != null) {
        scans = ScanUtils.listAllScans(scansEntry, noiseLevel, minNumberOfSignals, sort);
      }
      selectedScanI = 0;

      // no error
      lbMassListError.setVisible(false);
    } catch (MissingMassListException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      // create error label
      lbMassListError.setVisible(true);
    }
    // create chart
    createChart();
  }

  public void nextScan() {
    if (selectedScanI + 1 < scans.size()) {
      selectedScanI++;
      createChart();
    }
  }

  public void prevScan() {
    if (selectedScanI - 1 >= 0) {
      selectedScanI--;
      createChart();
    }
  }

  public ToggleButton getBtnToggleUse() {
    return btnToggleUse;
  }

  public boolean isSelected() {
    return btnToggleUse.isSelected();
  }

  public void setSelected(boolean state) {
    btnToggleUse.setSelected(state);
  }

  /**
   * Create chart of selected scan
   */
  public void createChart() {
    setValidSelection(false);
    pnChart.getChildren().clear();

    if (scans != null && !scans.isEmpty()) {
      // get MS/MS spectra window only for the spectra chart
      // create dataset

      if (spectrumPlot == null) {
        spectrumPlot = new SpectraPlot();
//        spectrumPlot.setMinSize(400, 400);
        if (listener != null) {
          // chart has changed
          listener.accept(spectrumPlot);
        }
      }
      spectrumPlot.removeAllDataSets();

      DataPointsDataSet data = new DataPointsDataSet("Data", getFilteredDataPoints());
      // green
      spectrumPlot.addDataSet(data, FxColorUtil.fxColorToAWT(colorUsedData), false, true);
      if (showRemovedData) {
        // orange
        DataPointsDataSet dataRemoved = new DataPointsDataSet("Removed",
            getFilteredDataPointsRemoved());
        spectrumPlot.addDataSet(dataRemoved, FxColorUtil.fxColorToAWT(colorRemovedData), false,
            true);
      }
      spectrumPlot.getChart().getLegend().setVisible(showLegend);
      // spectrumPlot.setMaximumSize(new Dimension(chartSize.width, 10000));
      spectrumPlot.setPrefSize(chartSize.width, chartSize.height);
      pnChart.setCenter(spectrumPlot);

      Scan scan = scans.get(selectedScanI);
      analyzeScan(scan);
      applySelectionState();
      setValidSelection(true);
    } else {
      // add error label
      Label error = new Label(
          MessageFormat.format("NO MS2 SPECTRA: 0 of {0} match the minimum criteria",
              getTotalScans()));
//      error.setFont(new Font("Tahoma", Font.BOLD, 13));
      error.setAlignment(Pos.CENTER);
      pnChart.setCenter(error);
      //
    }
    // set next and prev button enabled
    btnPrev.setDisable(!(selectedScanI - 1 >= 0));
    btnNext.setDisable(!(scans != null && selectedScanI + 1 < scans.size()));

  }

  private int getTotalScans() {
    if (row != null) {
      return row.getAllFragmentScans().size();
    }
    if (scansEntry != null) {
      return scansEntry.size();
    }
    return 0;
  }

  private void setValidSelection(boolean state) {
    validSelection = state;
  }

  private void analyzeScan(Scan scan) {
    MassList massList = scan.getMassList();
    if (massList != null) {
      DataPoint[] dp = massList.getDataPoints();
      double tic = ScanUtils.getTIC(dp, noiseLevel);
      int signals = ScanUtils.getNumberOfSignals(dp, noiseLevel);
      lbTIC.setText(MZmineCore.getConfiguration().getIntensityFormat().format(tic));
      lbSignals.setText("" + signals);
    }
  }

  /**
   * Remaining data points after filtering
   *
   * @return
   */
  @Nullable
  public DataPoint[] getFilteredDataPoints() {
    if (scans != null && !scans.isEmpty()) {
      Scan scan = scans.get(selectedScanI);
      MassList massList = scan.getMassList();
      if (massList != null) {
        return ScanUtils.getFiltered(massList.getDataPoints(), noiseLevel);
      }
    }
    return null;
  }

  /**
   * Removed data points
   *
   * @return
   */
  @Nullable
  public DataPoint[] getFilteredDataPointsRemoved() {
    if (scans != null && !scans.isEmpty()) {
      Scan scan = scans.get(selectedScanI);
      MassList massList = scan.getMassList();
      if (massList != null) {
        return ScanUtils.getBelowThreshold(massList.getDataPoints(), noiseLevel);
      }
    }
    return null;
  }

  public Label getLbMassListError() {
    return lbMassListError;
  }

  @Nullable
  public SpectraPlot getChart() {
    return spectrumPlot;
  }

  public void addChartChangedListener(Consumer<SpectraPlot> listener) {
    this.listener = listener;
  }

  public Label getLbTIC() {
    return lbTIC;
  }

  public Label getLbSignals() {
    return lbSignals;
  }

  /**
   * This will check adduct pattern and enforce it.
   *
   * @return The adduct or an empty String for wrong input
   */
  @NotNull
  public String getAdduct() {
    String adduct = txtAdduct.getText();

    String formatted = Objects.requireNonNullElse(((Object) IonTypeParser.parse(adduct)), "")
        .toString();
    if (formatted.isEmpty()) {
      txtAdduct.setBorder(new Border(
          new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
              new BorderWidths(1d))));
      return "";
    }

    if (!formatted.equals(adduct)) {
      txtAdduct.setText(formatted);
    }
    txtAdduct.setBorder(new Border(
        new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            new BorderWidths(1d))));
    return formatted;
  }

  public void setChartSize(Dimension dim) {
    chartSize = dim;
  }

  public void setShowRemovedData(boolean showRemovedData) {
    this.showRemovedData = showRemovedData;
  }

  /**
   * Valid spectrum and is selected? Still check for correct adduct
   *
   * @return
   */
  public boolean isValidAndSelected() {
    return isSelected() && validSelection;
  }

  public TextField getTxtCharge() {
    return txtCharge;
  }

  public TextField getTxtPrecursorMZ() {
    return txtPrecursorMZ;
  }

  public boolean hasAdduct() {
    return !getAdduct().isEmpty();
  }

}
