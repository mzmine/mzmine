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
package net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.chartgroups.ChartGroup;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.chartbasics.gui.wrapper.ChartViewWrapper;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.gnpslibrarysubmit.LibrarySubmitParameters.CompoundSource;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.components.GridBagPanel;

/**
 * Holds more charts for data reviewing
 * 
 * @author Robin Schmid
 *
 */
public class MSMSLibrarySubmissionWindow extends JFrame {

  protected Map<String, JComponent> parametersAndComponents;
  protected LibrarySubmitParameters param = new LibrarySubmitParameters();

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
  // only the last doamin axis
  private boolean onlyShowOneAxis = true;
  // click marker in all of the group
  private boolean showCrosshair = true;
  private JPanel pnSideMenu;

  private PeakListRow[] rows;
  private JPanel pnSettings;
  private GridBagPanel pnMetaData;
  private JPanel pnButtons;

  private JLabel lblSettings;
  private JLabel lblMetadata;
  private JLabel lblNoise;
  private JTextField txtNoiseLevel;
  private JScrollPane scrollCharts;
  private JLabel lblMinSignals;
  private JTextField txtMinSignals;
  private JLabel lblSorting;
  private JComboBox<ScanSortMode> comboSortMode;
  private final Color errorColor = Color.decode("#ff8080");
  private JLabel lblMassList;
  private JTextField txtMassListName;
  private ScanSelectPanel[] panel;
  private JLabel lblCompoundName;
  private JLabel lblMoleculeMass;
  private JLabel lblInstrument;
  private JLabel lblIonSource;
  private JLabel lblSmiles;
  private JLabel lblInchi;
  private JLabel lblInchiAux;
  private JLabel lblIonMode;
  private JLabel lblPub;
  private JLabel lblCas;
  private JLabel lblPi;
  private JTextField txtPI;
  private JTextField txtCompoundName;
  private JTextField txtMoleculeMass;
  private JTextField txtInstrument;
  private JTextField txtIonSource;
  private JTextField txtPubMed;
  private JTextField txtSMILES;
  private JTextField txtINCHI;
  private JTextField txtINCHIAUX;
  private JTextField txtCAS;
  private JComboBox<String> comboPolarity;
  private JLabel lblCompoundSource;
  private JComboBox<CompoundSource> comboSource;
  private JLabel lblDataCollector;
  private JTextField txtDataCollector;
  private JTextField txtFormula;
  private JLabel lblFormula;
  private JTextField txtPassword;
  private JTextField txtUsername;
  private JLabel lblUsername;
  private JLabel lblPassword;

  private JScrollPane scrollMeta;

  /**
   * Create the frame.
   */
  public MSMSLibrarySubmissionWindow() {
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setBounds(100, 100, 854, 619);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);


    pnSideMenu = new JPanel();
    contentPane.add(pnSideMenu, BorderLayout.EAST);
    pnSideMenu.setLayout(new BorderLayout(0, 0));

    pnSettings = new JPanel();
    pnSideMenu.add(pnSettings, BorderLayout.NORTH);
    pnSettings.setLayout(new MigLayout("", "[][grow]", "[][][][][]"));

    lblSettings = new JLabel("Settings");
    lblSettings.setFont(new Font("Tahoma", Font.BOLD, 11));
    pnSettings.add(lblSettings, "cell 0 0");

    lblMassList = new JLabel("masslist");
    pnSettings.add(lblMassList, "cell 0 1,alignx trailing");

    txtMassListName = new JTextField();
    txtMassListName.setToolTipText("Masslist name or empty to use the first masslist");
    txtMassListName.setColumns(10);
    pnSettings.add(txtMassListName, "cell 1 1,growx");
    txtMassListName.getDocument()
        .addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));

    lblNoise = new JLabel("noise level");
    pnSettings.add(lblNoise, "cell 0 2,alignx trailing");

    txtNoiseLevel = new JTextField();
    txtNoiseLevel.setToolTipText("Noise level to cut out noise/signals");
    txtNoiseLevel.setText("0");
    pnSettings.add(txtNoiseLevel, "cell 1 2,growx");
    txtNoiseLevel.setColumns(10);
    txtNoiseLevel.getDocument()
        .addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));

    lblMinSignals = new JLabel("min signals");
    pnSettings.add(lblMinSignals, "cell 0 3,alignx trailing");

    txtMinSignals = new JTextField();
    txtMinSignals.setToolTipText("Minimum signals to consider a scan");
    txtMinSignals.setText("3");
    txtMinSignals.setColumns(10);
    pnSettings.add(txtMinSignals, "cell 1 3,growx");
    txtMinSignals.getDocument()
        .addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));

    lblSorting = new JLabel("sorting");
    pnSettings.add(lblSorting, "cell 0 4,alignx trailing");

    comboSortMode = new JComboBox<>(ScanSortMode.values());
    comboSortMode
        .setToolTipText("Set the scan sorting property for all selected feature list rows");
    comboSortMode.setSelectedItem(ScanSortMode.MAX_TIC);
    comboSortMode.addItemListener(il -> updateSortModeOnAllSelectors());
    pnSettings.add(comboSortMode, "cell 1 4,growx");

    // buttons
    pnButtons = new JPanel();
    pnSideMenu.add(pnButtons, BorderLayout.SOUTH);

    JButton btnCheck = new JButton("Check");
    btnCheck.addActionListener(e -> checkParameters());
    pnButtons.add(btnCheck);

    JButton btnSubmit = new JButton("Submit");
    btnSubmit.addActionListener(e -> submitSpectra());
    pnButtons.add(btnSubmit);

    createMetaDataPanel();


    scrollCharts = new JScrollPane();
    contentPane.add(scrollCharts, BorderLayout.CENTER);

    pnCharts = new JPanel();
    pnCharts.setLayout(new GridLayout(0, 1));
    scrollCharts.setViewportView(pnCharts);

    addMenu();
  }

  private void createMetaDataPanel() {
    parametersAndComponents = new Hashtable<String, JComponent>();
    // Main panel which holds all the components in a grid
    pnMetaData = new GridBagPanel();
    scrollMeta = new JScrollPane(pnMetaData);
    scrollMeta.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollMeta.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pnSideMenu.add(scrollMeta, BorderLayout.CENTER);

    int rowCounter = 0;
    int vertWeightSum = 0;

    // Create labels and components for each parameter
    for (Parameter p : param.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;

      JComponent comp = up.createEditingComponent();
      comp.setToolTipText(up.getDescription());

      // Set the initial value
      Object value = up.getValue();
      if (value != null)
        up.setValueToComponent(comp, value);

      // By calling this we make sure the components will never be resized
      // smaller than their optimal size
      comp.setMinimumSize(comp.getPreferredSize());

      comp.setToolTipText(up.getDescription());

      JLabel label = new JLabel(p.getName());
      pnMetaData.add(label, 0, rowCounter);
      label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      JComboBox t = new JComboBox();
      int comboh = t.getPreferredSize().height;
      int comph = comp.getPreferredSize().height;

      // Multiple selection will be expandable, other components not
      int verticalWeight = comph > 2 * comboh ? 1 : 0;
      vertWeightSum += verticalWeight;

      pnMetaData.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight, GridBagConstraints.VERTICAL);

      rowCounter++;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void updateParameterSetFromComponents() {
    for (Parameter<?> p : param.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      JComponent component = parametersAndComponents.get(p.getName());
      up.setValueFromComponent(component);
    }
  }

  protected int getNumberOfParameters() {
    return param.getParameters().length;
  }

  private void createMetaDataPanelOld() {
    pnMetaData = new GridBagPanel();
    scrollMeta = new JScrollPane(pnMetaData);
    scrollMeta.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    scrollMeta.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pnSideMenu.add(scrollMeta, BorderLayout.CENTER);
    pnMetaData
        .setLayout(new MigLayout("", "[trailing][grow]", "[][][][][][][][][][][][][][][][][]"));

    lblMetadata = new JLabel("Metadata");
    lblMetadata.setFont(new Font("Tahoma", Font.BOLD, 11));
    pnMetaData.add(lblMetadata, "cell 0 0,alignx center");

    lblUsername = new JLabel("Username");
    pnMetaData.add(lblUsername, "cell 0 1,alignx trailing");

    txtUsername = new JTextField();
    txtUsername.setColumns(10);
    pnMetaData.add(txtUsername, "cell 1 1,growx");

    lblPassword = new JLabel("Password");
    pnMetaData.add(lblPassword, "cell 0 2,alignx trailing,aligny baseline");

    txtPassword = new JTextField();
    pnMetaData.add(txtPassword, "cell 1 2,growx");
    txtPassword.setColumns(10);

    lblPi = new JLabel("PI");
    lblPi.setToolTipText("Principle investigator");
    pnMetaData.add(lblPi, "cell 0 3,alignx trailing");

    txtPI = new JTextField();
    pnMetaData.add(txtPI, "cell 1 3,growx");
    txtPI.setColumns(10);

    lblDataCollector = new JLabel("Data collector");
    pnMetaData.add(lblDataCollector, "cell 0 4,alignx trailing");

    txtDataCollector = new JTextField();
    txtDataCollector.setColumns(10);
    pnMetaData.add(txtDataCollector, "cell 1 4,growx");

    lblCompoundName = new JLabel("Compound name");
    pnMetaData.add(lblCompoundName, "cell 0 5,alignx trailing");

    txtCompoundName = new JTextField();
    pnMetaData.add(txtCompoundName, "cell 1 5,growx");
    txtCompoundName.setColumns(10);

    lblMoleculeMass = new JLabel("Molecule mass");
    pnMetaData.add(lblMoleculeMass, "cell 0 6,alignx trailing");

    txtMoleculeMass = new JTextField();
    txtMoleculeMass.setColumns(10);
    pnMetaData.add(txtMoleculeMass, "cell 1 6,growx");

    lblFormula = new JLabel("Formula");
    pnMetaData.add(lblFormula, "cell 0 7,alignx trailing");

    txtFormula = new JTextField();
    txtFormula.setColumns(10);
    pnMetaData.add(txtFormula, "cell 1 7,growx");

    lblIonMode = new JLabel("Ion mode");
    pnMetaData.add(lblIonMode, "cell 0 8,alignx trailing");

    comboPolarity = new JComboBox<>();
    comboPolarity.setModel(new DefaultComboBoxModel<String>(new String[] {"POSITIVE", "NEGATIVE"}));
    comboPolarity.setSelectedIndex(0);
    pnMetaData.add(comboPolarity, "cell 1 8,growx");

    lblInstrument = new JLabel("Instrument");
    pnMetaData.add(lblInstrument, "cell 0 9,alignx trailing");

    txtInstrument = new JTextField();
    txtInstrument.setColumns(10);
    pnMetaData.add(txtInstrument, "cell 1 9,growx");

    lblIonSource = new JLabel("Ion source");
    pnMetaData.add(lblIonSource, "flowy,cell 0 10,alignx trailing");

    txtIonSource = new JTextField();
    txtIonSource.setColumns(10);
    pnMetaData.add(txtIonSource, "cell 1 10,growx");

    lblCompoundSource = new JLabel("Compound source");
    pnMetaData.add(lblCompoundSource, "cell 0 11,alignx trailing");

    comboSource = new JComboBox<>(CompoundSource.values());
    comboSource.setSelectedItem(CompoundSource.Crude);
    pnMetaData.add(comboSource, "cell 1 9,growx");

    lblPub = new JLabel("PubMed");
    pnMetaData.add(lblPub, "cell 0 12,alignx trailing");

    txtPubMed = new JTextField();
    txtPubMed.setColumns(10);
    pnMetaData.add(txtPubMed, "cell 1 12,growx");

    lblSmiles = new JLabel("SMILES");
    pnMetaData.add(lblSmiles, "cell 0 13,alignx trailing");

    txtSMILES = new JTextField();
    txtSMILES.setColumns(10);
    pnMetaData.add(txtSMILES, "cell 1 13,growx");

    lblInchi = new JLabel("InChI");
    pnMetaData.add(lblInchi, "cell 0 14,alignx trailing");

    txtINCHI = new JTextField();
    txtINCHI.setColumns(10);
    pnMetaData.add(txtINCHI, "cell 1 14,growx");

    lblInchiAux = new JLabel("InChI AUX");
    pnMetaData.add(lblInchiAux, "cell 0 15,alignx trailing");

    txtINCHIAUX = new JTextField();
    txtINCHIAUX.setColumns(10);
    pnMetaData.add(txtINCHIAUX, "cell 1 15,growx");

    lblCas = new JLabel("CAS");
    pnMetaData.add(lblCas, "cell 0 16,alignx trailing");

    txtCAS = new JTextField();
    txtCAS.setColumns(10);
    pnMetaData.add(txtCAS, "cell 1 16,growx");
  }

  protected boolean checkParameters() {
    // commit the changes to the parameter set
    updateParameterSetFromComponents();

    // check
    ArrayList<String> messages = new ArrayList<String>();
    boolean allParametersOK = param.checkParameterValues(messages);

    if (!allParametersOK) {
      StringBuilder message = new StringBuilder("Please check the parameter settings:\n\n");
      for (String m : messages) {
        message.append(m);
        message.append("\n");
      }
      MZmineCore.getDesktop().displayMessage(this, message.toString());
      return false;
    }
    return true;
  }

  private void submitSpectra() {
    if (checkParameters()) {
      // check number of ions

      // show accept dialog

    }
  }

  private void updateSortModeOnAllSelectors() {
    ScanSortMode sort = (ScanSortMode) comboSortMode.getSelectedItem();
    if (panel != null)
      for (ScanSelectPanel pn : panel)
        pn.setSortMode(sort);
  }

  private void updateSettingsOnAllSelectors() {
    if (checkInput()) {
      int minSignals = Integer.parseInt(txtMinSignals.getText());
      double noiseLevel = Double.parseDouble(txtNoiseLevel.getText());
      String massListName = txtMassListName.getText();
      if (panel != null)
        for (ScanSelectPanel pn : panel)
          pn.setFilter(massListName, noiseLevel, minSignals);
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

    JMenuItem setSize = new JMenuItem("chart size");
    menu.add(setSize);
    setSize.addActionListener(e -> {
      Dimension dim = SizeSelectDialog.getSizeInput();
      if (dim != null)
        setChartSize(dim);
    });

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
    if (panel != null && dim != null)
      for (ScanSelectPanel pn : panel)
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
    this.onlyShowOneAxis = onlyShowOneAxis;
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

  /**
   * Sort rows
   * 
   * @param rows
   * @param raw
   * @param sorting
   * @param direction
   */
  public void setData(PeakListRow[] rows, SortingProperty sorting, SortingDirection direction) {
    Arrays.sort(rows, new PeakListRowSorter(sorting, direction));
    setData(rows);
  }

  /**
   * Create charts and show
   * 
   * @param rows
   * @param raw
   */
  public void setData(PeakListRow[] rows) {
    this.rows = rows;
    this.panel = new ScanSelectPanel[rows.length];
    updateAllChartSelectors();
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
      int minSignals = Integer.parseInt(txtMinSignals.getText());
      double noiseLevel = Double.parseDouble(txtNoiseLevel.getText());
      ScanSortMode sort = (ScanSortMode) comboSortMode.getSelectedItem();
      String massListName = txtMassListName.getText();
      // MS2 of all rows
      for (int i = 0; i < rows.length; i++) {
        PeakListRow row = rows[i];
        ScanSelectPanel pn = new ScanSelectPanel(row, sort, noiseLevel, minSignals, massListName);
        panel[i] = pn;
        pn.addChartChangedListener(chart -> regroupCharts());
        pnCharts.add(pn);

        // add to group
        EChartPanel c = pn.getChart();
        if (c != null) {
          group.add(new ChartViewWrapper(c));
        }
      }
    }

    pnCharts.revalidate();
    pnCharts.repaint();
  }

  private void regroupCharts() {
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    if (panel != null) {
      for (ScanSelectPanel pn : panel) {
        EChartPanel chart = pn.getChart();
        if (chart != null)
          group.add(new ChartViewWrapper(chart));
      }
    }
  }

  private boolean checkInput() {
    boolean result = true;
    try {
      Integer.parseInt(txtMinSignals.getText());
      txtMinSignals.setBackground(Color.white);
    } catch (Exception e) {
      txtMinSignals.setBackground(errorColor);
      result = false;
    }
    try {
      Double.parseDouble(txtNoiseLevel.getText());
      txtNoiseLevel.setBackground(Color.white);
    } catch (Exception e) {
      txtNoiseLevel.setBackground(errorColor);
      result = false;
    }
    return result;
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

  public JComboBox getComboSortMode() {
    return comboSortMode;
  }

  public JTextField getTxtMinSignals() {
    return txtMinSignals;
  }

  public JTextField getTxtNoiseLevel() {
    return txtNoiseLevel;
  }

  public JTextField getTextField() {
    return txtMassListName;
  }

  public JTextField getTxtCAS() {
    return txtCAS;
  }

  public JTextField getTxtINCHIAUX() {
    return txtINCHIAUX;
  }

  public JTextField getTxtINCHI() {
    return txtINCHI;
  }

  public JTextField getTxtSMILES() {
    return txtSMILES;
  }

  public JTextField getTxtPubMed() {
    return txtPubMed;
  }

  public JComboBox getComboSource() {
    return comboSource;
  }

  public JTextField getTxtIonSource() {
    return txtIonSource;
  }

  public JTextField getTxtInstrument() {
    return txtInstrument;
  }

  public JComboBox getComboPolarity() {
    return comboPolarity;
  }

  public JTextField getTxtFormula() {
    return txtFormula;
  }

  public JTextField getTxtMoleculeMass() {
    return txtMoleculeMass;
  }

  public JTextField getTxtCompoundName() {
    return txtCompoundName;
  }

  public JTextField getTxtDataCollector() {
    return txtDataCollector;
  }

  public JTextField getTxtPI() {
    return txtPI;
  }

  public JTextField getTxtUsername() {
    return txtUsername;
  }

  public JTextField getTxtPassword() {
    return txtPassword;
  }
}
