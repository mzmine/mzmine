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

package net.sf.mzmine.modules.tools.isotopepatternpreview;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import net.sf.mzmine.chartbasics.chartthemes.EIsotopePatternChartTheme;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.visualization.spectra.datasets.ExtendedIsotopePatternDataSet;
import net.sf.mzmine.modules.visualization.spectra.renderers.SpectraToolTipGenerator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.DoubleComponent;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerComponent;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PercentComponent;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringComponent;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.FormulaUtils;

/**
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class IsotopePatternPreviewDialog extends ParameterSetupDialog {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intFormat = new DecimalFormat("0.00 %");
  private NumberFormat relFormat = new DecimalFormat("0.00000");

  private double minIntensity, mergeWidth;
  private int charge;
  private PolarityType pol;
  private String formula;

  private EChartPanel pnlChart;
  private JFreeChart chart;
  private XYPlot plot;
  private EIsotopePatternChartTheme theme;
  private JPanel newMainPanel;
  private JPanel pnlParameters;
  private JScrollPane pnText;
  private JTable table;
  private JSplitPane pnSplit;
  private JPanel pnlControl;

  private DoubleParameter pMergeWidth;
  private PercentParameter pMinIntensity;
  private StringParameter pFormula;
  private IntegerParameter pCharge;

  private DoubleComponent cmpMergeWidth;
  private PercentComponent cmpMinIntensity;
  private StringComponent cmpFormula;
  private IntegerComponent cmpCharge;

  private JLabel lblMergeWidth, lblMinIntensity, lblFormula, lblCharge; //lblStatus;

  private ExtendedIsotopePatternDataSet dataset;
  private SpectraToolTipGenerator ttGen;
  
  IsotopePatternPreviewTask task;
  Thread thread;
  private boolean newParameters;

  private long lastCalc;
  
  String[][] columns = {{"Exact mass / Da", "Intensity", "Isotope composition"},
      {"m/z", "Intensity", "Isotope composition"}};

  Color aboveMin, belowMin;

  ParameterSet customParameters;

  public IsotopePatternPreviewDialog(Window parent, boolean valueCheckRequired,
      ParameterSet parameters) {
    super(parent, valueCheckRequired, parameters);

    aboveMin = new Color(30, 180, 30);
    belowMin = new Color(200, 30, 30);
    
    lastCalc = 0;

    mzFormat = MZmineCore.getConfiguration().getMZFormat();

    newParameters = false;
//    task = new IsotopePatternPreviewTask(formula, minIntensity, mergeWidth, charge, pol, this);
//    thread = new Thread(task);
    
    formatChart();
    parametersChanged();
  }

  @Override
  protected void addDialogComponents() {
    super.addDialogComponents();

    pFormula = parameterSet.getParameter(IsotopePatternPreviewParameters.formula);
    pMinIntensity = parameterSet.getParameter(IsotopePatternPreviewParameters.minIntensity);
    pMergeWidth = parameterSet.getParameter(IsotopePatternPreviewParameters.mergeWidth);
    pCharge = parameterSet.getParameter(IsotopePatternPreviewParameters.charge);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    cmpMinIntensity =
        (PercentComponent) getComponentForParameter(IsotopePatternPreviewParameters.minIntensity);
    cmpMergeWidth =
        (DoubleComponent) getComponentForParameter(IsotopePatternPreviewParameters.mergeWidth);
    cmpCharge = (IntegerComponent) getComponentForParameter(IsotopePatternPreviewParameters.charge);
    cmpFormula =
        (StringComponent) getComponentForParameter(IsotopePatternPreviewParameters.formula);

    // panels
    newMainPanel = new JPanel(new BorderLayout());
    pnText = new JScrollPane();
    pnlChart = new EChartPanel(chart);
    pnSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlChart, pnText);
    table = new JTable();
    pnlParameters = new JPanel(new FlowLayout());
    pnlControl = new JPanel(new BorderLayout());

    pnText.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    pnText.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    pnText.setMinimumSize(new Dimension(350, 300));
    pnlChart.setMinimumSize(new Dimension(350, 200));
    pnlChart.setPreferredSize( // TODO: can you do this cleaner?
        new Dimension((int) (screenSize.getWidth() / 3), (int) (screenSize.getHeight() / 3)));
    table.setMinimumSize(new Dimension(350, 300));
    table.setDefaultEditor(Object.class, null);

    // controls
    ttGen = new SpectraToolTipGenerator();
    theme = new EIsotopePatternChartTheme();
    theme.initialize();

    // reorganize
    getContentPane().remove(mainPanel);
    organizeParameterPanel();
    pnlControl.add(pnlParameters, BorderLayout.CENTER);
    pnlControl.add(pnlButtons, BorderLayout.SOUTH);
    newMainPanel.add(pnSplit, BorderLayout.CENTER);
    newMainPanel.add(pnlControl, BorderLayout.SOUTH);
    getContentPane().add(newMainPanel);
    pnlButtons.remove(super.btnCancel);

    chart = ChartFactory.createXYBarChart("Isotope pattern preview", "m/z", false, "Abundance",
        new XYSeriesCollection(new XYSeries("")));
    pnlChart.setChart(chart);
    pnText.setViewportView(table);

    updateMinimumSize();
    pack();
  }

  private void organizeParameterPanel() {
    lblMergeWidth = new JLabel(pMergeWidth.getName());
    lblMinIntensity = new JLabel(pMinIntensity.getName());
    lblFormula = new JLabel(pFormula.getName());
    lblCharge = new JLabel(pCharge.getName());
//    lblStatus = new JLabel("Status");
//    lblStatus.setText("Status: waiting");

    mainPanel.remove(cmpCharge);
    mainPanel.remove(cmpMergeWidth);
    mainPanel.remove(cmpMinIntensity);
    mainPanel.remove(cmpFormula);

    lblFormula.setLabelFor(cmpFormula);
    lblMinIntensity.setLabelFor(cmpMinIntensity);
    lblMergeWidth.setLabelFor(cmpMergeWidth);
    lblCharge.setLabelFor(cmpCharge);
    
//    cmpCharge.setPreferredSize(new Dimension(30, cmpCharge.getHeight()));
    cmpCharge.setColumns(2);
    
    

    pnlParameters.add(lblFormula);
    pnlParameters.add(cmpFormula);
    pnlParameters.add(lblMinIntensity);
    pnlParameters.add(cmpMinIntensity);
    pnlParameters.add(lblMergeWidth);
    pnlParameters.add(cmpMergeWidth);
    pnlParameters.add(lblCharge);
    pnlParameters.add(cmpCharge);
//    pnlParameters.add(lblStatus);
  }

  public void actionPerformed(ActionEvent ae) {
    if (ae.getSource() == btnOK) {
      this.closeDialog(ExitCode.CANCEL);
    }
    updateWindow();
  }

  @Override
  protected void parametersChanged() {
    updateWindow();
  }

  // -----------------------------------------------------
  // methods
  // -----------------------------------------------------
  public void updateWindow() {
    if (!updateParameters()) {
      logger
          .warning("updateWindow() failed. Could not update parameters or parameters are invalid."
              + "\nPlease check the parameters.");
      return;
    }
    
    if(FormulaUtils.getFormulaSize(formula) > 5E3 && ((System.nanoTime() - lastCalc) * 1E-6 < 150)) {
      logger.info("Big formula " + formula + " size: " + FormulaUtils.getFormulaSize(formula) + " or last calculation recent: " + (System.nanoTime() -  lastCalc) / 1E6 + " ms");
    }

    if(task != null && thread != null && task.getStatus() == TaskStatus.PROCESSING && FormulaUtils.getFormulaSize(formula) > 1E4) {
      newParameters = true;
//      lblStatus.setText("Status: Queueing " + formula);
      task.setDisplayResult(false);
    }
    else {
//      lblStatus.setText("Status: Calculating " + formula);
      if(task != null)
        task.setDisplayResult(false);
      logger.info("Creating new Thread: " + formula);
      task = new IsotopePatternPreviewTask(formula, minIntensity, mergeWidth, charge, pol, this);
      thread = new Thread(task);
      thread.setPriority(10);
      thread.start();
    }

    lastCalc = System.nanoTime();
  }

  /**
   * this is being called by the calculation task to update the pattern
   * @param pattern
   */
  protected void updateChart(ExtendedIsotopePattern pattern) {
    dataset = new ExtendedIsotopePatternDataSet(pattern, minIntensity, mergeWidth);
    if (pol == PolarityType.NEUTRAL)
      chart = ChartFactory.createXYBarChart("Isotope pattern preview", "Exact mass / Da", false,
          "Abundance", dataset);
    else
      chart = ChartFactory.createXYBarChart("Isotope pattern preview", "m/z", false, "Abundance",
          dataset);

    formatChart();

    pnlChart.setChart(chart);
  }
  
  /**
   * this is being called by the calculation task to update the table
   * @param pattern
   */
  protected void updateTable(ExtendedIsotopePattern pattern) {
    DataPoint[] dp = pattern.getDataPoints();
    Object[][] data = new Object[dp.length][];
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      data[i] = new Object[3];
      data[i][0] = mzFormat.format(dp[i].getMZ());
      data[i][1] = relFormat.format(dp[i].getIntensity());
      data[i][2] = pattern.getIsotopeComposition(i);
    }

    if (pol == PolarityType.NEUTRAL)
      table = new JTable(data, columns[0]); // column 1 = "Exact mass / Da"
    else
      table = new JTable(data, columns[1]); // column 2 = "m/z"

    pnText.setViewportView(table);
    table.setDefaultEditor(Object.class, null); // make editing impossible
  }
  
  private void formatChart() {
    theme.apply(chart);
    plot = chart.getXYPlot();
//    plot.addRangeMarker(new ValueMarker(minIntensity, belowMin, new BasicStroke(1.0f)));
    ((NumberAxis) plot.getDomainAxis()).setNumberFormatOverride(mzFormat);
    ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(intFormat);

    XYItemRenderer r = plot.getRenderer();
    r.setSeriesPaint(0, aboveMin);
    r.setSeriesPaint(1, belowMin);
    r.setDefaultToolTipGenerator(ttGen);
  }

  private boolean updateParameters() {
    updateParameterSetFromComponents();
    if (!checkParameters()) {
      logger.info("updateParameters() failed due to invalid input.");
      return false;
    }

    formula = pFormula.getValue();
    mergeWidth = pMergeWidth.getValue();
    minIntensity = pMinIntensity.getValue();
    charge = pCharge.getValue();

    if (charge > 0) {
      pol = PolarityType.POSITIVE;
    } else if (charge < 0) {
      pol = PolarityType.NEGATIVE;
      charge *= -1;
    } else {
      pol = PolarityType.NEUTRAL;
    }

    return true;
  }

  private boolean checkParameters() {
    if (pFormula.getValue() == null || pFormula.getValue().equals("")
        || !FormulaUtils.checkMolecularFormula(pFormula.getValue())) {
      logger.info("Invalid input or Element == \"\" or invalid elements.");
      return false;
    }
    if (pMinIntensity.getValue() == null || pMinIntensity.getValue() > 1.0d
        || pMinIntensity.getValue() < 0.0d) {
      logger.info("Minimum intensity invalid. " + pMinIntensity.getValue());
      return false;
    }
    if (pMergeWidth.getValue() == null || pMergeWidth.getValue() < 0.0d) {
      logger.info("Merge width invalid. " + pMergeWidth.getValue());
      return false;
    }
    if (pCharge.getValue() == null) {
      logger.info("Charge invalid. " + pCharge.getValue());
      return false;
    }

    logger.info("Parameters valid");
    return true;
  }

  public void startNextThread() {
    if(newParameters) {
//      lblStatus.setText("Satus: Calculating " + formula);
      newParameters = false;
      logger.info("Creating new Thread: " + formula);
      task = new IsotopePatternPreviewTask(formula, minIntensity, mergeWidth, charge, pol, this);
      thread = new Thread(task);
      thread.setPriority(10);
      thread.start();
    }
  }
}
