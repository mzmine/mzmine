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

package net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.framework.documentfilter.DocumentSizeFilter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.AdductParser;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerWindow;
import net.sf.mzmine.util.exceptions.MissingMassListException;
import net.sf.mzmine.util.scans.ScanUtils;
import net.sf.mzmine.util.scans.sorting.ScanSortMode;

public class ScanSelectPanel extends JPanel {

  private static final int SIZE = 40;
  // icons
  static final Icon iconTIC = createIcon("icons/btnTIC.png");
  static final Icon iconTICFalse = createIcon("icons/btnTIC_grey.png");
  static final Icon iconSignals = createIcon("icons/btnSignals.png");
  static final Icon iconSignalsFalse = createIcon("icons/btnSignals_grey.png");
  static final Icon iconAccept = createIcon("icons/btnAccept.png");
  static final Icon iconCross = createIcon("icons/btnCross.png");
  static final Icon iconNext = createIcon("icons/btnNext.png");
  static final Icon iconPrev = createIcon("icons/btnPrev.png");

  private Logger log = Logger.getLogger(this.getClass().getName());
  private final Color errorColor = Color.decode("#ffb3b3");

  private JToggleButton btnToggleUse;
  private JTextField txtAdduct;
  private ScanSortMode sort;
  // null or empty to use first masslist
  private @Nullable String massListName;
  // noise level to cut off signals
  private double noiseLevel;
  // minimum of 1
  private int minNumberOfSignals;
  private JLabel lbMassListError;

  //
  private List<Scan> scans;
  private int selectedScanI;

  private JPanel pnChart;

  private boolean showLegend = true;

  private JToggleButton btnSignals;

  private JToggleButton btnMaxTic;

  private SpectraPlot spectrumPlot;

  private Consumer<EChartPanel> listener;
  private JLabel lblTic;
  private JLabel lblSignals;
  private JLabel lbTIC;
  private JLabel lbSignals;
  private Dimension chartSize = new Dimension(350, 320);
  private boolean validSelection = false;
  private JTextField txtCharge;
  private JTextField txtPrecursorMZ;
  private JLabel lblChargeMz;
  private JButton btnFromScan;

  // MS1 or MS2
  private boolean isFragmentScan = true;

  // data either row or scans
  private PeakListRow row;
  private Scan[] scansEntry;
  private JLabel lblAdduct;
  private JPanel pnData;

  /**
   * Create the panel.
   */
  public ScanSelectPanel(PeakListRow row, ScanSortMode sort, double noiseLevel,
      int minNumberOfSignals, String massListName) {
    this(sort, noiseLevel, minNumberOfSignals, massListName);
    this.row = row;
    // create chart with current sort mode
    setSortMode(sort);
    createChart();
    setMZandChargeFromScan();
  }

  public ScanSelectPanel(Scan[] scansEntry, ScanSortMode sort, double noiseLevel,
      int minNumberOfSignals, String massListName) {
    this(sort, noiseLevel, minNumberOfSignals, massListName);
    this.scansEntry = scansEntry;
    // create chart with current sort mode
    setSortMode(sort);
    createChart();
    setMZandChargeFromScan();
  }

  public ScanSelectPanel(ScanSortMode sort, double noiseLevel, int minNumberOfSignals,
      String massListName) {
    setBorder(new LineBorder(UIManager.getColor("textHighlight")));
    this.massListName = massListName;
    this.sort = sort;
    this.noiseLevel = noiseLevel;
    setMinNumberOfSignals(minNumberOfSignals);
    setLayout(new BorderLayout(0, 0));

    pnChart = new JPanel();
    add(pnChart, BorderLayout.CENTER);
    pnChart.setLayout(new BorderLayout(0, 0));

    JPanel pnMenu = new JPanel();
    add(pnMenu, BorderLayout.EAST);
    pnMenu.setLayout(new BorderLayout(0, 0));

    JPanel pnButtons = new JPanel();
    pnMenu.add(pnButtons, BorderLayout.WEST);
    pnButtons.setLayout(new MigLayout("", "[40px]", "[grow][40px][40px][40px][40px][40px][grow]"));

    btnToggleUse = new JToggleButton(iconCross);
    btnToggleUse.setSelectedIcon(iconAccept);
    btnToggleUse.setPreferredSize(new Dimension(SIZE, SIZE));
    btnToggleUse.setMaximumSize(new Dimension(SIZE, SIZE));
    pnButtons.add(btnToggleUse, "cell 0 1,grow");
    btnToggleUse.setSelected(true);
    btnToggleUse.addItemListener(il -> applySelectionState());

    JButton btnNext = new JButton(iconNext);
    btnNext.setPreferredSize(new Dimension(SIZE, SIZE));
    btnNext.setMaximumSize(new Dimension(SIZE, SIZE));
    btnNext.addActionListener(a -> nextScan());
    pnButtons.add(btnNext, "cell 0 2,grow");

    JButton btnPrev = new JButton(iconPrev);
    btnPrev.setPreferredSize(new Dimension(SIZE, SIZE));
    btnPrev.setMaximumSize(new Dimension(SIZE, SIZE));
    btnPrev.addActionListener(a -> prevScan());
    pnButtons.add(btnPrev, "cell 0 3,grow");

    btnMaxTic = new JToggleButton(iconTICFalse);
    btnMaxTic.setSelectedIcon(iconTIC);
    btnMaxTic.setPreferredSize(new Dimension(SIZE, SIZE));
    btnMaxTic.setMaximumSize(new Dimension(SIZE, SIZE));
    btnMaxTic.addItemListener(a -> {
      if (a.getStateChange() == ItemEvent.SELECTED)
        setSortMode(ScanSortMode.MAX_TIC);
    });
    pnButtons.add(btnMaxTic, "cell 0 4,grow");

    btnSignals = new JToggleButton(iconSignalsFalse);
    btnSignals.setSelectedIcon(iconSignals);
    btnSignals.setPreferredSize(new Dimension(SIZE, SIZE));
    btnSignals.setMaximumSize(new Dimension(SIZE, SIZE));
    btnSignals.addItemListener(a -> {
      if (a.getStateChange() == ItemEvent.SELECTED)
        setSortMode(ScanSortMode.NUMBER_OF_SIGNALS);
    });
    pnButtons.add(btnSignals, "cell 0 5,grow");

    ButtonGroup group = new ButtonGroup();
    group.add(btnSignals);
    group.add(btnMaxTic);

    pnData = new JPanel();
    pnMenu.add(pnData, BorderLayout.CENTER);
    pnData.setLayout(new MigLayout("", "[grow][][]", "[][][][][][][][]"));

    lblAdduct = new JLabel("Adduct:");
    pnData.add(lblAdduct, "cell 0 0 3 1");

    txtAdduct = new JTextField();
    Document doc = txtAdduct.getDocument();
    if (doc instanceof AbstractDocument)
      ((AbstractDocument) doc).setDocumentFilter(new DocumentSizeFilter(20));
    txtAdduct.setToolTipText(
        "Insert adduct in this format: M+H, M-H2O+H, 2M+Na, M+2H+2 (for doubly charged)");
    pnData.add(txtAdduct, "cell 0 1 3 1,growx");
    txtAdduct.setColumns(10);

    lblChargeMz = new JLabel("Charge; m/z");
    pnData.add(lblChargeMz, "cell 0 2");

    txtCharge = new JTextField();
    txtCharge.setToolTipText("Charge");
    txtCharge.setText("1");
    pnData.add(txtCharge, "cell 0 3,growx,aligny top");
    txtCharge.setColumns(4);

    txtPrecursorMZ = new JTextField();
    pnData.add(txtPrecursorMZ, "cell 0 4,growx,aligny top");
    txtPrecursorMZ.setColumns(9);

    btnFromScan = new JButton("From scan");
    btnFromScan.addActionListener(e -> setMZandChargeFromScan());
    pnData.add(btnFromScan, "cell 0 5,growx");

    lblTic = new JLabel("TIC=");
    pnData.add(lblTic, "cell 0 6,alignx trailing");

    lbTIC = new JLabel("0");
    pnData.add(lbTIC, "cell 1 6");

    lblSignals = new JLabel("signals: ");
    pnData.add(lblSignals, "cell 0 7,alignx trailing");

    lbSignals = new JLabel("0");
    pnData.add(lbSignals, "cell 1 7");

    lbMassListError = new JLabel("ERROR with masslist selection: Wrong name or no masslist");
    lbMassListError.setFont(new Font("Tahoma", Font.BOLD, 13));
    lbMassListError.setHorizontalAlignment(SwingConstants.CENTER);
    lbMassListError.setForeground(new Color(220, 20, 60));
    lbMassListError.setVisible(false);
    add(lbMassListError, BorderLayout.NORTH);
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
    pnData.revalidate();
    pnData.repaint();

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
    if (!isFragmentScan)
      return;

    if (scans != null && !scans.isEmpty()) {
      Scan scan = scans.get(selectedScanI);
      double mz = scan.getPrecursorMZ();
      if (mz == 0) {
        if (row != null)
          mz = row.getAverageMZ();
      }
      int charge = scan.getPrecursorCharge();
      if (charge == 0 && row != null)
        charge = row.getRowCharge();

      if (charge == 0)
        charge = 1;

      // set as text
      txtCharge.setText(String.valueOf(charge));
      txtPrecursorMZ.setText(MZmineCore.getConfiguration().getMZFormat().format(mz));
    }
  }

  public PeakListRow getRow() {
    return row;
  }

  public double getPrecursorMZ() {
    try {
      double c = Double.parseDouble(txtPrecursorMZ.getText());
      txtCharge.setBorder(BorderFactory.createLineBorder(Color.black));
      return c;
    } catch (Exception e) {
      txtCharge.setBorder(BorderFactory.createLineBorder(Color.red));
      return 0;
    }
  }

  public int getPrecursorCharge() {
    try {
      int c = Integer.parseInt(txtCharge.getText());
      txtCharge.setBorder(BorderFactory.createLineBorder(Color.black));
      return c;
    } catch (Exception e) {
      txtCharge.setBorder(BorderFactory.createLineBorder(Color.red));
      return 0;
    }
  }


  public boolean checkParameterValues(List<String> messages) {
    // no parameters for MS1 scan
    if (!isFragmentScan)
      return true;

    // for MS/MS scans:
    String adduct = getAdduct();
    if (adduct.isEmpty())
      messages.add("Adduct is not set properly: " + txtAdduct.getText());

    int charge = getPrecursorCharge();
    if (charge <= 0)
      messages.add("Charge is not set properly: " + txtCharge.getText());

    double mz = getPrecursorCharge();
    if (mz <= 0)
      messages.add("Precursor m/z is not set properly: " + txtPrecursorMZ.getText());

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
    EChartPanel chart = getChart();
    if (chart != null) {
      chart.getChart().getXYPlot().setBackgroundPaint(selected ? Color.WHITE : errorColor);
    }
  }

  public void setSortMode(ScanSortMode sort) {
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

  public void setFilter(String massListName, double noiseLevel, int minNumberOfSignals) {
    this.massListName = massListName;
    this.noiseLevel = noiseLevel;
    this.minNumberOfSignals = minNumberOfSignals;
    createSortedScanList();
  }


  /**
   * Creates a sorted list of all scans that match the minimum criteria
   */
  private void createSortedScanList() {
    if (row == null && scansEntry == null)
      return;
    // get all scans that match filter criteria
    try {
      if (row != null) {
        if (isFragmentScan) {
          // first entry is the best fragmentation scan
          scans = ScanUtils.listAllFragmentScans(row, massListName, noiseLevel, minNumberOfSignals,
              sort);
        } else {
          // get most representative MS 1 scans of all features
          scans =
              ScanUtils.listAllMS1Scans(row, massListName, noiseLevel, minNumberOfSignals, sort);
        }
      } else if (scansEntry != null) {
        scans =
            ScanUtils.listAllScans(scansEntry, massListName, noiseLevel, minNumberOfSignals, sort);
      }
      selectedScanI = 0;

      // no error
      lbMassListError.setVisible(false);
      revalidate();
      repaint();
    } catch (MissingMassListException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      // create error label
      lbMassListError.setVisible(true);
      revalidate();
      repaint();
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

  public JToggleButton getBtnToggleUse() {
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
    EChartPanel oldChart = spectrumPlot;
    // empty
    pnChart.removeAll();
    spectrumPlot = null;

    if (scans != null && !scans.isEmpty()) {
      Scan scan = scans.get(selectedScanI);
      RawDataFile raw = scan.getDataFile();
      // get MS/MS spectra window only for the spectra chart
      SpectraVisualizerWindow spectraWindow = new SpectraVisualizerWindow(raw);
      spectraWindow.loadRawData(scan);

      spectrumPlot = spectraWindow.getSpectrumPlot();
      spectrumPlot.getChart().getLegend().setVisible(showLegend);
      spectrumPlot.setMaximumSize(new Dimension(chartSize.width, 10000));
      spectrumPlot.setPreferredSize(chartSize);
      pnChart.add(spectrumPlot, BorderLayout.CENTER);

      analyzeScan(scan);
      applySelectionState();
      setValidSelection(true);
    } else {
      // add error label
      JLabel error = new JLabel(MessageFormat
          .format("NO MS2 SPECTRA: 0 of {0} match the minimum criteria", getTotalScans()));
      error.setFont(new Font("Tahoma", Font.BOLD, 13));
      error.setHorizontalAlignment(SwingConstants.CENTER);
      error.setForeground(new Color(220, 20, 60));
      pnChart.add(error, BorderLayout.CENTER);
      //
    }

    // listener on change
    if (listener != null && oldChart != spectrumPlot
        && (oldChart != null && !oldChart.equals(spectrumPlot))) {
      listener.accept(spectrumPlot);
    }

    revalidate();
    repaint();
  }

  private int getTotalScans() {
    if (row != null)
      return row.getAllMS2Fragmentations().length;
    if (scansEntry != null)
      return scansEntry.length;
    return 0;
  }

  private void setValidSelection(boolean state) {
    validSelection = state;
  }

  private void analyzeScan(Scan scan) {
    MassList massList = ScanUtils.getMassListOrFirst(scan, massListName);
    if (massList != null) {
      DataPoint[] dp = massList.getDataPoints();
      double tic = ScanUtils.getTIC(dp, noiseLevel);
      int signals = ScanUtils.getNumberOfSignals(dp, noiseLevel);
      lbTIC.setText(MZmineCore.getConfiguration().getIntensityFormat().format(tic));
      lbSignals.setText("" + signals);
      lbTIC.getParent().revalidate();
      lbTIC.getParent().repaint();
    }
  }

  @Nullable
  public DataPoint[] getFilteredDataPoints() {
    if (scans != null && !scans.isEmpty()) {
      Scan scan = scans.get(selectedScanI);
      MassList massList = ScanUtils.getMassListOrFirst(scan, massListName);
      if (massList != null)
        return ScanUtils.getFiltered(massList.getDataPoints(), noiseLevel);
    }
    return null;
  }

  public JLabel getLbMassListError() {
    return lbMassListError;
  }

  @Nullable
  public EChartPanel getChart() {
    return spectrumPlot;
  }

  public void addChartChangedListener(Consumer<EChartPanel> listener) {
    this.listener = listener;
  }

  public JLabel getLbTIC() {
    return lbTIC;
  }

  public JLabel getLbSignals() {
    return lbSignals;
  }

  /**
   * This will check adduct pattern and enforce it.
   * 
   * @return The adduct or an empty String for wrong input
   */
  @Nonnull
  public String getAdduct() {
    String adduct = txtAdduct.getText();

    String formatted = AdductParser.parse(adduct);
    if (formatted.isEmpty()) {
      txtAdduct.setBorder(BorderFactory.createLineBorder(Color.red));
      return "";
    }

    if (!formatted.equals(adduct)) {
      txtAdduct.setText(formatted);
    }
    txtAdduct.setBorder(BorderFactory.createLineBorder(Color.black));
    return formatted;
  }


  private static Icon createIcon(String path) {
    return new ImageIcon(new ImageIcon(ScanSelectPanel.class.getClassLoader().getResource(path))
        .getImage().getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH));
  }

  public void setChartSize(Dimension dim) {
    chartSize = dim;
  }

  /**
   * Valid spectrum and is selected? Still check for correct adduct
   * 
   * @return
   */
  public boolean isValidAndSelected() {
    return isSelected() && validSelection;
  }

  public JTextField getTxtCharge() {
    return txtCharge;
  }

  public JTextField getTxtPrecursorMZ() {
    return txtPrecursorMZ;
  }

  public boolean hasAdduct() {
    return !getAdduct().isEmpty();
  }

}
