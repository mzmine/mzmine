/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
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
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.jfree.chart.JFreeChart;

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class GraphicsExportDialogFX extends ParameterSetupDialog {

  private static final Logger logger = Logger.getLogger(GraphicsExportDialogFX.class.getName());
  private final Button btnRenewPreview;
  private final Button btnApply;
  private final Button btnSave;
  protected EStandardChartTheme theme;
  protected BorderPane pnChartPreview;
  protected JFreeChart chart;
  protected EChartViewer chartPanel;
  protected ExportChartThemeParameters chartParam;
  protected SimpleColorPalette colorPalette;
  // do not show dialogs when running all export in batch only show when single export
  private boolean openResultDialog = false;


  public GraphicsExportDialogFX(boolean valueCheckRequired, ParameterSet parameterSet,
      JFreeChart chart, final boolean openResultDialog) {
    super(valueCheckRequired, parameterSet);

    chartParam = (ExportChartThemeParameters) parameterSet.getParameter(
        GraphicsExportParameters.chartParameters).getValue();

    colorPalette = parameterSet.getParameter(GraphicsExportParameters.colorPalette).getValue();
    this.openResultDialog = openResultDialog;

    try {
      this.chart = (JFreeChart) chart.clone();
    } catch (Exception e1) {
      logger.log(Level.WARNING,
          "Clone not implemented (will use original) for chart of class" + chart.getClass(), e1);
      this.chart = chart;
    }

    theme = ChartThemeFactory2.createExportChartTheme("Export theme");
    chartParam.applyToChartTheme(theme);

    // do not set min width on this otherwise scrollpane freaks out and does not show bars
    pnChartPreview = new BorderPane();

    var centerNode = mainPane.getCenter();
    if (centerNode instanceof Region r) {
      r.setMinWidth(400);
    }

    var scrollChart = new StackPane(
        FxLayout.newScrollPane(pnChartPreview, ScrollBarPolicy.AS_NEEDED,
            ScrollBarPolicy.AS_NEEDED));
//    mainPane.setRight(scrollChart);
    chartPanel = new EChartViewer(this.chart, false, false, true, true, false);
    pnChartPreview.setCenter(chartPanel);
    mainPane.setCenter(FxLayout.newHBox(centerNode, scrollChart));
    HBox.setHgrow(scrollChart, Priority.ALWAYS);

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
    renewPreview();
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

        showResultDialog("Success", "Exported image " + path.getAbsolutePath());
      } catch (Exception e) {
        logger.log(Level.SEVERE, "File not written (" + path + ")", e);
        showResultDialog("Failed", "Failed to export image. " + e.getMessage());
      }
    }
  }

  private void showResultDialog(final String title, final String message) {
    if (openResultDialog) {
      DialogLoggerUtil.showMessageDialogForTime(title, message);
    } else {
      logger.fine(title + ": " + message);
    }
  }


  public void export() {
    applyTheme();
    renewPreview();
    saveGraphicsAs();
  }

  public BorderPane getPnChartPreview() {
    return pnChartPreview;
  }
}
