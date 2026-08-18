/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.FxFontUtil;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class GraphicsExportDialogFX extends ParameterSetupDialog {

  private static final Logger logger = Logger.getLogger(GraphicsExportDialogFX.class.getName());
  private final Button btnRenewPreview;
  private final Button btnApply;
  private final Button btnSave;
  private final PauseTransition livePreviewDelay = new PauseTransition(Duration.millis(200));
  protected EStandardChartTheme theme;
  protected BorderPane pnChartPreview;
  protected JFreeChart chart;
  protected EChartViewer chartPanel;
  private final StackPane previewHost;
  protected ExportChartThemeParameters chartParam;
  protected SimpleColorPalette colorPalette;
  // do not show dialogs when running all export in batch only show when single export
  private boolean openResultDialog = false;
  private boolean applyingTheme;


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

    previewHost = new StackPane(pnChartPreview);
    chartPanel = new EChartViewer(this.chart, false, false, true, true, false);
    pnChartPreview.setCenter(chartPanel);
    mainPane.setCenter(FxLayout.newHBox(centerNode, previewHost));
    HBox.setHgrow(previewHost, Priority.ALWAYS);
    previewHost.widthProperty().addListener((obs, oldValue, newValue) -> resizePreviewToFit());
    previewHost.heightProperty().addListener((obs, oldValue, newValue) -> resizePreviewToFit());

    // add buttons
    btnRenewPreview = new Button("Renew Preview");
    btnRenewPreview.setOnAction(e -> renewPreview());

    btnApply = new Button("Apply Theme");
    btnApply.setOnAction(e -> applyTheme());

    btnSave = new Button("Save");
    btnSave.setOnAction(e -> saveGraphicsAs());
    getButtonBar().getButtons().addAll(btnRenewPreview, btnApply, btnSave);

    livePreviewDelay.setOnFinished(event -> applyTheme());
    mainPane.addEventFilter(ActionEvent.ACTION, event -> scheduleLivePreview());
    mainPane.addEventFilter(KeyEvent.KEY_RELEASED, event -> scheduleLivePreview());

    setMinWidth(900.0);
    setMinHeight(400.0);

    centerOnScreen();
    renewPreview();
    Platform.runLater(this::resizePreviewToFit);
  }

  protected void applyTheme() {
    if (applyingTheme) {
      return;
    }
    applyingTheme = true;
    try {
      // update param
      updateParameterSetFromComponents();
      chartParam = (ExportChartThemeParameters) parameterSet.getParameter(
          GraphicsExportParameters.chartParameters).getValue();
      colorPalette = parameterSet.getParameter(GraphicsExportParameters.colorPalette).getValue();
      // apply settings
      chartParam.applyToChartTheme(theme);
      colorPalette.applyToChartTheme(theme);

      theme.apply(chartPanel.getChart());
      applyAnnotationTheme();
      disableCrosshair();
      renewPreview();
    } catch (RuntimeException error) {
      // A temporarily incomplete number while the user is typing should not break the dialog.
      logger.log(Level.FINE, "Cannot update live graphics preview yet", error);
    } finally {
      applyingTheme = false;
    }
  }

  private void scheduleLivePreview() {
    if (!applyingTheme) {
      livePreviewDelay.playFromStart();
    }
  }

  /** Applies the export item-label style to chart annotations such as NIST compound names. */
  private void applyAnnotationTheme() {
    final var itemLabelStyle = chartParam.getValue(ExportChartThemeParameters.itemLabelFont);
    final var annotationFont = FxFontUtil.fxFontToAWT(itemLabelStyle.getFont());
    final var annotationPaint = FxColorUtil.fxColorToAWT(itemLabelStyle.getColor());
    chartPanel.getChart().getXYPlot().getAnnotations().forEach(annotation -> {
      if (annotation instanceof XYTextAnnotation textAnnotation) {
        textAnnotation.setFont(annotationFont);
        textAnnotation.setPaint(annotationPaint);
      }
    });
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

      }
      resizePreviewToFit();
      chartPanel.getChart().fireChartChanged();
    } catch (Exception ex) {
      ex.printStackTrace();
      logger.log(Level.SEVERE, "Error while renewing preview of graphics export dialog ", ex);
    }
  }

  /** Fits the preview to the available dialog space while preserving the requested export ratio. */
  private void resizePreviewToFit() {
    if (chartPanel == null || previewHost == null) {
      return;
    }
    final GraphicsExportParameters exportParameters =
        (GraphicsExportParameters) parameterSet;
    final double outputWidth = exportParameters.getWidthPixel();
    final double outputHeight = exportParameters.getHeightPixel();
    if (outputWidth <= 0d || outputHeight <= 0d) {
      return;
    }

    final double availableWidth = Math.max(1d, previewHost.getWidth() - 20d);
    final double availableHeight = Math.max(1d, previewHost.getHeight() - 20d);
    final double scale = Math.min(availableWidth / outputWidth, availableHeight / outputHeight);
    final double previewWidth = Math.max(1d, outputWidth * scale);
    final double previewHeight = Math.max(1d, outputHeight * scale);
    chartPanel.setMinSize(0d, 0d);
    chartPanel.setPrefSize(previewWidth, previewHeight);
    chartPanel.setMaxSize(previewWidth, previewHeight);
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
      } finally {
        Platform.runLater(this::resizePreviewToFit);
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
