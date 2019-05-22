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
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import javax.swing.border.EmptyBorder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.chartgroups.ChartGroup;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.chartbasics.gui.wrapper.ChartViewWrapper;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.LibrarySubmitModule;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.LibrarySubmitTask;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitParameters;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectrumDataSet;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.ComboComponent;
import net.sf.mzmine.parameters.parametertypes.DoubleComponent;
import net.sf.mzmine.parameters.parametertypes.IntegerComponent;
import net.sf.mzmine.parameters.parametertypes.MassListComponent;
import net.sf.mzmine.parameters.parametertypes.OptionalParameterComponent;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.DialogLoggerUtil;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.components.GridBagPanel;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.scans.sorting.ScanSortMode;

/**
 * Holds more charts for data reviewing
 * 
 * @author Robin Schmid
 *
 */
public class MSMSLibrarySubmissionWindow extends JFrame {

  private Logger log = Logger.getLogger(this.getClass().getName());
  protected Map<String, JComponent> parametersAndComponents;
  protected final LibrarySubmitParameters paramSubmit;
  protected final LibraryMetaDataParameters paramMeta = new LibraryMetaDataParameters();

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

  private PeakListRow[] rows;
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


    this.helpID = GUIUtils.generateHelpID(paramSubmit);
    if (helpID != null) {
      HelpButton btnHelp = new HelpButton(helpID);
      pnButtons.add(btnHelp);
    }

    JButton btnCheck = new JButton("Check");
    btnCheck.addActionListener(e -> {
      if (checkParameters())
        DialogLoggerUtil.showMessageDialogForTime(this, "Check OK", "All parameters are set", 1500);
    });
    pnButtons.add(btnCheck);

    JButton btnSubmit = new JButton("Submit");
    btnSubmit.addActionListener(e -> submitSpectra());
    pnButtons.add(btnSubmit);

    parametersAndComponents = new Hashtable<String, JComponent>();
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
    // listen for changes in masslist selection and preprocessing
    MassListComponent mc = getMassListComponent();
    mc.addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));
    DoubleComponent nc = getNoiseLevelComponent();
    nc.addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));
    IntegerComponent minc = getMinSignalComponent();
    minc.addDocumentListener(new DelayedDocumentListener(e -> updateSettingsOnAllSelectors()));
    ComboComponent<ScanSortMode> sortc = getComboSortMode();
    sortc.addItemListener(e -> updateSortModeOnAllSelectors());
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


      // add separator
      if (p.getName().equals(LibrarySubmitParameters.LOCALFILE.getName())) {
        pnSubmitParam.addSeparator(0, rowCounter, 2);
        rowCounter++;
      }

      JLabel label = new JLabel(p.getName());
      pnSubmitParam.add(label, 0, rowCounter);
      label.setLabelFor(comp);

      parametersAndComponents.put(p.getName(), comp);

      JComboBox t = new JComboBox();
      int comboh = t.getPreferredSize().height;
      int comph = comp.getPreferredSize().height;


      int verticalWeight = comph > 2 * comboh ? 1 : 0;
      pnSubmitParam.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight, GridBagConstraints.VERTICAL);
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
    for (Parameter<?> p : paramSubmit.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      JComponent component = parametersAndComponents.get(p.getName());
      up.setValueFromComponent(component);
    }
    for (Parameter<?> p : paramMeta.getParameters()) {
      if (!(p instanceof UserParameter))
        continue;
      UserParameter up = (UserParameter) p;
      JComponent component = parametersAndComponents.get(p.getName());
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

    if (paramSubmit.checkParameterValues(messages) && paramMeta.checkParameterValues(messages)) {
      return true;
    } else {
      String message = messages.stream().collect(Collectors.joining("\n"));
      MZmineCore.getDesktop().displayMessage(this, message);
      return false;
    }
  }

  private JComponent getComponentForParameter(UserParameter p) {
    return parametersAndComponents.get(p.getName());
  }

  /**
   * Submit. Checks parameters and adduct for each selected ion
   */
  private void submitSpectra() {
    if (checkParameters()) {
      // check number of ions
      int ions = Arrays.stream(pnScanSelect).mapToInt(pn -> pn.isValidAndSelected() ? 1 : 0).sum();
      int adducts = Arrays.stream(pnScanSelect)
          .mapToInt(pn -> pn.isValidAndSelected() && pn.hasAdduct() ? 1 : 0).sum();
      // every valid selected ion needs an adduct
      if (ions != adducts) {
        MZmineCore.getDesktop().displayErrorMessage(this, "ERROR",
            MessageFormat.format(
                "Not all adducts are set: {0} ion spectra selected and only {1}  adducts set", ions,
                adducts));
        return;
      } else {
        if (ions == 0) {
          log.info("No MS/MS spectrum selected or valid");
          DialogLoggerUtil.showMessageDialogForTime(this, "Error",
              "No MS/MS spectrum selected or valid", 1500);
        } else {

          String allAdducts =
              Arrays.stream(pnScanSelect).filter(pn -> pn.isValidAndSelected() && pn.hasAdduct())
                  .map(ScanSelectPanel::getAdduct).collect(Collectors.joining(", "));
          // show accept dialog
          if (DialogLoggerUtil.showDialogYesNo(this, "Submission?",
              ions + " MS/MS spectra were selected. Submit? (" + allAdducts + ")")) {
            // submit to GNPS
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
            log.info("Added task to export library entries: " + ions
                + " MS/MS spectra were selected (" + allAdducts + ")");
            LibrarySubmitTask task = new LibrarySubmitTask(map);
            MZmineCore.getTaskController().addTask(task);
          }
        }
      }
    }
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
    ionParam.getParameter(LibrarySubmitIonParameters.ADDUCT).setValue(ion.getAdduct());
    ionParam.getParameter(LibrarySubmitIonParameters.CHARGE).setValue(ion.getPrecursorCharge());
    ionParam.getParameter(LibrarySubmitIonParameters.MZ).setValue(ion.getPrecursorMZ());
    return (LibrarySubmitIonParameters) ionParam.cloneParameterSet();
  }

  private void updateSortModeOnAllSelectors() {
    ScanSortMode sort = (ScanSortMode) getComboSortMode().getSelectedItem();
    if (pnScanSelect != null)
      for (ScanSelectPanel pn : pnScanSelect)
        pn.setSortMode(sort);
  }

  private void updateSettingsOnAllSelectors() {
    if (checkInput()) {
      Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
      Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
      String massListName = paramSubmit.getParameter(LibrarySubmitParameters.massList).getValue();
      if (pnScanSelect != null && minSignals != null && noiseLevel != null && massListName != null)
        for (ScanSelectPanel pn : pnScanSelect)
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
    this.pnScanSelect = new ScanSelectPanel[rows.length];

    // set rt
    double rt = Arrays.stream(rows).mapToDouble(PeakListRow::getAverageRT).average().orElse(-1);
    setRetentionTimeToComponent(rt);

    updateAllChartSelectors();
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
      String massListName = paramSubmit.getParameter(LibrarySubmitParameters.massList).getValue();
      if (minSignals != null && noiseLevel != null && massListName != null) {
        ScanSortMode sort = (ScanSortMode) getComboSortMode().getSelectedItem();

        // MS2 of all rows
        for (int i = 0; i < rows.length; i++) {
          PeakListRow row = rows[i];
          ScanSelectPanel pn = new ScanSelectPanel(row, sort, noiseLevel, minSignals, massListName);
          pnScanSelect[i] = pn;
          pn.addChartChangedListener(chart -> regroupCharts());
          pnCharts.add(pn);

          // add to group
          EChartPanel c = pn.getChart();
          if (c != null) {
            group.add(new ChartViewWrapper(c));
          }
        }
      }
    }

    pnCharts.revalidate();
    pnCharts.repaint();
  }

  private void regroupCharts() {
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    if (pnScanSelect != null) {
      for (ScanSelectPanel pn : pnScanSelect) {
        EChartPanel chart = pn.getChart();
        if (chart != null)
          group.add(new ChartViewWrapper(chart));
      }
    }
  }

  private boolean checkInput() {
    updateParameterSetFromComponents();
    Integer minSignals = paramSubmit.getParameter(LibrarySubmitParameters.minSignals).getValue();
    if (minSignals == null)
      getMinSignalComponent().getTextField().setBackground(errorColor);
    else
      getMinSignalComponent().getTextField().setBackground(Color.white);

    Double noiseLevel = paramSubmit.getParameter(LibrarySubmitParameters.noiseLevel).getValue();
    if (noiseLevel == null)
      getNoiseLevelComponent().getTextField().setBackground(errorColor);
    else
      getNoiseLevelComponent().getTextField().setBackground(Color.white);

    return noiseLevel != null && minSignals != null;
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



  private IntegerComponent getMinSignalComponent() {
    return (IntegerComponent) getComponentForParameter(LibrarySubmitParameters.minSignals);
  }

  private DoubleComponent getNoiseLevelComponent() {
    return (DoubleComponent) getComponentForParameter(LibrarySubmitParameters.noiseLevel);
  }

  private MassListComponent getMassListComponent() {
    return (MassListComponent) getComponentForParameter(LibrarySubmitParameters.massList);
  }

  private ComboComponent<ScanSortMode> getComboSortMode() {
    return (ComboComponent<ScanSortMode>) getComponentForParameter(LibrarySubmitParameters.sorting);
  }
}
