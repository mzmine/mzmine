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

package io.github.mzmine.gui.chartbasics.graphicsexport;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeFactory2;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import org.jfree.chart.JFreeChart;

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class GraphicsExportDialogFX extends ParameterSetupDialog {

  private static final Logger logger = Logger.getLogger(GraphicsExportDialogFX.class.getName());

  protected EStandardChartTheme theme;
  protected BorderPane pnChartPreview;
  protected JFreeChart chart;
  protected EChartViewer chartPanel;
  protected ExportChartThemeParameters chartParam;
  protected SimpleColorPalette colorPalette;

  private final Button btnRenewPreview;
  private final Button btnApply;
  private final Button btnSave;


  public GraphicsExportDialogFX(boolean valueCheckRequired, ParameterSet parameterSet,
      JFreeChart chart) {
    super(valueCheckRequired, parameterSet);

    chartParam = (ExportChartThemeParameters) parameterSet.getParameter(
        GraphicsExportParameters.chartParameters).getValue();

    colorPalette = parameterSet.getParameter(GraphicsExportParameters.colorPalette).getValue();

    try {
      this.chart = (JFreeChart) chart.clone();
    } catch (Exception e1) {
      logger.log(Level.WARNING, "Clone not implemented for chart of class" + chart.getClass(), e1);
      this.chart = chart;
    }

    theme = ChartThemeFactory2.createExportChartTheme("Export theme");
    chartParam.applyToChartTheme(theme);
    pnChartPreview = new BorderPane();

    pnChartPreview.setMinWidth(400);
    pnChartPreview.setMinHeight(300);
    mainPane.setRight(pnChartPreview);
    chartPanel = new EChartViewer(this.chart);
    pnChartPreview.setCenter(chartPanel);

    // add buttons
    btnRenewPreview = new Button("Renew Preview");
    btnRenewPreview.setOnAction(e -> renewPreview());

    btnApply = new Button("Apply Theme");
    btnApply.setOnAction(e -> applyTheme());

    btnSave = new Button("Save");
    btnSave.setOnAction(e -> saveGraphicsAs());
    getButtonBar().getButtons().addAll(btnRenewPreview, btnApply, btnSave);

    setMinWidth(900.0);
    setMinHeight(400.0);

    centerOnScreen();
  }

  protected void applyTheme() {
    // update param
    updateParameterSetFromComponents();
    chartParam = (ExportChartThemeParameters) parameterSet.getParameter(
        GraphicsExportParameters.chartParameters).getValue();
    colorPalette = parameterSet.getParameter(GraphicsExportParameters.colorPalette).getValue();
    // apply settings
    chartParam.applyToChartTheme(theme);
    colorPalette.applyToChartTheme(theme);

    theme.apply(chartPanel.getChart());
    disableCrosshair();
    // renewPreview();
  }

  protected void disableCrosshair() {
    chart.getXYPlot().setRangeCrosshairVisible(false);
    chart.getXYPlot().setDomainCrosshairVisible(false);
  }

  /**
   * renew chart preview with specified size
   */
  protected void renewPreview() {
    // set dimensions to chartpanel
    try {
      // update param
      updateParameterSetFromComponents();

      GraphicsExportParameters parameterSet = (GraphicsExportParameters) this.parameterSet;
      //
      if (parameterSet.isUseOnlyWidth()) {
        double height = (ChartLogicsFX.calcHeightToWidth(chartPanel,
            parameterSet.getWidthPixel()/* , false */));

        DoubleParameter p = parameterSet.getParameter(GraphicsExportParameters.height)
            .getEmbeddedParameter();
        DoubleComponent c = ((OptionalParameterComponent<DoubleComponent>) parametersAndComponents.get(
            p.getName())).getEmbeddedComponent();
        p.setValueToComponent(c, height);
        p.setValueFromComponent(c);

        chartPanel.setMinSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setPrefSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setMaxSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
      } else {
        chartPanel.setMinSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setPrefSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setMaxSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      logger.log(Level.SEVERE, "Error while renewing preview of graphics export dialog ", ex);
    }
  }

  protected void saveGraphicsAs() {
    updateParameterSetFromComponents();
    //
    GraphicsExportParameters parameterSet = (GraphicsExportParameters) this.parameterSet;

    if (parameterSet.checkParameterValues(null)) {
      File path = parameterSet.getFullpath();
      try {
        logger.info("Writing image to file: " + path.getAbsolutePath());
        ChartExportUtil.writeChartToImageFX(chartPanel, parameterSet);
        logger.info("Success" + path);
      } catch (Exception e) {
        e.printStackTrace();
        logger.log(Level.SEVERE, "File not written (" + path + ")", e);
        // DialogLoggerUtil.showErrorDialog(this, "File not written. ", e); TODO
      }
    }
  }

  public BorderPane getPnChartPreview() {
    return pnChartPreview;
  }
}
