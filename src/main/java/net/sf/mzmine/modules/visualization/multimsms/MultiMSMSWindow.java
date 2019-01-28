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
package net.sf.mzmine.modules.visualization.multimsms;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.xy.XYDataItem;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import net.sf.mzmine.chartbasics.chartgroups.ChartGroup;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.chartbasics.gui.wrapper.ChartViewWrapper;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.ms2.MSMSIonIdentity;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrum;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrumDataSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Holds more charts for data reviewing
 * 
 * @author Robin Schmid
 *
 */
public class MultiMSMSWindow extends JFrame {

  // annotations for MSMS
  private List<AbstractMSMSIdentity> msmsAnnotations;
  // to flag annotations in spectra

  private boolean exchangeTolerance = true;
  private MZTolerance mzTolerance = new MZTolerance(0.0015, 2.5d);

  // MS 1
  private ChartViewWrapper msone;

  // MS 2
  private ChartGroup group;
  //
  private JPanel contentPane;
  private JPanel pnCharts;
  private int col = 4;
  private int realCol = col;
  private boolean autoCol = true;
  private boolean alwaysShowBest = false;
  private boolean showTitle = false;
  private boolean showLegend = false;
  // only the last doamin axis
  private boolean onlyShowOneAxis = true;
  // click marker in all of the group
  private boolean showCrosshair = true;


  private JLabel lbRawIndex;
  private JPanel pnTopMenu;
  private JLabel lbRawName;
  private JButton nextRaw, prevRaw;
  private JCheckBox cbBestRaw;
  private JCheckBox cbUseBestForMissingRaw;

  private PeakListRow[] rows;
  private RawDataFile raw;
  private RawDataFile[] allRaw;
  private boolean createMS1;
  private int rawIndex;
  private boolean useBestForMissingRaw;

  /**
   * Create the frame.
   */
  public MultiMSMSWindow() {
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setBounds(100, 100, 853, 586);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);


    pnTopMenu = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    contentPane.add(pnTopMenu, BorderLayout.NORTH);
    lbRawIndex = new JLabel("");
    pnTopMenu.add(lbRawIndex);
    lbRawName = new JLabel("");
    pnTopMenu.add(lbRawName);

    prevRaw = new JButton("<");
    pnTopMenu.add(prevRaw);
    prevRaw.addActionListener(e -> {
      prevRaw();
    });

    nextRaw = new JButton(">");
    pnTopMenu.add(nextRaw);
    nextRaw.addActionListener(e -> {
      nextRaw();
    });

    cbBestRaw = new JCheckBox("use best for each");
    pnTopMenu.add(cbBestRaw);
    cbBestRaw.addItemListener(e -> {
      setAlwaysShowBest(cbBestRaw.isSelected());
    });

    cbUseBestForMissingRaw = new JCheckBox("use best missing raw");
    pnTopMenu.add(cbUseBestForMissingRaw);
    cbUseBestForMissingRaw.addItemListener(e -> {
      setUseBestForMissing(cbUseBestForMissingRaw.isSelected());
    });


    pnCharts = new JPanel();
    contentPane.add(pnCharts, BorderLayout.CENTER);
    pnCharts.setLayout(new GridLayout(0, 4));


    addMenu();
  }

  /**
   * Show best for missing MSMS in raw (if not selected none is shown)
   * 
   * @param selected
   */
  public void setUseBestForMissing(boolean selected) {
    useBestForMissingRaw = selected;
    updateAllCharts();
  }

  private void nextRaw() {
    if (allRaw == null)
      return;
    if (rawIndex < allRaw.length - 1) {
      rawIndex++;
      setRaw(allRaw[rawIndex], true);
    }
  }

  private void prevRaw() {
    if (allRaw == null)
      return;
    if (rawIndex > 0) {
      rawIndex--;
      setRaw(allRaw[rawIndex], true);
    }
  }

  /**
   * set raw data file and update
   * 
   * @param raw
   */
  public void setRaw(RawDataFile raw, boolean update) {
    this.raw = raw;

    this.rawIndex = 0;
    if (raw != null) {
      for (int i = 0; i < allRaw.length; i++) {
        if (raw.equals(allRaw[i])) {
          rawIndex = i;
          break;
        }
      }
    }

    lbRawName.setText(raw == null ? "" : raw.getName());
    lbRawIndex.setText("(" + rawIndex + ") ");
    updateAllCharts();
  }

  public void setAlwaysShowBest(boolean alwaysShowBest) {
    this.alwaysShowBest = alwaysShowBest;
    updateAllCharts();
  }

  private void addMenu() {
    JMenuBar menu = new JMenuBar();
    JMenu settings = new JMenu("Settings");
    menu.add(settings);

    JFrame thisframe = this;

    // set columns
    JMenuItem setCol = new JMenuItem("set columns");
    menu.add(setCol);
    setCol.addActionListener(e -> {
      try {
        col = Integer.parseInt(JOptionPane.showInputDialog("Columns", col));
        setAutoColumns(false);
        setColumns(col);
      } catch (Exception e2) {
      }
    });

    // reset zoom
    JMenuItem resetZoom = new JMenuItem("reset zoom");
    menu.add(resetZoom);
    resetZoom.addActionListener(e -> {
      if (group != null)
        group.resetZoom();
    });

    //
    addCheckBox(settings, "auto columns", autoCol,
        e -> setAutoColumns(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show one axis only", onlyShowOneAxis,
        e -> setOnlyShowOneAxis(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show legend", showLegend,
        e -> setShowLegend(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show title", showTitle,
        e -> setShowTitle(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show crosshair", showCrosshair,
        e -> setShowCrosshair(((JCheckBoxMenuItem) e.getSource()).isSelected()));;


    this.setJMenuBar(menu);
  }

  public void setColumns(int col2) {
    col = col2;
    renewCharts(group);
  }

  public void setAutoColumns(boolean selected) {
    this.autoCol = selected;
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
      axis.setVisible(!onlyShowOneAxis || i >= group.size() - realCol);
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
  public void setData(PeakListRow[] rows, RawDataFile[] allRaw, RawDataFile raw, boolean createMS1,
      SortingProperty sorting, SortingDirection direction) {
    Arrays.sort(rows, new PeakListRowSorter(sorting, direction));
    setData(rows, allRaw, raw, createMS1);
  }

  /**
   * Create charts and show
   * 
   * @param rows
   * @param raw
   */
  public void setData(PeakListRow[] rows, RawDataFile[] allRaw, RawDataFile raw,
      boolean createMS1) {
    this.rows = rows;
    this.allRaw = allRaw;
    this.createMS1 = createMS1;
    // set raw and not update
    setRaw(raw, false);

    updateAllCharts();
  }

  /**
   * Create new charts
   */
  public void updateAllCharts() {
    msone = null;
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    // MS1
    if (createMS1) {
      Scan scan = null;
      Feature best = null;
      for (PeakListRow r : rows) {
        Feature f = raw == null ? r.getBestPeak() : r.getPeak(raw);
        if (f != null && (best == null || f.getHeight() > best.getHeight())) {
          best = f;
        }
      }
      if (best != null) {
        scan = best.getDataFile().getScan(best.getRepresentativeScanNumber());
        EChartPanel cp = SpectrumChartFactory.createChartPanel(scan, showTitle, showLegend);
        if (cp != null)
          msone = new ChartViewWrapper(cp);
      }
    } else {
      // pseudo MS1 from all rows and isotope pattern
      EChartPanel cp = PseudoSpectrum.createChartPanel(rows, raw, false, "pseudo");
      if (cp != null) {
        cp.getChart().getLegend().setVisible(showLegend);
        cp.getChart().getTitle().setVisible(showTitle);
        msone = new ChartViewWrapper(cp);
      }
    }

    if (msone != null)
      group.add(msone);

    // COMMON
    // MS2 of all rows
    for (PeakListRow row : rows) {
      EChartPanel c = SpectrumChartFactory.createMSMSChartPanel(row, raw, showTitle, showLegend,
          alwaysShowBest, useBestForMissingRaw);

      if (c != null) {
        // add MSMS annotations of sub formulas
        addSubFormulaAnnotation(row,
            (PseudoSpectrumDataSet) c.getChart().getXYPlot().getDataset(0));

        group.add(new ChartViewWrapper(c));
      }
    }

    // add all MSMS annotations
    addAllMSMSAnnotations(rows, raw);
    renewCharts(group);
  }

  private void addSubFormulaAnnotation(PeakListRow row, PseudoSpectrumDataSet data) {
    MolecularFormulaIdentity form = null;
    if (row.getBestIonIdentity() != null && row.getBestIonIdentity().getNetwork() != null
        && row.getBestIonIdentity().getNetwork().getBestMolFormula() != null) {
      form = row.getBestIonIdentity().getNetwork().getBestMolFormula();

      if (row.getBestIonIdentity() != null && row.getBestIonIdentity().getBestMolFormula() != null)
        for (MolecularFormulaIdentity f : row.getBestIonIdentity().getMolFormulas()) {
          if (f.equalFormula(form)) {
            form = f;
            break;
          }
        }
    } else if (row.getBestIonIdentity() != null
        && row.getBestIonIdentity().getBestMolFormula() != null)
      form = row.getBestIonIdentity().getBestMolFormula();

    if (form != null && form.getMSMSannotation() != null) {
      try {
        IMolecularFormula ionf =
            row.getBestIonIdentity().getIonType().addToFormula(form.getFormulaAsObject());
        Map<DataPoint, String> ann = form.getMSMSannotation();
        ann.entrySet().forEach(e -> {
          // neutral loss
          IMolecularFormula loss = FormulaUtils.createMajorIsotopeMolFormula(e.getValue());
          try {
            loss = FormulaUtils.subtractFormula((IMolecularFormula) ionf.clone(), loss);
          } catch (CloneNotSupportedException e2) {
            loss = null;
          }

          String id = loss != null ? MolecularFormulaManipulator.getString(loss) + "\n" : "";
          id += "-" + e.getValue();
          data.addAnnotation(new XYDataItem(e.getKey().getMZ(), e.getKey().getIntensity()), id);
        });
      } catch (CloneNotSupportedException e2) {
      }
    }
  }

  /**
   * Adds all MS1 and MSMS annotations to all charts
   * 
   * @param rows
   * @param raw
   */
  public void addAllMSMSAnnotations(PeakListRow[] rows, RawDataFile raw) {
    for (PeakListRow row : rows) {
      // add MS1 annotations
      IonIdentity best = row.getBestIonIdentity();
      if (best == null)
        continue;

      Scan scan = SpectrumChartFactory.getMSMSScan(row, raw, alwaysShowBest, useBestForMissingRaw);
      if (scan != null) {
        Feature f = row.getPeak(scan.getDataFile());
        double precursorMZ = f != null ? f.getMZ() : row.getAverageMZ();
        // add ms1 adduct annotation
        addMSMSAnnotation(new MSMSIonIdentity(mzTolerance, new SimpleDataPoint(precursorMZ, 1f),
            best.getIonType()));

        // add all MSMS annotations (found in MSMS)
        if (row.hasIonIdentity()) {
          for (IonIdentity id : row.getIonIdentities()) {
            addMSMSAnnotations(id.getMSMSIdentities());
          }
        }
      }
    }
  }

  /**
   * 
   * @param group
   */
  public void renewCharts(ChartGroup group) {
    pnCharts.removeAll();
    if (group != null && group.size() > 0) {
      realCol = autoCol ? (int) Math.floor(Math.sqrt(group.size())) - 1 : col;
      if (realCol < 1)
        realCol = 1;
      GridLayout layout = new GridLayout(0, realCol);
      pnCharts.setLayout(layout);
      // add to layout
      int i = 0;
      for (ChartViewWrapper cp : group.getList()) {
        // show only the last domain axes
        ValueAxis axis = cp.getChart().getXYPlot().getDomainAxis();
        axis.setVisible(!onlyShowOneAxis || i >= group.size() - realCol);

        pnCharts.add(cp.getChartSwing());
        i++;
      }
    }
    pnCharts.revalidate();
    pnCharts.repaint();
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

    int start = msone == null ? 0 : 1;
    for (int i = start; i < group.getList().size(); i++)
      op.accept(group.getList().get(i).getChart());
  }
}
