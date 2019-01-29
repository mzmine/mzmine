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

package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;
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
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.autocarbon.AutoCarbonParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.ExtendedIsotopePatternDataSet;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraToolTipGenerator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialogWithEmptyPreview;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.util.FormulaUtils;

/**
 *
 * Extension of ParameterSetupDialog to allow a preview window
 *
 *
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class IsotopePeakScannerSetupDialog extends ParameterSetupDialogWithEmptyPreview {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intFormat = new DecimalFormat("0.00 %");

  private double minIntensity, mergeWidth;
  private int charge, minSize, minC, maxC;
  private String element;
  private boolean autoCarbon;

  private EChartPanel pnlChart;
  private JFreeChart chart;
  private XYPlot plot;
  private EIsotopePatternChartTheme theme;


  // components created by this class
  private JButton btnPrevPattern, btnNextPattern;
  private JFormattedTextField txtCurrentPatternIndex;

  private NumberFormatter form;

  // components created by parameters
  private OptionalModuleComponent cmpAutoCarbon;
  private JCheckBox cmpAutoCarbonCbx, cmpPreview;


  // relevant parameters
  private IntegerParameter pMinC, pMaxC, pMinSize, pCharge;
  private StringParameter pElement;
  private DoubleParameter pMinIntensity, pMergeWidth;
  private OptionalModuleParameter pAutoCarbon;

  private ExtendedIsotopePatternDataSet dataset;
  private SpectraToolTipGenerator ttGen;

  Color aboveMin, belowMin;

  ParameterSet autoCarbonParameters;

  public IsotopePeakScannerSetupDialog(Window parent, boolean valueCheckRequired,
      ParameterSet parameters) {
    super(parent, valueCheckRequired, parameters);

    aboveMin = new Color(30, 180, 30);
    belowMin = new Color(200, 30, 30);
    theme = new EIsotopePatternChartTheme();
    theme.initialize();
    ttGen = new SpectraToolTipGenerator();
  }

  @Override
  protected void addDialogComponents() {
    super.addDialogComponents();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    pnlChart = new EChartPanel(chart);
    pnlChart.setPreferredSize(
        new Dimension((int) (screenSize.getWidth() / 3), (int) (screenSize.getHeight() / 3)));
    pnlPreview.add(pnlChart, BorderLayout.CENTER);


    // get components
    cmpAutoCarbon = (OptionalModuleComponent) this
        .getComponentForParameter(IsotopePeakScannerParameters.autoCarbonOpt);
    cmpAutoCarbonCbx = (JCheckBox) cmpAutoCarbon.getComponent(0);
    cmpPreview =
        (JCheckBox) this.getComponentForParameter(IsotopePeakScannerParameters.showPreview);
    cmpPreview.setSelected(false); // i want to have the checkbox below the pattern settings
    // but it should be disabled by default. Thats why it's hardcoded here.

    // get parameters
    pElement = parameterSet.getParameter(IsotopePeakScannerParameters.element);
    pMinIntensity = parameterSet.getParameter(IsotopePeakScannerParameters.minPatternIntensity);
    pCharge = parameterSet.getParameter(IsotopePeakScannerParameters.charge);
    pMergeWidth = parameterSet.getParameter(IsotopePeakScannerParameters.mergeWidth);
    pAutoCarbon = parameterSet.getParameter(IsotopePeakScannerParameters.autoCarbonOpt);
    autoCarbonParameters = pAutoCarbon.getEmbeddedParameters();
    pMinC = autoCarbonParameters.getParameter(AutoCarbonParameters.minCarbon);
    pMaxC = autoCarbonParameters.getParameter(AutoCarbonParameters.maxCarbon);
    pMinSize = autoCarbonParameters.getParameter(AutoCarbonParameters.minPatternSize);

    // set up gui
    form = new NumberFormatter(NumberFormat.getInstance());
    form.setValueClass(Integer.class);
    form.setFormat(new DecimalFormat("0"));
    form.setAllowsInvalid(true);
    form.setMinimum(minC);
    form.setMaximum(maxC);

    btnPrevPattern = new JButton("Previous");
    btnPrevPattern.addActionListener(this);
    btnPrevPattern.setMinimumSize(btnPrevPattern.getPreferredSize());
    btnPrevPattern.setEnabled(cmpAutoCarbonCbx.isSelected());

    txtCurrentPatternIndex = new JFormattedTextField(form);
    txtCurrentPatternIndex.addActionListener(this);
    txtCurrentPatternIndex.setText(String.valueOf((minC + maxC) / 2));
    txtCurrentPatternIndex.setPreferredSize(new Dimension(50, 25));
    txtCurrentPatternIndex.setEditable(true);
    txtCurrentPatternIndex.setEnabled(cmpAutoCarbonCbx.isSelected());

    btnNextPattern = new JButton("Next");
    btnNextPattern.addActionListener(this);
    btnNextPattern.setPreferredSize(btnNextPattern.getMinimumSize());
    btnNextPattern.setEnabled(cmpAutoCarbonCbx.isSelected());

    chart = ChartFactory.createXYBarChart("Isotope pattern preview", "m/z", false, "Abundance",
        new XYSeriesCollection(new XYSeries("")));
    chart.getPlot().setBackgroundPaint(Color.WHITE);
    chart.getXYPlot().setDomainGridlinePaint(Color.GRAY);
    chart.getXYPlot().setRangeGridlinePaint(Color.GRAY);

    pnlPreviewButtons.add(btnPrevPattern);
    pnlPreviewButtons.add(txtCurrentPatternIndex);
    pnlPreviewButtons.add(btnNextPattern);

    pack();
  }


  @Override
  public void actionPerformed(ActionEvent ae) {
    super.actionPerformed(ae);
    updateParameterSetFromComponents();

    if (ae.getSource() == btnNextPattern) {
      logger.info(ae.getSource().toString());
      int current = Integer.parseInt(txtCurrentPatternIndex.getText());
      if (current < (maxC)) {
        current++;
      }
      txtCurrentPatternIndex.setText(String.valueOf(current));
      if (cmpPreview.isSelected())
        updatePreview();
    }

    else if (ae.getSource() == btnPrevPattern) {
      logger.info(ae.getSource().toString());
      int current = Integer.parseInt(txtCurrentPatternIndex.getText());
      if (current > (minC)) {
        current--;
      }
      txtCurrentPatternIndex.setText(String.valueOf(current));
      if (cmpPreview.isSelected())
        updatePreview();
    }

    else if (ae.getSource() == cmpPreview) {
      logger.info(ae.getSource().toString());

      if (cmpPreview.isSelected()) {
        newMainPanel.add(pnlPreview, BorderLayout.CENTER);
        pnlPreview.setVisible(true);
        updatePreview();
        updateMinimumSize();
        pack();
      } else {
        newMainPanel.remove(pnlPreview);
        pnlPreview.setVisible(false);
        updateMinimumSize();
        pack();
      }
    }

    else if (ae.getSource() == txtCurrentPatternIndex) {
      // logger.info(ae.getSource().toString());
      updatePreview();
    }

    else if (ae.getSource() == cmpAutoCarbonCbx) {
      btnNextPattern.setEnabled(cmpAutoCarbonCbx.isSelected());
      btnPrevPattern.setEnabled(cmpAutoCarbonCbx.isSelected());
      txtCurrentPatternIndex.setEnabled(cmpAutoCarbonCbx.isSelected());
    }
  }

  @Override
  protected void parametersChanged() {
    updatePreview();
  }

  // -----------------------------------------------------
  // methods
  // -----------------------------------------------------
  private void updatePreview() {
    if (!updateParameters()) {
      logger.warning(
          "updatePreview() failed. Could not update parameters or parameters are invalid. Please check the parameters.");
      return;
    }

    ExtendedIsotopePattern pattern = calculateIsotopePattern();
    if (pattern == null) {
      logger.warning("Could not calculate isotope pattern. Please check the parameters.");
      return;
    }

    updateChart(pattern);
  }

  private void updateChart(ExtendedIsotopePattern pattern) {
    dataset = new ExtendedIsotopePatternDataSet(pattern, minIntensity, mergeWidth);
    chart = ChartFactory.createXYBarChart("Isotope pattern preview", "m/z", false, "Abundance",
        dataset);
    formatChart();
    pnlChart.setChart(chart);
  }

  private boolean updateParameters() {
    updateParameterSetFromComponents();
    autoCarbon = pAutoCarbon.getValue();

    if (!checkParameters()) {
      logger.info("updateParameters() failed due to invalid input.");
      return false;
    }

    element = pElement.getValue();
    mergeWidth = pMergeWidth.getValue();
    minIntensity = pMinIntensity.getValue();
    charge = pCharge.getValue();

    if (autoCarbon)
      updateAutoCarbonParameters();
    return true;
  }

  private void updateAutoCarbonParameters() {
    minC = pMinC.getValue();
    maxC = pMaxC.getValue();
    minSize = pMinSize.getValue();

    form.setMaximum(maxC);
    form.setMinimum(minC);

    if (txtCurrentPatternIndex.getText().equals("")) // if the user did stuff we dont allow
      txtCurrentPatternIndex.setText(String.valueOf((minC + maxC) / 2));
    if (Integer.parseInt(txtCurrentPatternIndex.getText()) > maxC)
      txtCurrentPatternIndex.setText(String.valueOf(maxC));
    if (Integer.parseInt(txtCurrentPatternIndex.getText()) < minC)
      txtCurrentPatternIndex.setText(String.valueOf(minC));
  }

  private boolean checkParameters() {
    if (/* pElement.getValue().equals("") */pElement.getValue() == null
        || (pElement.getValue().equals("") && !autoCarbon) || pElement.getValue().contains(" ")
        || !FormulaUtils.checkMolecularFormula(pElement.getValue())) {
      logger.info("Invalid input or Element == \"\" and no autoCarbon or invalid formula.");
      return false;
    }
    if (pMinIntensity.getValue() == null || pMinIntensity.getValue() > 1.0d
        || pMinIntensity.getValue() < 0.0d) {
      logger.info("Minimum intensity invalid. " + pMinIntensity.getValue());
      return false;
    }
    if (pCharge.getValue() == null || pCharge.getValue() == 0) {
      logger.info("Charge invalid. " + pCharge.getValue());
      return false;
    }
    if (pMergeWidth.getValue() == null || pMergeWidth.getValue() < 0.0d) {
      logger.info("Merge width invalid. " + pMergeWidth.getValue());
      return false;
    }

    logger.info("Parameters valid");
    return true;
  }

  private ExtendedIsotopePattern calculateIsotopePattern() {
    if (!checkParameters())
      return null;

    String strPattern = "";
    int currentCarbonPattern = Integer.parseInt(txtCurrentPatternIndex.getText());

    if (autoCarbon)
      strPattern = "C" + String.valueOf(currentCarbonPattern) + element;
    else
      strPattern = element;

    if (strPattern.equals(""))
      return null;
    logger.info("Calculating isotope pattern: " + strPattern);

    ExtendedIsotopePattern pattern;
    PolarityType pol = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
    charge = (charge > 0) ? charge : charge * -1;
    try {
      // *0.2 so the user can see the peaks below the threshold
      pattern = (ExtendedIsotopePattern) IsotopePatternCalculator
          .calculateIsotopePattern(strPattern, minIntensity * 0.1, mergeWidth, charge, pol, true);
    } catch (Exception e) {
      logger.warning("The entered Sum formula is invalid.");
      return null;
    }
    return pattern;
  }

  private void formatChart() {
    theme.apply(chart);
    plot = chart.getXYPlot();
    plot.addRangeMarker(new ValueMarker(minIntensity, belowMin, new BasicStroke(1.0f)));
    ((NumberAxis) plot.getDomainAxis()).setNumberFormatOverride(mzFormat);
    ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(intFormat);

    XYItemRenderer r = plot.getRenderer();
    r.setSeriesPaint(0, aboveMin);
    r.setSeriesPaint(1, belowMin);
    r.setDefaultToolTipGenerator(ttGen);
  }
}
