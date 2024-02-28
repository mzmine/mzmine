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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.gui.chartbasics.chartthemes.EIsotopePatternChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.autocarbon.AutoCarbonParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ExtendedIsotopePatternDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraToolTipGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.FormulaUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.util.converter.NumberStringConverter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Extension of ParameterSetupDialog to allow a preview window
 *
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class IsotopePeakScannerSetupDialog extends ParameterSetupDialogWithPreview {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intFormat = new DecimalFormat("0.00 %");

  private double minIntensity, mergeWidth;
  private int charge, minSize, minC, maxC;
  private String element;
  private boolean autoCarbon;

  private EChartViewer pnlChart;
  private JFreeChart chart;
  private XYPlot plot;
  private EIsotopePatternChartTheme theme;

  // components created by this class
  private final BorderPane pnlPreview;
  private final FlowPane pnlPreviewButtons;
  private final Button btnPrevPattern, btnNextPattern;
  private final TextField txtCurrentPatternIndex;

  // private NumberFormatter form;

  // components created by parameters
  private OptionalModuleComponent cmpAutoCarbon;
  private CheckBox cmpAutoCarbonCbx, cmpPreview;

  // relevant parameters
  private IntegerParameter pMinC, pMaxC, pMinSize, pCharge;
  private StringParameter pElement;
  private DoubleParameter pMinIntensity, pMergeWidth;
  private OptionalModuleParameter pAutoCarbon;

  private ExtendedIsotopePatternDataSet dataset;
  private SpectraToolTipGenerator ttGen;

  Color aboveMin, belowMin;

  ParameterSet autoCarbonParameters;

  @Override
  protected void showPreview(boolean show) {
    super.showPreview(show);
    if(show) {
      updatePreview();
    }
  }

  public IsotopePeakScannerSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);

    aboveMin = MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT();
    belowMin = MZmineCore.getConfiguration().getDefaultColorPalette().getNegativeColorAWT();
    theme = new EIsotopePatternChartTheme();
    theme.initialize();
    ttGen = new SpectraToolTipGenerator();

    chart = ChartFactory.createXYBarChart("Isotope pattern preview", "m/z", false, "Abundance",
        new XYSeriesCollection(new XYSeries("")));
    pnlChart = new EChartViewer(chart);
    pnlChart.setMinSize(400, 300);
    pnlPreview = new BorderPane();
    pnlPreview.setCenter(pnlChart);

    // get components
    cmpAutoCarbon = (OptionalModuleComponent) this
        .getComponentForParameter(IsotopePeakScannerParameters.autoCarbonOpt);
    cmpAutoCarbonCbx = cmpAutoCarbon.getCheckbox();

    previewWrapperPane.setCenter(pnlPreview);

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
    /*
     * form = new NumberFormatter(NumberFormat.getInstance()); form.setValueClass(Integer.class);
     * form.setFormat(new DecimalFormat("0")); form.setAllowsInvalid(true); form.setMinimum(minC);
     * form.setMaximum(maxC);
     */
    btnPrevPattern = new Button("Previous");

    // btnPrevPattern.setMinimumSize(btnPrevPattern.getPreferredSize());
    btnPrevPattern.disableProperty().bind(cmpAutoCarbonCbx.selectedProperty().not());

    txtCurrentPatternIndex = new TextField();
    txtCurrentPatternIndex.setTextFormatter(
        new TextFormatter<Number>(new NumberStringConverter(NumberFormat.getIntegerInstance())));
    txtCurrentPatternIndex.setOnAction(e -> updatePreview());
    txtCurrentPatternIndex.setText(String.valueOf((minC + maxC) / 2));
    // txtCurrentPatternIndex.setPreferredSize(new Dimension(50, 25));
    // txtCurrentPatternIndex.setEditable(true);
    txtCurrentPatternIndex.disableProperty().bind(cmpAutoCarbonCbx.selectedProperty().not());

    btnNextPattern = new Button("Next");

    // btnNextPattern.setPreferredSize(btnNextPattern.getMinimumSize());
    btnNextPattern.disableProperty().bind(cmpAutoCarbonCbx.selectedProperty().not());

    btnPrevPattern.setOnAction(e -> {
      logger.info(e.getSource().toString());
      int current = Integer.parseInt(txtCurrentPatternIndex.getText());
      if (current > (minC)) {
        current--;
      }
      txtCurrentPatternIndex.setText(String.valueOf(current));
      if (cmpPreview.isSelected()) {
        updatePreview();
      }
    });

    btnNextPattern.setOnAction(e -> {
      logger.info(e.getSource().toString());
      int current = Integer.parseInt(txtCurrentPatternIndex.getText());
      if (current < (maxC)) {
        current++;
      }
      txtCurrentPatternIndex.setText(String.valueOf(current));
      if (cmpPreview.isSelected()) {
        updatePreview();
      }
    });

    pnlPreviewButtons = new FlowPane();
    pnlPreviewButtons.getChildren().addAll(btnPrevPattern, txtCurrentPatternIndex, btnNextPattern);

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

    SimpleIsotopePattern pattern = calculateIsotopePattern();
    if (pattern == null) {
      logger.warning("Could not calculate isotope pattern. Please check the parameters.");
      return;
    }

    updateChart(pattern);
  }

  private void updateChart(SimpleIsotopePattern pattern) {
    dataset = new ExtendedIsotopePatternDataSet(pattern, minIntensity, mergeWidth);
    chart.getXYPlot().setDataset(dataset);
    formatChart();
//    pnlChart.setChart(chart);
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

    if (autoCarbon) {
      updateAutoCarbonParameters();
    }
    return true;
  }

  private void updateAutoCarbonParameters() {
    minC = pMinC.getValue();
    maxC = pMaxC.getValue();
    minSize = pMinSize.getValue();

    // form.setMaximum(maxC);
    // form.setMinimum(minC);

    if (txtCurrentPatternIndex.getText().equals("")) // if the user did
    // stuff we dont allow
    {
      txtCurrentPatternIndex.setText(String.valueOf((minC + maxC) / 2));
    }
    if (Integer.parseInt(txtCurrentPatternIndex.getText()) > maxC) {
      txtCurrentPatternIndex.setText(String.valueOf(maxC));
    }
    if (Integer.parseInt(txtCurrentPatternIndex.getText()) < minC) {
      txtCurrentPatternIndex.setText(String.valueOf(minC));
    }
  }

  private boolean checkParameters() {
    if (/* pElement.getValue().equals("") */pElement.getValue() == null || (
        pElement.getValue().equals("") && !autoCarbon) || pElement.getValue().contains(" ")
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

  private SimpleIsotopePattern calculateIsotopePattern() {
    if (!checkParameters()) {
      return null;
    }

    String strPattern = "";
    int currentCarbonPattern = Integer.parseInt(txtCurrentPatternIndex.getText());

    if (autoCarbon) {
      strPattern = "C" + String.valueOf(currentCarbonPattern) + element;
    } else {
      strPattern = element;
    }

    if (strPattern.equals("")) {
      return null;
    }
    logger.info("Calculating isotope pattern: " + strPattern);

    SimpleIsotopePattern pattern;
    PolarityType pol = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
    charge = (charge > 0) ? charge : charge * -1;
    try {
      // *0.2 so the user can see the peaks below the threshold
      pattern = (SimpleIsotopePattern) IsotopePatternCalculator
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
